package org.kingdoms.commands.outposts;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.commands.admin.debug.CommandAdminEvaluate;
import org.kingdoms.gui.GUIParser;
import org.kingdoms.gui.InteractiveGUI;
import org.kingdoms.gui.KingdomsGUI;
import org.kingdoms.main.locale.KingdomsLang;
import org.kingdoms.main.locale.MessageHandler;
import org.kingdoms.main.locale.messager.MessageBuilder;
import org.kingdoms.outposts.Outpost;
import org.kingdoms.services.worldguard.ServiceWorldGuard;
import org.kingdoms.utils.LocationUtils;
import org.kingdoms.utils.bossbars.BossBarEditor;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.string.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        openOutpostEditor(player, outpost);
    }

    public static MessageBuilder getEditsOf(Outpost outpost) {
        return new MessageBuilder()
                .raw("outpost-name", outpost.getName())
                .raw("outpost-region", outpost.getRegion())
                .raw("outpost-min-online-members", outpost.getMinOnlineMembers())
                .raw("outpost-max-participants", outpost.getMaxParticipants())
                .raw("outpost-entrance-money", outpost.getCost())
                .raw("outpost-spawn", KingdomsLang.LOCATIONS_NORMAL.parse(LocationUtils.getLocationEdits(outpost.getSpawn())))
                .raw("outpost-center", KingdomsLang.LOCATIONS_NORMAL.parse(LocationUtils.getLocationEdits(outpost.getCenter())))
                .raw("outpost-entrance-resource-points-fee", outpost.getResourcePointsCost());
    }

    public static InteractiveGUI openOutpostEditor(Player player, Outpost outpost) {
        InteractiveGUI gui = GUIParser.parse(player, KingdomsGUI.OUTPOSTS_EDITOR.getPath(),
                getEditsOf(outpost).withContext(player));
        Objects.requireNonNull(gui, "GUI is null");

        //////////// Name
        gui.option("name").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_NAME_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            if (!StringUtils.isEnglish(input)) {
                context.sendError(KingdomsLang.COMMAND_OUTPOST_EDIT_NAME_INVALID);
                return;
            }
            if (Outpost.getOutpost(input) != null) {
                context.sendError(KingdomsLang.COMMAND_OUTPOST_EDIT_NAME_ALREADY_IN_USE);
                return;
            }

            outpost.setName(input);
            context.getSettings().raw("outpost-new-name", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_NAME_SET);
            openOutpostEditor(player, outpost);
        }).done();

        ///////////// Region
        gui.option("region").onNormalClicks(context -> {
            if (CommandOutpost.worldGuardMissing(player)) return;
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REGION_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            if (!ServiceWorldGuard.get().hasRegion(player.getWorld(), input)) {
                context.getSettings().raw("region", input);
                context.sendError(KingdomsLang.COMMAND_OUTPOST_CREATE_REGION_NOT_FOUND);
                return;
            }

            outpost.setRegion(input);
            context.getSettings().raw("outpost-new-region", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REGION_SET);
        }).done();

        gui.option("spawn").on(ClickType.LEFT, context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_SPAWN_SET);
            outpost.setSpawn(player.getLocation());
        }).on(ClickType.RIGHT, ctx -> {
            player.teleport(outpost.getSpawn());
            ctx.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_SPAWN_TELEPORTED);
        }).done();

        gui.option("center").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_CENTER_SET);
            outpost.setCenter(player.getLocation());
        }).on(ClickType.RIGHT, ctx -> {
            player.teleport(outpost.getSpawn());
            ctx.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_SPAWN_TELEPORTED);
        }).done();

        gui.option("max-participants").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            context.getSettings().raw("arg", input);
            int participants;
            try {
                participants = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                context.sendError(KingdomsLang.NOT_NUMBER);
                return;
            }
            if (participants <= 2) {
                context.sendError(KingdomsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_INVALID);
                return;
            }

            outpost.setMaxParticipants(participants);
            context.getSettings().raw("outpost-new-max-participants", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_SET);
        }).done();
        gui.option("min-online-members").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_MIN_ONLINE_MEMBERS_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            context.getSettings().raw("arg", input);
            int minOnlineMembers;
            try {
                minOnlineMembers = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                context.getSettings().raw("arg", input);
                return;
            }
            if (minOnlineMembers <= 1) {
                context.sendError(KingdomsLang.COMMAND_OUTPOST_EDIT_MIN_ONLINE_MEMBERS_INVALID);
                return;
            }

            context.getSettings().raw("outpost-new-min-online-members", input);
            outpost.setMinOnlineMembers(minOnlineMembers);
        }).done();


        gui.option("entrance-money").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_MONEY_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathCompiler.Sentence compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.setCost(compiled);
            context.getSettings().raw("outpost-new-entrance-money", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_MONEY_SET);
            openOutpostEditor(player, outpost);
        }).done();

        gui.option("entrance-resource-points-fee").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_RESOURCE_POINTS_FEE_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathCompiler.Sentence compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.setResourcePointsCost(compiled);
            context.getSettings().raw("outpost-new-entrance-resource-points-fee", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_RESOURCE_POINTS_FEE_SET);
            openOutpostEditor(player, outpost);
        }).done();

        gui.option("rewards").onNormalClicks(context -> {
            MessageHandler.sendPluginMessage(player, "&cThis section is not implemented yet.");
            player.closeInventory();
        }).done();

        gui.option("bossbar").onNormalClicks(context -> {
            BossBarEditor editor = new BossBarEditor(player, outpost.getBossBarSettings(), bossGUI -> {
                bossGUI.option("back").onNormalClicks(player::closeInventory);
            });

            editor.openGUI();
        }).done();

        gui.option("delete").onNormalClicks(context -> {
            if (outpost.getEvent() != null) {
                context.sendError(KingdomsLang.COMMAND_OUTPOST_EDIT_REMOVE_OUTPOST_IS_RUNNING);
                return;
            }

            Outpost.getOutposts().remove(outpost.getName());
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REMOVE_REMOVED);
            player.closeInventory();
        }).done();

        gui.openInventory();
        return gui;
    }

    public static void openBossBarSettingsGUI(Player player) {
        InteractiveGUI gui = GUIParser.parse(player, player, "bossbar/main-editor",
                Arrays.asList("outpost-name", 3)
        );
        Objects.requireNonNull(gui, "GUI is null");
    }

    public static void openOutpostRewardsGUIEditor(Player player) {
        InteractiveGUI gui = GUIParser.parse(player, player, "org/kingdoms/commands/outposts/rewards",
                Arrays.asList("outpost-name", 3)
        );
        Objects.requireNonNull(gui, "GUI is null");
    }

    @Override
    public @NonNull List<String> tabComplete(CommandTabContext context) {
        if (context.isAtArg(0)) return KingdomsCommand.tabComplete(Outpost.getOutposts().keySet());
        return KingdomsCommand.emptyTab();
    }
}
