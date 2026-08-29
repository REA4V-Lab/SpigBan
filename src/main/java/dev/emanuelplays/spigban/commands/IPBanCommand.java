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
import java.util.UUID;
import java.util.stream.Collectors;

public class IPBanCommand extends BasePunishmentCommand {

    public IPBanCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.ipban")) return true;
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.colorize(
                    getPrefix() + "§cUsage: /ipban <player|ip> [reason]"));
            return true;
        }
        String targetInput = args[0];
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        reason = resolveReason(reason);

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
            Optional<OfflinePlayer> opt = resolveTarget(sender, targetInput);
            if (opt.isEmpty()) return true;
            OfflinePlayer target = opt.get();
            uuid = target.getUniqueId();
            resolvedName = target.getName() != null ? target.getName() : targetInput;
            // Resolve IP from online player
            Player online = plugin.getServer().getPlayer(uuid);
            if (online != null && online.getAddress() != null) {
                ip = online.getAddress().getAddress().getHostAddress();
            }
            // For IP bans, we can proceed even if offline (use placeholder IP)
            if (ip == null) {
                ip = "0.0.0.0"; // Placeholder for offline IP banning
            }
        }

        Punishment p = plugin.getPunishmentManager().ipBan(sender, uuid, resolvedName, ip, reason);
        if (p == null) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("ban-ip-already",
                            Map.of("player", resolvedName))));
        } else {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().get("ban-ip-success",
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
