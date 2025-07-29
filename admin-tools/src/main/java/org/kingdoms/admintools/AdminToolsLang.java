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

    COMMAND_ADMIN_RESETKINGDOM_DESCRIPTION("{$s}Reset various information about a kingdom.", 1, 2, 3),
    COMMAND_ADMIN_RESETKINGDOM_USAGE("{$usage}admin resetKingdom &2<kingdom> &2<data components>", 1, 2, 3),
    COMMAND_ADMIN_RESETKINGDOM_DONE("{$p}Operation successful.", 1, 2, 3),
    COMMAND_ADMIN_RESETKINGDOM_UNKNOWN_FACTOR("{$e}Unknown data component{$colon} {$es}%component%", 1, 2, 3),
    COMMAND_ADMIN_RESETKINGDOM_INVALID_NEGATIVE_FACTOR("{$e}A negative data component {$e}'%component%' {$e}is not necessary when not using the {$e}'EVERYTHING' {$e}component.", 1, 2, 3),
    COMMAND_ADMIN_RESETKINGDOM_INVALID_POSITIVE_FACTOR("{$e}A positive data component {$e}'%component%' {$e}is not necessary when using the {$e}'EVERYTHING' {$e}component.", 1, 2, 3),
    COMMAND_ADMIN_RESETKINGDOM_FAILED("{$e}Failed to process data component {$e}'%component%' {$e}Check your console for more information.", 1, 2, 3),

    COMMAND_ADMIN_EXECUTEBOOK_DESCRIPTION("{$s}Let's you run really long commands using a book.", 1, 2, 3),
    COMMAND_ADMIN_EXECUTEBOOK_GIVEN("{$p}You can use this book to write the command inside. Execute the command again while holding the book when you're done.", 1, 2, 3),
    COMMAND_ADMIN_EXECUTEBOOK_NOT_A_BOOK("{$e}The item you're holding is not a writtable book and your hotbar is not empty for you to get one either.", 1, 2, 3),
    COMMAND_ADMIN_EXECUTEBOOK_BOOK_EMPTY("{$e}The book you're holding has no written content.", 1, 2, 3),

    COMMAND_ADMIN_CONDITION_DESCRIPTION("{$s}Evaluates an expression with placeholders and math functions.", 1, 2, 3),
    COMMAND_ADMIN_CONDITION_USAGE("{$e}Usage{$colon} {$es}/k admin evaluate <expression>", 1, 2, 3),
    COMMAND_ADMIN_CONDITION_FAILED("{$e}Translated Expression{$colon} {$s}%translated%\n{$e}Error{$colon} {$es}%result%", 1, 2, 3),
    COMMAND_ADMIN_CONDITION_EVALUATED("{$p}Translated Expression{$colon} hover:{{$s}%translated%;{$s}%object-code%\n\n{$p}What is this?\n" +
            "&7This is the final representation that the\nmath compiler could understand from your expression.\nYou can see any errors such as operator\n" +
            "precedence and how subexpressions are grouped.\nYou might not see seme operations since they were optimized out.}\n" +
            "{$p}Evaluated Expression{$colon} {$s}%result%", 1, 2, 3),

    COMMAND_ADMIN_FOREACH_DESCRIPTION("{$s}Executes a command based on a condition for all elements of a certain thing. This could be players in a kingdom, kingdoms in a nation, etc."),
    COMMAND_ADMIN_FOREACH_USAGE("{$usage}admin forEach &econtextType&5=&2<\"playersInKingdom\" | \"kingdomsInNation\"> &econtext&5=&2<kingdom | nation> &9[&econdition&5=&9[condition]&9] &ecommand&5=&2<command>"),
    COMMAND_ADMIN_FOREACH_UNKNOWN_CONTEXT_TYPE("{$usage}admin forEach &econtextType&5=&2<\"playersInKingdom\" | \"kingdomsInNation\"> &econtext&5=&2<kingdom | nation> &9[&econdition&5=&9[condition]&9] &ecommand&5=&2<command>"),

    COMMAND_ADMIN_FILES_DESCRIPTION("{$s}For debugging purposes. Writes a list of all the files inside Kingdoms folder to a file."),
    COMMAND_ADMIN_FILES_DONE("{$p}The file tree has been written to {$s}hover:{%output%;&7Click to open;/k admin openfile %sanitized_output%}"),
    COMMAND_ADMIN_MISSINGGUIS_DESCRIPTION("{$s}For debugging purposes. Writes a list of all the GUI files that the installed language packs are missing."),
    COMMAND_ADMIN_MISSINGGUIS_NO_LANGUAGE_PACKS_INSTALLED("{$e}Your server doesn't have any language packs installed."),
    COMMAND_ADMIN_MISSINGGUIS_NO_MISSING_GUIS("{$e}No missing GUIs was found."),
    COMMAND_ADMIN_MISSINGGUIS_DONE("{$p}The file tree has been written to {$s}hover:{%output%;&7Click to open;" +
            "/k admin openfile %sanitized_output%}"),

    COMMAND_ADMIN_ENTITY_DESCRIPTION("{$s}Finds all the entities around the player.", 1, 2, 3),

    COMMAND_ADMIN_COMMANDS_DESCRIPTION("{$s}Writes a list of commands with their permission in a file."),
    COMMAND_ADMIN_COMMANDS_DONE("{$p}Wrote command information in {$s}%output%. {$p}Check Kingdomss plugin folder."),

    COMMAND_ADMIN_GUI_DESCRIPTION("{$s}Open a container to automatically parse the contests within it as a GUI config.\n" +
            "You can also give a GUI path as a parameter for it to open that GUI so you can modify items freely. Once you close that GUI, it'll save the content." +
            "How the GUI contents are parsed for these GUIs are special. For all conditional options, the first possible condition is picked and none of the colors " +
            "or placeholders will be translated."),
    COMMAND_ADMIN_GUI_USAGE("{$usage}admin gui &9[gui path]"),
    COMMAND_ADMIN_GUI_OPENED("{$p}Parsing the container... Please wait."),
    COMMAND_ADMIN_GUI_ENABLED("{$p}GUI mode is now &9enabled{$p}. Open a container to parse the contents."),
    COMMAND_ADMIN_GUI_DISABLED("{$p}GUI mode is now {$e}disabled{$p}."),
    COMMAND_ADMIN_GUI_DONE("{$p}Done. Saved the config to the plugin's folder in a file named {$s}hover:{%file%;&7Click to open;/k admin openfile %file%}"),

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
