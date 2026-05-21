package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.utils.UUIDFetcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public UnbanCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.unban")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /unban <player>"); return true;
        }
        String targetName = args[0];
        Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(targetName);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", targetName))); return true;
        }
        OfflinePlayer target = opt.get();
        String resolvedName = target.getName() != null ? target.getName() : targetName;
        boolean success = plugin.getPunishmentManager().unban(sender, target.getUniqueId(), resolvedName);
        if (!success) sender.sendMessage(plugin.getMessageUtil().get("unban-not-banned", Map.of("player", resolvedName)));
        else sender.sendMessage(plugin.getMessageUtil().get("unban-success", Map.of("player", resolvedName)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest currently banned players (active bans, first page)
            return plugin.getDatabaseManager().getActiveBans(20, 0).stream()
                    .map(p -> p.getPlayerName())
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
