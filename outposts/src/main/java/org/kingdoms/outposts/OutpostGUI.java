package org.kingdoms.outposts;

import org.kingdoms.gui.GUIPathContainer;
import org.kingdoms.gui.KingdomsGUI;

public enum OutpostGUI implements GUIPathContainer {
    OUTPOSTS_EDITOR,
    OUTPOSTS_REWARDS_COMMANDS,
    OUTPOSTS_REWARDS_ITEMS,
    OUTPOSTS_REWARDS_MAIN;

    private final String path;

    OutpostGUI() {
        this.path = KingdomsGUI.translateEnumPath(this);
    }

    @Override
    public String getGUIPath() {
        return path;
    }
}
