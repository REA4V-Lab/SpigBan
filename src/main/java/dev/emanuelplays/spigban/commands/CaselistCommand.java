package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CaselistCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;

    public CaselistCommand(SpigBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

        for (Punishment p : punishments) {
            String typeColor = plugin.getMessageUtil().getRaw("case-type-" + p.getType().getMessageKey());
            String statusMark = p.isEffective() ? "§a●" : "§c●";

            String expiresStr;
            if (p.isPermanent()) {
                expiresStr = "§4Perm";
            } else if (p.isExpired()) {
                expiresStr = "§8Expired";
            } else {
                expiresStr = TimeUtil.formatDurationLong(p.getRemainingTime()) + " left";
            }

            sender.sendMessage(plugin.getMessageUtil().getRaw("caselist-entry", Map.of(
                    "case_id", p.getCaseId(),
                    "player", p.getPlayerName(),
                    "staff", p.getStaffName(),
                    "type_color", typeColor,
                    "type", p.getType().getDisplayName(),
                    "reason", p.getReason().length() > 32 ? p.getReason().substring(0, 29) + "..." : p.getReason(),
                    "expires", expiresStr,
                    "status", statusMark
            )));
        }

        sender.sendMessage(plugin.getMessageUtil().getRaw("caselist-footer", Map.of(
                "total", String.valueOf(total)
        )));

        if (page < totalPages) {
            sender.sendMessage(MessageUtil.colorize("  §7Next: §b/caselist " + (page + 1)));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}