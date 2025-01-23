package org.kingdoms.utils.paper;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.TestOnly;
import org.kingdoms.utils.paper.asyncchunks.AsyncChunks;

import java.util.function.Function;

public final class PaperLib {
    private static AsyncChunks asyncChunks;

    public static void init(Function<Integer, Boolean> versionChecker) {
        asyncChunks = AsyncChunks.generateInstance(versionChecker);
    }

    public static AsyncChunks getAsyncChunks() {
        return asyncChunks;
    }

    @TestOnly
    public static String getItemHoverEvent(ItemStack item) {
        // showItem -> https://github.com/PaperMC/Paper/blob/e08e6679fcaf5ce8b91db628309ed530e58a4133/patches/server/0010-Adventure.patch#L4832-L4847
        // unwrap (basically a asNMSCopy()) -> https://github.com/PaperMC/Paper/blob/e08e6679fcaf5ce8b91db628309ed530e58a4133/patches/server/0009-MC-Utils.patch#L5707-L5713
        // asAdventure -> https://github.com/PaperMC/Paper/blob/e08e6679fcaf5ce8b91db628309ed530e58a4133/patches/server/1020-Registry-Modification-API.patch#L423-L425
        // Bukkit.getServer().getItemFactory().asHoverEvent(this, op);
        // net.kyori.adventure.text.event.HoverEvent.showItem(UnaryOperator.identity().apply(
        //         net.kyori.adventure.text.event.HoverEvent.ShowItem.showItem(
        //                 item.getType().getKey(),
        //                 item.getAmount(),
        //                 io.papermc.paper.adventure.PaperAdventure.asAdventure(CraftItemStack.unwrap(item).getComponentsPatch())) // unwrap is fine here because the components patch will be safely copied
        // ));
        return item.asHoverEvent().toString();
    }
}
