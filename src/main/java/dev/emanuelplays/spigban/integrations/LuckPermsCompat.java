package dev.emanuelplays.spigban.integrations;

import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.database.models.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Optional LuckPerms compatibility.
 *
 * Implemented without compile-time dependency on LuckPerms.
 */
public class LuckPermsCompat {

    private final boolean enabled;

    public LuckPermsCompat(boolean enabled) {
        this.enabled = enabled;
    }

    public void maybeSyncOnPunishmentApplied(Punishment punishment) {
        if (!enabled) return;
        if (punishment == null) return;
        if (!isLuckPermsPresent()) return;

        // Lightweight no-fail behavior:
        // add an attachment-based permission for online players.
        // (LuckPerms API sync can be added later if i want ob)
        PunishmentType type = punishment.getType();
        if (type == null) return;
        String node = permissionNodeFor(type);
        if (node == null) return;

        OfflinePlayer offline = Bukkit.getOfflinePlayer(punishment.getPlayerUuid());
        if (!offline.isOnline()) return;
        Player online = offline.getPlayer();
        if (online == null) return;

        Plugin lp = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (lp == null) return;

        online.addAttachment(lp, node, true);
    }

    private boolean isLuckPermsPresent() {
        try {
            Class.forName("net.luckperms.api.LuckPerms");
            return true;
        } catch (Throwable ignored) {
            return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        }
    }

    private String permissionNodeFor(PunishmentType type) {
        if (type.isBan()) return "spigban.banned";
        if (type.isMute()) return "spigban.muted";
        return null;
    }
}


