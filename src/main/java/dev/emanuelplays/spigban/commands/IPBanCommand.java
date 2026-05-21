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

public class IPBanCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public IPBanCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.ipban")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /ipban <player|ip> [reason]"); return true;
        }
        String targetInput = args[0];
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

        // Determine target UUID/name and IP
        String ip = null;
        UUID uuid = null;
        String resolvedName = targetInput;

        // Check if it looks like a raw IP address
        if (targetInput.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            ip = targetInput;
            // We still need a UUID for the record – use a placeholder
            uuid = UUID.nameUUIDFromBytes(("IP:" + ip).getBytes());
            resolvedName = ip;
        } else {
            // It's a player name
            Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(targetInput);
            if (opt.isEmpty()) {
                sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", targetInput))); return true;
            }
            OfflinePlayer target = opt.get();
            uuid = target.getUniqueId();
            resolvedName = target.getName() != null ? target.getName() : targetInput;
            // Resolve IP from online player
            Player online = plugin.getServer().getPlayer(uuid);
            if (online != null && online.getAddress() != null) {
                ip = online.getAddress().getAddress().getHostAddress();
            }
            if (ip == null) {
                sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cCould not resolve IP for §f" + resolvedName
                        + "§c. They must be online, or provide the IP directly.");
                return true;
            }
        }

        Punishment p = plugin.getPunishmentManager().ipBan(sender, uuid, resolvedName, ip, reason);
        if (p == null) sender.sendMessage(plugin.getMessageUtil().get("ban-ip-already", Map.of("player", resolvedName)));
        else sender.sendMessage(plugin.getMessageUtil().get("ban-ip-success", Map.of("player", p.getPlayerName(), "case_id", p.getCaseId())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
