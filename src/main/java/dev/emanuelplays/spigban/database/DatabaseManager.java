package dev.emanuelplays.spigban.database;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;
import dev.emanuelplays.spigban.database.models.PunishmentType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages all database interactions for SpigBan.
 * Supports both SQLite (default) and MySQL.
 */
public class DatabaseManager {

    private final SpigBan plugin;
    private Connection connection;
    private String dbType;

    public DatabaseManager(SpigBan plugin) {
        this.plugin = plugin;
    }

    // ── Initialization ─────────────────────────────────────────────────────

    public void initialize() {
        dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        try {
            if (dbType.equals("mysql")) {
                setupMySQL();
            } else {
                dbType = "sqlite";
                setupSQLite();
            }
            createTables();
            plugin.getLogger().info("Database initialized (" + dbType.toUpperCase() + ").");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
        }
    }

    private void setupSQLite() throws Exception {
        Class.forName("org.sqlite.JDBC");
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File dbFile = new File(dataFolder, "spigban.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        // Enable WAL mode for better concurrent access
        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL;");
            s.execute("PRAGMA synchronous=NORMAL;");
        }
    }

    private void setupMySQL() throws Exception {
        String host     = plugin.getConfig().getString("database.host", "localhost");
        int    port     = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "spigban");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC";
        connection = DriverManager.getConnection(url, username, password);
    }

    private void createTables() throws SQLException {
        boolean isMySQL = dbType.equals("mysql");
        String autoIncr = isMySQL ? "INT NOT NULL AUTO_INCREMENT" : "INTEGER";
        String pkClause = isMySQL ? ", PRIMARY KEY (id)" : "PRIMARY KEY AUTOINCREMENT";
        String bigintType = isMySQL ? "BIGINT" : "INTEGER";

        String sql;
        if (isMySQL) {
            sql = "CREATE TABLE IF NOT EXISTS spigban_punishments ("
                    + "id " + autoIncr + ", "
                    + "case_id VARCHAR(20) NOT NULL UNIQUE, "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "player_name VARCHAR(32) NOT NULL, "
                    + "staff_uuid VARCHAR(36) DEFAULT NULL, "
                    + "staff_name VARCHAR(32) NOT NULL DEFAULT 'CONSOLE', "
                    + "type VARCHAR(20) NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "start_time " + bigintType + " NOT NULL, "
                    + "end_time " + bigintType + " NOT NULL DEFAULT -1, "
                    + "active TINYINT(1) NOT NULL DEFAULT 1, "
                    + "ip_address VARCHAR(45) DEFAULT NULL, "
                    + "notes TEXT DEFAULT NULL"
                    + pkClause
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS spigban_punishments ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "case_id TEXT NOT NULL UNIQUE, "
                    + "player_uuid TEXT NOT NULL, "
                    + "player_name TEXT NOT NULL, "
                    + "staff_uuid TEXT DEFAULT NULL, "
                    + "staff_name TEXT NOT NULL DEFAULT 'CONSOLE', "
                    + "type TEXT NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "start_time INTEGER NOT NULL, "
                    + "end_time INTEGER NOT NULL DEFAULT -1, "
                    + "active INTEGER NOT NULL DEFAULT 1, "
                    + "ip_address TEXT DEFAULT NULL, "
                    + "notes TEXT DEFAULT NULL"
                    + ");";
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            // Indexes for faster lookups
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON spigban_punishments (player_uuid);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ip_address ON spigban_punishments (ip_address);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_type_active ON spigban_punishments (type, active);");
        }
    }

    // ── Connection Management ─────────────────────────────────────────────

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("Database connection lost. Reconnecting...");
                if (dbType.equals("mysql")) {
                    setupMySQL();
                } else {
                    setupSQLite();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reconnect to database!", e);
        }
        return connection;
    }

    // ── Write Operations ───────────────────────────────────────────────────

    public synchronized void savePunishment(Punishment p) {
        String sql = "INSERT INTO spigban_punishments "
                + "(case_id, player_uuid, player_name, staff_uuid, staff_name, type, reason, "
                + "start_time, end_time, active, ip_address, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCaseId());
            ps.setString(2, p.getPlayerUuid().toString());
            ps.setString(3, p.getPlayerName());
            ps.setString(4, p.getStaffUuid() != null ? p.getStaffUuid().toString() : null);
            ps.setString(5, p.getStaffName());
            ps.setString(6, p.getType().name());
            ps.setString(7, p.getReason());
            ps.setLong(8, p.getStartTime());
            ps.setLong(9, p.getEndTime());
            ps.setInt(10, p.isActive() ? 1 : 0);
            ps.setString(11, p.getIpAddress());
            ps.setString(12, p.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save punishment " + p.getCaseId() + "!", e);
        }
    }

    public synchronized void updatePunishment(Punishment p) {
        String sql = "UPDATE spigban_punishments SET active = ?, notes = ?, end_time = ? WHERE case_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, p.isActive() ? 1 : 0);
            ps.setString(2, p.getNotes());
            ps.setLong(3, p.getEndTime());
            ps.setString(4, p.getCaseId());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update punishment " + p.getCaseId() + "!", e);
        }
    }

    public synchronized void deactivatePunishment(String caseId) {
        String sql = "UPDATE spigban_punishments SET active = 0 WHERE case_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, caseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to deactivate punishment " + caseId + "!", e);
        }
    }

    public synchronized void deactivateAllOfType(UUID playerUuid, PunishmentType... types) {
        for (PunishmentType type : types) {
            String sql = "UPDATE spigban_punishments SET active = 0 WHERE player_uuid = ? AND type = ? AND active = 1";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, type.name());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deactivate " + type + " for " + playerUuid, e);
            }
        }
    }

    public synchronized void deactivateAllIPBansForIP(String ipAddress) {
        String sql = "UPDATE spigban_punishments SET active = 0 WHERE ip_address = ? AND (type = 'IP_BAN' OR type = 'TEMP_IP_BAN') AND active = 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, ipAddress);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to deactivate IP bans for " + ipAddress, e);
        }
    }

    /**
     * Bulk-deactivates all expired temporary punishments. Called on a scheduler.
     */
    public synchronized int deactivateExpired() {
        String sql = "UPDATE spigban_punishments SET active = 0 WHERE active = 1 AND end_time != -1 AND end_time < ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clean up expired punishments!", e);
        }
        return 0;
    }

    /**
     * Purges all inactive/expired punishments from the database.
     */
    public synchronized int purgeInactive() {
        String sql = "DELETE FROM spigban_punishments WHERE active = 0";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to purge inactive punishments!", e);
        }
        return 0;
    }

    // ── Read Operations ────────────────────────────────────────────────────

    public synchronized Optional<Punishment> getPunishmentByCaseId(String caseId) {
        String sql = "SELECT * FROM spigban_punishments WHERE case_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, caseId.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(fromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get punishment by case ID!", e);
        }
        return Optional.empty();
    }

    public synchronized Optional<Punishment> getActiveBan(UUID playerUuid) {
        String sql = "SELECT * FROM spigban_punishments WHERE player_uuid = ? AND (type = 'BAN' OR type = 'TEMP_BAN') AND active = 1 ORDER BY start_time DESC LIMIT 1";
        return queryOne(sql, playerUuid.toString());
    }

    public synchronized Optional<Punishment> getActiveIPBan(String ipAddress) {
        if (ipAddress == null) return Optional.empty();
        String sql = "SELECT * FROM spigban_punishments WHERE ip_address = ? AND (type = 'IP_BAN' OR type = 'TEMP_IP_BAN') AND active = 1 ORDER BY start_time DESC LIMIT 1";
        return queryOne(sql, ipAddress);
    }

    public synchronized Optional<Punishment> getActiveMute(UUID playerUuid) {
        String sql = "SELECT * FROM spigban_punishments WHERE player_uuid = ? AND (type = 'MUTE' OR type = 'TEMP_MUTE') AND active = 1 ORDER BY start_time DESC LIMIT 1";
        return queryOne(sql, playerUuid.toString());
    }

    public synchronized List<Punishment> getPlayerHistory(UUID playerUuid, int limit, int offset) {
        String sql = "SELECT * FROM spigban_punishments WHERE player_uuid = ? ORDER BY start_time DESC LIMIT ? OFFSET ?";
        List<Punishment> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player history!", e);
        }
        return list;
    }

    public synchronized int countPlayerHistory(UUID playerUuid) {
        return countWhere("SELECT COUNT(*) FROM spigban_punishments WHERE player_uuid = ?", playerUuid.toString());
    }

    public synchronized int countActiveWarnings(UUID playerUuid) {
        return countWhere("SELECT COUNT(*) FROM spigban_punishments WHERE player_uuid = ? AND type = 'WARN' AND active = 1", playerUuid.toString());
    }

    public synchronized List<Punishment> getActiveBans(int limit, int offset) {
        String sql = "SELECT * FROM spigban_punishments WHERE (type = 'BAN' OR type = 'TEMP_BAN') AND active = 1 ORDER BY start_time DESC LIMIT ? OFFSET ?";
        return queryList(sql, limit, offset);
    }




    public synchronized int countActiveBans() {
        return countWhere("SELECT COUNT(*) FROM spigban_punishments WHERE (type = 'BAN' OR type = 'TEMP_BAN') AND active = 1");
    }

    public synchronized List<Punishment> getActiveMutes(int limit, int offset) {
        String sql = "SELECT * FROM spigban_punishments WHERE (type = 'MUTE' OR type = 'TEMP_MUTE') AND active = 1 ORDER BY start_time DESC LIMIT ? OFFSET ?";
        return queryList(sql, limit, offset);
    }


    public synchronized int countActiveMutes() {
        return countWhere("SELECT COUNT(*) FROM spigban_punishments WHERE (type = 'MUTE' OR type = 'TEMP_MUTE') AND active = 1");
    }

    public synchronized int countTotal() {
        return countWhere("SELECT COUNT(*) FROM spigban_punishments");
    }

    public synchronized boolean caseIdExists(String caseId) {
        return countWhere("SELECT COUNT(*) FROM spigban_punishments WHERE case_id = ?", caseId) > 0;
    }

    /**
     * Returns a paginated list of all punishments (used by /caselist).
     */
    public synchronized List<Punishment> getAllPunishments(int limit, int offset) {
        String sql = "SELECT * FROM spigban_punishments ORDER BY start_time DESC LIMIT ? OFFSET ?";
        return queryList(sql, limit, offset);
    }

    /**
     * Returns a list of all case IDs (used for tab completion).
     */
    public synchronized List<String> getAllCaseIds(int limit) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT case_id FROM spigban_punishments ORDER BY start_time DESC LIMIT ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("case_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get case IDs!", e);
        }
        return list;
    }

    // ── Private Helpers ─────────────────────────────────────────────

    private Optional<Punishment> queryOne(String sql, String param) {

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(fromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Query failed.", e);
        }
        return Optional.empty();
    }


    private List<Punishment> queryList(String sql, int limit, int offset) {
        List<Punishment> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Query failed.", e);
        }
        return list;
    }

    private List<Punishment> queryList(String sql) {
        List<Punishment> list = new ArrayList<>();
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(fromResultSet(rs));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Query failed: " + sql, e);
        }
        return list;
    }


    private int countWhere(String sql, String... params) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Count query failed: " + sql, e);
        }
        return 0;
    }

    private Punishment fromResultSet(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.setId(rs.getInt("id"));
        p.setCaseId(rs.getString("case_id"));
        p.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        p.setPlayerName(rs.getString("player_name"));
        String staffUuidStr = rs.getString("staff_uuid");
        p.setStaffUuid(staffUuidStr != null && !staffUuidStr.isEmpty() ? UUID.fromString(staffUuidStr) : null);
        p.setStaffName(rs.getString("staff_name"));
        p.setType(PunishmentType.valueOf(rs.getString("type")));
        p.setReason(rs.getString("reason"));
        p.setStartTime(rs.getLong("start_time"));
        p.setEndTime(rs.getLong("end_time"));
        p.setActive(rs.getInt("active") == 1);
        p.setIpAddress(rs.getString("ip_address"));
        p.setNotes(rs.getString("notes"));
        return p;
    }

    // ── Shutdown ───────────────────────────────────────────────────────────

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection.", e);
        }
    }

    public String getDbType() {
        return dbType;
    }
}
