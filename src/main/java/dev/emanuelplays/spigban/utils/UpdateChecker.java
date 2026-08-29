package dev.emanuelplays.spigban.utils;

import dev.emanuelplays.spigban.SpigBan;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Checks for updates on GitHub releases.
 */
public class UpdateChecker {

    private final SpigBan plugin;
    private final String currentVersion;
    private final String resource = "https://api.github.com/repos/REA4V-Lab/SpigBan/releases/latest";

    public UpdateChecker(SpigBan plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    /**
     * Starts the update checking task if enabled in config.
     */
    public void start() {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("update-checking.enabled", true)) {
            return;
        }
        int interval = config.getInt("update-checking.interval", 60);
        if (interval < 10) {
            interval = 10; // minimum 10 minutes
        }
        // Run first check after a short delay, then repeat
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60 * 5, // initial delay 5 minutes
                20L * 60 * interval); // interval in ticks
    }

    /**
     * Checks for updates and notifies if an update is available.
     */
    public void checkForUpdates() {
        String latestVersion = fetchLatestVersion();
        if (latestVersion == null || latestVersion.isEmpty()) {
            plugin.getLogger().warning("Could not parse version from GitHub response.");
            return;
        }

        if (isNewerVersion(latestVersion, currentVersion)) {
            plugin.getLogger().info("=== SpigBan Update Available ===");
            plugin.getLogger().info("Current version: " + currentVersion);
            plugin.getLogger().info("Latest version:  " + latestVersion);
            plugin.getLogger().info("Download at: https://github.com/REA4V-Lab/SpigBan/releases/latest");
            plugin.getLogger().info("================================");

            if (configGetBoolean("update-checking.notify-on-update", true)) {
                // Notify console and online players with spigban.notify permission
                String message = plugin.getMessageUtil().getPrefix() + "§e[✗] An update is available! §f(v"
                        + latestVersion + " §e> §fv" + currentVersion + "§e). §7Check console for details.";
                for (var player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("spigban.notify")) {
                        player.sendMessage(message);
                    }
                }
                // Also send to console
                Bukkit.getConsoleSender().sendMessage(message);
            }
        } else {
            // Optionally log that we are up to date
            // plugin.getLogger().info("SpigBan is up to date (v" + currentVersion + ")");
        }
    }

    /**
     * Fetches the latest version from GitHub.
     * @return the latest version string (without leading 'v') or null if failed
     */
    public String getLatestVersion() {
        return fetchLatestVersion();
    }

    /**
     * Fetches the latest version from GitHub (internal method).
     * @return the latest version string (without leading 'v') or null if failed
     */
    private String fetchLatestVersion() {
        try {
            URL url = new URL(resource);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "SpigBan Update Checker");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                plugin.getLogger().warning("Update check failed: HTTP " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            return extractTagName(json);
        } catch (Exception e) {
            plugin.getLogger().warning("Update check encountered an error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the tag_name from the GitHub API JSON response.
     */
    private String extractTagName(String json) {
        // Simple parsing: look for "tag_name": "value"
        int start = json.indexOf("\"tag_name\"");
        if (start == -1) return null;
        start = json.indexOf(':', start);
        if (start == -1) return null;
        start = json.indexOf('\"', start + 1);
        if (start == -1) return null;
        int end = json.indexOf('\"', start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }

    /**
     * Compares two version strings, returns true if latest is newer than current.
     * Handles versions with or without leading 'v'.
     */
    private boolean isNewerVersion(String latest, String current) {
        // Strip leading 'v' if present
        if (latest.startsWith("v")) latest = latest.substring(1);
        if (current.startsWith("v")) current = current.substring(1);

        // Split by dots and compare each part
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int l = i < latestParts.length ? parseIntSafe(latestParts[i]) : 0;
            int c = i < currentParts.length ? parseIntSafe(currentParts[i]) : 0;
            if (l < c) return false;
            if (l > c) return true;
        }
        return false; // equal
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            // If not a number, treat as 0
            return 0;
        }
    }

    private boolean configGetBoolean(String key, boolean defaultValue) {
        return plugin.getConfig().getBoolean(key, defaultValue);
    }

    /**
     * Checks if a newer version is available.
     * @return true if a newer version is available, false otherwise or if unable to check
     */
    public boolean isNewerVersionAvailable() {
        String latest = getLatestVersion();
        if (latest == null) {
            return false;
        }
        return isNewerVersion(latest, currentVersion);
    }
}