package org.kingdoms.commands.outposts;

import org.bukkit.command.CommandSender;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.locale.messenger.StaticMessenger;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.outposts.OutpostsLang;
import org.kingdoms.services.managers.SoftService;

public class CommandOutpost extends KingdomsParentCommand {
    public static boolean worldGuardMissing(CommandSender sender) {
        if (SoftService.WORLD_GUARD.isAvailable()) return false;
        new StaticMessenger("&4You need to install &ehover:{&nWorldGuard;Click to open the download page;@https://dev.bukkit.org/projects/worldguard} " +
                "&4plugin in order to use Outpost events."
        ).sendError(sender, new MessagePlaceholderProvider().usePrefix());
        return true;
    }

    protected static OutpostEventSettings getOutpost(CommandContext context, int index) {
        String outpostName = context.arg(index);
        OutpostEventSettings outpost = OutpostEventSettings.getOutpost(outpostName);
        if (outpost == null) context.sendError(OutpostsLang.COMMAND_OUTPOST_NOT_FOUND, "outpost", outpostName);
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
        new CommandOutpostTeleport(this);
    }
}
