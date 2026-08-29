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

public class WarnCommand extends BasePunishmentCommand {

    public WarnCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.warn")) return true;
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(
                    getPrefix() + "§cUsage: /warn <player> <reason>"));
            return true;
        }
        String targetName = args[0];
        if (!checkSelfTarget(sender, targetName)) return true;
        if (!checkBypass(sender, targetName)) return true;
        Optional<OfflinePlayer> opt = resolveTarget(sender, targetName);
        if (opt.isEmpty()) return true;
        OfflinePlayer target = opt.get();
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        reason = resolveReason(reason);
        Punishment p = plugin.getPunishmentManager().warn(sender, target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName, reason);
        int warnCount = plugin.getDatabaseManager().countActiveWarnings(target.getUniqueId());
        sender.sendMessage(MessageUtil.colorize(
                plugin.getMessageUtil().get("warn-success",
                        Map.of("player", p.getPlayerName(), "case_id", p.getCaseId(), "warn_count", String.valueOf(warnCount)))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return getOnlinePlayerNames(args[0]);
        if (args.length == 2) return getReasonTemplateKeys(args[1]);
        return Collections.emptyList();
    }
}
