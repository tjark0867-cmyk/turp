package app.turp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.turp.data.DEFAULT_COLOR
import app.turp.data.UserProfile
import app.turp.ui.theme.TextHigh

/**
 * Kopfzeile der Tabs: links eigenes Profilbild (öffnet das Profil) und optional
 * ein Symbol, in der Mitte der Titel, rechts die Gems (öffnen den Shop).
 */
@Composable
fun TabHeader(
    title: String,
    me: UserProfile?,
    gems: Long,
    showDot: Boolean,
    onOpenProfile: () -> Unit,
    onOpenShop: () -> Unit,
    extra: @Composable RowScope.() -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 8.dp)
    ) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                url = me?.photos?.firstOrNull().orEmpty(),
                name = me?.name.orEmpty(),
                color = me?.color ?: DEFAULT_COLOR,
                size = 38.dp,
                ring = Color.White,
                online = true,
                modifier = Modifier.clickable(onClick = onOpenProfile)
            )
            extra()
        }
        Text(
            title,
            color = TextHigh,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.Center)
        )
        GemPill(
            gems = gems,
            showDot = showDot,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onOpenShop)
        )
    }
}

/** Fette Abschnitts-Überschrift wie "Neue Freunde" */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, color = TextHigh, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = modifier)
}
