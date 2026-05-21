package dev.emanuelplays.spigban.utils;

import dev.emanuelplays.spigban.SpigBan;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles loading and formatting of all user-facing messages.
 */
public class MessageUtil {

    private final SpigBan plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageUtil(SpigBan plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File msgFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!msgFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(msgFile);
        // Fallback defaults from resource
        var defaults = plugin.getResource("messages.yml");
        if (defaults != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaults)));
        }
        prefix = colorize(messages.getString("prefix", "&8[&bSpig&3Ban&8] "));
    }

    /**
     * Returns a raw (un-prefixed) message with color codes translated.
     */
    public String getRaw(String key) {
        String val = messages.getString(key);
        if (val == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return "§c[MISSING: " + key + "]";
        }
        return colorize(val);
    }

    /**
     * Returns a prefixed message.
     */
    public String get(String key) {
        return prefix + getRaw(key);
    }

    /**
     * Returns a prefixed message with placeholders applied.
     *
     * @param key          messages.yml key
     * @param placeholders Map of {placeholder} → value pairs
     */
    public String get(String key, Map<String, String> placeholders) {
        return prefix + format(getRaw(key), placeholders);
    }

    /**
     * Returns an un-prefixed message with placeholders applied.
     */
    public String getRaw(String key, Map<String, String> placeholders) {
        return format(getRaw(key), placeholders);
    }

    /**
     * Formats a list of strings (e.g., ban screen lines) with placeholders.
     */
    public List<String> getLines(String path, Map<String, String> placeholders) {
        List<String> raw = messages.getStringList(path);
        return raw.stream()
                .map(line -> colorize(format(line, placeholders)))
                .collect(Collectors.toList());
    }

    /**
     * Builds a multi-line ban screen from config.
     */
    public String buildBanScreen(Map<String, String> placeholders) {
        List<String> lines = plugin.getConfig().getStringList("ban-screen.lines");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(colorize(format(line, placeholders)));
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Builds a multi-line mute screen from config (used in chat denial message).
     */
    public String buildMuteScreen(Map<String, String> placeholders) {
        List<String> lines = plugin.getConfig().getStringList("mute-screen.lines");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(colorize(format(line, placeholders)));
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    public String getPrefix() {
        return prefix;
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private String format(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String val = entry.getValue() != null ? entry.getValue() : "";
            text = text.replace("{" + entry.getKey() + "}", val);
        }
        return text;
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String strip(String text) {
        return ChatColor.stripColor(colorize(text));
    }
}
