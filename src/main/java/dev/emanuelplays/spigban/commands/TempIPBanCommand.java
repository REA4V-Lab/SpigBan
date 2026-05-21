package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
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

public class TempIPBanCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public TempIPBanCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.tempipban")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /tempipban <player|ip> <duration> [reason]"); return true;
        }
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(plugin.getMessageUtil().get("invalid-duration", Map.of("duration", args[1]))); return true;
        }
        String targetInput = args[0];
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";
        String ip = null;
        UUID uuid = null;
        String resolvedName = targetInput;

        if (targetInput.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            ip = targetInput;
            uuid = UUID.nameUUIDFromBytes(("IP:" + ip).getBytes());
            resolvedName = ip;
        } else {
            Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(targetInput);
            if (opt.isEmpty()) {
                sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", targetInput))); return true;
            }
            OfflinePlayer target = opt.get();
            uuid = target.getUniqueId();
            resolvedName = target.getName() != null ? target.getName() : targetInput;
            Player online = plugin.getServer().getPlayer(uuid);
            if (online != null && online.getAddress() != null) ip = online.getAddress().getAddress().getHostAddress();
            if (ip == null) {
                sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cPlayer must be online to temp-IP-ban by name."); return true;
            }
        }

        Punishment p = plugin.getPunishmentManager().tempIpBan(sender, uuid, resolvedName, ip, duration, reason);
        if (p == null) sender.sendMessage(plugin.getMessageUtil().get("ban-ip-already", Map.of("player", resolvedName)));
        else sender.sendMessage(plugin.getMessageUtil().get("tempipban-success",
                Map.of("player", p.getPlayerName(), "case_id", p.getCaseId(), "duration", TimeUtil.formatDuration(duration))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("30m","1h","6h","12h","1d","3d","7d");
        return Collections.emptyList();
    }
}
