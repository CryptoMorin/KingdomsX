package org.kingdoms.enginehub;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.locale.LanguageEntry;
import org.kingdoms.locale.messenger.DefinedMessenger;

public enum EngineHubLang implements DefinedMessenger {
    COMMAND_CLAIM_IN_REGION("{$e}You cannot claim lands in protected regions.", 1, 2),
    COMMAND_CLAIM_NEAR_REGION("{$e}You cannot claim lands near protected regions.", 1, 2),

    COMMAND_ADMIN_SCHEMATIC_TIPS_SIDE_EFFECTS("{$sep}[&9!{$sep}] &9Note {$point} &7You can use &2hover:{/k admin schematic origin;&7Click to copy;|/k admin schematic origin}" +
            " &7command to change the origin of the schematic for accurate placement of the building for Kingdoms.", 1, 2, 3, 4),

    INVASION_BLOCKED_DAMAING_CHAMPION_IN_PROTECTED_REGION("{$e}You cannot damage the champion in this region.", 1, 2),

    WORLDEDIT_EXCLUDED("{$e}A total of {$es}%blocks% {$e}were excluded from WorldEdit since they weren't in your kingdoms land.", 1),

    COMMAND_ADMIN_SCHEMATIC_SAVE_DESCRIPTION("{$s}Saves a schematic from the current player's WorldEdit selection clipboard.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_SAVE_USAGE("{$usage}admin schematic save &2<schematic name>", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_SELECTION("{$e}You need to make a selection first using {$es}hover:{WorldEdit;&7Click here to get\nthe selection tool;//wand} {$e}and copying it using the {$es}hover:{//copy;&7Click to run;//copy} {$e}command.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_SAVE_EMPTY_CLIPBOARD("{$e}You need to copy your selection with the {$es}hover:{//copy;&7Click to run;//copy} {$e}command.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_SAVE_ALREADY_EXISTS("{$e}A schematic with that name already exists. If you're sure that you want to override it, hover:{&7click here;&4Click to override;/k admin schematic save %schematic_name%}.\n" +
            "{$sep}[&c!{$sep}] &cWARNING {$point} &4This will cause issues if there is already a building with this schematic is placed in the world" +
            "from kingdoms. Please make sure to either remove all of them before overriding this file, or choose another name and change the settings to reflect the new name in the config.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_SAVE_SAVED("{$p}Successfully saved this schematic to{$colon} {$s}%schematic_file%", 1, 2, 3, 4),

    COMMAND_ADMIN_SCHEMATIC_LOAD_DESCRIPTION("{$s}Loads a schematic from Kingdom's schematics folder into player's WorldEdit clipboard.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_LOAD_USAGE("{$usage}admin schematic load &2<schematic name>", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_LOAD_DOESNT_EXISTS("{$e}Schematic {$es}%schematic_name% {$e}doesn't exists in Kingdoms schematic folder.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_LOAD_LOADED("{$p}Successfully loaded the schematic into your clipboard. You can paste it with hover:{{$s}//paste;&7Click to paste;//paste}\n" +
            "{$p}You can hover:{{$s}click here;&7Click to override;/k admin schematic save %schematic_name%} {$p}to override the saved file once you're done.", 1, 2, 3, 4),

    COMMAND_ADMIN_SCHEMATIC_LIST_DESCRIPTION("{$s}Lists all the available schematics.", 1, 2, 3, 4),

    COMMAND_ADMIN_SCHEMATIC_ORIGIN_DESCRIPTION("{$s}Changes the copy origin of the current WorldEdit clipboard.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_ORIGIN_USAGE("{$usage}admin schematic origin &2<x> <y> <z>", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_ORIGIN_CHANGED("{$p}Successfully changed the origin of the clipboard.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_ORIGIN_TOOL_ENABLED("{$p}Origin tool is now enabled. Hit a block on the schematic to make it the origin.\n" +
            "&7Gray {$point} &7Previous origin.\n" +
            "&2Green {$point} &2New origin.", 1, 2, 3, 4, 5),
    COMMAND_ADMIN_SCHEMATIC_ORIGIN_TOOL_DISABLED("{$e}Origin tool is now disabled.", 1, 2, 3, 4, 5),

    COMMAND_ADMIN_SCHEMATIC_CONVERTALL_DESCRIPTION("{$s}Converts all the schematics in the Kingdoms folder to the specified format.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_CONVERTALL_USAGE("{$usage}admin schematic convertAll &2<format>", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_CONVERTALL_UNKNOWN_FORMAT("{$e}Unknown schematic format{$colon} {$es}%format%", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_CONVERTALL_CONVERTED("{$p}Converted a total of {$s}%converted% {$p}schematics.\nStats{$colon} {$s}%stats%", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_CONVERTALL_CONVERT_ERROR("{$e}Error while converting {$es}%file%{$colon} {$es}%error%", 1, 2, 3, 4),

    COMMAND_ADMIN_SCHEMATIC_SETUP_DESCRIPTION("{$s}Prepares this addon for use. Changes the structure/turret configs in order to adjust holograms and functional points based on the schematics.", 1, 2, 3, 4),
    COMMAND_ADMIN_SCHEMATIC_SETUP_DONE("{$p}Addon is ready. Restart to apply the changes.\n" +
            "{$note} If you ever decided to remove the addon, you'd have to revert your building.yml settings manually.\n" +
            "{$warning} You cannot remove the addon unless all buildings placed down by this addon are removed.", 1, 2, 3, 4),
    ;

    private final LanguageEntry languageEntry;
    private final String defaultValue;

    EngineHubLang(String defaultValue, int... group) {
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
