pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuranPremiumGitHub"
include(":app")
include(":core:design")
include(":feature:home")

include(":core:quran")
include(":feature:mushaf")

include(":core:audio")
include(":core:prayer")
include(":feature:audio")
include(":feature:prayer")
include(":feature:qibla")
include(":core:settings")
include(":feature:settings")

include(":core:practice")
include(":feature:hifz")
include(":feature:khatma")
include(":feature:adhkar")
include(":core:stats")
include(":feature:stats")