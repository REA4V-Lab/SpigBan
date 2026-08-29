# Changelog

## Version 2.0.2

### 🔧 Fixes & Improvements
- **Fixed incompatible types error in CaseCommand**: Resolved compilation error where void was being returned instead of boolean
- **Fixed plugin startup issues**: Removed a problematic dependency that was preventing the plugin from loading
- **Updated case management**: The `/case` command now supports additional actions:
  - `/case <id>` - View case details (original behavior)
  - `/case <id> delete` - Remove a case permanently
  - `/case <id> close` - Deactivate a case (equivalent to ending the punishment early)
  - `/case <id> save` - Placeholder for future save functionality
- **Fixed support command**: The `/kofi` command now works correctly for showing clickable support links

### ➕ New Features
- **Automatic update checking**: The plugin now periodically checks for new versions on GitHub
  - Checks every 60 minutes (configurable in config.yml)
  - Notifies administrators and players with permission when an update is available
  - Provides direct link to the latest release
- **Version check command**: Added `/spigban version` command to:
  - Check for updates from GitHub releases
  - Display current plugin version
  - Show update information in console

### 📝 Notes
- All existing punishment features (bans, mutes, warns, kicks, etc.) work exactly as before
- Database support (SQLite/MySQL) and LuckPerms integration remain unchanged
- Configuration files and command structure are familiar to users of previous versions
- This update focuses on stability, usability, and keeping the plugin up-to-date

### 💡 How to Update
1. Replace the old SpigBan.jar with the new one
2. Restart your server
3. The update checker will run automatically in the background
4. Use `/spigban version` to manually check for updates