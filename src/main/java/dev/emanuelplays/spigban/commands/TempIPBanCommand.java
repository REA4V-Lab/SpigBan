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
import java.util.UUID;
import java.util.stream.Collectors;

public class TempIPBanCommand extends BaseTempPunishmentCommand {

    public TempIPBanCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.tempipban")) return true;
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(
                    getPrefix() + "§cUsage: /tempipban <player|ip> <duration> [reason]"));
            return true;
        }
        long duration = parseDuration(args[1], sender, false);
        if (duration < 0) return true; // Duration parsing failed or permanent requested
        String targetInput = args[0];
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";
        reason = resolveReason(reason);
        String ip = null;
        UUID uuid = null;
        String resolvedName = targetInput;

        if (targetInput.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            ip = targetInput;
            uuid = UUID.nameUUIDFromBytes(("IP:" + ip).getBytes());
            resolvedName = ip;
        } else {
            Optional<OfflinePlayer> opt = resolveTarget(sender, targetInput);
            if (opt.isEmpty()) return true;
            OfflinePlayer target = opt.get();
            uuid = target.getUniqueId();
            resolvedName = target.getName() != null ? target.getName() : targetInput;
            Player online = plugin.getServer().getPlayer(uuid);
            if (online != null && online.getAddress() != null) ip = online.getAddress().getAddress().getHostAddress();
            // For IP bans, we can proceed even if offline (use placeholder IP)
            if (ip == null) {
                ip = "0.0.0.0"; // Placeholder for offline IP banning
            }
        }

        Punishment p = plugin.getPunishmentManager().tempIpBan(sender, uuid, resolvedName, ip, duration, reason);
        if (p == null) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("ban-ip-already",
                            Map.of("player", resolvedName))));
        } else {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("tempipban-success",
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
