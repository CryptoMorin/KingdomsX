package org.kingdoms.commands.outposts;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.*;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.outposts.*;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.utils.MathUtils;

import java.util.List;
import java.util.UUID;

public class CommandOutpostJoin extends KingdomsCommand {
    public CommandOutpostJoin(KingdomsParentCommand parent) {
        super("join", parent, PermissionDefault.TRUE);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (context.assertPlayer()) return CommandResult.FAILED;
        if (context.requireArgs(1)) return CommandResult.FAILED;
        if (context.assertHasKingdom()) return CommandResult.FAILED;

        OutpostEventSettings outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return CommandResult.FAILED;
        context.var("outpost", outpost.getName()).var("min", outpost.getMinOnlineMembers());

        OutpostEvent event = outpost.getEvent();
        if (event == null) {
            return context.fail(OutpostsLang.COMMAND_OUTPOST_JOIN_EVENT_NOT_STARTED);
        }

        Player player = context.senderAsPlayer();
        KingdomPlayer kp = context.getKingdomPlayer();
        Kingdom kingdom = kp.getKingdom();

        if (OutpostEvent.getKingdomsInEvents().containsKey(kingdom.getId())) {
            return context.fail(OutpostsLang.COMMAND_OUTPOST_JOIN_ALREADY);
        }

        if (!kp.isAdmin()) {
            if (event.isFull()) {
                return context.fail(OutpostsLang.COMMAND_OUTPOST_JOIN_EVENT_FULL);
            }

            if (kingdom.getOnlineMembers().size() < outpost.getMinOnlineMembers()) {
                return context.fail(OutpostsLang.COMMAND_OUTPOST_JOIN_MIN_ONLINE_MEMBERS);
            }

            if (!kp.hasPermission(OutpostAddon.OUTPOST_JOIN_PERMISSION)) {
                return context.fail(OutpostsLang.COMMAND_OUTPOST_JOIN_PERMISSION);
            }

            MessagePlaceholderProvider settings = new MessagePlaceholderProvider().withContext(kingdom);

            long rp = 0;
            double cost = 0;

            if (outpost.getResourcePointsCost() != null) {
                rp = (long) MathUtils.eval(outpost.getResourcePointsCost(), settings);
                context.var("cost", rp);
                if (!kingdom.getResourcePoints().has(rp)) {
                    return context.fail(OutpostsLang.COMMAND_OUTPOST_JOIN_NOT_ENOUGH_RESOURCE_POINTS);
                }
            }

            if (outpost.getMoneyCost() != null) {
                cost = MathUtils.eval(outpost.getMoneyCost(), settings);
                context.var("cost", cost);
                if (!kingdom.getBank().has(cost)) {
                    return context.fail(OutpostsLang.COMMAND_OUTPOST_JOIN_NOT_ENOUGH_MONEY);
                }
            }

            kingdom.getResourcePoints().add(-rp);
            kingdom.getBank().add(-cost);
        }

        for (Player member : kingdom.getOnlineMembers()) {
            context.sendMessage(member, OutpostsLang.COMMAND_OUTPOST_JOIN_JOINED);
        }

        context.var("kingdom", kingdom.getName());
        for (UUID kingdomId : event.getParticipants().keySet()) {
            Kingdom participant = Kingdom.getKingdom(kingdomId);
            for (Player member : participant.getOnlineMembers()) {
                context.sendMessage(member, OutpostsLang.COMMAND_OUTPOST_JOIN_ANNOUNCEMENT);
            }
        }

        event.participate(player, kingdom);
        kingdom.log(new LogKingdomOutpostJoin(player.getUniqueId(), outpost.getName()));
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull List<String> tabComplete(@NonNull CommandTabContext context) {
        return context.isAtArg(0) ? context.suggest(0, OutpostEvent.getEvents().keySet()) : emptyTab();
    }
}
