package org.kingdoms.commands.outposts;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.outposts.Outpost;
import org.kingdoms.outposts.OutpostDataHandler;
import org.kingdoms.outposts.OutpostsLang;
import org.kingdoms.services.managers.ServiceHandler;
import org.kingdoms.services.managers.SoftService;

import java.util.List;

public class CommandOutpostCreate extends KingdomsCommand {
    public CommandOutpostCreate(KingdomsParentCommand parent) {
        super("create", parent);
    }

    @Override
    public void execute(CommandContext context) {
        if (context.assertPlayer()) return;
        if (context.requireArgs(2)) return;
        if (CommandOutpost.worldGuardMissing(context.getSender())) return;

        // TODO fix thess
        String[] args = context.args;
        CommandSender sender = context.getSender();

        String outpostName = args[0];
        if (Outpost.getOutpost(outpostName) != null) {
            OutpostsLang.COMMAND_OUTPOST_CREATE_NAME_ALREADY_TAKEN.sendMessage(sender, "outpost", outpostName);
            return;
        }

        String region = args[1];
        Player player = context.senderAsPlayer();
        if (!ServiceHandler.getWorldGuardService().hasRegion(player.getWorld(), region)) {
            OutpostsLang.COMMAND_OUTPOST_CREATE_REGION_NOT_FOUND.sendMessage(sender, "region", region);
            return;
        }

        Outpost outpost = new Outpost(outpostName, region, player.getLocation(), player.getLocation());
        Outpost.registerOutpost(outpost);
        OutpostDataHandler.saveOutposts();
        OutpostsLang.COMMAND_OUTPOST_CREATE_CREATED.sendMessage(sender, "outpost", outpostName, "region", region);
    }

    @Override
    public @NonNull
    List<String> tabComplete(@NonNull CommandSender sender, @NonNull String[] args) {
        if (args.length == 1) return KingdomsCommand.tabComplete("<name>");
        if (sender instanceof Player && args.length == 2 && SoftService.WORLD_GUARD.isAvailable()) {
            return KingdomsCommand.tabComplete(ServiceHandler.getWorldGuardService().getRegions(((Player) sender).getWorld()));
        }
        return KingdomsCommand.emptyTab();
    }
}
