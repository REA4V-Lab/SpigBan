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

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public MuteCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.mute")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /mute <player> [reason]"); return true;
        }
        String targetName = args[0];
        Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(targetName);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", targetName))); return true;
        }
        OfflinePlayer target = opt.get();
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        Punishment p = plugin.getPunishmentManager().mute(sender, target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName, reason);
        if (p == null) sender.sendMessage(plugin.getMessageUtil().get("mute-already", Map.of("player", targetName)));
        else sender.sendMessage(plugin.getMessageUtil().get("mute-success",
                Map.of("player", p.getPlayerName(), "case_id", p.getCaseId())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
