plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.alma.aitrader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alma.aitrader"
        minSdk = 26
        targetSdk = 35
        versionCode = 51
        versionName = "5.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
