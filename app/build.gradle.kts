plugins {
    id("com.android.application")
}

// Release signing: the key comes from GitHub secrets (see .github/workflows/build.yml).
val keystorePath: String? = System.getenv("SIGNING_KEYSTORE")

android {
    namespace = "se.jonsolberg.teatimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "se.jonsolberg.teatimer"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5"
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
