package org.kingdoms.commands.outposts;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.*;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.outposts.OutpostEvent;
import org.kingdoms.outposts.OutpostsLang;
import org.kingdoms.utils.time.TimeFormatter;
import org.kingdoms.utils.time.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandOutpostStart extends KingdomsCommand {
    public CommandOutpostStart(KingdomsParentCommand parent) {
        super("start", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.requireArgs(3)) return CommandResult.FAILED;
        if (CommandOutpost.worldGuardMissing(context.getMessageReceiver())) return CommandResult.FAILED;

        // TODO fix thess
        String[] args = context.args;
        CommandSender sender = context.getMessageReceiver();

        OutpostEventSettings outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return CommandResult.FAILED;

        if (OutpostEvent.isEventRunning(outpost.getName())) {
            OutpostsLang.COMMAND_OUTPOST_START_ALREADY_STARTED.sendError(sender, "outpost", outpost.getName());
            return CommandResult.FAILED;
        }

        Long time = TimeUtils.parseTime(args[1]);
        Long startsIn = TimeUtils.parseTime(args[2]);

        if (time == null || time <= 0) {
            OutpostsLang.COMMAND_OUTPOST_START_INVALID_TIME.sendError(sender, "time", args[1]);
            return CommandResult.FAILED;
        }

        if (startsIn == null || startsIn < 0) {
            OutpostsLang.COMMAND_OUTPOST_START_INVALID_TIME.sendError(sender, "time", args[2]);
            return CommandResult.FAILED;
        }

        if (startsIn > 1000) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                OutpostsLang.COMMAND_OUTPOST_START_SCHEDULED.sendMessage(player, "outpost", outpost.getName(), "start", TimeFormatter.of(startsIn));
            }
        }

        OutpostEvent.startEvent(outpost, time, TimeUnit.MILLISECONDS.toSeconds(startsIn) * 20L);
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull
    List<String> tabComplete(@NonNull CommandTabContext context) {
        if (context.isAtArg(0)) return new ArrayList<>(OutpostEventSettings.getOutposts().keySet());
        if (context.isAtArg(1)) return Collections.singletonList("<time>");
        if (context.isAtArg(2)) return Collections.singletonList("<start time>");
        return KingdomsCommand.emptyTab();
    }
}
