package dev.emanuelplays.spigban.integrations;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.database.models.Punishment;

/**
 * Small façade to keep integration code out of business logic.
 */
public class LuckPermsHook {

    private final LuckPermsCompat compat;

    public LuckPermsHook(SpigBan plugin) {
        boolean enabled = plugin.getConfig().getBoolean("integrations.luckperms.enabled", true);
        this.compat = new LuckPermsCompat(enabled);
    }

    public void onPunishmentApplied(Punishment punishment) {
        compat.maybeSyncOnPunishmentApplied(punishment);
    }
}

