package org.kingdoms.server.inventory;

import java.util.Arrays;

public interface InventorySlot {
    int[] getSlots();

    /**
     * For things that are only intended to be a single slot.
     */
    default int getSlot() {
        int[] slots = getSlots();
        if (slots.length != 1) throw new UnsupportedOperationException("Not a single slot: " + this);
        return slots[0];
    }

    static int[] inherit(InventorySlot... slots) {
        return Arrays.stream(slots).flatMapToInt(x -> Arrays.stream(x.getSlots())).toArray();
    }

    static int[] range(int min, int max) {
        int[] slots = new int[max - min];

        int index = 0;
        for (int i = min; i < max; i++) {
            slots[index++] = i;
        }
        return slots;
    }

    enum Player implements InventorySlot {
        HELMET(39),
        CHESTPLATE(38),
        LEGGINGS(37),
        BOOTS(36),
        UPPER_STORAGE(9, 35),
        ARMOR(HELMET, CHESTPLATE, LEGGINGS, BOOTS),

        HOTBAR(0, 8),
        OFFHAND(40);

        private final int[] slots;

        Player(int[] slots) {this.slots = slots;}

        Player(Player... slots) {
            this.slots = inherit(slots);
        }

        Player(int min, int max) {
            this.slots = range(min, max);
        }

        Player(int slot) {this.slots = new int[]{slot};}

        @Override
        public int[] getSlots() {
            return slots;
        }
    }
}
