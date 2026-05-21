package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.managers.CaseManager;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

/**
 * /case <caseId> — Displays detailed info about a punishment case.
 *
 * Example output:
 * ──────── Case SPGB-A1B2C3 ────────
 *   Player : Steve (uuid...)
 *   Staff  : EmanuelPlays
 *   Type   : Temp Ban
 *   Reason : Hacking
 *   Date   : 01/06/2025 14:32:00
 *   Expires: 08/06/2025 14:32:00 (in 6 days, 23 hours)
 *   Status : ACTIVE
 *   IP     : 192.168.1.1  (only for IP bans)
 *   Notes  : (none)
 * ──────────────────────────────────
 */
public class CaseCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public CaseCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.case")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /case <caseId>"); return true;
        }

        String caseId = CaseManager.normalize(args[0]);
        Optional<Punishment> opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);

        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("case-not-found", Map.of("case_id", caseId)));
            return true;
        }

        Punishment p = opt.get();

        // Auto-deactivate if expired but still marked active
        if (p.isActive() && p.isExpired()) {
            plugin.getDatabaseManager().deactivatePunishment(p.getCaseId());
            p.setActive(false);
        }

        String typeColor = plugin.getMessageUtil().getRaw("case-type-" + p.getType().getMessageKey());
        String statusStr;
        if (!p.isActive()) {
            statusStr = p.isExpired()
                    ? plugin.getMessageUtil().getRaw("case-status-expired")
                    : plugin.getMessageUtil().getRaw("case-status-inactive");
        } else {
            statusStr = plugin.getMessageUtil().getRaw("case-status-active");
        }

        // Active warn count for this player (contextual)
        int warnCount = p.getType().isWarn()
                ? plugin.getDatabaseManager().countActiveWarnings(p.getPlayerUuid())
                : -1;

        // Build the case display
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-header", Map.of("case_id", p.getCaseId())));
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-player",
                Map.of("player", p.getPlayerName(), "uuid", p.getPlayerUuid().toString())));
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-staff",
                Map.of("staff", p.getStaffName())));
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-type",
                Map.of("type_color", typeColor, "type", p.getType().getDisplayName())));
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-reason",
                Map.of("reason", p.getReason())));
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-date",
                Map.of("date", TimeUtil.formatDate(p.getStartTime()))));
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-expires",
                Map.of("expires", TimeUtil.formatExpiry(p.getEndTime()))));
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-status",
                Map.of("status", statusStr)));

        // Duration line (only for timed punishments)
        if (p.getType().isTemp() || (!p.isPermanent() && p.getEndTime() > 0)) {
            long total = p.getEndTime() - p.getStartTime();
            sender.sendMessage(MessageUtil.colorize("  §7Duration§8: §f" + TimeUtil.formatDurationLong(total)));
        }

        // Remaining time (if still active)
        if (p.isActive() && !p.isExpired() && !p.isPermanent()) {
            sender.sendMessage(MessageUtil.colorize("  §7Remaining§8: §a" + TimeUtil.formatDurationLong(p.getRemainingTime())));
        }

        // IP address (IP bans only)
        if (p.getType().isIPBan() && p.getIpAddress() != null) {
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-ip",
                    Map.of("ip", p.getIpAddress())));
        }

        // Warn count context
        if (warnCount >= 0) {
            sender.sendMessage(MessageUtil.colorize("  §7Active Warns§8: §f" + warnCount));
        }

        // Notes
        String notes = p.getNotes() != null && !p.getNotes().isEmpty() ? p.getNotes() : "(none)";
        sender.sendMessage(plugin.getMessageUtil().getRaw("case-notes", Map.of("notes", notes)));

        sender.sendMessage(plugin.getMessageUtil().getRaw("case-footer"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
