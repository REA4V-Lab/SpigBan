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

import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

public class MuteCommand extends BasePunishmentCommand {

    public MuteCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.mute")) return true;
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.colorize(
                    getPrefix() + "§cUsage: /mute <player> [reason]"));
            return true;
        }
        String targetName = args[0];
        if (!checkSelfTarget(sender, targetName)) return true;
        if (!checkBypass(sender, targetName)) return true;
        Optional<OfflinePlayer> opt = resolveTarget(sender, targetName);
        if (opt.isEmpty()) return true;
        OfflinePlayer target = opt.get();
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        reason = resolveReason(reason);
        Punishment p = plugin.getPunishmentManager().mute(sender, target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName, reason);
        if (p == null) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("mute-already",
                            Map.of("player", targetName))));
        } else {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("mute-success",
                            Map.of("player", p.getPlayerName(), "case_id", p.getCaseId()))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return getOnlinePlayerNames(args[0]);
        if (args.length == 2) return getReasonTemplateKeys(args[1]);
        return Collections.emptyList();
    }
}
