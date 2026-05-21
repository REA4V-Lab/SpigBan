package dev.emanuelplays.spigban;

import dev.emanuelplays.spigban.commands.*;
import dev.emanuelplays.spigban.database.DatabaseManager;
import dev.emanuelplays.spigban.listeners.ChatListener;
import dev.emanuelplays.spigban.listeners.LoginListener;
import dev.emanuelplays.spigban.managers.CaseManager;
import dev.emanuelplays.spigban.managers.PunishmentManager;
import dev.emanuelplays.spigban.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author EmanuelPlays
 * @version 1.0.0
 */
public class SpigBan extends JavaPlugin {

    private static SpigBan instance;

    private DatabaseManager  databaseManager;
    private PunishmentManager punishmentManager;
    private CaseManager      caseManager;
    private MessageUtil      messageUtil;
    private dev.emanuelplays.spigban.integrations.LuckPermsHook luckPermsHook;


    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Init subsystems in dependency order
        messageUtil      = new MessageUtil(this);
        databaseManager  = new DatabaseManager(this);
        databaseManager.initialize();
        caseManager      = new CaseManager(this);
        punishmentManager = new PunishmentManager(this);
        luckPermsHook = new dev.emanuelplays.spigban.integrations.LuckPermsHook(this);


        registerCommands();
        registerListeners();
        scheduleCleanup();

        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║  SpigBan v" + getDescription().getVersion() + " enabled!          ║");
        getLogger().info("║  by EmanuelPlays              ║");

        getLogger().info("║  Database: " + databaseManager.getDbType().toUpperCase() + "                      ║");
        getLogger().info("╚══════════════════════════════════╝");

    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
        getLogger().info("SpigBan disabled. Goodbye!");
    }

    // ── Command registration ───────────────────────────────────────────────

    private void registerCommands() {
        // Ban commands
        BanCommand      banCmd      = new BanCommand(this);
        TempBanCommand  tempBanCmd  = new TempBanCommand(this);
        IPBanCommand    ipBanCmd    = new IPBanCommand(this);
        TempIPBanCommand tempIpCmd  = new TempIPBanCommand(this);

        reg("ban",       banCmd,     banCmd);
        reg("tempban",   tempBanCmd, tempBanCmd);
        reg("ipban",     ipBanCmd,   ipBanCmd);
        reg("tempipban", tempIpCmd,  tempIpCmd);

        // Mute commands
        MuteCommand     muteCmd     = new MuteCommand(this);
        TempMuteCommand tempMuteCmd = new TempMuteCommand(this);

        reg("mute",     muteCmd,     muteCmd);
        reg("tempmute", tempMuteCmd, tempMuteCmd);

        // Moderation commands
        KickCommand   kickCmd   = new KickCommand(this);
        WarnCommand   warnCmd   = new WarnCommand(this);
        UnbanCommand  unbanCmd  = new UnbanCommand(this);
        UnmuteCommand unmuteCmd = new UnmuteCommand(this);
        UnwarnCommand unwarnCmd = new UnwarnCommand(this);

        reg("kick",   kickCmd,   kickCmd);
        reg("warn",   warnCmd,   warnCmd);
        reg("unban",  unbanCmd,  unbanCmd);
        reg("unmute", unmuteCmd, unmuteCmd);
        reg("unwarn", unwarnCmd, unwarnCmd);

        // Info commands
        CaseCommand     caseCmd     = new CaseCommand(this);
        HistoryCommand  historyCmd = new HistoryCommand(this);
        BanListCommand  banListCmd  = new BanListCommand(this);
        MuteListCommand muteListCmd = new MuteListCommand(this);
        CaselistCommand caselistCmd = new CaselistCommand(this);

        reg("case",     caseCmd,     caseCmd);
        reg("history",  historyCmd,  historyCmd);
        reg("banlist",  banListCmd,  banListCmd);
        reg("mutelist", muteListCmd, muteListCmd);
        reg("caselist", caselistCmd, caselistCmd);


        // Admin command
        SpigBanCommand adminCmd = new SpigBanCommand(this);
        reg("spigban", adminCmd, adminCmd);
    }

    private void reg(String name,
                     org.bukkit.command.CommandExecutor executor,
                     org.bukkit.command.TabCompleter completer) {
        var cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command /" + name + " not found in plugin.yml!");
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(completer);
    }

    // ── Listener registration ──────────────────────────────────────────────

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new LoginListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
    }

    // ── Scheduled tasks ────────────────────────────────────────────────────

    private void scheduleCleanup() {
        int intervalMinutes = getConfig().getInt("database.cleanup-interval", 5);
        if (intervalMinutes <= 0) return;

        long ticks = intervalMinutes * 60L * 20L;
        new BukkitRunnable() {
            @Override
            public void run() {
                int cleaned = databaseManager.deactivateExpired();
                if (cleaned > 0)
                    getLogger().info("[Cleanup] Deactivated " + cleaned + " expired punishment(s).");
            }
        }.runTaskTimerAsynchronously(this, ticks, ticks);
    }

    // ── Static accessor ────────────────────────────────────────────────────

    public static SpigBan getInstance() { return instance; }

    // ── Getters ────────────────────────────────────────────────────────────

    public DatabaseManager   getDatabaseManager()   { return databaseManager;   }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public CaseManager       getCaseManager()       { return caseManager;       }
    public MessageUtil       getMessageUtil()       { return messageUtil;        }
    public dev.emanuelplays.spigban.integrations.LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
}

