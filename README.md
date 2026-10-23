# Rem's AI Companion

A client-side Minecraft mod that brings AI chat into the game, with Rem-inspired artwork and voice reactions.

## Features

- Streaming AI responses with configurable providers and models.
- Saved conversations, custom system prompts, and optional chat summaries.
- Markdown, code blocks, and message copying.
- Multiple languages and customizable voice settings.

## Getting started

1. Install the JAR for your Minecraft version and mod loader. Fabric also requires Fabric API.
2. Press **R** to open the menu.
3. Open **API Settings**, choose a provider, and enter your API key and model.
4. Start chatting. Press **Esc** to close the menu.

An internet connection and your own API credentials are required. Provider fees may apply. Servers do not need to install the mod.

## About

- This mod was written with the help of AI.
- I wrote the module's core structure, file system, and the main software component that sends requests to the AI APIs.
- Client-side: The menus, settings menu, and all menu-related elements were created with the help of AI.
- The Rem images in the mod were drawn by me. (I'm really terrible at design.)

## Supported versions

- **Fabric:** 1.18.2, 1.19.2, 1.20.1, 1.21–1.21.11, and 26.3.
- **Forge:** 1.18.2, 1.19.2, and 1.20.1.
- **NeoForge:** 1.21.1–1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, and 26.3.

## Building

Use JDK 25 and Python 3.11+.

```powershell
# Fabric 26.3
.\gradlew.bat build

# Choose a supported Minecraft version
.\gradlew.bat -p ports/fabric "-Pmc=1.20.1" build
.\gradlew.bat -p ports/forge "-Pmc=1.20.1" build
.\gradlew.bat -p ports/neoforge "-Pmc=1.21.1" build
```

See the [Fabric build guide](ports/fabric/README.md) and [Forge / NeoForge build guide](ports/loaders/README.md) for details.
