# Fabric version ports

The working 26.3 project remains at the repository root. This standalone build shares its AI/storage code and **all** resources (including translations and audio). `prepare.py` generates the client adapters into a version-specific build directory. Edit the canonical client or the adapter templates, never the generated files.

Set the release version once with `mod_version` in the root `gradle.properties`. Port filenames add the loader and exact Minecraft version automatically.

## Build

Build tools: JDK 25, Python 3, and the repository Gradle wrapper. JDK 25 runs Gradle; `--release` produces Java 17 bytecode for 1.18.2/1.20.1, Java 21 bytecode for 1.21.x, and Java 25 bytecode for 26.2. Python is a build tool only; players do not need it.

From the repository root, in PowerShell:

```powershell
.\gradlew.bat -p ports/fabric "-Pmc=1.20.1" build
.\ports\fabric\build-ports.ps1
```

The script accepts a subset, for example:

```powershell
.\ports\fabric\build-ports.ps1 -Versions @('1.18.2', '1.20.1', '26.2')
```

After the full matrix passes, `python ports/fabric/package-dist.py` verifies and bundles the installable JARs with installation instructions and SHA-256 checksums into one ZIP in `dist/`.

Installable mod JARs (remapped for 1.x; unobfuscated for 26.2) are written to `dist/fabric/`. Do not distribute the unremapped development JARs inside `ports/fabric/build/`. Source JARs and test reports are in that version's build directory.

Install only the one JAR matching your Minecraft version, not the whole collection. The port metadata lists the two Fabric API modules actually used; the normal Fabric API distribution supplies both.

Each JAR declares one exact Minecraft version. Do not mark a JAR compatible with the entire 1.21 series: input events, rendering, identifiers and other Minecraft APIs change between those releases. The port build matrix includes 1.18.2, 1.20.1, 1.21.1, 1.21.4, 1.21.11 and 26.2. Fabric 26.3 remains in the root build.

## Installation and verification

Players need Fabric Loader 0.19.5 or newer, Fabric API for their exact Minecraft version, and the matching mod JAR. Install on the client. Use Java 17 or later for 1.18.2–1.20.1; Java 21 or later for 1.21.x; Java 25 for 26.2/26.3.

All versions use the existing `config/rems_ai_companion/` JSON format and asset paths. Private development configuration, API keys and chat history are not bundled.

Automated builds verify compilation, remapping and the shared tests against each Minecraft version's own libraries. They do not replace an in-game test. Before publishing a version, check: R opens / Esc closes; text entry including Shift+Enter, paste and selection; API/profile/prompt menus; response streaming and Stop; Copy and Markdown links; voice/portrait; search, scrolling, regeneration; Summary; and saving/reopening chats. Test a translated language at the GUI scale used by players as well.

To start the development client for a target:

```powershell
.\gradlew.bat -p ports/fabric "-Pmc=1.20.1" runClient
```

Each development target uses its own `ports/fabric/run/<minecraft-version>/` directory, keeping test worlds and local settings separate.

API requests made during manual tests use the configured provider and may incur its normal charges.
