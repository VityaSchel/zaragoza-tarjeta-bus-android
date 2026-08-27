pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://git.hloth.dev/api/packages/hloth/maven") {
            content { includeModule("dev.hloth", "zgz-transport") }
        }
        mavenCentral()
    }
}

rootProject.name = "ZaragozaTarjetaBus"
include(":app")
