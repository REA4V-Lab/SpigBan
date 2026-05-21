# SpigBan

**SpigBan** is an advanced all-in-one punishment management plugin for **Spigot 1.21.x**.

## Features
- Ban, temp-ban, IP-ban, temp-IP-ban
- Mute, temp-mute
- Kick
- Warn / Unwarn
- Case viewer + punishment history
- Admin command suite (`/spigban`)
- SQLite (default) or MySQL support
- Optional LuckPerms integration
- Configurable ban/mute screens and broadcast settings

## Installation
1. Build the plugin: `mvn package`
2. Put the generated jar in your server `plugins/` folder
3. Start the server
4. Configure `config.yml` (optional)

## Commands
Commands and permissions are defined in `src/main/resources/plugin.yml`.

Common examples:
- `/ban <player> [reason]`
- `/tempban <player> <duration> [reason]`
- `/mute <player> [reason]`
- `/tempmute <player> <duration> [reason]`
- `/warn <player> <reason>`
- `/case <caseId>`
- `/history <player> [page]`
- Admin: `/spigban <reload|info|purge>`

## Configuration
See `src/main/resources/config.yml`.

### Database
- `database.type`: `sqlite` (default) or `mysql`
- MySQL uses `host`, `port`, `database`, `username`, `password`
- Cleanup: `database.cleanup-interval` (minutes, `0` disables)

### Broadcast
- `broadcast.enabled`: broadcast to staff with `spigban.notify`
- `broadcast.console`: also print to console

### LuckPerms
- `integrations.luckperms.enabled`: enable LuckPerms-related behavior

### Screens
- `ban-screen.lines`: message keys from `messages.yml` with placeholders
- `mute-screen.lines`: same concept as ban screens

## Placeholders (in screen lines)
- `{reason}`, `{duration}`, `{staff}`
- `{case_id}`, `{date}`, `{expires}`

## Building
- Java: **21**
- Build: `mvn package`

## Support
- Website/GitHub: https://github.com/REA4V-Lab/SpigBan


## License & CLA
- License: see `LICENSE`.
- CLA: see `CLA.md`.
- CLA bot integration: configured under `.github/` (cla-assistant).




