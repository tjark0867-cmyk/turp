plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Die App lässt sich auch ohne Firebase-Konfiguration bauen. Sie zeigt dann
// einen Hinweis-Bildschirm. Sobald app/google-services.json existiert,
// wird Firebase automatisch eingebunden.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Beim Bauen auf GitHub: Versionsnummer = Build-Nummer, Update-Adresse = dieses Repository
val ciVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val updateRepo = (project.findProperty("updateRepo") as String?).orEmpty()
val updateUrl = if (updateRepo.isNotBlank()) "https://raw.githubusercontent.com/$updateRepo/apk/" else ""

// Fester Signatur-Schlüssel (liegt geschützt im Secret Manager des Firebase-Projekts).
// Nur mit immer demselben Schlüssel lassen sich Updates über die alte App installieren.
val signingStore = (project.findProperty("signingStore") as String?)
    ?.takeIf { it.isNotBlank() }?.let { file(it) }?.takeIf { it.isFile }
val signingPassword = (project.findProperty("signingPassword") as String?).orEmpty()

android {
    namespace = "app.turp"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.turp"
        minSdk = 26
        targetSdk = 35
        versionCode = ciVersionCode
        versionName = "1.0.$ciVersionCode"
        buildConfigField("String", "UPDATE_URL", "\"$updateUrl\"")
        // Muss zur Region der Cloud Functions passen (functions/index.js)
        buildConfigField("String", "FUNCTIONS_REGION", "\"europe-west3\"")
    }

    signingConfigs {
        if (signingStore != null) {
            create("turp") {
                storeFile = signingStore
                storePassword = signingPassword
                keyAlias = "turp"
                keyPassword = signingPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("turp")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            )
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
