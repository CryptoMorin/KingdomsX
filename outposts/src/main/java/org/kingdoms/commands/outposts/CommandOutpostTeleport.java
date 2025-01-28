package org.kingdoms.commands.outposts;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.*;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.LocationLocale;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.outposts.OutpostEvent;
import org.kingdoms.outposts.OutpostsLang;
import org.kingdoms.utils.PaperUtils;

import java.util.List;

public class CommandOutpostTeleport extends KingdomsCommand {
    public CommandOutpostTeleport(KingdomsParentCommand parent) {
        super("teleport", parent, PermissionDefault.TRUE);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.assertPlayer()) return CommandResult.FAILED;
        if (context.requireArgs(1)) return CommandResult.FAILED;
        if (context.assertHasKingdom()) return CommandResult.FAILED;

        OutpostEventSettings outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return CommandResult.FAILED;
        context.var("outpost", outpost.getName()).var("min", outpost.getMinOnlineMembers());

        context.sendMessage(KingdomsLang.COMMAND_ADMIN_LAND_PREPARING);
        SimpleChunkLocation chunk = SimpleChunkLocation.of(outpost.getSpawn());
        PaperUtils.prepareChunks(chunk).thenRun(() -> {
            Player player = context.senderAsPlayer();
            Location playerDir = player.getLocation();
            Location location = chunk.getCenterLocation();

            location.setYaw(playerDir.getYaw());
            location.setPitch(playerDir.getPitch());
            Kingdoms.taskScheduler().sync().execute(() -> player.teleport(outpost.getSpawn()));

            LocationLocale.of(location).withBuilder(context.getMessageContext()).build();
            LocationLocale.of(chunk).withBuilder(context.getMessageContext()).withPrefix("chunk_").build();
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_TELEPORT_TELEPORTED);
        });

        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull List<String> tabComplete(@NonNull CommandTabContext context) {
        return context.isAtArg(0) ? context.suggest(0, OutpostEvent.getEvents().keySet()) : emptyTab();
    }
}
