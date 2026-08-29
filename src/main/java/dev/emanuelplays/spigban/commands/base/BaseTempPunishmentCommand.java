package dev.emanuelplays.spigban.commands.base;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Base class for temporary punishment commands providing common duration handling.
 */
public abstract class BaseTempPunishmentCommand extends BasePunishmentCommand {

    public BaseTempPunishmentCommand(SpigBan plugin) {
        super(plugin);
    }

    /**
     * Parses and validates a duration string.
     *
     * @param durationStr The duration string to parse (e.g., "30m", "2h", "1d")
     * @param sender The command sender (for error messages)
     * @param allowPermanent Whether to allow permanent duration requests
     * @return Duration in milliseconds, or negative value for special cases:
     *         -2 = invalid duration
     *         -1 = permanent requested but not allowed (if allowPermanent=false)
     *         -3 = permanent requested and allowed (if allowPermanent=true)
     */
    protected long parseDuration(String durationStr, CommandSender sender, boolean allowPermanent) {
        long duration = TimeUtil.parseDuration(durationStr);

        if (duration == -2L) {
            // Invalid duration format
            sender.sendMessage(MessageUtil.colorize(
                    plugin.getMessageUtil().getPrefix() +
                    plugin.getMessageUtil().get("invalid-duration",
                            Map.of("duration", durationStr))));
            return -2L;
        }

        if (duration == -1L) {
            // Permanent requested
            if (!allowPermanent) {
                sender.sendMessage(MessageUtil.colorize(
                        plugin.getMessageUtil().getPrefix() +
                        plugin.getMessageUtil().get("use-permanent-command")));
                return -1L;
            }
            return -3L; // Permanent allowed
        }

        return duration; // Valid temporary duration
    }

    /**
     * Gets duration suggestions for tab completion.
     * This can be overridden by subclasses or made configurable.
     *
     * @return List of duration suggestion strings
     */
    @Override
    protected List<String> getDurationSuggestions() {
        // Get suggestions from config if available, otherwise use defaults
        var sec = plugin.getConfig().getConfigurationSection("duration-suggestions");
        if (sec != null) {
            return sec.getStringList("list");
        }
        return Arrays.asList("30m", "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d", "1mo");
    }
}