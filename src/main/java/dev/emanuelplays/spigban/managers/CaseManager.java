package dev.emanuelplays.spigban.managers;

import dev.emanuelplays.spigban.SpigBan;

import java.util.Random;

/**
 * Generates and validates unique punishment case IDs.
 * Format: {PREFIX}-{RANDOM} e.g. "SPGB-A1B2C3"
 */
public class CaseManager {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SpigBan plugin;
    private final Random random = new Random();

    private final String prefix;
    private final int length;

    public CaseManager(SpigBan plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("case-id.prefix", "SPGB").toUpperCase();
        int cfgLen = plugin.getConfig().getInt("case-id.length", 6);
        this.length = Math.max(4, Math.min(10, cfgLen));
    }

    /**
     * Generates a unique case ID that does not exist in the database.
     * Retries up to 20 times before throwing an exception.
     */
    public String generate() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String id = prefix + "-" + randomPart();
            if (!plugin.getDatabaseManager().caseIdExists(id)) {
                return id;
            }
        }
        // Highly unlikely; increase length and try once more
        String fallback = prefix + "-" + randomPartLong();
        if (!plugin.getDatabaseManager().caseIdExists(fallback)) return fallback;
        throw new RuntimeException("Unable to generate a unique case ID after 20 attempts.");
    }

    private String randomPart() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String randomPartLong() {
        StringBuilder sb = new StringBuilder(length + 4);
        for (int i = 0; i < length + 4; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Normalizes a case ID input (trim + uppercase).
     */
    public static String normalize(String raw) {
        return raw.trim().toUpperCase();
    }
}
