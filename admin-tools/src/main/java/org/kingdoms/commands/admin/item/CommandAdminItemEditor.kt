package org.kingdoms.commands.admin.item

import com.cryptomorin.xseries.*
import com.cryptomorin.xseries.profiles.builder.XSkull
import com.cryptomorin.xseries.profiles.objects.Profileable
import com.cryptomorin.xseries.reflection.XReflection
import com.google.common.base.Enums
import org.bukkit.Material
import org.bukkit.attribute.AttributeModifier
import org.bukkit.attribute.AttributeModifier.Operation
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.admin.CommandAdminLanguagePack
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.gui.*
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.LangConstants
import org.kingdoms.locale.compiler.MessageCompiler
import org.kingdoms.locale.compiler.MessageCompilerSettings
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.nbt.tag.*
import org.kingdoms.platform.bukkit.item.ItemNBT
import org.kingdoms.utils.ItemStackPlaceholderProvider
import org.kingdoms.utils.ProcessToMessage
import org.kingdoms.utils.cache.EnumCache
import org.kingdoms.utils.internal.functional.TriConsumer
import org.kingdoms.utils.internal.numbers.AnyNumber.Companion.abstractNumber
import org.kingdoms.utils.internal.numbers.NumberConstraint
import org.kingdoms.utils.internal.numbers.NumberProcessor
import org.kingdoms.utils.string.Strings
import org.kingdoms.utils.string.tree.StringPathBuilder
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.stream.Collectors

@Cmd("editor")
@CmdParent(CommandAdminItem::class)
class CommandAdminItemEditor : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        context.assertPlayer()
        val player = context.senderAsPlayer()

        var item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) item = XMaterial.STONE.parseItem()!!
        else player.inventory.setItemInMainHand(null)

        ItemEditor(player, item).openGUI()
        return CommandResult.SUCCESS
    }
}

class ItemEditor(
    private val player: Player,
    private var item: ItemStack,
) {
    private val rootNBT = ItemNBT.getTag(item)

    private fun getEditsForItem(): MessagePlaceholderProvider = ItemStackPlaceholderProvider.of(item)

    @Suppress("removal", "DEPRECATION")
    private fun getEditsForAttribute(attribute: XAttribute, modifier: AttributeModifier): MessagePlaceholderProvider =
        MessagePlaceholderProvider()
            .raws(
                "attribute_type", Strings.capitalize(attribute.name()),
                "attribute_uuid",
                try {
                    modifier.uniqueId
                } catch (ex: Throwable) {
                    modifier.key
                },
                "attribute_name", modifier.name,
                "attribute_amount", modifier.amount,
                "attribute_operation", Strings.capitalize(modifier.operation.name),
                "attribute_equipment_slot", Strings.capitalize(modifier.slot?.name ?: "Any"),
            )

    private fun getTypeOfNBT(nbt: NBTTag<*>) = when (nbt) {
        is NBTTagString -> "string"
        is NBTTagInt -> "int"
        is NBTTagDouble -> "double"
        is NBTTagByte -> "byte"
        is NBTTagLong -> "long"
        is NBTTagShort -> "short"
        is NBTTagEnd -> "end"
        is NBTTagFloat -> "float"
        is NBTTagCompound -> "compound"
        is NBTTagList<*> -> "list"
        is NBTTagIntArray -> "intArray"
        is NBTTagLongArray -> "longArray"
        is NBTTagByteArray -> "byteArray"
        else -> nbt.javaClass.simpleName
    }

    private fun buildPathsFrom(list: MutableList<String>, nbt: NBTTagCompound, currentPath: String) {
        if (nbt.value().isEmpty()) {
            list.add(currentPath)
            return
        }

        for ((key, value) in nbt.value()) {
            val path = if (currentPath.isEmpty()) key else "$currentPath/$key"
            if (value is NBTTagCompound) {
                buildPathsFrom(list, value, path)
            } else {
                list.add(path)
            }
        }
    }

    fun constructNBT(type: String, value: Collection<String>): NBTTag<*>? =
        when (type) {
            "NBTTagCompound" -> NBTTagCompound.empty()
            "NBTTagString" -> NBTTagString.of(value.joinToString())
            "NBTTagList" -> NBTTagList.of(NBTTagType.STRING, value.map { x -> NBTTagString.of(x) })
            "NBTTagEnd" -> NBTTagEnd.instance()

            "NBTTagByte" -> NBTTagByte.of(value.joinToString().toByte())
            "NBTTagShort" -> NBTTagShort.of(value.joinToString().toShort())
            "NBTTagInt" -> NBTTagInt.of(value.joinToString().toInt())
            "NBTTagLong" -> NBTTagLong.of(value.joinToString().toLong())
            "NBTTagFloat" -> NBTTagFloat.of(value.joinToString().toFloat())
            "NBTTagDouble" -> NBTTagDouble.of(value.joinToString().toDouble())

            "NBTTagByteArray" -> NBTTagByteArray.of(*value.map { x -> x.toByte() }.toByteArray())
            "NBTTagIntArray" -> NBTTagIntArray.of(*value.map { x -> x.toInt() }.toIntArray())
            "NBTTagLongArray" -> NBTTagLongArray.of(*value.map { x -> x.toLong() }.toLongArray())

            else -> null
        }

    inner class NBTEditor(
        val nbt: NBTTagCompound,
        val path: Array<String>,
        val handler: Consumer<InteractiveGUI>,
    ) {
        fun openNBTAdder(): InteractiveGUI {
            val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_NBT$TYPES`)

            for (option in gui.getOptions("nbt")) {
                val newName: AtomicReference<String> = AtomicReference()
                option.option.onNormalClicks { ctx ->
                    ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_NBT_NAME_ENTER)
                    ctx.startConversation()
                }.setConversation { ctx, input ->
                    val isCompound = option.name == "NBTTagCompound"

                    if (newName.get() == null && !isCompound) {
                        ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_NBT_VALUE_ENTER)
                        newName.set(input)
                    } else {
                        if (isCompound) newName.set(input)
                        val newNBT = try {
                            constructNBT(option.name, if (isCompound) emptyList() else input.split(" "))
                        } catch (ex: Throwable) {
                            ctx.messageContext.raw("error", ex.message)
                            ctx.sendError(AdminToolsLang.ITEM_EDITOR_NBT_VALUE_ERROR)
                            return@setConversation
                        }

                        this.nbt.set(newName.get(), newNBT)
                        ctx.endConversation()

                        item = ItemNBT.setTag(item, rootNBT)
                        if (newNBT is NBTTagCompound) {
                            NBTEditor(newNBT, path.plus(newName.get())) { innerGUI ->
                                innerGUI.push(
                                    "back",
                                    { NBTEditor(nbt, path, handler).openNBTViewerGUI() })
                            }
                                .openNBTViewerGUI()
                        } else {
                            this.openNBTViewerGUI()
                        }
                    }
                }.done()
            }

            gui.open()
            return gui
        }

        fun openNBTViewerGUI(): InteractiveGUI {
            val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_NBT`)
            gui.messageContext.parse(
                "path",
                LangConstants.S_COLOR + path.joinToString(" ${LangConstants.SEP}➡ ${LangConstants.S_COLOR}")
            )

            val tag = gui.getReusableOption("tag")!!

            for ((key, value) in nbt.value()) {
                tag.messageContext.raw("nbt", value.javaClass.simpleName)
                tag.messageContext.raw("key", key)
                tag.messageContext.raw("tag", getTypeOfNBT(value))

                val stringValue: List<String> = when (value) {
                    is NBTTagCompound -> {
                        val builtPaths = ArrayList<String>()
                        buildPathsFrom(builtPaths, value, "")
                        StringPathBuilder(builtPaths).toStringTree(CommandAdminLanguagePack.TREE_STYLE)
                            .print().lines.map { x -> x.toString() }
                    }

                    is NBTTagList<*> -> value.value().map { x -> x.toString() }
                    else -> listOf(value.value().toString())
                }

                tag.editItem { item ->
                    val meta = item.itemMeta
                    var lore = Strings.splitByLength(stringValue, 40)
                    if (lore.size > 20) {
                        lore = lore.take(20)
                        lore.add("...")
                    }
                    meta?.lore = lore
                    item.itemMeta = meta
                    return@editItem item
                }

                tag.on(ClickType.RIGHT) { _ ->
                    nbt.remove(key)
                    openNBTViewerGUI()
                }

                if (value is NBTTagCompound) {
                    tag.on(ClickType.LEFT) { ->
                        NBTEditor(value, path.plus(key)) { innerGUI ->
                            innerGUI.push(
                                "back", { NBTEditor(nbt, path, handler).openNBTViewerGUI() })
                        }.openNBTViewerGUI()
                    }
                } else {
                    tag.on(ClickType.LEFT) { ctx ->
                        ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_NBT_VALUE_ENTER)
                    }.setConversation { ctx, input ->
                        this.nbt.set(key, constructNBT(value::class.simpleName!!, input.split(" ")))
                        ctx.endConversation()
                        openNBTViewerGUI()
                    }
                }

                tag.done()
                if (!tag.hasNext()) break
            }

            gui.option("add-nbt").onNormalClicks { _ -> openNBTAdder() }.done()

            handler.accept(gui)
            return finalize(gui)
        }
    }

    fun openGUI(): InteractiveGUI {
        val gui: InventoryInteractiveGUI = GUIBuilder(KingdomsGUI.`ITEM$EDITOR_MAIN`)
            .forPlayer(player)
            .withSettings(getEditsForItem())
            .inventoryGUIOnly()
            .build()!!
        val meta = item.itemMeta!!

        gui.option("item").editItem { item }.done()

        gui.option("name").onNormalClicks { ctx ->
            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_NAME_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            meta.setDisplayName(input)
            item.itemMeta = meta
            ctx.endConversation()
            openGUI()
        }.done()
        gui.option("lore").onNormalClicks { ctx ->
            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_LORE_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            // Don't use \n character, minecraft interprets them as two separate characters.
            meta.lore = input.split("\\n").toList()
            item.itemMeta = meta
            ctx.endConversation()
            openGUI()
        }.editItem { optionItem ->
            optionItem.itemMeta?.let { optionMeta ->
                optionMeta.lore = meta.lore
                optionItem.itemMeta = optionMeta
            }
            return@editItem optionItem
        }.done()
        gui.option("material").onNormalClicks { ctx ->
            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_MATERIAL_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            val material = XMaterial.matchXMaterial(input)
            if (!material.isPresent) {
                ctx.messageContext.raw("material", input)
                ctx.sendError(KingdomsLang.INVALID_MATERIAL)
                return@setConversation
            }

            material.get().setType(item)
            ctx.endConversation()
            openGUI()
        }.editItem { optionItem ->
            optionItem.type = item.type
            return@editItem optionItem
        }.done()
        gui.option("amount").onNormalClicks { ctx ->
            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_AMOUNT_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            val amount = ProcessToMessage.getNumberOrError(
                NumberProcessor(input)
                    .withAllDecorators()
                    .withConstraints(NumberConstraint.INTEGER_ONLY, NumberConstraint.POSITIVE),
                "item amount", ctx
            ) ?: return@setConversation

            if (amount > 127.abstractNumber) {
                ctx.sendError(AdminToolsLang.ITEM_EDITOR_AMOUNT_WARNING)
            }

            item.amount = amount.value.toInt()
            ctx.endConversation()
            openGUI()
        }.done()
        gui.option("custom-model-data").onNormalClicks { ctx ->
            if (!XReflection.supports(14)) {
                ctx.sendError(AdminToolsLang.ITEM_EDITOR_CUSTOM_MODEL_DATA_NOT_SUPPORTED)
                return@onNormalClicks
            }

            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_CUSTOM_MODEL_DATA_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            val amount = ProcessToMessage.getNumberOrError(
                NumberProcessor(input)
                    .withAllDecorators()
                    .withConstraints(NumberConstraint.INTEGER_ONLY, NumberConstraint.POSITIVE),
                "custom model data", ctx
            ) ?: return@setConversation

            meta.setCustomModelData(amount.value.toInt())
            item.itemMeta = meta
            ctx.endConversation()
            openGUI()
        }.done()

        gui.option("enchantments").onNormalClicks { -> openEnchantsGUI() }.editItem { innerItem ->
            val innerMeta = innerItem.itemMeta!!
            val lore: MutableList<String> = if (innerMeta.hasLore()) innerMeta.lore!! else mutableListOf()

            for ((enchant, level) in meta.enchants) {
                lore.add(
                    MessageCompiler.compile(
                        "{\$dot} ${LangConstants.P_COLOR}${XEnchantment.of(enchant)}${LangConstants.SEP}: ${LangConstants.S_COLOR}$level",
                        MessageCompilerSettings.none().colorize().translatePlaceholders()
                    ).buildPlain(MessagePlaceholderProvider())
                )
            }

            innerMeta.lore = lore
            innerItem.itemMeta = innerMeta
            return@editItem innerItem
        }.done()
        gui.option("attributes").onNormalClicks { -> openAttributeGUI() }.done()
        gui.option("flags").onNormalClicks { -> openFlagsGUI() }.done()
        gui.option("nbt").onNormalClicks { ->
            NBTEditor(rootNBT, emptyArray()) { innerGUI ->
                innerGUI.push(
                    "back",
                    { openGUI() })
            }.openNBTViewerGUI()
        }
            .editItem { innerItem ->
                val innerMeta = innerItem.itemMeta!!
                val lore: MutableList<String> = if (innerMeta.hasLore()) innerMeta.lore!! else mutableListOf()

                val builtPaths = mutableListOf<String>()
                buildPathsFrom(builtPaths, rootNBT, "")
                lore.addAll(
                    StringPathBuilder(builtPaths).toStringTree(CommandAdminLanguagePack.TREE_STYLE)
                        .print().lines.map { x -> x.toString() })

                innerMeta.lore = lore
                innerItem.itemMeta = innerMeta
                return@editItem innerItem
            }.done()
        gui.option("unbreakable").onNormalClicks { ->
            meta.isUnbreakable = !meta.isUnbreakable
            item.itemMeta = meta
            openGUI()
        }.done()

        gui.onClose {
            if (gui.wasSwitched()) return@onClose
            val slot = gui.option("item").constructGUIOptionObject().settings.slots.first()
            val itemInSlot = gui.inventory.getItem(slot)
            if (itemInSlot != null) XItemStack.giveOrDrop(player, itemInSlot)
        }

        // This one's special, don't finalize it.
        gui.open()
        return gui
    }

    fun openAttributeGUI(): InteractiveGUI {
        val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_LIST`, getEditsForItem())
        val meta = item.itemMeta!!

        gui.option("add-attribute").onNormalClicks { ->
            val attr = XAttribute.ATTACK_DAMAGE
            val takenNames =
                if (meta.hasAttributeModifiers()) meta.attributeModifiers!!.values().map { it.name }.toHashSet()
                else hashSetOf()

            var chosenName = "attribute"
            var chosenNameInc = 1
            while (takenNames.contains(chosenName)) {
                chosenName = "attribute_$chosenNameInc"
                chosenNameInc++
            }

            val modifier =
                createAttributeModifier(null, chosenName, 1.0, Operation.ADD_NUMBER, EquipmentSlot.HAND)
            meta.addAttributeModifier(attr.get()!!, modifier)
            item.itemMeta = meta
            openAttributeBuilderGUI(attr, modifier) { newAttr, mod, gui ->
                gui.push("back", {
                    meta.removeAttributeModifier(attr.get()!!, modifier)
                    meta.addAttributeModifier(newAttr.get()!!, mod)
                    item.itemMeta = meta
                    openAttributeGUI()
                })
            }
        }.done()

        gui.option("remove-all").onNormalClicks { ->
            meta.attributeModifiers = null
            item.itemMeta = meta
            openAttributeGUI()
        }.done()

        val attrOpt = gui.getReusableOption("attribute")!!
        meta.attributeModifiers?.entries()?.let {
            for ((attribute, modifier) in it) {
                attrOpt.on(ClickType.LEFT) { ->
                    openAttributeBuilderGUI(XAttribute.of(attribute), modifier) { newAttr, newMod, gui ->
                        gui.push("back", {
                            meta.removeAttributeModifier(attribute, modifier)
                            meta.addAttributeModifier(newAttr.get()!!, newMod)
                            item.itemMeta = meta
                            openAttributeGUI()
                        })
                    }
                }.on(ClickType.RIGHT) { ->
                    meta.removeAttributeModifier(attribute, modifier)
                    item.itemMeta = meta
                    openAttributeGUI()
                }
                attrOpt.editItem { item ->
                    val inheritItem = getItemFromGUI(
                        KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_TYPES`,
                        player,
                        Strings.configOption(XAttribute.of(attribute).name())
                    )
                    return@editItem item.inheritFrom(inheritItem, inheritLore = false)
                }
                attrOpt.messageContext.addAll(getEditsForAttribute(XAttribute.of(attribute), modifier).placeholders)
                attrOpt.done()
            }
        }

        gui.push("back", { openGUI() })
        return finalize(gui)
    }

    fun getItemFromGUI(gui: KingdomsGUI, player: Player, optionName: String): ItemStack {
        val fakeGUI = GUIAccessor.prepare(player, gui)
        val opt = fakeGUI.option(optionName).setEdits("enabled", true)
        val obj = opt.constructGUIOptionObject()
        if (obj === null) return XMaterial.STONE.parseItem()!!

        obj.defineVariables(opt.messageContext)
        return obj.item
    }

    fun ItemStack.inheritFrom(other: ItemStack, inheritLore: Boolean = true): ItemStack {
        this.type = other.type

        if (other.hasItemMeta()) {
            val otherMeta = other.itemMeta!!
            val meta = this.itemMeta!!

            if (otherMeta is SkullMeta) {
                // We can't use xskull directly, we'd get this error:
                //  Profile doesn't have a value: ProfileInstruction
                val xskull = XSkull.of(otherMeta)
                val skin = xskull.profile
                if (skin !== null) {
                    XSkull.of((meta as SkullMeta)).profile(Profileable.of(skin, true)).apply()
                }
            }

            if (inheritLore && otherMeta.hasLore()) {
                meta.lore = otherMeta.lore
            }

            this.itemMeta = meta
        }

        return this
    }

    @Suppress("DEPRECATION") fun openAttributeBuilderGUI(
        attribute: XAttribute, modifier: AttributeModifier,
        guiModifier: TriConsumer<XAttribute, AttributeModifier, InteractiveGUI>,
    ): InteractiveGUI {
        // public AttributeModifier(@NotNull UUID uuid, @NotNull String name, double amount, @NotNull Operation operation, @Nullable EquipmentSlot slot) {
        val gui = GUIAccessor.prepare(
            player,
            KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_EDITOR`,
            getEditsForAttribute(attribute, modifier)
        )

        gui.option("type").onNormalClicks { ->
            openAttributeTypesGUI({ type ->
                openAttributeBuilderGUI(type, modifier, guiModifier)
            }, { innerGUI -> innerGUI.push("back", { openAttributeBuilderGUI(attribute, modifier, guiModifier) }) })
        }.editItem { item ->
            return@editItem item.inheritFrom(
                getItemFromGUI(
                    KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_TYPES`,
                    player,
                    Strings.configOption(attribute.name())
                )
            )
        }.done()

        gui.option("operation").onNormalClicks { ->
            openAttributeOperationsGUI(modifier.operation, { operation ->
                openAttributeBuilderGUI(
                    attribute,
                    createAttributeModifier(modifier, modifier.name, modifier.amount, operation, modifier.slot),
                    guiModifier
                )
            }, { innerGUI -> innerGUI.push("back", { openAttributeBuilderGUI(attribute, modifier, guiModifier) }) })
        }.editItem { item ->
            return@editItem item.inheritFrom(
                getItemFromGUI(
                    KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_OPERATIONS`,
                    player,
                    Strings.configOption(modifier.operation)
                )
            )
        }.done()

        gui.option("equipment-slot").onNormalClicks { ->
            openEquipmentSlotSelector(modifier.slot, { slot ->
                openAttributeBuilderGUI(
                    attribute,
                    createAttributeModifier(modifier, modifier.name, modifier.amount, modifier.operation, slot),
                    guiModifier
                )
            }, { innerGUI -> innerGUI.push("back", { openAttributeBuilderGUI(attribute, modifier, guiModifier) }) })
        }.editItem { item ->
            return@editItem item.inheritFrom(
                getItemFromGUI(
                    KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_EQUIPMENT$SLOTS`, player,
                    Strings.configOption(modifier.slot?.name ?: "none")
                )
            )
        }.done()

        gui.option("uuid").onNormalClicks { ctx ->
            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_ATTRIBUTES_UUID_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            val id: UUID
            try {
                id = UUID.fromString(input)
            } catch (ex: IllegalArgumentException) {
                ctx.sendError(KingdomsLang.INVALID_UUID)
                return@setConversation
            }

            ctx.endConversation()
            openAttributeBuilderGUI(
                attribute,
                createAttributeModifier(modifier, id.toString(), modifier.amount, modifier.operation, modifier.slot),
                guiModifier
            )
        }.done()

        gui.option("name").onNormalClicks { ctx ->
            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_ATTRIBUTES_NAME_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            ctx.endConversation()
            openAttributeBuilderGUI(
                attribute,
                createAttributeModifier(modifier, input, modifier.amount, modifier.operation, modifier.slot),
                guiModifier
            )
        }.done()

        gui.option("amount").onNormalClicks { ctx ->
            ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_ATTRIBUTES_AMOUNT_ENTER)
            ctx.startConversation()
        }.setConversation { ctx, input ->
            val amount = ProcessToMessage.getNumberOrError(NumberProcessor(input).withAllDecorators(), "modifier", ctx)
                ?: return@setConversation
            ctx.endConversation()
            openAttributeBuilderGUI(
                attribute,
                createAttributeModifier(
                    modifier,
                    modifier.name,
                    amount.value.toDouble(),
                    modifier.operation,
                    modifier.slot
                ),
                guiModifier
            )
        }.done()


        guiModifier.accept(attribute, modifier, gui)
        return finalize(gui)
    }

    private fun createAttributeModifier(
        @Suppress("UNUSED_PARAMETER") previous: AttributeModifier?, name: String, amount: Double,
        operation: Operation, slot: EquipmentSlot?
    ): AttributeModifier {
        return XAttribute.createModifier(name, amount, operation, slot)
        // return if (XReflection.supports(18)) {
        //     AttributeModifier(
        //         previous?.key ?: NamespacedKey(Kingdoms.get(), name),
        //         amount, operation, slot?.group ?: EquipmentSlotGroup.ANY
        //     )
        // } else {
        //     AttributeModifier(previous?.uniqueId ?: UUID.randomUUID(), name, amount, operation, slot)
        // }
    }

    fun openAttributeTypesGUI(selected: Consumer<XAttribute>, guiModifier: Consumer<InteractiveGUI>): InteractiveGUI {
        val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_TYPES`)

        for (attr in XAttribute.getValues()) {
            gui.option(Strings.configOption(attr.name())).onNormalClicks { ->
                selected.accept(attr)
            }.done()
        }

        guiModifier.accept(gui)
        return finalize(gui)
    }

    fun openEquipmentSlotSelector(
        currentValue: EquipmentSlot?,
        selected: Consumer<EquipmentSlot?>,
        guiModifier: Consumer<InteractiveGUI>,
    ): InteractiveGUI {
        val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_EQUIPMENT$SLOTS`)

        for (equipSlot in EnumCache.EQUIPMENT_SLOTS) {
            gui.option(Strings.configOption(equipSlot)).setEdits("enabled", currentValue == equipSlot)
                .onNormalClicks { ->
                    selected.accept(equipSlot)
                }.done()
        }

        gui.option("none").setEdits("enabled", currentValue == null).onNormalClicks { ->
            selected.accept(null)
        }.done()

        guiModifier.accept(gui)
        return finalize(gui)
    }

    fun openAttributeOperationsGUI(
        currentValue: Operation,
        selected: Consumer<Operation>,
        guiModifier: Consumer<InteractiveGUI>,
    ): InteractiveGUI {
        val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_ATTRIBUTES_OPERATIONS`)

        for (operation in Operation.values()) {
            gui
                .option(Strings.configOption(operation)).setEdits("enabled", currentValue == operation)
                .onNormalClicks { -> selected.accept(operation) }
                .done()
        }

        guiModifier.accept(gui)
        return finalize(gui)
    }

    fun openFlagsGUI() {
        val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_FLAGS`, getEditsForItem())
        val meta = item.itemMeta!!

        for (option in gui.getOptions("flag")) {
            val flagOpt =
                Enums.getIfPresent(
                    ItemFlag::class.java,
                    option.name.uppercase(Locale.ENGLISH).replace('-', '_')
                )
            if (flagOpt.isPresent) {
                val flag = flagOpt.get()
                option.option.setEdits("supported", true, "enabled", meta.hasItemFlag(flag)).onNormalClicks { ->
                    if (meta.hasItemFlag(flag)) meta.removeItemFlags(flag)
                    else meta.addItemFlags(flag)

                    item.itemMeta = meta
                    openFlagsGUI()
                }.done()
            } else {
                option.option.setEdits("supported", false, "enabled", false).done()
            }
        }

        gui.option("remove-all").onNormalClicks { ->
            meta.removeItemFlags(
                *XItemFlag.getValues().stream()
                .filter { it.isSupported }.map { it.get() }
                .collect(Collectors.toList()).toTypedArray())
            item.itemMeta = meta
            openFlagsGUI()
        }.done()

        gui.option("back").onNormalClicks { -> openGUI() }.done()
        finalize(gui)
    }

    fun openEnchantsGUI() {
        val gui = GUIAccessor.prepare(player, KingdomsGUI.`ITEM$EDITOR_ENCHANTMENTS`, getEditsForItem())
        val meta = item.itemMeta!!

        for (entry in gui.getOptions("enchant")) {
            val xEnchant = XEnchantment.of(entry.name.replace('-', '_'))
            val option = entry.option
            if (!xEnchant.isPresent || !xEnchant.get().isSupported) {
                option.setEdits("enabled", false, "supported", false).done()
            } else {
                val enchant = xEnchant.get().get()!!
                option
                    .setEdits(
                        "enabled", meta.hasEnchant(enchant),
                        "supported", true,
                        "level", meta.getEnchantLevel(enchant)
                    )
                    .on(ClickType.LEFT) { ctx ->
                        ctx.sendMessage(AdminToolsLang.ITEM_EDITOR_ENCHANT_ENTER)
                        ctx.startConversation()
                    }.setConversation { ctx, input ->
                        val level = ProcessToMessage.getNumberOrError(
                            NumberProcessor(input)
                                .withAllDecorators()
                                .withConstraints(NumberConstraint.INTEGER_ONLY, NumberConstraint.POSITIVE),
                            "enchant level", ctx
                        ) ?: return@setConversation
                        meta.addEnchant(enchant, level.value.toInt(), true)
                        item.itemMeta = meta
                        ctx.endConversation()
                        openEnchantsGUI()
                    }.on(ClickType.RIGHT) { ->
                        if (meta.hasEnchant(enchant)) {
                            meta.removeEnchant(enchant)
                            item.itemMeta = meta
                            openEnchantsGUI()
                        }
                    }.done()
            }
        }

        gui.option("remove-all").onNormalClicks { ->
            meta.enchants.keys.forEach { enchantment -> meta.removeEnchant(enchantment) }
            item.itemMeta = meta
            openEnchantsGUI()
        }.done()
        gui.option("back").onNormalClicks { -> openGUI() }.done()
        finalize(gui)
    }

    private fun finalize(gui: InteractiveGUI): InteractiveGUI {
        gui.onClose {
            if (gui.wasSwitched()) return@onClose
            XItemStack.giveOrDrop(player, item)
        }
        gui.open()
        return gui
    }
}