pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NothingLedger"

include(":core:model")
include(":core:design")
include(":core:database")
include(":domain:ledger")
include(":feature:entry")
include(":feature:timeline")
include(":feature:accounts")
include(":feature:categories")
include(":feature:shell")
include(":app")

