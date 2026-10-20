package app.turp.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Käufe über Google Play. Die App kauft nur ein – gutgeschrieben wird ausschließlich
 * vom Server, nachdem er den Kauf bei Google geprüft hat (functions: verifyPurchase).
 */
object Billing {

    /** Ein Gem-Paket. [listPrice] wird gezeigt, solange Google Play noch keinen Preis geliefert hat. */
    data class Pack(val id: String, val gems: Int, val listPrice: String, val badge: String? = null)

    // Muss zu functions/billing.js und den Produkten in der Play Console passen
    val PACKS = listOf(
        Pack("gems_50", 50, "0,99 €"),
        Pack("gems_150", 150, "2,49 €"),
        Pack("gems_350", 350, "4,99 €", badge = "Beliebt"),
        Pack("gems_800", 800, "9,99 €"),
        Pack("gems_2000", 2000, "19,99 €", badge = "Bester Wert"),
    )
    const val PLUS = "kumi_plus"
    const val PLUS_LIST_PRICE = "3,99 €"

    sealed interface Event {
        data class Credited(val gems: Long) : Event
        data object PlusActive : Event
        data object Pending : Event
        data class Failed(val message: String) : Event
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var client: BillingClient? = null
    private val inFlight: MutableSet<String> = Collections.synchronizedSet(HashSet())

    private val _ready = MutableStateFlow<Boolean?>(null)

    /** null = verbindet noch, true = Käufe möglich, false = kein Google Play (z. B. App nicht aus dem Store) */
    val ready: StateFlow<Boolean?> = _ready

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events

    /** Einmal nach dem Login starten: verbindet mit Google Play und holt offene Käufe nach */
    fun start(context: Context) {
        if (client != null) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _ready.value = true
                    loadProducts()
                    restore()
                } else {
                    Log.w("kumi", "Google Play Billing nicht verfügbar: ${result.responseCode} ${result.debugMessage}")
                    _ready.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                // Die Bibliothek verbindet sich selbst neu
            }
        })
    }

    private fun loadProducts() {
        val c = client ?: return
        val oneTime = PACKS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val subs = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PLUS)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        // Pro Abfrage nur eine Produktart
        listOf(oneTime, subs).forEach { list ->
            c.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(list).build()) { result, details ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val found = details.productDetailsList.associateBy { it.productId }
                    _products.update { it + found }
                } else {
                    Log.w("kumi", "Produkte nicht geladen: ${result.responseCode} ${result.debugMessage}")
                }
            }
        }
    }

    /** Offene (noch nicht gutgeschriebene) Käufe und laufende Abos dem Server melden */
    fun restore() {
        val c = client ?: return
        if (_ready.value != true) return
        listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS).forEach { type ->
            c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.forEach { handle(it, quiet = true) }
                }
            }
        }
    }

    /** Preis, wie Google ihn für das Land des Nutzers liefert (z. B. "0,99 €") */
    fun price(productId: String): String? {
        val d = _products.value[productId] ?: return null
        return if (d.productType == BillingClient.ProductType.SUBS) {
            baseOffer(d)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        } else {
            d.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
        }
    }

    fun canBuy(productId: String): Boolean = _ready.value == true && _products.value.containsKey(productId)

    /**
     * Startet den Kauf. Vorher prüft der Server, ob er erlaubt ist (z. B. Monatslimit unter 18).
     * Fehler werden als Exception mit verständlicher Meldung geworfen.
     */
    suspend fun buy(context: Context, productId: String) {
        val activity = context.findActivity() ?: throw IllegalStateException("Kauf konnte nicht gestartet werden.")
        val c = client ?: throw IllegalStateException("Google Play ist nicht verbunden.")
        val details = _products.value[productId]
            ?: throw IllegalStateException("Käufe gehen nur mit kumi aus dem Play Store.")

        val check = callFunction("purchaseCheck", mapOf("productId" to productId))
        val tag = check["accountTag"] as? String ?: ""

        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        offerToken(details)?.let { item.setOfferToken(it) }
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(item.build()))
            .apply { if (tag.isNotBlank()) setObfuscatedAccountId(tag) }
            .build()
        val result = c.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK &&
            result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED
        ) {
            throw IllegalStateException("Kauf konnte nicht gestartet werden (${result.responseCode}).")
        }
    }

    private fun baseOffer(d: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        val offers = d.subscriptionOfferDetails ?: return null
        return offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
    }

    private fun offerToken(d: ProductDetails): String? =
        if (d.productType == BillingClient.ProductType.SUBS) {
            baseOffer(d)?.offerToken
        } else {
            d.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
        }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handle(it, quiet = false) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            else -> _events.tryEmit(Event.Failed("Der Kauf hat nicht geklappt (${result.responseCode}). Es wurde nichts abgebucht."))
        }
    }

    private fun handle(purchase: Purchase, quiet: Boolean) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            if (!quiet) _events.tryEmit(Event.Pending)
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val token = purchase.purchaseToken
        val productId = purchase.products.firstOrNull() ?: return
        if (!inFlight.add(token)) return
        scope.launch {
            try {
                val r = callFunction(
                    "verifyPurchase",
                    mapOf("productId" to productId, "purchaseToken" to token),
                    timeoutSeconds = 60
                )
                when (r["status"]) {
                    "ok" -> if (productId == PLUS) {
                        if (!quiet) _events.emit(Event.PlusActive)
                    } else {
                        val gems = (r["gems"] as? Number)?.toLong() ?: 0L
                        if (gems > 0) _events.emit(Event.Credited(gems))
                    }
                    "pending" -> if (!quiet) _events.emit(Event.Pending)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("kumi", "Kauf konnte nicht geprüft werden", e)
                if (!quiet) _events.emit(Event.Failed(e.toUserMessage()))
            } finally {
                inFlight.remove(token)
            }
        }
    }
}

/** Die Activity hinter einem Compose-Context (für den Kauf-Dialog von Google) */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
