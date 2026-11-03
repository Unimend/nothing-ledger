plugins { alias(libs.plugins.kotlin.jvm) }

kotlin {
    jvmToolchain(17)
    compilerOptions { allWarningsAsErrors.set(true); explicitApi() }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":domain:ledger"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test { useJUnitPlatform() }
