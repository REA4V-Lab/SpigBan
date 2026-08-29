package dev.emanuelplays.spigban.managers;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.database.models.PunishmentType;
import dev.emanuelplays.spigban.utils.MessageUtil;
import dev.emanuelplays.spigban.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * High-level punishment business logic.
 * All ban/mute/warn/kick operations go through here.
 */
public class PunishmentManager {

    private final SpigBan plugin;
    private final Logger punishLogger;

    public PunishmentManager(SpigBan plugin) {
        this.plugin = plugin;
        this.punishLogger = setupFileLogger();
    }

    // ── BAN ────────────────────────────────────────────────────────────────

    /**
     * Issues a permanent ban.
     *
     * @return the created Punishment, or null if the player is already banned
     */
    public Punishment ban(CommandSender staff, UUID targetUuid, String targetName, String reason) {
        if (isActiveBan(targetUuid)) return null;
        Punishment p = build(staff, targetUuid, targetName, PunishmentType.BAN, reason, -1L, null);
        save(p);

        try {
            plugin.getLuckPermsHook().onPunishmentApplied(p);
        } catch (Throwable ignored) {
        }

        kickIfOnline(targetUuid, buildBanScreen(p));
        broadcast("ban-broadcast", staff, targetName, reason, TimeUtil.formatDuration(-1L), p.getCaseId());
        log(staff, p);
        return p;
    }

    /**
     * Issues a temporary ban.
     *
     * @param durationMillis duration in milliseconds
     */
    public Punishment tempBan(CommandSender staff, UUID targetUuid, String targetName, long durationMillis, String reason) {
        if (isActiveBan(targetUuid)) return null;
        long endTime = System.currentTimeMillis() + durationMillis;
        Punishment p = build(staff, targetUuid, targetName, PunishmentType.TEMP_BAN, reason, endTime, null);
        save(p);

        try {
            plugin.getLuckPermsHook().onPunishmentApplied(p);
        } catch (Throwable ignored) {
        }

        kickIfOnline(targetUuid, buildBanScreen(p));
        broadcast("tempban-broadcast", staff, targetName, reason, TimeUtil.formatDuration(durationMillis), p.getCaseId());
        log(staff, p);
        return p;
    }

    /**
     * Issues a permanent IP ban.
     *
     * @param ipAddress the IP address to ban (resolved before calling)
     */
    public Punishment ipBan(CommandSender staff, UUID targetUuid, String targetName, String ipAddress, String reason) {
        if (isActiveIPBan(ipAddress)) return null;
        Punishment p = build(staff, targetUuid, targetName, PunishmentType.IP_BAN, reason, -1L, ipAddress);
        save(p);

        try {
            plugin.getLuckPermsHook().onPunishmentApplied(p);
        } catch (Throwable ignored) {
        }

        kickIfOnline(targetUuid, buildBanScreen(p));
        broadcast("ban-ip-broadcast", staff, targetName, reason, "Permanent", p.getCaseId());
        log(staff, p);
        return p;
    }

    /**
     * Issues a temporary IP ban.
     */
    public Punishment tempIpBan(CommandSender staff, UUID targetUuid, String targetName, String ipAddress,
                                long durationMillis, String reason) {
        if (isActiveIPBan(ipAddress)) return null;
        long endTime = System.currentTimeMillis() + durationMillis;
        Punishment p = build(staff, targetUuid, targetName, PunishmentType.TEMP_IP_BAN, reason, endTime, ipAddress);
        save(p);

        try {
            plugin.getLuckPermsHook().onPunishmentApplied(p);
        } catch (Throwable ignored) {
        }

        kickIfOnline(targetUuid, buildBanScreen(p));
        broadcast("tempipban-broadcast", staff, targetName, reason, TimeUtil.formatDuration(durationMillis), p.getCaseId());
        log(staff, p);
        return p;
    }

    // ── MUTE ───────────────────────────────────────────────────────────────

    public Punishment mute(CommandSender staff, UUID targetUuid, String targetName, String reason) {
        if (isActiveMute(targetUuid)) return null;
        Punishment p = build(staff, targetUuid, targetName, PunishmentType.MUTE, reason, -1L, null);
        save(p);

        try {
            plugin.getLuckPermsHook().onPunishmentApplied(p);
        } catch (Throwable ignored) {
        }

        notifyTarget(targetUuid, buildMuteMessage(p));
        broadcast("mute-broadcast", staff, targetName, reason, "Permanent", p.getCaseId());
        log(staff, p);
        return p;
    }

    public Punishment tempMute(CommandSender staff, UUID targetUuid, String targetName, long durationMillis, String reason) {
        if (isActiveMute(targetUuid)) return null;
        long endTime = System.currentTimeMillis() + durationMillis;
        Punishment p = build(staff, targetUuid, targetName, PunishmentType.TEMP_MUTE, reason, endTime, null);
        save(p);

        try {
            plugin.getLuckPermsHook().onPunishmentApplied(p);
        } catch (Throwable ignored) {
        }

        notifyTarget(targetUuid, buildMuteMessage(p));
        broadcast("tempmute-broadcast", staff, targetName, reason, TimeUtil.formatDuration(durationMillis), p.getCaseId());
        log(staff, p);
        return p;
    }

    // ── KICK ───────────────────────────────────────────────────────────────

    public Punishment kick(CommandSender staff, Player target, String reason) {
        Punishment p = build(staff, target.getUniqueId(), target.getName(), PunishmentType.KICK, reason, 0L, null);
        save(p);
        String msg = buildKickScreen(p);
        target.kickPlayer(msg);
        broadcast("kick-broadcast", staff, target.getName(), reason, "N/A", p.getCaseId());
        log(staff, p);
        return p;
    }

    // ── WARN ───────────────────────────────────────────────────────────────

    public Punishment warn(CommandSender staff, UUID targetUuid, String targetName, String reason) {
        Punishment p = build(staff, targetUuid, targetName, PunishmentType.WARN, reason, 0L, null);
        save(p);
        int warnCount = plugin.getDatabaseManager().countActiveWarnings(targetUuid);

        // Notify the target
        Map<String, String> ph = commonPlaceholders(staff, targetName, reason, "N/A", p.getCaseId());
        ph.put("warn_count", String.valueOf(warnCount));
        notifyTarget(targetUuid, plugin.getMessageUtil().get("warn-receive", ph));

        broadcast("warn-broadcast", staff, targetName, reason, "N/A", p.getCaseId());
        log(staff, p);

        // Handle threshold actions
        handleWarnThreshold(staff, targetUuid, targetName, warnCount);
        return p;
    }

    private void handleWarnThreshold(CommandSender staff, UUID targetUuid, String targetName, int warnCount) {
        ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("warn-thresholds");
        if (thresholds == null) return;

        if (!thresholds.contains(String.valueOf(warnCount))) return;
        ConfigurationSection entry = thresholds.getConfigurationSection(String.valueOf(warnCount));
        if (entry == null) return;

        String actionStr = entry.getString("action", "").toUpperCase();
        String reason = entry.getString("reason", "Reached " + warnCount + " warnings");
        String durStr = entry.getString("duration", "perm");

        try {
            PunishmentType action = PunishmentType.valueOf(actionStr);
            notifyStaff(plugin.getMessageUtil().getRaw("warn-threshold",
                    Map.of("player", targetName, "warn_count", String.valueOf(warnCount), "action", action.getDisplayName())));

            long dur = TimeUtil.parseDuration(durStr);

            switch (action) {
                case KICK -> {
                    Player online = Bukkit.getPlayer(targetUuid);
                    if (online != null) kick(staff, online, reason);
                }
                case MUTE -> mute(staff, targetUuid, targetName, reason);
                case TEMP_MUTE -> tempMute(staff, targetUuid, targetName, dur, reason);
                case BAN -> ban(staff, targetUuid, targetName, reason);
                case TEMP_BAN -> tempBan(staff, targetUuid, targetName, dur, reason);
                case IP_BAN -> {
                    String ip = resolveIP(targetUuid);
                    if (ip != null) ipBan(staff, targetUuid, targetName, ip, reason);
                    else ban(staff, targetUuid, targetName, reason);
                }
                default -> plugin.getLogger().warning("Unsupported warn threshold action: " + actionStr);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid warn-threshold action '" + actionStr + "' in config.yml");
        }
    }

    // ── UNBAN / UNMUTE / UNWARN ────────────────────────────────────────────

    public boolean unban(CommandSender staff, UUID targetUuid, String targetName) {
        Optional<Punishment> ban = plugin.getDatabaseManager().getActiveBan(targetUuid);
        if (ban.isEmpty()) {
            // Also check IP bans linked to that player
            return false;
        }
        plugin.getDatabaseManager().deactivateAllOfType(targetUuid, PunishmentType.BAN, PunishmentType.TEMP_BAN);
        Map<String, String> ph = Map.of("player", targetName, "staff", senderName(staff));
        broadcast("unban-broadcast", ph);
        log(staff, ban.get());
        return true;
    }

    public boolean unmute(CommandSender staff, UUID targetUuid, String targetName) {
        Optional<Punishment> mute = plugin.getDatabaseManager().getActiveMute(targetUuid);
        if (mute.isEmpty()) return false;
        plugin.getDatabaseManager().deactivateAllOfType(targetUuid, PunishmentType.MUTE, PunishmentType.TEMP_MUTE);
        Map<String, String> ph = Map.of("player", targetName, "staff", senderName(staff));
        broadcast("unmute-broadcast", ph);
        return true;
    }

    public boolean unwarnByCase(CommandSender staff, String caseId) {
        Optional<Punishment> opt = plugin.getDatabaseManager().getPunishmentByCaseId(caseId);
        if (opt.isEmpty() || opt.get().getType() != PunishmentType.WARN || !opt.get().isActive()) return false;
        plugin.getDatabaseManager().deactivatePunishment(caseId);
        return true;
    }

    public int unwarnAll(UUID targetUuid) {
        // Count active warns first
        int count = plugin.getDatabaseManager().countActiveWarnings(targetUuid);
        if (count == 0) return 0;
        plugin.getDatabaseManager().deactivateAllOfType(targetUuid, PunishmentType.WARN);
        return count;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public boolean isActiveBan(UUID uuid) {
        Optional<Punishment> p = plugin.getDatabaseManager().getActiveBan(uuid);
        if (p.isEmpty()) return false;
        if (p.get().isExpired()) {
            plugin.getDatabaseManager().deactivatePunishment(p.get().getCaseId());
            return false;
        }
        return true;
    }

    public boolean isActiveIPBan(String ip) {
        if (ip == null) return false;
        Optional<Punishment> p = plugin.getDatabaseManager().getActiveIPBan(ip);
        if (p.isEmpty()) return false;
        if (p.get().isExpired()) {
            plugin.getDatabaseManager().deactivatePunishment(p.get().getCaseId());
            return false;
        }
        return true;
    }

    public boolean isActiveMute(UUID uuid) {
        Optional<Punishment> p = plugin.getDatabaseManager().getActiveMute(uuid);
        if (p.isEmpty()) return false;
        if (p.get().isExpired()) {
            plugin.getDatabaseManager().deactivatePunishment(p.get().getCaseId());
            return false;
        }
        return true;
    }

    private Punishment build(CommandSender staff, UUID targetUuid, String targetName,
                               PunishmentType type, String reason, long endTime, String ip) {
        return new Punishment(
                plugin.getCaseManager().generate(),
                targetUuid, targetName,
                staff instanceof Player p ? p.getUniqueId() : null,
                senderName(staff),
                type, resolveReason(reason),
                System.currentTimeMillis(), endTime, true, ip, null
        );
    }

    private void save(Punishment p) {
        plugin.getDatabaseManager().savePunishment(p);
    }

    public String resolveReason(String reason) {
        if (reason == null || reason.isEmpty()) return "No reason provided";
        if (reason.startsWith("#")) {
            String key = reason.substring(1).toLowerCase();
            String template = plugin.getConfig().getString("reason-templates." + key);
            return template != null ? template : reason;
        }
        return reason;
    }

    private void kickIfOnline(UUID uuid, String message) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            Bukkit.getScheduler().runTask(plugin, () -> online.kickPlayer(message));
        }
    }

    private void notifyTarget(UUID uuid, String message) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) online.sendMessage(message);
    }

    public void notifyStaff(String message) {
        String colored = MessageUtil.colorize(plugin.getMessageUtil().getPrefix() + message);
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("spigban.notify"))
                .forEach(p -> p.sendMessage(colored));
        if (plugin.getConfig().getBoolean("broadcast.console", true)) {
            plugin.getLogger().info(MessageUtil.strip(message));
        }
    }

    private void broadcast(String msgKey, CommandSender staff, String targetName,
                            String reason, String duration, String caseId) {
        if (!plugin.getConfig().getBoolean("broadcast.enabled", true)) return;
        Map<String, String> ph = commonPlaceholders(staff, targetName, reason, duration, caseId);
        notifyStaff(plugin.getMessageUtil().getRaw(msgKey, ph));
    }

    private void broadcast(String msgKey, Map<String, String> placeholders) {
        if (!plugin.getConfig().getBoolean("broadcast.enabled", true)) return;
        notifyStaff(plugin.getMessageUtil().getRaw(msgKey, placeholders));
    }

    private Map<String, String> commonPlaceholders(CommandSender staff, String targetName,
                                                    String reason, String duration, String caseId) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", targetName);
        ph.put("staff", senderName(staff));
        ph.put("reason", reason);
        ph.put("duration", duration);
        ph.put("case_id", caseId);
        ph.put("date", TimeUtil.formatDate(System.currentTimeMillis()));
        return ph;
    }

    private String buildBanScreen(Punishment p) {
        Map<String, String> ph = new HashMap<>();
        ph.put("reason", p.getReason());
        ph.put("staff", p.getStaffName());
        ph.put("case_id", p.getCaseId());
        ph.put("date", TimeUtil.formatDate(p.getStartTime()));
        ph.put("expires", TimeUtil.formatExpiry(p.getEndTime()));
        return plugin.getMessageUtil().buildBanScreen(ph);
    }

    private String buildMuteMessage(Punishment p) {
        Map<String, String> ph = new HashMap<>();
        ph.put("reason", p.getReason());
        ph.put("staff", p.getStaffName());
        ph.put("case_id", p.getCaseId());
        ph.put("expires", TimeUtil.formatExpiry(p.getEndTime()));
        return plugin.getMessageUtil().buildMuteScreen(ph);
    }

    private String buildKickScreen(Punishment p) {
        return MessageUtil.colorize(
                "&c&m----------------------------------------------------\n" +
                "&c&lYou have been &4&lKICKED\n" +
                "&c&m----------------------------------------------------\n" +
                "&7Reason&8: &f" + p.getReason() + "\n" +
                "&7Staff&8:  &f" + p.getStaffName() + "\n" +
                "&7Case ID&8: &e" + p.getCaseId() + "\n" +
                "&c&m----------------------------------------------------"
        );
    }

    private String resolveIP(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.getAddress() != null) {
            return online.getAddress().getAddress().getHostAddress();
        }
        return null;
    }

    public static String senderName(CommandSender sender) {
        return sender instanceof Player ? sender.getName() : "CONSOLE";
    }

    private void log(CommandSender staff, Punishment p) {
        punishLogger.info(String.format("[%s] %s issued %s to %s | Reason: %s | Case: %s",
                TimeUtil.formatDate(System.currentTimeMillis()),
                senderName(staff), p.getType().getDisplayName(),
                p.getPlayerName(), p.getReason(), p.getCaseId()));
    }

    private Logger setupFileLogger() {
        Logger logger = Logger.getLogger("SpigBanPunishments");
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) return logger;
        try {
            String filename = plugin.getConfig().getString("logging.file", "punishments.log");
            java.io.File logFile = new java.io.File(plugin.getDataFolder(), filename);
            FileHandler fh = new FileHandler(logFile.getAbsolutePath(), true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not set up punishment log file.", e);
        }
        return logger;
    }
}

