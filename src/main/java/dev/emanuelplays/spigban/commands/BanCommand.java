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

/**
 * Ban command implementation.
 */
public class BanCommand extends BasePunishmentCommand {

    public BanCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.ban")) return true;
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.colorize(
                    getPrefix() + "§cUsage: /ban <player> [reason] [reason]"));
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
        Punishment p = plugin.getPunishmentManager().ban(sender, target.getUniqueId(),
                target.getName() != null ? target.getName() : targetName, reason);
        if (p == null) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("ban-already",
                            Map.of("player", targetName))));
        } else {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("ban-success",
                            Map.of("player", p.getPlayerName(), "case_id", p.getCaseId()))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return getOnlinePlayerNames(args[0]);
        if (args.length == 2) {
            var sec = plugin.getConfig().getConfigurationSection("reason-templates");
            if (sec != null) return getReasonTemplateKeys(args[1]);
        }
        return Collections.emptyList();
    }
}