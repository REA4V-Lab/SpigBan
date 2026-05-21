package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class KickCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public KickCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.kick")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /kick <player> [reason]"); return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-online", Map.of("player", args[0]))); return true;
        }
        if (target.hasPermission("spigban.bypass")) {
            sender.sendMessage(plugin.getMessageUtil().get("target-bypasses", Map.of("player", target.getName()))); return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        Punishment p = plugin.getPunishmentManager().kick(sender, target, reason);
        sender.sendMessage(plugin.getMessageUtil().get("kick-success", Map.of("player", p.getPlayerName())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
