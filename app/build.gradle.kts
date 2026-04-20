plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sagar.rabit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.rabit"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            isMinifyEnabled = true
            isShrinkResources = true
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
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.biometric)
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Widgets (Glance)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation("com.jcraft:jsch:0.1.55")
    implementation("com.tananaev:adblib:1.3")
    implementation("commons-codec:commons-codec:1.16.0")
    
    implementation(libs.mediapipe.genai)
    implementation(libs.jmdns)
    implementation(libs.zxing.core)
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // ADB TLS PSK Encryption
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.77")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.77")
    
    // QR Code Scanning
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // WebRTC for P2P Hosting
    implementation("io.github.webrtc-sdk:android:125.6422.07")

    // Firebase for Production Signaling
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation("com.google.firebase:firebase-database-ktx")
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
