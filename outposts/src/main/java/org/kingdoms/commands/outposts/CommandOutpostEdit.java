package org.kingdoms.commands.outposts;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.outposts.Outpost;
import org.kingdoms.outposts.OutpostEditor;

import java.util.List;

public class CommandOutpostEdit extends KingdomsCommand {
    public CommandOutpostEdit(KingdomsParentCommand parent) {
        super("edit", parent);
    }

    @Override
    public void execute(CommandContext context) {
        if (context.assertPlayer()) return;
        if (context.requireArgs(1)) return;

        Outpost outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return;

        Player player = context.senderAsPlayer();
        new OutpostEditor(player, outpost).openOutpostEditor();
    }

    @Override
    public @NonNull List<String> tabComplete(CommandTabContext context) {
        if (context.isAtArg(0)) return KingdomsCommand.tabComplete(Outpost.getOutposts().keySet());
        return KingdomsCommand.emptyTab();
    }
}
