package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.commands.base.BaseTempPunishmentCommand;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import dev.emanuelplays.spigban.utils.UUIDFetcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

public class TempBanCommand extends BaseTempPunishmentCommand {

    public TempBanCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.tempban")) return true;
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(
                    getPrefix() + "§cUsage: /tempban <player> <duration> [reason]"));
            return true;
        }
        String targetName = args[0];
        if (!checkSelfTarget(sender, targetName)) return true;
        if (!checkBypass(sender, targetName)) return true;
        Optional<OfflinePlayer> opt = resolveTarget(sender, targetName);
        if (opt.isEmpty()) return true;
        OfflinePlayer target = opt.get();
        long duration = parseDuration(args[1], sender, false);
        if (duration < 0) return true; // Duration parsing failed or permanent requested
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";
        reason = resolveReason(reason);
        Punishment p = plugin.getPunishmentManager().tempBan(sender, target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName, duration, reason);
        if (p == null) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("ban-already",
                            Map.of("player", targetName))));
        } else {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("tempban-success",
                            Map.of("player", p.getPlayerName(), "case_id", p.getCaseId(),
                                    "duration", TimeUtil.formatDuration(duration)))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return getOnlinePlayerNames(args[0]);
        if (args.length == 2) return getDurationSuggestions();
        return Collections.emptyList();
    }
}
