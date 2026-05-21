package dev.emanuelplays.spigban.database.models;

import java.util.UUID;

/**
 * Represents a single punishment record stored in the SpigBan database.
 * Every punishment (ban, mute, warn, kick, IP ban) is stored as a Punishment.
 */
public class Punishment {

    private int id;
    private String caseId;
    private UUID playerUuid;
    private String playerName;
    private UUID staffUuid;      // null if issued by console
    private String staffName;    // "CONSOLE" if issued by console
    private PunishmentType type;
    private String reason;
    private long startTime;      // epoch millis
    private long endTime;        // epoch millis; -1L = permanent
    private boolean active;
    private String ipAddress;    // only populated for IP bans
    private String notes;        // optional staff notes

    public Punishment() {
    }

    // ── Builder-style constructor ───────────────────────────────────────────

    public Punishment(String caseId, UUID playerUuid, String playerName,
                      UUID staffUuid, String staffName, PunishmentType type,
                      String reason, long startTime, long endTime,
                      boolean active, String ipAddress, String notes) {
        this.caseId = caseId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
        this.ipAddress = ipAddress;
        this.notes = notes;
    }

    // ── Utility methods ────────────────────────────────────────────────────

    /**
     * Whether this punishment has no expiration date.
     */
    public boolean isPermanent() {
        return endTime == -1L;
    }

    /**
     * Whether this punishment has passed its end time (only meaningful when active).
     */
    public boolean isExpired() {
        if (isPermanent()) return false;
        return System.currentTimeMillis() >= endTime;
    }

    /**
     * Remaining milliseconds until expiration. Returns -1 if permanent.
     */
    public long getRemainingTime() {
        if (isPermanent()) return -1L;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    /**
     * Whether this punishment is currently in effect (active and not expired).
     */
    public boolean isEffective() {
        return active && !isExpired();
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public int getId() { return id; }
    public String getCaseId() { return caseId; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public UUID getStaffUuid() { return staffUuid; }
    public String getStaffName() { return staffName; }
    public PunishmentType getType() { return type; }
    public String getReason() { return reason; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public boolean isActive() { return active; }
    public String getIpAddress() { return ipAddress; }
    public String getNotes() { return notes; }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setStaffUuid(UUID staffUuid) { this.staffUuid = staffUuid; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public void setType(PunishmentType type) { this.type = type; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setActive(boolean active) { this.active = active; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setNotes(String notes) { this.notes = notes; }
}
