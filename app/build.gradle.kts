plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.unimend.nothingledger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.unimend.nothingledger"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.0.3-ledger-rules"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":domain:ledger"))
}

