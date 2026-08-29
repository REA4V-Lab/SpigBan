package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.UpdateChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /spigban <reload|info|purge [confirm]>
 */
public class SpigBanCommand implements CommandExecutor, TabCompleter {

    private final SpigBan plugin;
    private boolean awaitingPurgeConfirm = false;

    public SpigBanCommand(SpigBan plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spigban.admin")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission")); return true;
        }
        if (args.length == 0) {
            sendHelp(sender); return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                plugin.reloadConfig();
                plugin.getMessageUtil().reload();
                sender.sendMessage(plugin.getMessageUtil().get("plugin-reload"));
            }

            case "info" -> {
                int total  = plugin.getDatabaseManager().countTotal();
                int bans   = plugin.getDatabaseManager().countActiveBans();
                int mutes  = plugin.getDatabaseManager().countActiveMutes();
                String db  = plugin.getDatabaseManager().getDbType().toUpperCase();

                sender.sendMessage(plugin.getMessageUtil().getRaw("info-header"));
                sender.sendMessage(plugin.getMessageUtil().getRaw("info-version",
                        Map.of("version", plugin.getDescription().getVersion())));
                sender.sendMessage(plugin.getMessageUtil().getRaw("info-database", Map.of("database", db)));
                sender.sendMessage(plugin.getMessageUtil().getRaw("info-total-punishments",
                        Map.of("total", String.valueOf(total))));
                sender.sendMessage(plugin.getMessageUtil().getRaw("info-active-bans",
                        Map.of("bans", String.valueOf(bans))));
                sender.sendMessage(plugin.getMessageUtil().getRaw("info-active-mutes",
                        Map.of("mutes", String.valueOf(mutes))));
                sender.sendMessage(plugin.getMessageUtil().getRaw("info-footer"));
            }

            case "purge" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
                    int removed = plugin.getDatabaseManager().purgeInactive();
                    sender.sendMessage(plugin.getMessageUtil().getPrefix()
                            + "§aPurged §f" + removed + " §ainactive / expired punishment records.");
                    awaitingPurgeConfirm = false;
                } else {
                    sender.sendMessage(plugin.getMessageUtil().get("purge-confirm"));
                    awaitingPurgeConfirm = true;
                }
            }

            case "cleanup" -> {
                int cleaned = plugin.getDatabaseManager().deactivateExpired();
                sender.sendMessage(plugin.getMessageUtil().getPrefix()
                        + "§aMarked §f" + cleaned + " §aexpired punishment(s) as inactive.");
            }

            case "version" -> {
                UpdateChecker updater = new UpdateChecker(plugin);
                String latest = updater.getLatestVersion();
                if (latest != null && !latest.isEmpty()) {
                    boolean isNew = updater.isNewerVersionAvailable();
                    if (isNew) {
                        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§eCurrent version: §f" + plugin.getDescription().getVersion() + " §c✗ Outdated");
                        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§eLatest version:  §f" + latest);
                    } else {
                        sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§eCurrent version: §f" + plugin.getDescription().getVersion() + " §a✓ Up to date");
                    }
                } else {
                    sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§eCurrent version: §f" + plugin.getDescription().getVersion());
                    sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cCould not check for updates.");
                }
            }

            case "check" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageUtil().getPrefix() + "§cUsage: /spigban check <player>");
                    return true;
                }
                var opt = dev.emanuelplays.spigban.utils.UUIDFetcher.getOfflinePlayer(args[1]);
                if (opt.isEmpty()) {
                    sender.sendMessage(plugin.getMessageUtil().get("player-not-found", Map.of("player", args[1])));
                    return true;
                }
                var target = opt.get();
                String name = target.getName() != null ? target.getName() : args[1];
                boolean banned = plugin.getPunishmentManager().isActiveBan(target.getUniqueId());
                boolean muted  = plugin.getPunishmentManager().isActiveMute(target.getUniqueId());
                int warns = plugin.getDatabaseManager().countActiveWarnings(target.getUniqueId());
                int total = plugin.getDatabaseManager().countPlayerHistory(target.getUniqueId());

                sender.sendMessage(MessageUtil.colorize("§8[§bSpigBan Check§8] §f" + name));
                sender.sendMessage(MessageUtil.colorize("  §7Banned§8:  " + (banned ? "§c✖ Yes" : "§a✔ No")));
                sender.sendMessage(MessageUtil.colorize("  §7Muted§8:   " + (muted  ? "§c✖ Yes" : "§a✔ No")));
                sender.sendMessage(MessageUtil.colorize("  §7Warns§8:   §f" + warns + " §7active"));
                sender.sendMessage(MessageUtil.colorize("  §7Total§8:   §f" + total + " §7punishment records"));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.colorize("§8&m------- §b§lSpigBan Admin §8&m-------"));
        sender.sendMessage(MessageUtil.colorize("  §b/spigban reload §7- Reload config & messages"));
        sender.sendMessage(MessageUtil.colorize("  §b/spigban info §7- Plugin statistics"));
        sender.sendMessage(MessageUtil.colorize("  §b/spigban check <player> §7- Check player status"));
        sender.sendMessage(MessageUtil.colorize("  §b/spigban cleanup §7- Mark expired punishments inactive"));
        sender.sendMessage(MessageUtil.colorize("  §b/spigban purge [confirm] §7- Delete inactive records"));
        sender.sendMessage(MessageUtil.colorize("  §b/spigban version §7- Check for updates and show current version"));
        sender.sendMessage(MessageUtil.colorize("§8&m-------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "info", "purge", "cleanup", "check", "version").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("purge")) return List.of("confirm");
            if (args[0].equalsIgnoreCase("check")) return plugin.getServer().getOnlinePlayers()
                    .stream().map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
