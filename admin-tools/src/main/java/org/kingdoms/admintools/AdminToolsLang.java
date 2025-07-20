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

    ITEM_EDITOR_NAME_ENTER("{$p}Enter the item's name {$cancel}", 2, 3),
    ITEM_EDITOR_LORE_ENTER("{$p}Enter the item's lore separated by newlines {$cancel}", 2, 3),
    ITEM_EDITOR_MATERIAL_ENTER("{$p}Enter the item's material {$cancel}", 2, 3),
    ITEM_EDITOR_AMOUNT_ENTER("{$p}Enter the amount of items {$cancel}", 2, 3),
    ITEM_EDITOR_AMOUNT_WARNING("{$e}The specified amount isn't standard. Minecraft item counts can only go up to {$es}127{$e}, any number higher than that will " +
            "either not show the item or cause other weird behaviors.", 2, 3),
    ITEM_EDITOR_ENCHANT_ENTER("{$p}Enter the enchantment level {$cancel}", 2, 3),
    ITEM_EDITOR_ATTRIBUTES_AMOUNT_ENTER("{$p}Enter the attribute's amount {$cancel}", 2, 3, 4),
    ITEM_EDITOR_ATTRIBUTES_NAME_ENTER("{$p}Enter the attribute's name {$cancel}", 2, 3, 4),
    ITEM_EDITOR_ATTRIBUTES_UUID_ENTER("{$p}Enter the attribute's UUID {$cancel}", 2, 3, 4),
    ITEM_EDITOR_CUSTOM_MODEL_DATA_ENTER("{$p}Enter the custom model data number {$cancel}", 2, 5),
    ITEM_EDITOR_CUSTOM_MODEL_DATA_NOT_SUPPORTED("{$e}Custom model datas are not supported in your server version.", 2, 5),
    ITEM_EDITOR_NBT_NAME_ENTER("{$p}Please enter the NBT's path name {$cancel}", 2, 3, 4),
    ITEM_EDITOR_NBT_VALUE_ENTER("{$p}Please enter the NBT's value {$cancel}", 2, 3, 4),
    ITEM_EDITOR_NBT_VALUE_ERROR("{$e}Error when setting value{$colon} {$es}%error%", 2, 3, 4),

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
