package dev.emanuelplays.spigban.commands.base;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.UUIDFetcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base class for all punishment-related commands providing common functionality.
 * Extends BaseCommand to inherit common command functionality.
 */
public abstract class BasePunishmentCommand extends BaseCommand {

    public BasePunishmentCommand(SpigBan plugin) {
        super(plugin);
    }

    /**
     * Prevents self-targeting punishment.
     * @param sender The command sender
     * @param targetName The target player name
     * @return true if self-targeting is allowed, false if it should be prevented
     */
    protected boolean checkSelfTarget(CommandSender sender, String targetName) {
        if (sender instanceof Player player &&
                player.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().getPrefix() +
                    plugin.getMessageUtil().get("self-target")));
            return false;
        }
        return true;
    }

    /**
     * Checks if the target has bypass permission.
     * @param sender The command sender
     * @param targetName The target player name
     * @return true if target can be punished, false if they have bypass
     */
    protected boolean checkBypass(CommandSender sender, String targetName) {
        Optional<OfflinePlayer> opt =
                UUIDFetcher.getOfflinePlayer(targetName);
        if (opt.isPresent() && opt.get().isOnline() && opt.get().getPlayer() != null) {
            Player targetPlayer = opt.get().getPlayer();
            if (targetPlayer.hasPermission("spigban.bypass")) {
                sender.sendMessage(MessageUtil.colorize(
                        plugin.getMessageUtil().getPrefix() +
                        plugin.getMessageUtil().get("target-bypasses",
                                Map.of("player", targetPlayer.getName()))));
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves a target player name to an OfflinePlayer object.
     * @param sender The command sender (for error messages)
     * @param targetName The target player name
     * @return Optional containing the OfflinePlayer if found, empty otherwise
     */
    protected Optional<OfflinePlayer> resolveTarget(
            CommandSender sender, String targetName) {

        Optional<OfflinePlayer> opt =
                UUIDFetcher.getOfflinePlayer(targetName);
        if (opt.isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().getPrefix() +
                    plugin.getMessageUtil().get("player-not-found",
                            Map.of("player", targetName))));
        }
        return opt;
    }

    /**
     * Resolves a reason string, expanding template references if applicable.
     * Delegates to PunishmentManager's resolveReason method.
     * @param reason The reason string (may start with # for template)
     * @return The resolved reason string
     */
    protected String resolveReason(String reason) {
        return plugin.getPunishmentManager().resolveReason(reason);
    }

    /**
     * Gets online player names for tab completion.
     * @param partial The partial name to match
     * @return List of matching online player names
     */
    protected List<String> getOnlinePlayerNames(String partial) {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Gets reason template keys from configuration for tab completion.
     * @param partial The partial template key to match
     * @return List of matching reason template keys prefixed with #
     */
    protected List<String> getReasonTemplateKeys(String partial) {
        var sec = plugin.getConfig().getConfigurationSection("reason-templates");
        if (sec == null) return Collections.emptyList();
        return sec.getKeys(false).stream()
                .map(key -> "#" + key)
                .filter(key -> key.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Gets duration suggestions for tab completion.
     * Subclasses can override to provide custom suggestions.
     * @return List of duration suggestion strings
     */
    protected List<String> getDurationSuggestions() {
        // Default suggestions - can be overridden or made configurable
        return Arrays.asList("30m", "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d", "1mo");
    }
}