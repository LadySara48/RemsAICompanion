import java.util.Properties

plugins { id("net.fabricmc.fabric-loom-remap") version "1.17.21" }

val mc = providers.gradleProperty("mc").getOrElse("1.20.1")
val apiVersions = mapOf(
    "1.18.2" to "0.77.0+1.18.2", "1.20.1" to "0.92.12+1.20.1",
    "1.21.1" to "0.116.17+1.21.1", "1.21.4" to "0.119.4+1.21.4",
    "1.21.11" to "0.141.6+1.21.11"
)
require(mc in apiVersions) { "Unsupported port target: $mc" }
group = "org.hearlov.remscompanion"
val sharedProperties = Properties().apply { file("../../gradle.properties").inputStream().use { load(it) } }
version = "${sharedProperties.getProperty("mod_version")}+fabric-$mc"
base { archivesName.set("rems-ai-companion") }
layout.buildDirectory.set(file("build/$mc"))
repositories { mavenCentral() }
loom { runs.named("client") { runDir("run/$mc") } }
dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.19.5")
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", apiVersions.getValue(mc)))
    modImplementation(fabricApi.module("fabric-key-binding-api-v1", apiVersions.getValue(mc)))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
val generated = layout.buildDirectory.dir("portSources")
val preparePort = tasks.register<Exec>("preparePort") {
    inputs.dir("../../src/main/java"); inputs.dir("../../src/client/java")
    inputs.dir("compat"); inputs.file("prepare.py"); inputs.property("mc", mc)
    outputs.dir(generated)
    commandLine("python", "prepare.py", mc, generated.get().asFile.absolutePath)
}
sourceSets.main {
    java.setSrcDirs(listOf("../../src/main/java", generated))
    resources.setSrcDirs(listOf("../../src/main/resources"))
}
sourceSets.test { java.setSrcDirs(listOf("../../src/test/java")) }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)); withSourcesJar() }
tasks.named("sourcesJar") { dependsOn(preparePort) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
tasks.compileJava { dependsOn(preparePort); options.release.set(if (mc.startsWith("1.21")) 21 else 17) }
tasks.compileTestJava { options.release.set(25) }
tasks.test { useJUnitPlatform() }
tasks.processResources {
    inputs.property("version", project.version); inputs.property("minecraft", mc)
    filesMatching("fabric.mod.json") {
        filter { line -> line.replace("\"26.3\"", "\"$mc\"")
            .replace("\">=25\"", if (mc.startsWith("1.21")) "\">=21\"" else "\">=17\"")
            .replace("\"fabric-api\": \"*\"", "\"fabric-lifecycle-events-v1\": \"*\", \"fabric-key-binding-api-v1\": \"*\"") }
        expand("version" to project.version)
    }
}
tasks.remapJar { destinationDirectory.set(file("../../dist/fabric")) }
