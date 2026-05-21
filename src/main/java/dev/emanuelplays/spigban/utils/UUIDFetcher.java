package dev.emanuelplays.spigban.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves player names and UUIDs, supporting both online and offline players.
 */
public class UUIDFetcher {

    /**
     * Attempts to find an OfflinePlayer by name (case-insensitive).
     * Checks online players first, then the server's known offline cache.
     */
    public static Optional<OfflinePlayer> getOfflinePlayer(String name) {
        // Check online players first (fastest)
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return Optional.of(online);

        // Check offline players cached by the server
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);

        // Only return if we have UUID evidence they've played before
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return Optional.of(offline);
        }

        // Last resort: iterate all offline players (may be slow on large servers)
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(name)) {
                return Optional.of(op);
            }
        }

        return Optional.empty();
    }

    /**
     * Gets a player name from a UUID, checking online players first.
     */
    public static String getName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8) + "...";
    }

    /**
     * Returns the online Player if they are currently on the server.
     */
    public static Optional<Player> getOnlinePlayer(String name) {
        return Optional.ofNullable(Bukkit.getPlayerExact(name));
    }

    private UUIDFetcher() {
    }
}
