# EssentialsMiniReload

[![Maven Build](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/maven-build.yml/badge.svg)](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/maven-build.yml)
[![Release Latest Jar](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/release-latest-jar.yml/badge.svg)](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/release-latest-jar.yml)

EssentialsMiniReload is a modular essentials plugin for modern Minecraft servers. It bundles everyday server tools such as homes, warps, spawn, economy, kits, moderation, private messages, mail, nicknames, backpacks, player utilities, and world controls without needing a large plugin stack.

## Requirements

- Minecraft server with Bukkit/Spigot-compatible API `1.20+`
- Java `17`
- Maven `3.8+` for local builds
- Paper or Purpur is recommended for production servers

Optional integrations:

| Plugin | Purpose |
| --- | --- |
| Vault | Economy support |
| LuckPerms | Permission management |
| ProtocolLib | Extended packet-based features such as skin refreshes |

## Installation

1. Download the latest jar from GitHub Releases or build it locally.
2. Put the jar into your server `plugins` folder.
3. Start the server once so the config files are generated.
4. Configure `plugins/EssentialsMini/config.yml`.
5. Restart the server.

## Build From Source

```bash
mvn clean package
```

The plugin jar is written to `target/`. The shaded plugin jar is the normal `EssentialsMini-*.jar`; ignore `original-*.jar`.

## GitHub Actions

- `Maven Build` runs on every push and pull request, builds the Maven project, and uploads the jar as an artifact.
- `Release Latest Jar` can be started manually from the Actions tab. It downloads the latest successful `Maven Build` jar and publishes it to a GitHub Release.

For manual releases, you can provide a tag, release name, branch, draft flag, and prerelease flag. If no tag is provided, the workflow uses the Maven project version.

## Storage

EssentialsMiniReload supports multiple storage modes depending on the feature and configuration:

- YAML files for lightweight local data
- SQLite
- MySQL
- MongoDB

Economy features require Vault and an economy provider.

## Command Overview

### Teleportation

- `/spawn`, `/setspawn`
- `/home`, `/sethome`, `/delhome`, `/homegui`
- `/warp`, `/setwarp`, `/delwarp`, `/warps`
- `/back`, `/top`
- `/tpa`, `/tpahere`, `/tpaaccept`, `/tpadeny`, `/tphereall`

### Economy

- `/balance`, `/balancetop`
- `/eco`
- `/pay`
- `/bank`

### Kits And Items

- `/kits`
- `/createkit`
- `/item`
- `/repair`
- `/enchant`
- `/renameitem`
- `/signitem`
- `/playerheads`

### Moderation

- `/mute`, `/tempmute`, `/removetempmute`, `/muteinfo`
- `/muteforplayer`
- `/ban`, `/tempban`, `/eban`, `/eunban`
- `/globalmute`
- `/maintenance`
- `/clearchat`
- `/silent`
- `/srestart`

### Chat And Player Tools

- `/msg`, `/r`, `/spy`, `/msgtoggle`
- `/mail`
- `/afk`
- `/nick`
- `/nicklist`
- `/timeplayed`
- `/online`, `/offline`

### Inventory And Utility

- `/fly`, `/flyspeed`, `/walkspeed`
- `/gamemode`, `/godmode`
- `/heal`, `/feed`
- `/invsee`, `/enderchest`
- `/backpack`, `/trash`
- `/workbench`
- `/sleep`

### World Control

- `/day`, `/night`
- `/sun`, `/rain`, `/thunder`
- `/plweather`, `/resetplweather`
- `/pltime`, `/resetpltime`
- `/lightningstrike`
- `/firework`

## Permissions

Permissions use the `essentialsmini.` prefix.

Examples:

- `essentialsmini.fly`
- `essentialsmini.home`
- `essentialsmini.kits.<kitname>`
- `essentialsmini.item`
- `essentialsmini.mute`
- `essentialsmini.tempmute`
- `essentialsmini.ban`

Use `essentialsmini.*` for broad administrator access where supported by your permission plugin.

## Configuration Files

The plugin creates and uses several files in its data folder, including:

- `config.yml`
- language files such as `messages_en-EN.yml`
- `locations.yml`
- `kits.yml`
- `mail.yml`
- `tempMutes.yml`
- `nicks.yml`

Back up these files before upgrading on a production server.

## Project Info

- Main class: `ch.framedev.essentialsmini.main.Main`
- Maven artifact: `EssentialsMini`
- Current project version: `1.0.7-1.20.6-HIGHER-RELEASE`
- Website: [framedev.ch](https://framedev.ch)

## License

This project is licensed under the MIT License. See [LICENSE](./LICENSE).

## Author

Developed by [FrameDev](https://github.com/frame-dev).

Issues and suggestions are welcome in [GitHub Issues](https://github.com/frame-dev/EssentialsMiniReload/issues).
