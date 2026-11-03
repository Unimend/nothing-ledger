plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.unimend.nothingledger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.unimend.nothingledger"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.0.7-entry-clarity"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:design"))
    implementation(project(":domain:ledger"))
    implementation(project(":feature:entry"))
    implementation(project(":feature:timeline"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:categories"))
    implementation(project(":feature:shell"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}

