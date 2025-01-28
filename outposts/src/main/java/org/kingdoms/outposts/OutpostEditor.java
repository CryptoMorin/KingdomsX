package org.kingdoms.outposts;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.commands.admin.debugging.CommandAdminEvaluate;
import org.kingdoms.commands.outposts.CommandOutpost;
import org.kingdoms.data.Pair;
import org.kingdoms.enginehub.EngineHubAddon;
import org.kingdoms.gui.*;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.outposts.settings.OutpostArenaMob;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.utils.LocationUtils;
import org.kingdoms.utils.ProcessToMessage;
import org.kingdoms.utils.bossbars.BossBarEditor;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.compilers.expressions.MathExpression;
import org.kingdoms.utils.internal.numbers.AnyNumber;
import org.kingdoms.utils.internal.numbers.NumberConstraint;
import org.kingdoms.utils.internal.numbers.NumberProcessor;
import org.kingdoms.utils.string.Strings;
import org.kingdoms.utils.time.TimeUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class OutpostEditor {
    private final Player player;
    private final OutpostEventSettings outpost;

    public OutpostEditor(Player player, OutpostEventSettings outpost) {
        this.player = player;
        this.outpost = outpost;
    }

    MessagePlaceholderProvider getEdits() {
        return new MessagePlaceholderProvider()
                .withContext(player)
                .raw("outpost-name", outpost.getName())
                .raw("outpost-region", outpost.getRegion())
                .raw("outpost-min-online-members", outpost.getMinOnlineMembers())
                .raw("outpost-max-participants", outpost.getMaxParticipants())
                .raw("outpost-entrance-money", outpost.getMoneyCost() == null ? 0 : outpost.getMoneyCost())
                .raw("outpost-entrance-resource-points-fee", outpost.getResourcePointsCost() == null ? 0 : outpost.getResourcePointsCost())
                .raw("outpost-spawn", LocationUtils.parseLocation(outpost.getSpawn()))
                .raw("outpost-center", LocationUtils.parseLocation(outpost.getCenter()))
                .raw("outpost-rewards-resource-points", outpost.getRewards().getResourcePoints() == null ? 0 : outpost.getRewards().getResourcePoints())
                .raw("outpost-rewards-money", outpost.getResourcePointsCost() == null ? 0 : outpost.getResourcePointsCost());
    }

    public InteractiveGUI openOutpostEditor() {
        InteractiveGUI gui = GUIAccessor.prepare(player, OutpostGUI.EDITOR, getEdits());
        Objects.requireNonNull(gui, "GUI is null");

        //////////// Name
        gui.option("name").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_NAME_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            if (!Strings.isEnglish(input)) {
                context.sendError(OutpostsLang.COMMAND_OUTPOST_EDIT_NAME_INVALID);
                return;
            }
            if (OutpostEventSettings.getOutpost(input) != null) {
                context.sendError(OutpostsLang.COMMAND_OUTPOST_EDIT_NAME_ALREADY_IN_USE);
                return;
            }

            outpost.setName(input);
            context.getMessageContext().raw("outpost-new-name", input);
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_NAME_SET);
            openOutpostEditor();
        }).done();

        ///////////// Region
        gui.option("region").onNormalClicks(context -> {
            if (CommandOutpost.worldGuardMissing(player)) return;
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REGION_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            if (!EngineHubAddon.INSTANCE.getWorldGuard().hasRegion(player.getWorld(), input)) {
                context.getMessageContext().raw("region", input);
                context.sendError(OutpostsLang.COMMAND_OUTPOST_CREATE_REGION_NOT_FOUND);
                return;
            }

            outpost.setRegion(input);
            context.endConversation();
            context.getMessageContext().raw("outpost-new-region", input);
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REGION_SET);
            openOutpostEditor();
        }).done();

        gui.option("spawn").on(ClickType.LEFT, context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_SPAWN_SET);
            outpost.setSpawn(player.getLocation());
        }).on(ClickType.RIGHT, ctx -> {
            player.teleport(outpost.getSpawn());
            ctx.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_SPAWN_TELEPORTED);
        }).done();

        gui.option("center").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_CENTER_SET);
            outpost.setCenter(player.getLocation());
        }).on(ClickType.RIGHT, ctx -> {
            player.teleport(outpost.getSpawn());
            ctx.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_SPAWN_TELEPORTED);
        }).done();

        gui.push("arena-mobs", this::openArenaMobs);

        gui.option("max-participants").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            context.getMessageContext().raw("arg", input);
            int maxParticipants;
            try {
                maxParticipants = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                context.sendError(KingdomsLang.INVALID_NUMBER);
                return;
            }
            if (maxParticipants <= 2) {
                context.sendError(OutpostsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_INVALID);
                return;
            }

            outpost.setMaxParticipants(maxParticipants);
            context.endConversation();
            context.getMessageContext().raw("outpost-new-max-participants", input);
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_MAX_PARTICIPANTS_SET);
            openOutpostEditor();
        }).done();
        gui.option("min-online-members").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_MIN_ONLINE_MEMBERS_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            context.getMessageContext().raw("arg", input);
            int minOnlineMembers;
            try {
                minOnlineMembers = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                context.getMessageContext().raw("arg", input);
                return;
            }
            if (minOnlineMembers <= 1) {
                context.sendError(OutpostsLang.COMMAND_OUTPOST_EDIT_MIN_ONLINE_MEMBERS_INVALID);
                return;
            }

            context.endConversation();
            outpost.setMinOnlineMembers(minOnlineMembers);
            openOutpostEditor();
        }).done();


        gui.option("entrance-money").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_MONEY_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathExpression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.setMoneyCost(compiled);
            context.endConversation();
            context.getMessageContext().raw("outpost-new-entrance-money", input);
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_MONEY_SET);
            openOutpostEditor();
        }).done();

        gui.option("entrance-resource-points-fee").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_RESOURCE_POINTS_FEE_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathExpression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.setResourcePointsCost(compiled);
            context.endConversation();
            context.getMessageContext().raw("outpost-new-entrance-resource-points-fee", input);
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_ENTRANCE_RESOURCE_POINTS_FEE_SET);
            openOutpostEditor();
        }).done();

        gui.push("rewards", this::openOutpostRewardsGUIEditor);

        gui.option("bossbar").onNormalClicks(context -> {
            if (outpost.getBossBarSettings() == null) outpost.setDefaultBossBarSettings();
            BossBarEditor editor = new BossBarEditor(player, outpost.getBossBarSettings(), bossGUI -> {
                bossGUI.option("back").onNormalClicks(player::closeInventory);
            });

            editor.openGUI();
        }).done();

        gui.option("delete").onNormalClicks(context -> {
            if (outpost.getEvent() != null) {
                context.sendError(OutpostsLang.COMMAND_OUTPOST_EDIT_REMOVE_OUTPOST_IS_RUNNING);
                return;
            }

            OutpostEventSettings.getOutposts().remove(outpost.getName());
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REMOVE_REMOVED);
            player.closeInventory();
        }).done();

        gui.open();
        return gui;
    }

    public InteractiveGUI openArenaMobs() {
        InteractiveGUI gui = GUIAccessor.prepare(player, OutpostGUI.ARENA$MOBS, getEdits());
        if (gui == null) return null;

        ReusableOptionHandler arenaMobOpt = gui.getReusableOption("arena-mobs");
        for (OutpostArenaMob arenaMob : outpost.getArenaMobs()) {
            if (!arenaMobOpt.hasNext()) break;
            arenaMobOpt
                    .editMessageContext(arenaMob::addMessageContextEdits)
                    .on(ClickType.LEFT, () -> openArenaMob(arenaMob))
                    .on(ClickType.RIGHT, () -> {
                        outpost.getArenaMobs().remove(arenaMob);
                        openArenaMobs();
                    })
                    .done();
        }

        gui.push("add", () -> {
            OutpostArenaMob arenaMob = new OutpostArenaMob(null, null, 0, null, null, null);
            outpost.getArenaMobs().add(arenaMob);
            openArenaMob(arenaMob);
        });


        gui.push("back", this::openOutpostEditor);
        gui.open();
        return gui;
    }

    public InteractiveGUI openArenaMob(OutpostArenaMob arenaMob) {
        MessagePlaceholderProvider edits = getEdits();
        arenaMob.addMessageContextEdits(edits);
        InteractiveGUI gui = GUIAccessor.prepare(player, OutpostGUI.ARENA$MOB, edits);
        if (gui == null) return null;

        gui.option("label").onNormalClicks(ctx -> {
            OutpostsLang.COMMAND_OUTPOST_EDIT_ARENA_MOBS_MAX_SPAWN_COUNT.sendMessage(player);
            ctx.startConversation();
        }).setConversation((ctx, input) -> {
            arenaMob.setLabel(input);
            ctx.endConversation();
            openArenaMob(arenaMob);
        }).done();

        gui.option("max-spawn-count").onNormalClicks(ctx -> {
            OutpostsLang.COMMAND_OUTPOST_EDIT_ARENA_MOBS_MAX_SPAWN_COUNT.sendMessage(player);
            ctx.startConversation();
        }).setConversation((ctx, input) -> {
            NumberProcessor processor = new NumberProcessor(input);
            processor.withAllDecorators().withConstraints(NumberConstraint.POSITIVE, NumberConstraint.INTEGER_ONLY);
            Pair<AnyNumber, Messenger> processed = ProcessToMessage.getNumber(processor);
            if (processed.isValuePresent()) {
                ctx.sendError(processed.getValue());
                return;
            }

            arenaMob.setMaxSpawnCount(processed.getKey().getValue().intValue());
            ctx.endConversation();
            openArenaMob(arenaMob);
        }).done();

        gui.option("spawn-interval").onNormalClicks(ctx -> {
            OutpostsLang.COMMAND_OUTPOST_EDIT_ARENA_MOBS_SPAWN_INTERVAL.sendMessage(player);
            ctx.startConversation();
        }).setConversation((ctx, input) -> {
            Long time = TimeUtils.parseTime(input);
            if (time == null) {
                ctx.sendMessage(KingdomsLang.INVALID_TIME, "time", input);
                return;
            }

            arenaMob.setSpawnInterval(Duration.ofMillis(time));
            ctx.endConversation();
            openArenaMob(arenaMob);
        }).done();

        gui.option("spawn-location").onNormalClicks(ctx -> {
            OutpostsLang.COMMAND_OUTPOST_EDIT_ARENA_MOBS_SPAWN_LOCATION.sendMessage(player);
            ctx.startConversation();
        }).setConversation((ctx, input) -> {
            arenaMob.setSpawnLocation(player.getLocation());
            ctx.endConversation();
            openArenaMob(arenaMob);
        }).done();

        gui.option("damage-bonus").onNormalClicks(ctx -> {
            OutpostsLang.COMMAND_OUTPOST_EDIT_ARENA_MOBS_DAMAGE_BONUS_ENTER.sendMessage(player);
            ctx.startConversation();
        }).setConversation((ctx, input) -> {
            MathExpression compiled;
            try {
                compiled = MathCompiler.compile(input);
            } catch (Exception ex) {
                ctx.sendError(OutpostsLang.COMMAND_OUTPOST_EDIT_ARENA_MOBS_DAMAGE_BONUS_WRONG_MATH, "eqn", input);
                return;
            }
            Set<String> vars =compiled.getVariables();
            for (String var : vars) {
                if (!var.equals("dmg")) {
                    ctx.sendError(OutpostsLang.COMMAND_OUTPOST_EDIT_ARENA_MOBS_DAMAGE_BONUS_UNKNOWN_VARIABLE, "eqn", input, "variable", var);
                    return;
                }
            }

            arenaMob.setDamageBonus(compiled);
            ctx.endConversation();
            openArenaMob(arenaMob);
        }).done();

        gui.push("back", this::openArenaMobs);
        gui.open();
        return gui;
    }

    public InteractiveGUI openOutpostRewardsGUIEditor() {
        InteractiveGUI gui = GUIAccessor.prepare(player, OutpostGUI.REWARDS_MAIN, getEdits());

        gui.option("money").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REWARDS_MONEY_ENTER);
            context.startConversation();
        }).setConversation((context, input) -> {
            MathExpression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.getRewards().setMoney(input);
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REWARDS_MONEY_SET);
            context.endConversation();
            openOutpostRewardsGUIEditor();
        }).done();

        gui.option("resource-points").onNormalClicks(context -> {
            context.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REWARDS_RESOURCE_POINTS_ENTER);
            context.startConversation();
        }).setConversation((ctx, input) -> {
            MathExpression compiled = CommandAdminEvaluate.compile(player, input);
            if (compiled == null) return;

            outpost.getRewards().setResourcePoints(input);
            ctx.endConversation();
            ctx.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REWARDS_RESOURCE_POINTS_SET);
            openOutpostRewardsGUIEditor();
        }).done();
        gui.push("items", () -> openOutpostRewardsItemGUIEditor(0));
        gui.push("commands", this::openOutpostRewardsCommandGUIEditor);

        gui.push("back", this::openOutpostEditor);
        gui.open();
        return gui;
    }

    public InteractiveGUI openOutpostRewardsItemGUIEditor(int page) {
        InventoryInteractiveGUI gui = new GUIBuilder(OutpostGUI.REWARDS_ITEMS)
                .forPlayer(player)
                .withSettings(getEdits())
                .inventoryGUIOnly()
                .build();

        ReusableOptionHandler itemsOption = gui.getReusableOption("items");
        Collection<ItemStack> items = outpost.getRewards().getItems();

        for (ItemStack item : items) {
            if (!itemsOption.hasNext()) break;
            itemsOption.editItem(templateItem -> item).done();
        }

        gui.onClose(true, () -> outpost.getRewards().setItems(gui.getInteractableItems()));
        gui.push("back", this::openOutpostRewardsGUIEditor);

        gui.open();
        return gui;
    }

    public InteractiveGUI openOutpostRewardsCommandGUIEditor() {
        InteractiveGUI gui = GUIAccessor.prepare(player, OutpostGUI.REWARDS_COMMANDS, getEdits());

        ReusableOptionHandler cmdsOption = gui.getReusableOption("commands");
        List<String> commands = outpost.getRewards().getCommands();
        for (int i = 0; i < commands.size(); i++) {
            String cmd = commands.get(i);
            final int finalI = i;

            cmdsOption.getMessageContext().raw("command", cmd);
            cmdsOption.on(ClickType.LEFT, (ctx) -> {
                ctx.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REWARDS_COMMAND_ENTER);
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
            ctx.sendMessage(OutpostsLang.COMMAND_OUTPOST_EDIT_REWARDS_COMMAND_ENTER);
            ctx.startConversation();
        }).setConversation((ctx, input) -> {
            commands.add(input);
            ctx.endConversation();
            openOutpostRewardsCommandGUIEditor();
        }).done();

        gui.push("back", this::openOutpostRewardsGUIEditor);
        gui.open();
        return gui;
    }
}
