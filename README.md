# EssentialsMiniReload

[![Maven Build](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/maven-build.yml/badge.svg)](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/maven-build.yml)
[![Release Latest Jar](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/release-latest-jar.yml/badge.svg)](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/release-latest-jar.yml)

EssentialsMiniReload is a modular essentials plugin for modern Minecraft servers. It bundles everyday server tools such as homes, warps, spawn, economy, kits, moderation, private messages, mail, nicknames, backpacks, portable utility stations, player utilities, and world controls without needing a large plugin stack.

## Requirements

- Minecraft server with Bukkit/Spigot-compatible API `1.20+`
- Java `17`
- Maven `3.8+` for local builds
- Paper or Purpur is recommended for production servers

For older Minecraft versions, use the original EssentialsMini plugin: [EssentialsMini on SpigotMC](https://www.spigotmc.org/resources/essentialsmini.82775/).

Optional integrations:

| Plugin | Purpose |
| --- | --- |
| Vault | Economy support |
| LuckPerms | Permission management |
| ProtocolLib | Extended packet-based features such as live skin refreshes |
| Geyser / Floodgate | Optional Bedrock player support |

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

- `/emhelp` opens the full EssentialsMiniReload command help GUI. Hover over any command item to see its description, usage, aliases, and permission. Console users can run `/emhelp text`.

### Teleportation

- `/spawn`, `/setspawn`
- `/home`, `/sethome`, `/delhome`, `/homegui`
- `/warp`, `/setwarp`, `/delwarp`, `/warps`
- `/back`, `/top`
- `/tpa`, `/tpahere`, `/tpaaccept`, `/tpadeny`, `/tphereall`

### Economy

- `/balance`, `/balancetop`
- `/eco <add|remove|set> <amount> [player]`
- `/eco <add|remove|set> <player> <amount>`
- `/pay <amount> <player>`
- `/pay <player> <amount>`
- `/bank`

### Kits And Items

- `/kits` opens the kit selector GUI
- `/kits <kit>`
- `/createkit`
- `/item`
- `/repair`
- `/enchant`
- `/renameitem`
- `/signitem`
- `/playerheads`
- `/retrieve`, `/retrieve all`
- `/showrecipe`, `/showrecipe <item>`

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
- `/nick`, `/nick reset`
- `/nicklist`
- `/timeplayed`
- `/online`, `/offline`
- `/near`, `/near <radius>`

### Inventory And Utility

- `/fly`, `/flyspeed`, `/walkspeed`
- `/gamemode`, `/godmode`
- `/heal`, `/feed`
- `/invsee`, `/enderchest`
- `/backpack`, `/trash`
- `/workbench`, `/craft`
- `/anvil`, `/grindstone`, `/smithingtable`
- `/cartographytable`, `/loom`, `/stonecutter`
- `/sleep`

### World Control

- `/day`, `/night`
- `/sun`, `/rain`, `/thunder`
- `/plweather`, `/resetplweather`
- `/pltime`, `/resetpltime`
- `/lightningstrike`
- `/firework`

### Admin And Help

- `/emhelp`, `/essentialshelp`
- `/essentialsmini`
- `/essentialsmini reload`
- `/essentialsmini help`
- `/essentialsmini version`
- `/essentialsmini economy <on|off>`
- `/essentialsmini messages <list|add|remove|create>`

## Permissions

Permissions use the `essentialsmini.` prefix.

Examples:

- `essentialsmini.fly`
- `essentialsmini.help`
- `essentialsmini.home`
- `essentialsmini.kits.<kitname>`
- `essentialsmini.item`
- `essentialsmini.retrieve`
- `essentialsmini.showrecipe`
- `essentialsmini.workbench`
- `essentialsmini.anvil`
- `essentialsmini.near`
- `essentialsmini.mute`
- `essentialsmini.tempmute`
- `essentialsmini.ban`
- `essentialsmini.messages`

Use `essentialsmini.*` for broad administrator access where supported by your permission plugin.

## Configuration Files

The plugin creates and uses several files in its data folder, including:

- `config.yml`
- language files such as `messages_en-EN.yml`
- custom language files such as `messages_nl-NL.yml`
- `locations.yml`
- `kits.yml`
- `mail.yml`
- `tempMutes.yml`
- `nicks.yml`

Back up these files before upgrading on a production server.

Existing `config.yml` files are updated with missing default options and bundled comments on startup or `/essentialsmini reload` without overwriting configured values.

### Custom Message Locales

Custom message files can be added to the plugin data folder using the `messages_<locale>.yml` naming format. For example, create `plugins/EssentialsMini/messages_nl-NL.yml` for Dutch.

If a custom file is detected but not enabled, the plugin logs a prompt. Add it with:

```text
/essentialsmini messages add nl-NL
```

You can also create a new custom message file from the English messages and enable it immediately:

```text
/essentialsmini messages create nl-NL
```

The same setting can be managed directly in `config.yml`:

```yaml
messages:
  customLocales:
    - nl-NL
  promptForCustomLocales: true
```

Use `/essentialsmini messages list` to see loaded locales, enabled custom locales, and custom files that still need to be added.

### Customization

The `customization` section in `config.yml` makes common visible parts of the plugin toggleable and configurable:

- `customization.prefixes`: enable feature-specific prefixes for economy, moderation, mail, teleport, help, kits, backpack, trash, and utility stations.
- `customization.notifications`: configure chat, actionbar, title, and sound delivery for teleport requests, teleport completion, teleport delay, mail received, and money received.
- `customization.disabledCommands`: set a default disabled-command message and per-command overrides such as `help`, `me`, and `plugins`.
- `customization.guis`: customize titles and item materials/names/lore for home, warp, kit selector, trash, backpack, and utility station interfaces.

Example feature prefix setup:

```yaml
customization:
  prefixes:
    enabled: true
    features:
      economy:
        enabled: true
        value: '&6[&eEconomy&6] &f'
```

Example notification setup:

```yaml
customization:
  notifications:
    moneyReceived:
      enabled: true
      chat:
        enabled: true
        message: '&aYou received &6%Money% &afrom &6%Player%&a.'
      actionbar:
        enabled: true
        message: '&a+%Money% from &6%Player%'
      sound:
        enabled: true
        name: ENTITY_EXPERIENCE_ORB_PICKUP
```

For `/nick <nickname> <skin>` troubleshooting, set `skinDebug: true` in `config.yml`, reload or restart the server, then check the console for `[SkinDebug]` lines.

Skin refresh supports Spigot, Paper, and Purpur by trying Paper/Purpur profile APIs first and falling back to ProtocolLib/NMS profile updates on Spigot-style servers.

## Project Info

- Main class: `ch.framedev.essentialsmini.main.Main`
- Maven artifact: `EssentialsMini`
- Current project version: `1.1.2-1.20.6-HIGHER-RELEASE`
- Website: [framedev.ch](https://framedev.ch)
- Issues/support: [https://github.com/frame-dev/EssentialsMiniReload/issues](https://github.com/frame-dev/EssentialsMiniReload/issues)

## License

This project is licensed under the MIT License. See [LICENSE](./LICENSE).

## Author

Developed by [FrameDev](https://github.com/frame-dev).

Issues and suggestions are welcome at [https://github.com/frame-dev/EssentialsMiniReload/issues](https://github.com/frame-dev/EssentialsMiniReload/issues).
