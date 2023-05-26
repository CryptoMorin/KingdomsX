package org.kingdoms.commands.outposts;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.outposts.Outpost;
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
    public void execute(CommandContext context) {
        if (context.requireArgs(3)) return;
        if (CommandOutpost.worldGuardMissing(context.getSender())) return;

        // TODO fix thess
        String[] args = context.args;
        CommandSender sender = context.getSender();

        Outpost outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return;

        if (OutpostEvent.isEventRunning(outpost.getName())) {
            OutpostsLang.COMMAND_OUTPOST_START_ALREADY_STARTED.sendError(sender, "outpost", outpost.getName());
            return;
        }

        Long time = TimeUtils.parseTime(args[1], TimeUnit.MINUTES);
        Long startsIn = TimeUtils.parseTime(args[2], TimeUnit.MINUTES);

        if (time == null || time <= 0) {
            OutpostsLang.COMMAND_OUTPOST_START_INVALID_TIME.sendError(sender, "time", args[1]);
            return;
        }

        if (startsIn == null || startsIn < 0) {
            OutpostsLang.COMMAND_OUTPOST_START_INVALID_TIME.sendError(sender, "time", args[2]);
            return;
        }

        if (startsIn > 1000) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                OutpostsLang.COMMAND_OUTPOST_START_SCHEDULED.sendMessage(player, "outpost", outpost.getName(), "start", TimeFormatter.of(startsIn));
            }
        }

        OutpostEvent.startEvent(outpost, time, TimeUnit.MILLISECONDS.toSeconds(startsIn) * 20L);
    }

    @Override
    public @NonNull
    List<String> tabComplete(@NonNull CommandSender sender, @NonNull String[] args) {
        if (args.length == 1) return new ArrayList<>(Outpost.getOutposts().keySet());
        if (args.length == 2) return Collections.singletonList("<time>");
        if (args.length == 3) return Collections.singletonList("<start time>");
        return KingdomsCommand.emptyTab();
    }
}
