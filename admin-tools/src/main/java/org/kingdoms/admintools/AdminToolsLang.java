package org.kingdoms.admintools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.locale.LanguageEntry;
import org.kingdoms.locale.messenger.DefinedMessenger;

public enum AdminToolsLang implements DefinedMessenger {
    COMMAND_ADMIN_DEBUGGING_HITPOINTS_DESCRIPTION("{$s}A command to show damage indicators when you deal damage to an entity.", 1, 2, 3, 4),
    COMMAND_ADMIN_DEBUGGING_HITPOINTS_ENABLED("{$p}Hit Points{$colon} &2Enabled", 1, 2, 3, 4),
    COMMAND_ADMIN_DEBUGGING_HITPOINTS_DISABLED("{$p}Hit Points{$colon} &cDisabled", 1, 2, 3, 4),
    COMMAND_ADMIN_DEBUGGING_HITPOINTS_INDICATOR_DEALT("&5Dealt Damage{$colon}\n" +
            "  {$dot} {$p}Damage{$colon} {$s}%fancy@damage%\n" +
            "  {$dot} {$p}Source{$colon} {$s}%entity%\n" +
            "  {$dot} {$p}Tool{$colon} {$s}%tool%\n" +
            "  {$dot} {$p}Cause{$colon} {$s}%cause%", 1, 2, 3, 4, 5),
    COMMAND_ADMIN_DEBUGGING_HITPOINTS_INDICATOR_RECEIVED("&5Took Damage{$colon}\n" +
            "  {$dot} {$p}Damage{$colon} {$s}%fancy@damage%\n" +
            "  {$dot} {$p}Source{$colon} {$s}%entity%\n" +
            "  {$dot} {$p}Tool{$colon} {$s}%tool%\n" +
            "  {$dot} {$p}Cause{$colon} {$s}%cause%", 1, 2, 3, 4, 5),

    COMMAND_ADMIN_SQL_DESCRIPTION("{$p}SQL related commands when you're running a SQL database.", 1, 2, 3),
    COMMAND_ADMIN_SQL_NOT_USING_SQL("{$e}The database you're using is not a SQL database{$colon} {$es}%database%", 1, 2, 3),

    COMMAND_ADMIN_SQL_EXECUTE_DESCRIPTION("{$p}Execute a direct SQL statement and get the results.", 1, 2, 3, 4),
    COMMAND_ADMIN_SQL_EXECUTE_EXECUTED("{$p}Result{$colon} {$s}%result%", 1, 2, 3, 4),

    ;

    private final LanguageEntry languageEntry;
    private final String defaultValue;

    AdminToolsLang(String defaultValue, int... group) {
        this.defaultValue = defaultValue;
        this.languageEntry = DefinedMessenger.getEntry(null, this, group);
    }

    @NotNull
    @Override
    public LanguageEntry getLanguageEntry() {
        return languageEntry;
    }

    @Nullable
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
