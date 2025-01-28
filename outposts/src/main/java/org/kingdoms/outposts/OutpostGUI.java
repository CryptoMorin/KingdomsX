package org.kingdoms.outposts;

import org.kingdoms.gui.GUIPathContainer;

public enum OutpostGUI implements GUIPathContainer {
    EDITOR,
    REWARDS_COMMANDS,
    REWARDS_ITEMS,
    ARENA$MOBS, ARENA$MOB,
    REWARDS_MAIN;

    private final String path;

    OutpostGUI() {
        this.path = "outposts/" + GUIPathContainer.translateEnumPath(this);
    }

    @Override
    public String getGUIPath() {
        return path;
    }
}
