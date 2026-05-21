package dev.emanuelplays.spigban.listeners;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LoginListener implements Listener {

    private final SpigBan plugin;

    public LoginListener(SpigBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("spigban.bypass")) return;

        UUID playerUuid = event.getPlayer().getUniqueId();
        String ip = event.getAddress().getHostAddress();

        Optional<Punishment> ban = plugin.getDatabaseManager().getActiveBan(playerUuid);
        if (ban.isPresent()) {
            Punishment p = ban.get();
            if (p.isExpired()) {
                plugin.getDatabaseManager().deactivatePunishment(p.getCaseId());
            } else {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, buildScreen(p));
                return;
            }
        }

        Optional<Punishment> ipBan = plugin.getDatabaseManager().getActiveIPBan(ip);
        if (ipBan.isPresent()) {
            Punishment p = ipBan.get();
            if (p.isExpired()) {
                plugin.getDatabaseManager().deactivatePunishment(p.getCaseId());
            } else {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, buildScreen(p));
            }
        }
    }

    private String buildScreen(Punishment p) {
        Map<String, String> ph = new HashMap<>();
        ph.put("reason",  p.getReason());
        ph.put("staff",   p.getStaffName());
        ph.put("case_id", p.getCaseId());
        ph.put("date",    TimeUtil.formatDate(p.getStartTime()));
        ph.put("expires", TimeUtil.formatExpiry(p.getEndTime()));
        return plugin.getMessageUtil().buildBanScreen(ph);
    }
}
