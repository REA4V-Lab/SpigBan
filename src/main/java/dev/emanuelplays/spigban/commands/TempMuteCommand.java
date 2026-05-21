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

public class TempMuteCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public TempMuteCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.tempmute")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /tempmute <player> <duration> [reason]"); return true;
        }
        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(plugin.getMessageUtil().get("invalid-duration", Map.of("duration", args[1]))); return true;
        }
        Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(args[0]);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", args[0]))); return true;
        }
        OfflinePlayer target = opt.get();
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";
        Punishment p = plugin.getPunishmentManager().tempMute(sender, target.getUniqueId(),
                target.getName() != null ? target.getName() : args[0], duration, reason);
        if (p == null) sender.sendMessage(plugin.getMessageUtil().get("mute-already", Map.of("player", args[0])));
        else sender.sendMessage(plugin.getMessageUtil().get("tempmute-success",
                Map.of("player", p.getPlayerName(), "case_id", p.getCaseId(), "duration", TimeUtil.formatDuration(duration))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("5m","15m","30m","1h","6h","12h","1d","3d","7d");
        return Collections.emptyList();
    }
}
