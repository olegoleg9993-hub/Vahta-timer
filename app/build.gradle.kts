plugins {
    id("com.android.application")
}

android {
    namespace = "ru.oilab.shifttimer"
    compileSdk = 35
    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "ru.oilab.shifttimer"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "0.2.9"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
