package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import dev.emanuelplays.spigban.utils.UUIDFetcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class HistoryCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public HistoryCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.history")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /history <player> [page]"); return true;
        }

        String targetName = args[0];
        int page = 1;
        if (args.length > 1) {
            try { page = Math.max(1, Integer.parseInt(args[1])); }
            catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessageUtil().get("invalid-page")); return true;
            }
        }

        Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(targetName);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", targetName))); return true;
        }

        OfflinePlayer target = opt.get();
        String resolvedName = target.getName() != null ? target.getName() : targetName;
        int perPage = plugin.getConfig().getInt("pagination.entries-per-page", 10);
        int total = plugin.getDatabaseManager().countPlayerHistory(target.getUniqueId());

        if (total == 0) {
            sender.sendMessage(plugin.getMessageUtil().get("history-empty", Map.of("player", resolvedName)));
            return true;
        }

        int totalPages = (int) Math.ceil((double) total / perPage);
        page = Math.min(page, totalPages);
        int offset = (page - 1) * perPage;

        List<Punishment> punishments = plugin.getDatabaseManager()
                .getPlayerHistory(target.getUniqueId(), perPage, offset);

        // Active warning count
        int activeWarns = plugin.getDatabaseManager().countActiveWarnings(target.getUniqueId());

        sender.sendMessage(plugin.getMessageUtil().getRaw("history-header",
                Map.of("player", resolvedName, "page", String.valueOf(page),
                        "total_pages", String.valueOf(totalPages))));

        // Summary line
        sender.sendMessage(MessageUtil.colorize(
                "  §7Active Warnings§8: §f" + activeWarns + "  §8|  §7Total Punishments§8: §f" + total));
        sender.sendMessage(MessageUtil.colorize("  §8§m" + "─".repeat(42)));

        for (Punishment p : punishments) {
            String typeColor = plugin.getMessageUtil().getRaw("case-type-" + p.getType().getMessageKey());
            String statusMark = p.isEffective() ? "§a●" : "§c●";
            String expiresStr = p.isPermanent() ? "§4Perm"
                    : (p.isExpired() ? "§8Expired" : "§a" + TimeUtil.formatDurationLong(p.getRemainingTime()) + " left");

            sender.sendMessage(plugin.getMessageUtil().getRaw("history-entry",
                    Map.of(
                        "case_id",    p.getCaseId(),
                        "date",       TimeUtil.formatDate(p.getStartTime()),
                        "type_color", typeColor,
                        "type",       p.getType().getDisplayName(),
                        "staff",      p.getStaffName(),
                        "reason",     p.getReason().length() > 32 ? p.getReason().substring(0, 29) + "..." : p.getReason()
                    )) + " §8| " + expiresStr + " " + statusMark);
        }

        sender.sendMessage(plugin.getMessageUtil().getRaw("history-footer",
                Map.of("total", String.valueOf(total))));

        if (page < totalPages) {
            sender.sendMessage(MessageUtil.colorize(
                    "  §7Next page: §b/history " + resolvedName + " " + (page + 1)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
