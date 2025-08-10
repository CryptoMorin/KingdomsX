package org.kingdoms.commands.admin.info;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.admintools.AdminToolsLang;
import org.kingdoms.commands.*;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.data.Pair;
import org.kingdoms.locale.KingdomsLang;

import java.util.List;

public class CommandAdminInfoPlayer extends KingdomsCommand {
    public CommandAdminInfoPlayer(KingdomsParentCommand parent) {
        super("player", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (!context.isPlayer() && context.requireArgs(1)) return CommandResult.FAILED;

        // TODO fix thess
        String[] args = context.args;
        CommandSender sender = context.getMessageReceiver();

        OfflinePlayer player;
        if (args.length == 0) player = (Player) sender;
        else {
            player = context.getPlayer(0);
            if (player == null) return CommandResult.FAILED;
        }

        KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
        Kingdom kingdom = kp.getKingdom();
        boolean hasKingdom = kingdom != null;

        StringBuilder claims = new StringBuilder(kp.getClaims().size() * 30);
        for (SimpleChunkLocation claim : kp.getClaims()) {
            if (claims.length() != 0) claims.append(" &8| ");
            claims.append("&5").append(claim.getWorld()).append("&7, &6").append(claim.getX()).append("&7, &6").append(claim.getZ());
        }

        Pair<Integer, Integer> map = kp.getMapSize();
        context.getMessageContext().placeholders("id", player.getUniqueId(), "kingdom_id", hasKingdom ? kingdom.getId() : "~",
                "visualizer", kp.isUsingMarkers() ? "&2Yes" : "&cNo", "admin", kp.isAdmin() ? "&2Yes" : "&cNo", "spy", kp.isSpy() ? "&2Yes" : "&cNo",
                "invites", kp.getInvites().size(), "auto_claim", kp.getAutoClaim() != null ? "&2Yes" : "&cNo", "auto_map", kp.isAutoMap() ? "&2Yes" : "&cNo",
                "map_height", map != null ? map.getKey() : '~', "map_width", map != null ? map.getValue() : '~',
                "claims", claims.toString(), "compressed", kp.getCompressedData());

        Player online = player.getPlayer();
        if (online != null) {
            context.getMessageContext().withContext(online);
            context.var("op", online.isOnline());
        } else {
            context.getMessageContext().withContext(player);
        }

        context.getMessageContext().onUnknownPlaceholder(placeholder -> "?");
        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_PLAYER_INFO);
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull List<String> tabComplete(@NonNull CommandTabContext context) {
        if (context.isAtArg(0)) return context.getPlayers(0);
        return context.emptyTab();
    }
}