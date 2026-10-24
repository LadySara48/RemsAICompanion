import java.util.Properties

plugins { id("net.fabricmc.fabric-loom") version "1.17.21" }

val mc = providers.gradleProperty("mc").get()
require(mc == "26.2") { "Unsupported modern Fabric port: $mc" }
val shared = Properties().apply { file("../../gradle.properties").inputStream().use { load(it) } }
group = "org.hearlov.remscompanion"
version = "${shared.getProperty("mod_version")}+fabric-$mc"
base { archivesName.set("rems-ai-companion") }
layout.buildDirectory.set(file("build/$mc"))
repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven { url = uri("https://repo.maven.apache.org/maven2") } }
        filter { includeGroup("org.lwjgl") }
    }
    exclusiveContent {
        forRepository { maven { url = uri("https://libraries.minecraft.net") } }
        filter { includeGroup("com.mojang") }
    }
}
loom { runs.named("client") { runDir("run/$mc") } }
dependencies {
    minecraft("com.mojang:minecraft:$mc")
    implementation("net.fabricmc:fabric-loader:${shared.getProperty("loader_version")}")
    // Fabric API 0.161.0+26.2: only the two modules used by this client.
    implementation("net.fabricmc.fabric-api:fabric-lifecycle-events-v1:4.1.4+29b6eb019e")
    implementation("net.fabricmc.fabric-api:fabric-key-mapping-api-v1:2.0.5+e2bdee789e")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
val generated = layout.buildDirectory.dir("portSources")
val preparePort = tasks.register<Exec>("preparePort") {
    inputs.dir("../../src/main/java"); inputs.dir("../../src/client/java")
    inputs.file("prepare.py"); inputs.property("mc", mc)
    outputs.dir(generated)
    commandLine("python", "prepare.py", mc, generated.get().asFile.absolutePath)

}
sourceSets.main {
    java.setSrcDirs(listOf("../../src/main/java", generated))
    resources.setSrcDirs(listOf("../../src/main/resources"))
}
sourceSets.test { java.setSrcDirs(listOf("../../src/test/java")) }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)); withSourcesJar() }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release.set(25) }
tasks.compileJava { dependsOn(preparePort) }
tasks.named("sourcesJar") { dependsOn(preparePort) }
tasks.test { useJUnitPlatform() }
tasks.processResources {
    inputs.property("version", project.version); inputs.property("minecraft", mc)
    filesMatching("fabric.mod.json") {
        filter { line -> line.replace("\"26.3\"", "\"$mc\"")
            .replace("\"fabric-api\": \"*\"", "\"fabric-lifecycle-events-v1\": \"*\", \"fabric-key-mapping-api-v1\": \"*\"") }
        expand("version" to project.version)
    }
}
tasks.jar { destinationDirectory.set(file("../../dist/fabric")) }
