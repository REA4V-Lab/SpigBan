package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class BanListCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public BanListCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.banlist")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        int page = 1;
        if (args.length > 0) {
            try { page = Math.max(1, Integer.parseInt(args[0])); }
            catch (NumberFormatException e) { sender.sendMessage(plugin.getMessageUtil().get("invalid-page")); return true; }
        }
        int perPage = plugin.getConfig().getInt("pagination.entries-per-page", 10);
        int total = plugin.getDatabaseManager().countActiveBans();

        if (total == 0) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§7There are no active bans.");
            return true;
        }

        int totalPages = (int) Math.ceil((double) total / perPage);
        page = Math.min(page, totalPages);
        List<Punishment> bans = plugin.getDatabaseManager().getActiveBans(perPage, (page - 1) * perPage);

        sender.sendMessage(plugin.getMessageUtil().getRaw("banlist-header",
                Map.of("page", String.valueOf(page), "total_pages", String.valueOf(totalPages))));

        for (Punishment p : bans) {
            String expires = p.isPermanent() ? "§4Never" : TimeUtil.formatDurationLong(p.getRemainingTime()) + " left";
            sender.sendMessage(plugin.getMessageUtil().getRaw("banlist-entry",
                    Map.of("case_id", p.getCaseId(), "player", p.getPlayerName(),
                           "staff", p.getStaffName(), "reason",
                           p.getReason().length() > 28 ? p.getReason().substring(0, 25) + "..." : p.getReason(),
                           "expires", expires)));
        }

        sender.sendMessage(MessageUtil.colorize("  §8Total: §f" + total + " §8active ban(s)."));
        if (page < totalPages) sender.sendMessage(MessageUtil.colorize("  §7Next: §b/banlist " + (page + 1)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
