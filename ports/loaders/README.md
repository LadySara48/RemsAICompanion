# Forge and NeoForge ports

The canonical AI, chat storage and UI source remains in the root project's `src/`. These builds reuse the vanilla GUI bridges under `ports/fabric/compat`; that directory name does **not** introduce a Fabric runtime dependency. `prepare.py` adds only the target loader's configuration path, loader label, client lifecycle and key mapping registration. All translations, portraits and voices are shared without modification.

## Supported build targets

- Forge: Minecraft **1.18.2, 1.20.1** (Java 17+).
- NeoForge: **1.21.1, 1.21.11** (Java 21+), **26.3** (Java 25+).
- NeoForge 1.20.1 is not included, following the final agreed target list.

Pinned loader versions are listed in `versions.json`. Several historical NeoForge targets only have beta builds; their exact version strings preserve that label. Set `mod_version` once in the root `gradle.properties`; every loader build uses it.

## Build

Build tools: JDK 25, Python 3.11+, and the root Gradle wrapper. The Foojay toolchain resolver downloads the Java versions required by the Minecraft development tools if they are missing. Players do not need Python or Gradle.

From the repository root in PowerShell:

```powershell
.\gradlew.bat -p ports/forge "-Pmc=1.20.1" build
.\gradlew.bat -p ports/neoforge "-Pmc=1.21.1" build
.\ports\loaders\build-ports.ps1
```

To build a subset:

```powershell
.\ports\loaders\build-ports.ps1 -Loader forge -Versions @('1.20.1','1.18.2')
.\ports\loaders\build-ports.ps1 -Loader neoforge -Versions @('26.3')
```

Installable JARs go to `dist/forge/` and `dist/neoforge/`. Forge's **reobfuscated** JAR is collected after `reobfJar`; its development JAR must not be distributed. NeoForge uses its regular production JAR. Source JARs and test reports remain inside each target's build directory.

To verify the shipping artifacts and bundle them:

```powershell
python ports/loaders/verify-dist.py forge
python ports/loaders/verify-dist.py neoforge
python ports/loaders/package-dist.py
```

Each ZIP includes the exact-version JARs, installation instructions, pinned loader versions and SHA-256 checksums. Install only **one** JAR matching the loader and Minecraft version, not the entire collection. Fabric API is not required.

## Runtime and data

The mod is client-side. Forge's common bootstrap defers all client class access to the client distribution; NeoForge's mod entrypoint is restricted to `Dist.CLIENT`. No server channel or server companion mod is required. The R key is registered through the loader's key mapping/setup event. Shutdown flushes chat data and updates the one-set backup through `GameShuttingDownEvent`. Forge 1.18.2 predates that event and uses a JVM shutdown hook which waits for the same bounded writer flush.

The existing `config/rems_ai_companion/` JSON layout is preserved. The mandatory game context uses the actual `Forge` or `NeoForge` loader label. Local API keys, chat history and development configuration are never packaged as resources.

Development clients have separate `ports/<loader>/run/<minecraft-version>/` directories:

```powershell
.\gradlew.bat -p ports/forge "-Pmc=1.18.2" runClient
.\gradlew.bat -p ports/neoforge "-Pmc=26.3" runClient
```

## Verification limits

Shared tests exercise requests/stream parsing, JSON storage and recovery, summaries, response versions, settings and usage accounting. Packaging checks cover the metadata, Java bytecode level, Forge reobfuscation, absence of Fabric references and byte-for-byte asset preservation. These checks do not claim a successful in-game run.

Before marking each target tested, check R / Esc / Close; text entry and Shift+Enter; copy, selection and Markdown links; provider settings and streaming/Stop; sound and portraits; chat save/reopen/delete/search; scrolling and regeneration; Summary; and backup recovery. Also check entering a server which does not have the mod. Any provider calls made while testing use that provider's normal billing.
