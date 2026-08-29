package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /caselist — View all punishment cases with subcommands for info and deletion.
 */
public class CaselistCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;

    public CaselistCommand(SpigBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle subcommands
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("info") && args.length >= 2) {
                return showCaseInfo(sender, args[1]);
            }
            if (args[0].equalsIgnoreCase("delete") && args.length >= 2) {
                return deleteCase(sender, args[1]);
            }
        }

        // Default: list page
        if (!sender.hasPermission("spigban.caselist")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessageUtil().get("invalid-page"));
                return true;
            }
        }

        int perPage = plugin.getConfig().getInt("pagination.entries-per-page", 10);
        int total = plugin.getDatabaseManager().countTotal();
        if (total == 0) {
            sender.sendMessage(plugin.getMessageUtil().getRaw("caselist-empty"));
            return true;
        }

        int totalPages = (int) Math.ceil((double) total / perPage);
        page = Math.min(page, totalPages);

        int offset = (page - 1) * perPage;
        List<Punishment> punishments = plugin.getDatabaseManager().getAllPunishments(perPage, offset);

        sender.sendMessage(plugin.getMessageUtil().getRaw("caselist-header", Map.of(
                "page", String.valueOf(page),
                "total_pages", String.valueOf(totalPages)
        )));

        String infoButtonText = plugin.getMessageUtil().getRaw("info-button");
        String deleteButtonText = plugin.getMessageUtil().getRaw("delete-button");

        for (Punishment p : punishments) {
            String raw = plugin.getMessageUtil().getRaw("caselist-entry", Map.of(
                    "case_id", p.getCaseId(),
                    "player", p.getPlayerName(),
                    "staff", p.getStaffName(),
                    "type_color", plugin.getMessageUtil().getRaw("case-type-" + p.getType().getMessageKey()),
                    "type", p.getType().getDisplayName(),
                    "status", p.isEffective() ? "§a●" : "§c●"
            ));

            // Replace button placeholders with actual button texts
            raw = raw.replace("{info_button}", infoButtonText)
                    .replace("{delete_button}", deleteButtonText);

            sender.sendMessage(raw);
        }

        sender.sendMessage(plugin.getMessageUtil().getRaw("caselist-footer", Map.of(
                "total", String.valueOf(total)
        )));

        if (page < totalPages) {
            sender.sendMessage(MessageUtil.colorize("  §7Next: §b/caselist " + (page + 1)));
        }

        return true;
    }

    
    /**
     * Shows detailed info for a case.
     */
    private boolean showCaseInfo(CommandSender sender, String caseId) {
        caseId = plugin.getCaseManager().normalize(caseId);
        var opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("casenotfound", Map.of("case_id", caseId)));
            return true;
        }
        Punishment p = opt.get();

        if (sender instanceof Player player) {
            sender.sendMessage("");
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-header", Map.of("case_id", p.getCaseId())));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-player",
                    Map.of("player", p.getPlayerName(), "uuid", p.getPlayerUuid().toString())));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-staff",
                    Map.of("staff", p.getStaffName())));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-type",
                    Map.of("type_color", plugin.getMessageUtil().getRaw("case-type-" + p.getType().getMessageKey()),
                            "type", p.getType().getDisplayName())));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-reason",
                    Map.of("reason", p.getReason())));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-date",
                    Map.of("date", TimeUtil.formatDate(p.getStartTime()))));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-expires",
                    Map.of("expires", p.isPermanent() ? "Permanent" : TimeUtil.formatDate(p.getEndTime()))));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-status",
                    Map.of("status", p.isEffective() ? "ACTIVE" : (p.isExpired() ? "EXPIRED" : "INACTIVE"))));
            if (!p.isPermanent() && !p.isExpired()) {
                sender.sendMessage(MessageUtil.colorize("  §7Duration§8: §f" + TimeUtil.formatDurationLong(p.getEndTime() - p.getStartTime())));
                sender.sendMessage(MessageUtil.colorize("  §7Remaining§8: §a" + TimeUtil.formatDurationLong(p.getRemainingTime())));
            }
            if (p.getIpAddress() != null) {
                sender.sendMessage(plugin.getMessageUtil().getRaw("case-ip",
                        Map.of("ip", p.getIpAddress())));
            }
            String notes = p.getNotes() != null && !p.getNotes().isEmpty() ? p.getNotes() : "(none)";
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-notes", Map.of("notes", notes)));
            sender.sendMessage(plugin.getMessageUtil().getRaw("case-footer"));
            sender.sendMessage("");
        } else {
            sender.sendMessage("=== Case Details ===");
            sender.sendMessage("Case ID: " + p.getCaseId());
            sender.sendMessage("Player: " + p.getPlayerName() + " (" + p.getPlayerUuid() + ")");
            sender.sendMessage("Staff: " + p.getStaffName());
            sender.sendMessage("Type: " + p.getType().getDisplayName());
            sender.sendMessage("Reason: " + p.getReason());
            sender.sendMessage("Date: " + TimeUtil.formatDate(p.getStartTime()));
            sender.sendMessage("Expires: " + (p.isPermanent() ? "Permanent" : TimeUtil.formatDate(p.getEndTime())));
            sender.sendMessage("Status: " + (p.isEffective() ? "Active" : (p.isExpired() ? "Expired" : "Inactive")));
            if (!p.isPermanent() && !p.isExpired()) {
                sender.sendMessage("Duration: " + TimeUtil.formatDurationLong(p.getEndTime() - p.getStartTime()));
                sender.sendMessage("Time Left: " + TimeUtil.formatDurationLong(p.getRemainingTime()));
            }
            if (p.getIpAddress() != null) {
                sender.sendMessage("IP: " + p.getIpAddress());
            }
            sender.sendMessage("Notes: " + (p.getNotes() != null ? p.getNotes() : "(none)"));
            sender.sendMessage("====================");
        }
        return true;
    }

    /**
     * Deletes a case by its ID.
     */
    private boolean deleteCase(CommandSender sender, String caseId) {
        caseId = plugin.getCaseManager().normalize(caseId);
        if (!sender.hasPermission("spigban.caselist.delete")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }
        var opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("casenotfound", Map.of("case_id", caseId)));
            return true;
        }
        Punishment p = opt.get();
        boolean removed = plugin.getPunishmentManager().unwarnByCase(sender, caseId);
        if (removed) {
            sender.sendMessage(plugin.getMessageUtil().get("casesdeleted", Map.of("case_id", caseId)));
        } else {
            sender.sendMessage(plugin.getMessageUtil().get("casenotdeleted", Map.of("case_id", caseId)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest subcommands: info, delete, or a page number
            return List.of("info", "delete");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            // Suggest case IDs for info (we'll need a method to get case IDs)
            return plugin.getDatabaseManager().getAllCaseIds(10);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            // Suggest case IDs for delete
            return plugin.getDatabaseManager().getAllCaseIds(10);
        }
        return Collections.emptyList();
    }
}