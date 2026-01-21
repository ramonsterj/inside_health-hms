pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "dev.detekt") {
                useModule("dev.detekt:detekt-gradle-plugin:${requested.version}")
            }
        }
    }
}

rootProject.name = "hms-api"
