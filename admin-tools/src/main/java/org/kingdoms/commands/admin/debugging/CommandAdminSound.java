package org.kingdoms.commands.admin.debugging;

import com.cryptomorin.xseries.XSound;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.admintools.AdminToolsLang;
import org.kingdoms.commands.*;
import org.kingdoms.commands.admin.CommandAdmin;
import org.kingdoms.commands.annotations.Cmd;
import org.kingdoms.commands.annotations.CmdParent;
import org.kingdoms.commands.annotations.CmdPerm;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.server.permission.PermissionDefaultValue;
import org.kingdoms.utils.string.Strings;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Cmd("sound")
@CmdParent(CommandAdmin.class)
@CmdPerm(PermissionDefaultValue.OP)
public class CommandAdminSound extends KingdomsCommand {
    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        context.requireArgs(1);

        XSound.Record record;
        try {
            record = XSound.parse(String.join(",", context.args));
        } catch (Throwable ex) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_SOUND_ERROR, "error", ex.getMessage());
            return CommandResult.FAILED;
        }
        if (record == null) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_SOUND_ERROR, "error", "No Sound");
        }

        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_SOUND_PLAYING, "sound", record.toString());
        record.soundPlayer().forPlayers(context.senderAsPlayer()).play();
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull List<String> tabComplete(CommandTabContext context) {
        if (context.isAtArg(0)) {
            String arg = context.arg(0);
            boolean publicSound;
            if (arg.startsWith("~")) {
                publicSound = true;
                arg = arg.substring(1);
            } else {
                publicSound = false;
            }

            String category;
            int index = arg.indexOf('@');
            if (index != -1) {
                category = arg.substring(0, index);
                arg = arg.substring(index + 1);
            } else {
                category = null;
            }

            String sound = Strings.replace(arg.toUpperCase(Locale.ENGLISH), '-', '_').toString();
            String soundPrefix = (publicSound ? "~" : "") + (category != null ? category + '@' : "");
            return XSound.getValues().stream()
                    .map(XSound::name)
                    .filter(x -> x.toUpperCase(Locale.ENGLISH).replace('-', '_').contains(sound))
                    .map(x -> soundPrefix + x)
                    .collect(Collectors.toList());
        }

        if (context.isAtArg(1)) return context.tabComplete("&9[volume]");
        if (context.isAtArg(2)) return context.tabComplete("&6[pitch]");
        if (context.isAtArg(3)) return context.tabComplete("&5[seed]");

        return context.emptyTab();
    }
}
