# SpigBan

**SpigBan** is an advanced all-in-one punishment management plugin for **Spigot 1.21.x**.
    
<a href="https://cla-assistant.io/REA4V-Lab/SpigBan"><img src="https://cla-assistant.io/readme/badge/REA4V-Lab/SpigBan" alt="CLA assistant" /></a>
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


## License & CLA
- License: see `LICENSE`.
- CLA: see `CLA.md`.
- CLA bot integration: configured under `.github/` (cla-assistant).




