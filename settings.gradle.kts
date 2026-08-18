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
        // sora-editor is published via JitPack
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "DiaBo"
include(":app")
