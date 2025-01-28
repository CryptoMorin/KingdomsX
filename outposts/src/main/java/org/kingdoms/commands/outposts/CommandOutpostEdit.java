package org.kingdoms.commands.outposts;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.*;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.outposts.OutpostEditor;

import java.util.List;

public class CommandOutpostEdit extends KingdomsCommand {
    public CommandOutpostEdit(KingdomsParentCommand parent) {
        super("edit", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.assertPlayer()) return CommandResult.FAILED;
        if (context.requireArgs(1)) return CommandResult.FAILED;

        OutpostEventSettings outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return CommandResult.FAILED;

        Player player = context.senderAsPlayer();
        new OutpostEditor(player, outpost).openOutpostEditor();
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull List<String> tabComplete(CommandTabContext context) {
        if (context.isAtArg(0)) return KingdomsCommand.tabComplete(OutpostEventSettings.getOutposts().keySet());
        return KingdomsCommand.emptyTab();
    }
}
