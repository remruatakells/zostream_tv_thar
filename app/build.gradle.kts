plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
}

kotlin {
    jvmToolchain(17)
}

composeCompiler {
    enableStrongSkippingMode = true
}

android {

    namespace = "com.buannel.studio.pvt.ltd.zostream"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.buannel.studio.pvt.ltd.zostream"
        minSdk = 21
        targetSdk = 36
        versionCode = 202937
        versionName = "2029.3.7"
        buildConfigField("boolean", "AMAZON_IAP_ENABLED", "true")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.amazon.appstore.sdk)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(libs.androidx.core.ktx)
    implementation(composeBom)
    // To use Compose Foundation 1.7.0
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling)
    // Compose integration with activities
    implementation(libs.androidx.activity.compose)

    // TV Compose
    implementation(libs.androidx.tv.material)

    // ViewModel utilities for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Lifecycle utilities for Compose
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // Async image loading
    implementation(libs.coil.compose)
    // Dependency injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.custom.qr.generator)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation (libs.logging.interceptor)

    implementation(libs.glide)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
}
