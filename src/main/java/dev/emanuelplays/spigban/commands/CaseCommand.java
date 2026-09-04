package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.commands.base.BaseCommand;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.managers.CaseManager;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.Optional;

/**
 * /case <caseId> [delete/save/close/reason/notes] — Displays detailed info about a punishment case or performs actions.
 *
 * Examples:
 *   /case SPGB-A1B2C3          — Shows case details
 *   /case SPGB-A1B2C3 delete   — Deletes the case
 *   /case SPGB-A1B2C3 close    — Closes (deactivates) the case
 *   /case SPGB-A1B2C3 save     — Placeholder for saving (not implemented)
 *   /case SPGB-A1B2C3 reason <new reason> — Updates the reason for the case
 *   /case SPGB-A1B2C3 notes <new notes>   — Updates the notes for the case
 */
public class CaseCommand extends BaseCommand {

    public CaseCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.case")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /case <caseId> [delete/save/close/reason <text>/notes <text>]");
            return true;
        }

        String caseId = CaseManager.normalize(args[0]);
        Optional<Punishment> opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);

        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("case-not-found", Map.of("case_id", caseId)));
            return true;
        }

        Punishment punishment = opt.get();

        // If only caseId is provided, show case details
        if (args.length == 1) {
            showCaseDetails(sender, punishment);
            return true;
        }

        // Handle subcommands with exactly two arguments (caseId and subcommand)
        if (args.length == 2) {
            String subcmd = args[1].toLowerCase();
            switch (subcmd) {
                case "delete":
                    return handleDeleteCase(sender, caseId);
                case "close":
                    return handleCloseCase(sender, caseId);
                case "save":
                    return handleSaveCase(sender, caseId);
                default:
                    sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUnknown subcommand. Use: delete, save, close, reason <text>, notes <text>");
                    return true;
            }
        }

        // Handle subcommands with three arguments (caseId, subcommand, value)
        if (args.length == 3) {
            String subcmd = args[1].toLowerCase();
            String value = args[2];
            switch (subcmd) {
                case "reason":
                    return handleReasonCase(sender, caseId, value);
                case "notes":
                    return handleNotesCase(sender, caseId, value);
                default:
                    sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUnknown subcommand. Use: delete, save, close, reason <text>, notes <text>");
                    return true;
            }
        }

        // Too many arguments
        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /case <caseId> [delete/save/close/reason <text>/notes <text>]");
        return true;
    }

    /**
     * Shows detailed case information.
     */
    private void showCaseDetails(CommandSender sender, Punishment p) {
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
    }

    /**
     * Handles deleting a case.
     */
    private boolean handleDeleteCase(CommandSender sender, String caseId) {
        boolean removed = plugin.getPunishmentManager().deletePunishmentByCaseId(caseId);
        if (removed) {
            sender.sendMessage(plugin.getMessageUtil().get("casesdeleted", Map.of("case_id", caseId)));
        } else {
            sender.sendMessage(plugin.getMessageUtil().get("casenotdeleted", Map.of("case_id", caseId)));
        }
        return true;
    }

    /**
     * Handles closing (deactivating) a case.
     */
    private boolean handleCloseCase(CommandSender sender, String caseId) {
        plugin.getDatabaseManager().deactivatePunishment(caseId);
        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "Case §e" + caseId + " §ahas been closed.");
        return true;
    }

    /**
     * Handles saving a case (placeholder).
     */
    private boolean handleSaveCase(CommandSender sender, String caseId) {
        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "Saving case §e" + caseId + " §fis not yet implemented.");
        return true;
    }

    /**
     * Handles updating the reason for a case.
     */
    private boolean handleReasonCase(CommandSender sender, String caseId, String newReason) {
        Optional<Punishment> opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("case-not-found", Map.of("case_id", caseId)));
            return true;
        }

        Punishment punishment = opt.get();
        punishment.setReason(newReason);
        plugin.getDatabaseManager().updatePunishment(punishment);
        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "Reason for case §e" + caseId + " §ahas been updated.");
        return true;
    }

    /**
     * Handles updating the notes for a case.
     */
    private boolean handleNotesCase(CommandSender sender, String caseId, String newNotes) {
        Optional<Punishment> opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("case-not-found", Map.of("case_id", caseId)));
            return true;
        }

        Punishment punishment = opt.get();
        punishment.setNotes(newNotes);
        plugin.getDatabaseManager().updatePunishment(punishment);
        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "Notes for case §e" + caseId + " §ahave been updated.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // We could implement tab completion for case IDs here, but for simplicity we return empty.
        // If you want to add case ID tab completion, you would need to fetch case IDs from the database.
        return Collections.emptyList();
    }
}