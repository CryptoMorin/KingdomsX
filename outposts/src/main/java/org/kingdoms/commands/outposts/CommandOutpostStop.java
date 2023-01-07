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

import java.util.ArrayList;
import java.util.List;

public class CommandOutpostStop extends KingdomsCommand {
    public CommandOutpostStop(KingdomsParentCommand parent) {
        super("stop", parent);
    }

    @Override
    public void execute(CommandContext context) {
        if (context.requireArgs(1)) return;

        Outpost outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return;

        OutpostEvent event = OutpostEvent.getEvent(outpost.getName());
        if (event == null) {
            context.sendError(OutpostsLang.COMMAND_OUTPOST_STOP_NOT_STARTED, "outpost", outpost.getName());
            return;
        }

        event.stop(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            OutpostsLang.COMMAND_OUTPOST_STOP_STOPPED.sendMessage(player, "outpost", outpost.getName());
        }
    }

    @Override
    public @NonNull
    List<String> tabComplete(@NonNull CommandSender sender, @NonNull String[] args) {
        if (args.length == 1) return new ArrayList<>(Outpost.getOutposts().keySet());
        return KingdomsCommand.emptyTab();
    }
}
