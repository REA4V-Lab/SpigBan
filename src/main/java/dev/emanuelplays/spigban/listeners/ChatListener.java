package dev.emanuelplays.spigban.listeners;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Blocks chat and certain commands for muted players.
 */
public class ChatListener implements Listener {

    private final SpigBan plugin;

    // Commands that muted players are still allowed to run
    private static final java.util.Set<String> ALLOWED_COMMANDS = java.util.Set.of(
            "/help", "/rules", "/spawn", "/tp", "/home", "/homes", "/warp",
            "/msg", "/whisper", "/tell", "/r", "/reply"
    );

    public ChatListener(SpigBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer().hasPermission("spigban.bypass")) return;

        UUID uuid = event.getPlayer().getUniqueId();
        Optional<Punishment> mute = plugin.getDatabaseManager().getActiveMute(uuid);

        if (mute.isEmpty()) return;

        Punishment p = mute.get();
        if (p.isExpired()) {
            plugin.getDatabaseManager().deactivatePunishment(p.getCaseId());
            return;
        }

        event.setCancelled(true);

        Map<String, String> ph = new HashMap<>();
        ph.put("reason",  p.getReason());
        ph.put("staff",   p.getStaffName());
        ph.put("case_id", p.getCaseId());
        ph.put("expires", TimeUtil.formatExpiry(p.getEndTime()));

        event.getPlayer().sendMessage(plugin.getMessageUtil().get("muted-chat-attempt", ph));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("spigban.bypass")) return;

        String msg = event.getMessage().toLowerCase();
        // Only block chat-related commands (e.g. /say, /me)
        if (!msg.startsWith("/say ") && !msg.startsWith("/me ")) return;

        UUID uuid = event.getPlayer().getUniqueId();
        Optional<Punishment> mute = plugin.getDatabaseManager().getActiveMute(uuid);

        if (mute.isEmpty()) return;

        Punishment p = mute.get();
        if (p.isExpired()) {
            plugin.getDatabaseManager().deactivatePunishment(p.getCaseId());
            return;
        }

        event.setCancelled(true);

        Map<String, String> ph = new HashMap<>();
        ph.put("reason",  p.getReason());
        ph.put("staff",   p.getStaffName());
        ph.put("case_id", p.getCaseId());
        ph.put("expires", TimeUtil.formatExpiry(p.getEndTime()));

        event.getPlayer().sendMessage(plugin.getMessageUtil().get("muted-chat-attempt", ph));
    }
}
