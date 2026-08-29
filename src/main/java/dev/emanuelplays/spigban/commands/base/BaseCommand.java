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
 * Base class for all commands providing common functionality like permission checking,
 * target resolution, and tab completion helpers.
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final SpigBan plugin;

    public BaseCommand(SpigBan plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the sender has the required permission.
     * @param sender The command sender
     * @param permission The permission node to check
     * @return true if sender has permission, false otherwise
     */
    protected boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().getPrefix() +
                    plugin.getMessageUtil().get("no-permission")));
            return false;
        }
        return true;
    }

    /**
     * Gets the plugin prefix from the message utility.
     * @return The plugin prefix string (including trailing space if configured)
     */
    protected String getPrefix() {
        return plugin.getMessageUtil().getPrefix();
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
     * Executes the command.
     * This method must be implemented by subclasses.
     *
     * @param sender  The sender of the command
     * @param command The command which was executed
     * @param label   The alias of the command which was used
     * @param args    The arguments passed to the command
     * @return true if the command was successful, false otherwise
     */
    @Override
    public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);

    /**
     * Tab completion for the command.
     * This method must be implemented by subclasses.
     *
     * @param sender  The sender of the command
     * @param command The command which was executed
     * @param alias   The alias of the command which was used
     * @param args    The arguments passed to the command
     * @return A list of possible completions for the last argument
     */
    @Override
    public abstract List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args);
}