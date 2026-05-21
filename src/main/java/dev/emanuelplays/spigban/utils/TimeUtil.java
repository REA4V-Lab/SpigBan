package dev.emanuelplays.spigban.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilities for parsing and formatting time durations.
 */
public class TimeUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    /**
     * Parses a duration string into milliseconds.
     * Supports: 30s, 15m, 2h, 7d, 2w, 1mo, 1y, perm, permanent
     *
     * @param input the duration string
     * @return milliseconds, or -1L for permanent, or -2L if invalid
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -2L;
        String s = input.trim().toLowerCase();
        if (s.equals("perm") || s.equals("permanent") || s.equals("-1")) return -1L;

        long multiplier = 0;
        long value;

        try {
            if (s.endsWith("mo")) {
                value = Long.parseLong(s.substring(0, s.length() - 2));
                multiplier = 30L * 24 * 60 * 60 * 1000;
            } else if (s.endsWith("y")) {
                value = Long.parseLong(s.substring(0, s.length() - 1));
                multiplier = 365L * 24 * 60 * 60 * 1000;
            } else if (s.endsWith("w")) {
                value = Long.parseLong(s.substring(0, s.length() - 1));
                multiplier = 7L * 24 * 60 * 60 * 1000;
            } else if (s.endsWith("d")) {
                value = Long.parseLong(s.substring(0, s.length() - 1));
                multiplier = 24L * 60 * 60 * 1000;
            } else if (s.endsWith("h")) {
                value = Long.parseLong(s.substring(0, s.length() - 1));
                multiplier = 60L * 60 * 1000;
            } else if (s.endsWith("m")) {
                value = Long.parseLong(s.substring(0, s.length() - 1));
                multiplier = 60L * 1000;
            } else if (s.endsWith("s")) {
                value = Long.parseLong(s.substring(0, s.length() - 1));
                multiplier = 1000L;
            } else {
                return -2L; // unrecognized unit
            }
        } catch (NumberFormatException e) {
            return -2L;
        }

        if (value <= 0) return -2L;
        return value * multiplier;
    }

    /**
     * Formats a millisecond duration into a human-readable string.
     * e.g. 3661000L → "1 hour, 1 minute, 1 second"
     *
     * @param millis duration in milliseconds; -1 = "Permanent"
     */
    public static String formatDuration(long millis) {
        if (millis == -1L) return "Permanent";
        if (millis <= 0) return "Expired";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long days    = hours / 24;
        long weeks   = days / 7;
        long months  = days / 30;
        long years   = days / 365;

        if (years > 0)   return plural(years, "year");
        if (months > 0)  return plural(months, "month");
        if (weeks > 0)   return plural(weeks, "week");
        if (days > 0)    return plural(days, "day");
        if (hours > 0)   return plural(hours, "hour");
        if (minutes > 0) return plural(minutes, "minute");
        return plural(seconds, "second");
    }

    /**
     * Formats a duration with full compound breakdown, e.g. "2 days, 3 hours, 15 minutes".
     */
    public static String formatDurationLong(long millis) {
        if (millis == -1L) return "Permanent";
        if (millis <= 0) return "Expired";

        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours   = (millis / (1000 * 60 * 60)) % 24;
        long days    = millis / (1000 * 60 * 60 * 24);

        StringBuilder sb = new StringBuilder();
        if (days > 0)    append(sb, plural(days,    "day"));
        if (hours > 0)   append(sb, plural(hours,   "hour"));
        if (minutes > 0) append(sb, plural(minutes, "minute"));
        if (seconds > 0 && days == 0) append(sb, plural(seconds, "second"));

        return sb.length() > 0 ? sb.toString() : "less than a second";
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(part);
    }

    /**
     * Formats an epoch-millis timestamp into a readable date string.
     */
    public static String formatDate(long epochMillis) {
        if (epochMillis <= 0) return "Never";
        return DATE_FORMAT.format(new Date(epochMillis));
    }

    /**
     * Returns "Never" if permanent, otherwise formats the expiry time.
     */
    public static String formatExpiry(long endTime) {
        if (endTime == -1L) return "Never (Permanent)";
        if (endTime < System.currentTimeMillis()) return "Expired";
        return formatDate(endTime) + " (in " + formatDurationLong(endTime - System.currentTimeMillis()) + ")";
    }

    private static String plural(long value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private TimeUtil() {
    }
}
