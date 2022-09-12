package org.kingdoms.outposts;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.commands.admin.debug.CommandAdminEvaluate;
import org.kingdoms.commands.outposts.CommandOutpost;
import org.kingdoms.gui.GUIParser;
import org.kingdoms.gui.InteractiveGUI;
import org.kingdoms.gui.KingdomsGUI;
import org.kingdoms.gui.ReusableOptionHandler;
import org.kingdoms.main.locale.KingdomsLang;
import org.kingdoms.main.locale.messager.MessageBuilder;
import org.kingdoms.services.worldguard.ServiceWorldGuard;
import org.kingdoms.utils.LocationUtils;
import org.kingdoms.utils.bossbars.BossBarEditor;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.string.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class OutpostEditor {
    private final Player player;
    private final Outpost outpost;

    public OutpostEditor(Player player, Outpost outpost) {
        this.player = player;
        this.outpost = outpost;
    }

    MessageBuilder getEdits() {
        return new MessageBuilder()
                .withContext(player)
                .raw("outpost-name", outpost.getName())
                .raw("outpost-region", outpost.getRegion())
                .raw("outpost-min-online-members", outpost.getMinOnlineMembers())
                .raw("outpost-max-participants", outpost.getMaxParticipants())
                .raw("outpost-entrance-money", outpost.getRewards().getMoney() == null ? 0 : outpost.getRewards().getMoney())
                .raw("outpost-spawn", KingdomsLang.LOCATIONS_NORMAL.parse(LocationUtils.getLocationEdits(outpost.getSpawn())))
                .raw("outpost-center", KingdomsLang.LOCATIONS_NORMAL.parse(LocationUtils.getLocationEdits(outpost.getCenter())))
                .raw("outpost-entrance-resource-points-fee", outpost.getResourcePointsCost() == null ? 0 : outpost.getResourcePointsCost())
                .raw("outpost-rewards-resource-points", outpost.getRewards().getResourcePoints() == null ? 0 : outpost.getRewards().getResourcePoints())
                .raw("outpost-rewards-money", outpost.getResourcePointsCost() == null ? 0 : outpost.getResourcePointsCost())
                ;
    }

    public InteractiveGUI openOutpostEditor() {
        InteractiveGUI gui = GUIParser.parse(player, KingdomsGUI.OUTPOSTS_EDITOR.getPath(), getEdits());
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
            openOutpostEditor();
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
            context.endConversation();
            context.getSettings().raw("outpost-new-region", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REGION_SET);
            openOutpostEditor();
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
                context.sendError(KingdomsLang.INVALID_NUMBER);
                return;
            }
            if (participants <= 2) {
                context.sendError(KingdomsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_INVALID);
                return;
            }

            outpost.setMaxParticipants(participants);
            context.endConversation();
            context.getSettings().raw("outpost-new-max-participants", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_SET);
            openOutpostEditor();
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

            context.endConversation();
            outpost.setMinOnlineMembers(minOnlineMembers);
            openOutpostEditor();
        }).done();


        gui.option("entrance-money").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_MONEY_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathCompiler.Expression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.setMoneyCost(compiled);
            context.endConversation();
            context.getSettings().raw("outpost-new-entrance-money", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_MONEY_SET);
            openOutpostEditor();
        }).done();

        gui.option("entrance-resource-points-fee").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_RESOURCE_POINTS_FEE_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathCompiler.Expression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.setResourcePointsCost(compiled);
            context.endConversation();
            context.getSettings().raw("outpost-new-entrance-resource-points-fee", input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_RESOURCE_POINTS_FEE_SET);
            openOutpostEditor();
        }).done();

        gui.push("rewards", this::openOutpostRewardsGUIEditor);

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

    public InteractiveGUI openOutpostRewardsGUIEditor() {
        InteractiveGUI gui = GUIParser.parse(player, "outposts/rewards/main", getEdits());

        gui.option("money").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REWARDS_MONEY_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathCompiler.Expression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.getRewards().setMoney(input);
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REWARDS_MONEY_SET);
            context.endConversation();
            openOutpostRewardsGUIEditor();
        }).done();

        gui.option("resource-points").onNormalClicks(context -> {
            context.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REWARDS_RESOURCE_POINTS_ENTER);
            context.startConversation();
        }).setConversation((ctx, input) -> {
            MathCompiler.Expression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.getRewards().setResourcePoints(input);
            ctx.endConversation();
            ctx.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REWARDS_RESOURCE_POINTS_SET);
            openOutpostRewardsGUIEditor();
        }).done();
        gui.push("items", () -> openOutpostRewardsItemGUIEditor(0));
        gui.push("commands", this::openOutpostRewardsCommandGUIEditor);

        gui.push("back", this::openOutpostEditor);
        gui.openInventory();
        return gui;
    }

    public InteractiveGUI openOutpostRewardsItemGUIEditor(int page) {
        InteractiveGUI gui = GUIParser.parse(player, "outposts/rewards/items", getEdits());

        ReusableOptionHandler itemsOption = gui.getReusableOption("items");
        Collection<ItemStack> items = outpost.getRewards().getItems();

        for (ItemStack item : items) {
            if (!itemsOption.hasNext()) break;
            itemsOption.editItem(templateItem -> item).done();
        }

        gui.onClose(() -> outpost.getRewards().setItems(gui.getInteractableItems()));
        gui.push("back", this::openOutpostRewardsGUIEditor);

        gui.openInventory();
        return gui;
    }

    public InteractiveGUI openOutpostRewardsCommandGUIEditor() {
        InteractiveGUI gui = GUIParser.parse(player, "outposts/rewards/commands", getEdits());

        ReusableOptionHandler cmdsOption = gui.getReusableOption("commands");
        List<String> commands = outpost.getRewards().getCommands();
        for (int i = 0; i < commands.size(); i++) {
            String cmd = commands.get(i);
            final int finalI = i;

            cmdsOption.getSettings().raw("command", cmd);
            cmdsOption.on(ClickType.LEFT, (ctx) -> {
                ctx.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REWARDS_COMMAND_ENTER);
                ctx.startConversation();
            }).setConversation((ctx, input) -> {
                commands.add(input);
                ctx.endConversation();
                openOutpostRewardsCommandGUIEditor();
            });

            cmdsOption.on(ClickType.RIGHT, () -> {
                commands.remove(finalI);
                openOutpostRewardsCommandGUIEditor();
            });

            cmdsOption.done();
            if (!cmdsOption.hasNext()) break;
        }

        gui.option("add").onNormalClicks(ctx -> {
            ctx.sendMessage(KingdomsLang.COMMAND_OUTPOST_EDIT_REWARDS_COMMAND_ENTER);
            ctx.startConversation();
        }).setConversation((ctx, input) -> {
            commands.add(input);
            ctx.endConversation();
            openOutpostRewardsCommandGUIEditor();
        }).done();

        gui.push("back", this::openOutpostRewardsGUIEditor);
        gui.openInventory();
        return gui;
    }
}
