package org.kingdoms.commands.admin;

import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.admintools.AdminToolsLang;
import org.kingdoms.commands.*;
import org.kingdoms.commands.annotations.Cmd;
import org.kingdoms.commands.annotations.CmdParent;
import org.kingdoms.commands.annotations.CmdPerm;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.Language;
import org.kingdoms.managers.TempKingdomsFolder;
import org.kingdoms.server.permission.PermissionDefaultValue;
import org.kingdoms.utils.string.Strings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Cmd("commands")
@CmdParent(CommandAdmin.class)
@CmdPerm(PermissionDefaultValue.OP)
public class CommandAdminCommands extends KingdomsCommand {
    @Override
    public CommandResult execute(CommandContext context) {
        boolean defaultsOnly = false;
        if (context.hasArgs(1)) {
            if (context.arg(0).equalsIgnoreCase("defaults")) defaultsOnly = true;
        }

        Path path = TempKingdomsFolder.getOrCreateFile("command-permissions.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (KingdomsCommand cmd : KingdomsCommandHandler.getCommands(Language.getDefault()).values()) {
                if (defaultsOnly && cmd.getPermission().getDefault() != PermissionDefault.TRUE) continue;
                writer.write(cmd.getName() + " (" + Strings.join(cmd.getAliases().get(Language.getDefault()).toArray(), ", ") + ") " + cmd.getPermission().getName());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_COMMANDS_DONE, "output", path.toAbsolutePath());
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull List<String> tabComplete(CommandTabContext context) {
        return context.isAtArg(0) ? context.tabComplete("defaults") : context.emptyTab();
    }
}
