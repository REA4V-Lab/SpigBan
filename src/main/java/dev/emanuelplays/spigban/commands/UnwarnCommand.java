package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.managers.CaseManager;
import dev.emanuelplays.spigban.utils.UUIDFetcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class UnwarnCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    public UnwarnCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.unwarn")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /unwarn <caseId|player> [all]"); return true;
        }

        String input = args[0];
        boolean removeAll = args.length > 1 && args[1].equalsIgnoreCase("all");

        // If the input contains a dash it's likely a case ID (e.g. SPGB-ABC123)
        if (input.contains("-") && !removeAll) {
            String caseId = CaseManager.normalize(input);
            boolean removed = plugin.getPunishmentManager().unwarnByCase(sender, caseId);
            if (!removed) {
                sender.sendMessage(plugin.getMessageUtil().get("unwarn-not-found", Map.of("case_id", caseId)));
            } else {
                // Look up name from DB
                var opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);
                String name = opt.map(p -> p.getPlayerName()).orElse("Unknown");
                sender.sendMessage(plugin.getMessageUtil().get("unwarn-success",
                        Map.of("case_id", caseId, "player", name)));
            }
        } else {
            // Treat as player name — remove all active warns
            Optional<OfflinePlayer> opt = UUIDFetcher.getOfflinePlayer(input);
            if (opt.isEmpty()) {
                sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", input))); return true;
            }
            OfflinePlayer target = opt.get();
            String resolvedName = target.getName() != null ? target.getName() : input;
            int count = plugin.getPunishmentManager().unwarnAll(target.getUniqueId());
            if (count == 0) sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§f" + resolvedName + " §7has no active warnings.");
            else sender.sendMessage(plugin.getMessageUtil().get("unwarn-all-success",
                    Map.of("count", String.valueOf(count), "player", resolvedName)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return plugin.getServer().getOnlinePlayers().stream()
                .map(p -> p.getName())
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        return Collections.emptyList();
    }
}
