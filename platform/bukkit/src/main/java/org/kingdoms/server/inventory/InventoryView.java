package org.kingdoms.server.inventory;

public class InventoryView {
    public static BukkitInventoryView of(Object any) {
        if (any.getClass().isInterface()) return new NewInventoryView(any);
        else return new OldInventoryView(any);
    }
}
