package org.kingdoms.commands.outposts;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.main.locale.KingdomsLang;
import org.kingdoms.main.locale.messager.MessageBuilder;
import org.kingdoms.outposts.LogKingdomOutpostJoin;
import org.kingdoms.outposts.Outpost;
import org.kingdoms.outposts.OutpostAddon;
import org.kingdoms.outposts.OutpostEvent;
import org.kingdoms.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandOutpostJoin extends KingdomsCommand {
    public CommandOutpostJoin(KingdomsParentCommand parent) {
        super("join", parent, PermissionDefault.TRUE);
    }

    @Override
    public void execute(CommandContext context) {
        if (context.assertPlayer()) return;
        if (context.requireArgs(1)) return;

        // TODO fix thess
        String[] args = context.args;
        CommandSender sender = context.sender;

        Outpost outpost = CommandOutpost.getOutpost(context, 0);
        if (outpost == null) return;

        OutpostEvent event = outpost.getEvent();
        if (event == null) {
            KingdomsLang.COMMAND_OUTPOST_JOIN_EVENT_NOT_STARTED.sendMessage(sender, "outpost", args[0]);
            return;
        }

        Player player = context.senderAsPlayer();
        KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
        Kingdom kingdom = kp.getKingdom();
        if (kingdom == null) {
            KingdomsLang.NO_KINGDOM_DEFAULT.sendMessage(player);
            return;
        }

        if (OutpostEvent.getKingdomsInEvents().containsKey(kingdom.getId())) {
            KingdomsLang.COMMAND_OUTPOST_JOIN_ALREADY.sendError(player);
            return;
        }

        if (!kp.isAdmin()) {
            if (event.isFull()) {
                KingdomsLang.COMMAND_OUTPOST_JOIN_EVENT_FULL.sendMessage(sender, "outpost", args[0]);
                return;
            }

            if (kingdom.getOnlineMembers().size() < outpost.getMinOnlineMembers()) {
                KingdomsLang.COMMAND_OUTPOST_JOIN_MIN_ONLINE_MEMBERS.sendMessage(sender, "outpost", args[0], "min", outpost.getMinOnlineMembers());
                return;
            }

            if (!kp.hasPermission(OutpostAddon.OUTPOST_JOIN_PERMISSION)) {
                KingdomsLang.COMMAND_OUTPOST_JOIN_PERMISSION.sendMessage(sender, "outpost", args[0]);
                return;
            }

            MessageBuilder settings = new MessageBuilder().withContext(kingdom);

            long rp = 0;
            double cost = 0;

            if (outpost.getResourcePointsCost() != null) {
                rp = (long) MathUtils.eval(outpost.getResourcePointsCost(), settings);
                if (!kingdom.hasResourcePoints(rp)) {
                    KingdomsLang.COMMAND_OUTPOST_JOIN_NOT_ENOUGH_RESOURCE_POINTS.sendMessage(sender, "outpost", args[0], "cost", rp);
                    return;
                }
            }

            if (outpost.getMoneyCost() != null) {
                cost = MathUtils.eval(outpost.getMoneyCost(), settings);
                if (!kingdom.hasMoney(cost)) {
                    KingdomsLang.COMMAND_OUTPOST_JOIN_NOT_ENOUGH_MONEY.sendMessage(sender, "outpost", args[0], "cost", cost);
                    return;
                }
            }

            kingdom.addResourcePoints(-rp);
            kingdom.addBank(-cost);
        }

        for (Player member : kingdom.getOnlineMembers()) {
            KingdomsLang.COMMAND_OUTPOST_JOIN_JOINED.sendMessage(member, "outpost", args[0]);
        }
        for (UUID kingdomId : event.getParticipants().keySet()) {
            Kingdom participant = Kingdom.getKingdom(kingdomId);
            for (Player member : participant.getOnlineMembers()) {
                KingdomsLang.COMMAND_OUTPOST_JOIN_ANNOUNCEMENT.sendMessage(member, "kingdom", kingdom.getName(), "outpost", args[0]);
            }
        }

        event.participate(player, kingdom);
        kingdom.log(new LogKingdomOutpostJoin(player.getUniqueId(), outpost.getName()));
    }

    @Override
    public @NonNull List<String> tabComplete(@NonNull CommandSender sender, @NonNull String[] args) {
        if (args.length == 1) return new ArrayList<>(OutpostEvent.getEvents().keySet());
        return new ArrayList<>();
    }
}
