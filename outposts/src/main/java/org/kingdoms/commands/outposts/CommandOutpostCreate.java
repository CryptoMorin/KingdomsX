package org.kingdoms.commands.outposts;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.*;
import org.kingdoms.enginehub.EngineHubAddon;
import org.kingdoms.main.KLogger;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.outposts.OutpostDataHandler;
import org.kingdoms.outposts.OutpostsLang;
import org.kingdoms.services.managers.SoftService;

import java.util.List;

public class CommandOutpostCreate extends KingdomsCommand {
    public CommandOutpostCreate(KingdomsParentCommand parent) {
        super("create", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.assertPlayer()) return CommandResult.FAILED;
        if (context.requireArgs(2)) return CommandResult.FAILED;
        if (CommandOutpost.worldGuardMissing(context.getMessageReceiver())) return CommandResult.FAILED;

        // TODO fix thess
        String[] args = context.args;
        CommandSender sender = context.getMessageReceiver();

        String outpostName = args[0];
        if (OutpostEventSettings.getOutpost(outpostName) != null) {
            OutpostsLang.COMMAND_OUTPOST_CREATE_NAME_ALREADY_TAKEN.sendMessage(sender, "outpost", outpostName);
            return CommandResult.FAILED;
        }

        String region = args[1];
        Player player = context.senderAsPlayer();
        if (!EngineHubAddon.INSTANCE.getWorldGuard().hasRegion(player.getWorld(), region)) {
            OutpostsLang.COMMAND_OUTPOST_CREATE_REGION_NOT_FOUND.sendMessage(sender, "region", region);
            return CommandResult.FAILED;
        }

        OutpostEventSettings outpost = new OutpostEventSettings(outpostName, region, player.getLocation(), player.getLocation());
        OutpostEventSettings.registerOutpost(outpost);
        OutpostDataHandler.saveOutposts();
        OutpostsLang.COMMAND_OUTPOST_CREATE_CREATED.sendMessage(sender, "outpost", outpostName, "region", region);
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull
    List<String> tabComplete(@NonNull CommandTabContext context) {
        if (context.isAtArg(0)) return context.tabComplete("<name>");
        if (context.isPlayer() && context.isAtArg(1) && SoftService.WORLD_GUARD.isAvailable()) {
            return context.tabComplete(EngineHubAddon.INSTANCE.getWorldGuard().getRegions(context.senderAsPlayer().getWorld()));
        }
        return context.emptyTab();
    }
}
