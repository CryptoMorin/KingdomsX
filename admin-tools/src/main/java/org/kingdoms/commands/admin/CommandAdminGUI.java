package org.kingdoms.commands.admin;

import com.cryptomorin.xseries.XItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.admintools.AdminToolsLang;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.annotations.Cmd;
import org.kingdoms.commands.annotations.CmdParent;
import org.kingdoms.commands.annotations.CmdPerm;
import org.kingdoms.data.Pair;
import org.kingdoms.gui.GUIBuilder;
import org.kingdoms.gui.GUIOption;
import org.kingdoms.gui.InventoryInteractiveGUI;
import org.kingdoms.gui.objects.ConditionalGUIOptionObject;
import org.kingdoms.gui.objects.GUIOptionBuilder;
import org.kingdoms.gui.objects.GUIOptionObject;
import org.kingdoms.gui.objects.GUIOptionParser;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.Language;
import org.kingdoms.locale.compiler.MessageCompiler;
import org.kingdoms.locale.compiler.MessageCompilerSettings;
import org.kingdoms.locale.compiler.pieces.MessagePiece;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.managers.TempKingdomsFolder;
import org.kingdoms.server.permission.PermissionDefaultValue;
import org.kingdoms.utils.config.ConfigSection;
import org.kingdoms.utils.config.adapters.YamlFile;
import org.kingdoms.utils.fs.FSUtil;
import org.kingdoms.utils.string.Strings;
import org.snakeyaml.common.ScalarStyle;
import org.snakeyaml.nodes.ScalarNode;
import org.snakeyaml.nodes.Tag;

import java.nio.file.Path;
import java.util.*;

@Cmd("gui")
@CmdParent(CommandAdmin.class)
@CmdPerm(PermissionDefaultValue.OP)
public class CommandAdminGUI extends KingdomsCommand implements Listener {
    private static final Map<UUID, String> VIEWING = new HashMap<>();

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        UUID id = context.senderAsPlayer().getUniqueId();

        if (context.hasArgs(1)) {
            String guiName = Strings.remove(context.arg(0).replace('.', '/').replace('\\', '/').toLowerCase(Locale.ENGLISH), ".yml");
            if (!Language.getDefault().getGUIs().containsKey(guiName)) {
                context.sendError(KingdomsLang.COMMAND_GUI_NOT_FOUND, "gui", guiName);
                return CommandResult.FAILED;
            }

            VIEWING.put(id, guiName);

            Player player = context.senderAsPlayer();
            InventoryInteractiveGUI gui = new GUIBuilder(guiName).forPlayer(player).inventoryGUIOnly().build();
            Inventory inv = gui.getInventory();
            for (Map.Entry<String, GUIOptionBuilder> left : gui.getRemainingOptions().entrySet()) {
                GUIOptionBuilder optionBuilder = left.getValue();

                while (optionBuilder instanceof ConditionalGUIOptionObject) {
                    ConditionalGUIOptionObject cond = (ConditionalGUIOptionObject) optionBuilder;
                    optionBuilder = cond.getOptions()[0].getObject();
                }
                GUIOptionObject optionObject = (GUIOptionObject) optionBuilder;

                GUIOption option = new GUIOption(left.getKey(), optionObject);
                for (int slot : option.getSettings().getSlots()) {
                    inv.setItem(slot, option.getSettings().getItem().clone());
                }
            }
            player.openInventory(inv);
            return CommandResult.FAILED;
        }

        if (VIEWING.containsKey(id)) {
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_GUI_DISABLED);
            VIEWING.remove(id);
        } else {
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_GUI_ENABLED);
            VIEWING.put(id, "");
        }

        return CommandResult.SUCCESS;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String guiName = VIEWING.remove(player.getUniqueId());
        if (guiName == null || guiName.isEmpty()) return;

        Path file = serializeInventory(event.getInventory());
        AdminToolsLang.COMMAND_ADMIN_GUI_DONE.sendMessage(player, "file", Kingdoms.getFolder().relativize(file));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String guiName = VIEWING.get(player.getUniqueId());
        if (guiName == null || !guiName.isEmpty()) return;
        VIEWING.remove(player.getUniqueId());

        AdminToolsLang.COMMAND_ADMIN_GUI_OPENED.sendMessage(player);
        Path file = serializeInventory(event.getInventory());

        AdminToolsLang.COMMAND_ADMIN_GUI_DONE.sendMessage(player, "file", Kingdoms.getFolder().relativize(file));
        event.setCancelled(true);
    }

    public static Path serializeInventory(final Inventory inventory) {
        Path file = FSUtil.findSlotForCounterFile(TempKingdomsFolder.getOrCreateFolder("GUIs"), "parsed-gui", "yml");
        YamlFile adapter = new YamlFile(file.toFile()).load();
        ConfigSection config = adapter.getConfig();

        config.set("title", "{$sep}-=( {$p}" + inventory.getType().name().toLowerCase(Locale.ENGLISH) + " {$sep})=-");
        if (inventory.getType() == InventoryType.CHEST) config.set("rows", (inventory.getSize() + 1) / 9);
        else config.set("type", inventory.getType().name());

        ConfigSection items = config.createSection("options");
        ItemStack[] content = inventory.getContents();
        @SuppressWarnings("unchecked")
        Pair<ItemStack, List<Integer>>[] added = new Pair[content.length];

        for (int i = 0; i < content.length; i++) {
            ItemStack item = content[i];
            if (item == null) continue;

            boolean isSimilar = false;
            for (int j = 0; j < i; j++) {
                Pair<ItemStack, List<Integer>> other = added[j];
                if (other != null && item.equals(other.getKey())) {
                    other.getValue().add(i);
                    isSimilar = true;
                    break;
                }
            }
            if (isSimilar) continue;

            List<Integer> slots = new ArrayList<>(1);
            slots.add(i);
            added[i] = Pair.of(item, slots);
        }

        Set<String> taken = new HashSet<>();
        int takenItemNameCounter = 0;
        for (Pair<ItemStack, List<Integer>> result : added) {
            if (result == null) continue;

            ItemStack item = result.getKey();
            String name = Strings.replace(item.getType().name().toLowerCase(Locale.ENGLISH), '_', '-').toString(), finalName = name;
            while (!taken.add(finalName)) finalName = name + takenItemNameCounter++;

            ConfigSection section = items.createSection(finalName);
            List<Integer> slots = result.getValue();
            if (slots.size() == 1) {
                if (inventory.getType() == InventoryType.CHEST) {
                    int[] pos = GUIOptionParser.rawSlotToXY(slots.get(0));
                    section.set("posx", pos[0]);
                    section.set("posy", pos[1]);
                } else {
                    section.set("slot", slots.get(0));
                }
            } else {
                section.set("slots", slots);
            }
            new XItemStack.Serializer()
                    .withItem(item)
                    .withConfig(section.toBukkitConfigurationSection())
                    .write();

            section.set("flags", null); // All flags are automatically included on all GUI options.
            if (com.google.common.base.Strings.isNullOrEmpty(section.getString("name"))) section.set("name", "");
            else {
                String optionName = section.getString("name");
                MessageCompiler compiled = new MessageCompiler(optionName,
                        MessageCompilerSettings.none().validate().colorize().hovers());
                boolean containsFormatting = compiled.compileObject().hasPiece(x -> x instanceof MessagePiece.Color || x instanceof MessagePiece.Hover);
                section.set("name", (containsFormatting ? "" : "{$p}") + optionName);
            }

            List<String> lore = section.getStringList("lore");
            if (lore.size() > 1)
                section.set("lore", new ScalarNode(Tag.STR, String.join("\n", lore), ScalarStyle.LITERAL));
        }

        adapter.saveConfig();
        return file;
    }

    @Override
    public @NonNull List<String> tabComplete(@NonNull CommandTabContext context) {
        if (context.isAtArg(0)) return context.suggest(0, Language.getDefault().getGUIs().keySet());
        return context.emptyTab();
    }
}
