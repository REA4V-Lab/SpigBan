package dev.emanuelplays.spigban.database.models;

public enum PunishmentType {

    BAN("Ban", "banned", "&c"),
    TEMP_BAN("Temp Ban", "temp-banned", "&6"),
    IP_BAN("IP Ban", "IP banned", "&4"),
    TEMP_IP_BAN("Temp IP Ban", "temp IP-banned", "&c"),
    MUTE("Mute", "muted", "&e"),
    TEMP_MUTE("Temp Mute", "temp-muted", "&6"),
    WARN("Warning", "warned", "&f"),
    KICK("Kick", "kicked", "&d");

    private final String displayName;
    private final String pastTense;
    private final String colorCode;

    PunishmentType(String displayName, String pastTense, String colorCode) {
        this.displayName = displayName;
        this.pastTense = pastTense;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPastTense() {
        return pastTense;
    }

    public String getColorCode() {
        return colorCode;
    }

    public boolean isBan() {
        return this == BAN || this == TEMP_BAN || this == IP_BAN || this == TEMP_IP_BAN;
    }

    public boolean isIPBan() {
        return this == IP_BAN || this == TEMP_IP_BAN;
    }

    public boolean isMute() {
        return this == MUTE || this == TEMP_MUTE;
    }

    public boolean isWarn() {
        return this == WARN;
    }

    public boolean isKick() {
        return this == KICK;
    }

    public boolean isTemp() {
        return this == TEMP_BAN || this == TEMP_IP_BAN || this == TEMP_MUTE;
    }

    /**
     * Returns the messages.yml type key for color lookup (e.g. "ban", "tempban").
     */
    public String getMessageKey() {
        return this.name().toLowerCase().replace("_", "");
    }
}
