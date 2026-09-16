pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradleup.nmcp.settings").version("1.6.2")
}

rootProject.name = "exposed-option"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {

    }
}

nmcpSettings {
    centralPortal {
        username = System.getenv("CENTRAL_SONATYPE_USER")
        password = System.getenv("CENTRAL_SONATYPE_PASS")
        publishingType = "AUTOMATIC"
    }
}