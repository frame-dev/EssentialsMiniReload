[![Java CI with Maven](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/maven.yml/badge.svg)](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/maven.yml)
[![Release on Push](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/release.yml/badge.svg)](https://github.com/frame-dev/EssentialsMiniReload/actions/workflows/release.yml)

# EssentialsMiniReload

A powerful, modular Essentials plugin for Minecraft 1.20+ servers. EssentialsMiniReload provides all the critical features you need — homes, warps, economy, kits, chat moderation, and more — without the bloat.

---

# ***Require Paper or Purpur Server***

---

## 🚀 Features

- ✨ Over 100 modular, toggleable commands
- 🗃️ Local **file**, **SQLite**, **MySQL**, and **MongoDB** support
- ⚡ Fully asynchronous storage operations
- 💰 Vault-based economy (balance, pay, bank, etc.)
- 🔨 Moderation: mute, tempmute, ban, tempban, globalmute
- 🏠 Homes, warps, spawn, back, home GUI
- 🎒 Kits system with creation & selection
- 🎯 Inventory tools: invsee, backpack, trash, saveinventory
- 🛡️ Utility commands: fly, gamemode, god, heal, feed, repair, enchant
- 🌦️ Weather/time control: day, night, sun, rain, thunder, player-specific time/weather
- 👀 Player info: online, offline, afk, nicknames, timeplayed
- ✅ Clean permission system and YAML config

---

## 🗃️ Storage Backends

Set your preferred storage type in `config.yml`:

- `file` *(default)* — lightweight local storage
- `sqlite` — flat-file SQL backend
- `mysql` — remote SQL database
- `mongodb` - MongoDB support

---

## 💬 Commands Overview

> EssentialsMiniReload registers 100+ commands. Here's a categorized overview.

### 🔀 Teleportation
- `/home`, `/sethome`, `/delhome`, `/homegui`
- `/warp`, `/setwarp`, `/delwarp`, `/warps`
- `/back`, `/spawn`, `/setspawn`
- `/tpa`, `/tpahere`, `/tpaaccept`, `/tpadeny`, `/tphereall`
- `/position`, `/showlocation`

### 🛡️ Moderation
- `/mute`, `/tempmute`, `/removetempmute`, `/muteinfo`, `/muteforplayer`
- `/ban`, `/tempban`, `/eban`, `/eunban`, `/removetempban`
- `/globalmute`, `/silent`, `/clearchat`, `/maintenance`
- `/srestart`

### 💰 Economy (Vault)
- `/balance`, `/balancetop`, `/eco`, `/pay`
- `/bank` (create, deposit, withdraw, add/remove member, balance)

### ⚔️ Kits
- `/kits`, `/createkit`

### 🛠️ Utilities
- `/fly`, `/walkspeed`, `/flyspeed`, `/godmode`, `/gamemode`
- `/heal`, `/feed`, `/workbench`, `/repair`, `/enchant`, `/item`
- `/invsee`, `/enderchest`, `/backpack`, `/saveinventory`, `/trash`
- `/sleep`, `/signitem`, `/renameitem`, `/playerheads`

### 💬 Chat & Messaging
- `/msg`, `/r`, `/spy`, `/afk`
- `/msgtoggle`, `/tptoggle`
- `/nick`, `/nicklist`

### 🌤️ World Control
- `/day`, `/night`, `/sun`, `/rain`, `/thunder`
- `/plweather`, `/resetplweather`, `/pltime`, `/resetpltime`
- `/lightningstrike`, `/firework`

### 📊 Player Info
- `/online`, `/offline`, `/xp`, `/timeplayed`, `/mysql`

---

## 🔐 Permissions

- All commands are permission-based and configurable
- Use `essentialsmini.*` for full access
- Examples:
    - `essentialsmini.fly`
    - `essentialsmini.kits`
    - `essentialsmini.invsee`
    - `essentialsmini.balance`
    - `essentialsmini.ban`

Use with LuckPerms or any standard permission manager.

---

## 🔌 Soft Dependencies

| Plugin              | Purpose                     |
|---------------------|-----------------------------|
| Vault               | Economy system              |
| SpigotMongoDBUtils  | MongoDB support             |
| LuckPerms           | Permissions (recommended)   |
| ProtocolLib         | Optional extended features  |

---

---

## In Progress

- [ ] **SkinChanger**: Add option to change Skin and Playername
- [ ] **NameChanger**: Add option to change Playername

## 🛠️ Development Info

- Language: Java
- Build: Maven (`pom.xml`)
- Main class: `ch.framedev.essentialsmini.main.Main`

---

## 👤 Author

Developed by [FrameDev](https://github.com/frame-dev)  
Website: [framedev.ch](https://framedev.ch)

---

## 📄 License

MIT License. See [LICENSE](./LICENSE).

---

> For issues or suggestions, feel free to open a [GitHub Issue](https://github.com/frame-dev/EssentialsMiniReload/issues).
