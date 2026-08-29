package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.commands.base.BasePunishmentCommand;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.UUIDFetcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class KickCommand extends BasePunishmentCommand {

    public KickCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.kick")) return true;
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.colorize(
                    getPrefix() + "§cUsage: /kick <player> [reason]"));
            return true;
        }
        String targetName = args[0];
        if (!checkSelfTarget(sender, targetName)) return true;
        Optional<OfflinePlayer> opt = resolveTarget(sender, targetName);
        if (opt.isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("player-not-online",
                            Map.of("player", targetName))));
            return true;
        }
        OfflinePlayer target = opt.get();
        if (target.isOnline() && target.getPlayer() != null && target.getPlayer().hasPermission("spigban.bypass")) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("target-bypasses",
                            Map.of("player", target.getName()))));
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        reason = resolveReason(reason);
        Punishment p = plugin.getPunishmentManager().kick(sender,
                target.getPlayer() != null ? target.getPlayer() : null,
                reason);
        if (p != null) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("kick-success",
                            Map.of("player", p.getPlayerName()))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return getOnlinePlayerNames(args[0]);
        return Collections.emptyList();
    }
}
