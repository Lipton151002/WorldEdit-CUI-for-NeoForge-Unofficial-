# WorldEdit CUI for NeoForge (Unofficial)

**English** | [Русский](README.ru.md)

An unofficial **NeoForge** port of [WorldEditCUI](https://github.com/EngineHub/WorldEditCUI), the
graphical user interface for [WorldEdit](https://enginehub.org/worldedit). It draws your WorldEdit
selections right in the world: cuboids, polygons, cylinders, ellipsoids and convex polyhedrons,
with grids and points.

> **Not an official EngineHub project.** The original WorldEditCUI is Fabric-only; this repository
> ports it to NeoForge for every major Minecraft version from 1.20.1 to 26.3.
> Please report problems with this port here, not to EngineHub.

WorldEditCUI is only the client-side frontend. WorldEdit itself must be installed on the server
(Paper, Spigot, Folia, NeoForge or Fabric), or in your `mods` folder for singleplayer.

## Supported versions

One jar per Minecraft version:

| Minecraft | Loader | Jar |
|---|---|---|
| 26.3 | NeoForge 26.3 | `WorldEditCUI-NeoForge-26.3+01.jar` |
| 26.2 | NeoForge 26.2 | `WorldEditCUI-NeoForge-26.2+01.jar` |
| 26.1.x | NeoForge 26.1 | `WorldEditCUI-NeoForge-26.1.2+01.jar` |
| 1.21.11 | NeoForge 21.11 | `WorldEditCUI-NeoForge-1.21.11+01.jar` |
| 1.21.10 | NeoForge 21.10 | `WorldEditCUI-NeoForge-1.21.10+01.jar` |
| 1.21.8 | NeoForge 21.8 | `WorldEditCUI-NeoForge-1.21.8+01.jar` |
| 1.21.5 | NeoForge 21.5 | `WorldEditCUI-NeoForge-1.21.5+01.jar` |
| 1.21.4 | NeoForge 21.4 | `WorldEditCUI-NeoForge-1.21.4+01.jar` |
| 1.21.3 | NeoForge 21.3 | `WorldEditCUI-NeoForge-1.21.3+01.jar` |
| 1.21, 1.21.1 | NeoForge 21.0 / 21.1 | `WorldEditCUI-NeoForge-1.21.1+01.jar` |
| 1.20.6 | NeoForge 20.6 | `WorldEditCUI-NeoForge-1.20.6+01.jar` |
| 1.20.4 | NeoForge 20.4 | `WorldEditCUI-NeoForge-1.20.4+01.jar` |
| 1.20.1 | NeoForge 47.1 **or** Forge 47 | `WorldEditCUI-NeoForge-1.20.1+01.jar` |

1.20.2 is not supported: NeoForge for 1.20.2 was short-lived and WorldEdit was never released for it.

## Installation

1. Install [NeoForge](https://neoforged.net/) for your Minecraft version.
2. Download the jar for your exact Minecraft version from the
   [Releases](../../releases) page and put it into your `mods` folder.

The WorldEditCUI protocol library is bundled where needed, so no other mods are required.
The mod is client-side only; do not install it on a server.

## Usage

Selections are shown automatically once you make one with WorldEdit (for example with `//wand`).

Optional key bindings live in **Options → Controls → WorldEditCUI** and are unbound by default:

- toggle selection visibility
- clear the selection (`//sel`)
- show chunk borders

Colours and line styles can be changed in **Mods → WorldEditCUI → Config**.
Settings are stored in `config/worldeditcui.config.json`.

## Compatibility notes

- **WorldEdit on the client (singleplayer).** On 1.20.1 and 1.20.6–1.21.4, older WorldEdit releases
  register the CUI network channel themselves. The port detects this and reuses WorldEdit's channel
  instead of registering a second one, so both mods can be installed together.
- **Shaders.** Iris is supported on 26.2 and 26.3, the same as upstream.

## Building from source

Requirements: a JDK 17 or newer to start Gradle. The build downloads the JDKs it needs
(17, 21 and 25) through the Foojay toolchain resolver.

```sh
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows
```

All jars are collected into `dist/`. The first build downloads and decompiles every supported
Minecraft version, which takes a while and about 10 GB of disk space in the Gradle cache.

Useful tasks:

| Task | What it does |
|---|---|
| `gradlew :1.21.11:build` | Build a single version |
| `gradlew :1.21.11:runClient` | Start a development client (with WorldEdit where available) |
| `gradlew clean` | Remove build output and `dist/` |

`build.bat` runs a clean build with the Gradle cache in a dedicated folder
(`%USERPROFILE%\Desktop\worldgui-dev\gradle-home`); edit it if you prefer another location.

## Project layout

```
build.gradle.kts            shared build logic for all versions
settings.gradle.kts         includes every folder in versions/ automatically
templates/                  mod metadata template (neoforge.mods.toml / mods.toml)
versions/<minecraft>/
  gradle.properties         Minecraft, NeoForge/Forge, Java and dependency versions
  src/main/java/org/enginehub/worldeditcui/
    ...                     common code taken from the matching upstream release
    neoforge/               NeoForge-specific glue (events, rendering hook, networking)
```

The common code of each version is kept as close as possible to the matching upstream
WorldEditCUI release; everything platform-specific lives in the `neoforge` package.
Built with [ModDevGradle](https://github.com/neoforged/ModDevGradle)
(the legacy Forge plugin for 1.20.1).

## Credits

- [EngineHub](https://enginehub.org/) and the
  [WorldEditCUI contributors](https://github.com/EngineHub/WorldEditCUI/graphs/contributors): the original mod
- [lahwran](https://github.com/lahwran): creator of the original WorldEdit CUI
- Mumfrey, yetanotherx, mikroskeem, TomyLobo and zml: previous maintainers
- [sk89q](https://github.com/sk89q): WorldEdit

## License

This project is based on the source code of
[WorldEditCUI](https://github.com/EngineHub/WorldEditCUI) and, like the original, is licensed under
the [Eclipse Public License 2.0](LICENSE). Original copyright notices are kept in the source files.
