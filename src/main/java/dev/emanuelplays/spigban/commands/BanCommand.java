package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.UUIDFetcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class BanCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;

    public BanCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.ban")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /ban <player> [reason]"); return true;
        }
        String targetName = args[0];
        if (sender instanceof Player p && p.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(plugin.getMessageUtil().get("self-target")); return true;
        }
        Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(targetName);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", targetName))); return true;
        }
        OfflinePlayer target = opt.get();
        if (target.isOnline() && target.getPlayer() != null && target.getPlayer().hasPermission("spigban.bypass")) {
            sender.sendMessage(plugin.getMessageUtil().get("target-bypasses",
                Map.of("player", target.getName() != null ? target.getName() : targetName))); return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        Punishment p = plugin.getPunishmentManager().ban(sender, target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName, reason);
        if (p == null) sender.sendMessage(plugin.getMessageUtil().get("ban-already", Map.of("player", targetName)));
        else sender.sendMessage(plugin.getMessageUtil().get("ban-success", Map.of("player", p.getPlayerName(), "case_id", p.getCaseId())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) {
            var sec = plugin.getConfig().getConfigurationSection("reason-templates");
            if (sec != null) return sec.getKeys(false).stream().map(k -> "#" + k)
                    .filter(k -> k.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
