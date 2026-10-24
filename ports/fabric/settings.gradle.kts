pluginManagement {
    repositories { maven("https://maven.fabricmc.net/"); mavenCentral(); gradlePluginPortal() }
}
rootProject.name = "RemsAICompanion-Fabric-Ports"

// Minecraft 26.x ships unobfuscated and uses Loom without remapping.
if (providers.gradleProperty("mc").orNull == "26.2") {
    rootProject.buildFileName = "build-modern.gradle.kts"
}
