package org.kingdoms.commands.outposts;

import org.bukkit.command.CommandSender;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.main.locale.KingdomsLang;
import org.kingdoms.main.locale.compiler.MessageCompiler;
import org.kingdoms.main.locale.messager.MessageBuilder;
import org.kingdoms.outposts.Outpost;
import org.kingdoms.services.managers.SoftService;

public class CommandOutpost extends KingdomsParentCommand {
    protected static boolean worldGuardMissing(CommandSender sender) {
        if (SoftService.WORLD_GUARD.isAvailable()) return false;
        MessageCompiler.compile("&4You need to install hover:{&e&nWorldGuard;Click to open the download page;@https://dev.bukkit.org/projects/worldguard} " +
                "plugin in order to use Outpost events."
        ).getExtraProvider().err().send(sender, new MessageBuilder().usePrefix());
        return true;
    }

    protected static Outpost getOutpost(CommandContext context, int index) {
        String outpostName = context.arg(index);
        Outpost outpost = Outpost.getOutpost(outpostName);
        if (outpost == null) context.sendError(KingdomsLang.COMMAND_OUTPOST_NOT_FOUND, "outpost", outpostName);
        return outpost;
    }

    public CommandOutpost() {
        super("outpost");
        if (isDisabled()) return;

        new CommandOutpostCreate(this);
        new CommandOutpostJoin(this);
        new CommandOutpostStart(this);
        new CommandOutpostStop(this);
        new CommandOutpostEdit(this);
    }
}
