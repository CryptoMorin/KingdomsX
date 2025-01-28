package org.kingdoms.commands.outposts;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.*;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.outposts.OutpostEvent;
import org.kingdoms.outposts.OutpostsLang;

import java.util.ArrayList;
import java.util.List;

public class CommandOutpostStop extends KingdomsCommand {
    public CommandOutpostStop(KingdomsParentCommand parent) {
        super("stop", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.requireArgs(1)) return CommandResult.FAILED;

        OutpostEventSettings outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return CommandResult.FAILED;

        OutpostEvent event = OutpostEvent.getEvent(outpost.getName());
        if (event == null) {
            context.sendError(OutpostsLang.COMMAND_OUTPOST_STOP_NOT_STARTED, "outpost", outpost.getName());
            return CommandResult.FAILED;
        }

        event.stop(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            OutpostsLang.COMMAND_OUTPOST_STOP_STOPPED.sendMessage(player, "outpost", outpost.getName());
        }

        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull
    List<String> tabComplete(@NonNull CommandTabContext context) {
        if (context.isAtArg(0)) return new ArrayList<>(OutpostEventSettings.getOutposts().keySet());
        return KingdomsCommand.emptyTab();
    }
}
