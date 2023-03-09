rootProject.name = "radar-spring"

include("radar-spring-auth")

pluginManagement {
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val ktlintPluginVersion: String by settings
    val dependencyUpdatesVersion: String by settings
    val nexusPluginVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("com.github.ben-manes.versions") version dependencyUpdatesVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintPluginVersion
        id("io.github.gradle-nexus.publish-plugin") version nexusPluginVersion
    }
}
