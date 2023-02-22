package org.kingdoms.peacetreaties.config;

import org.kingdoms.gui.GUIPathContainer;

public enum PeaceTreatyGUI implements GUIPathContainer {
    EDITOR,
    PEACE$TREATIES,
    KEEP$LANDS,
    DEFAULT$TERM$EDITOR,
    ;

    private final String path;

    PeaceTreatyGUI() {
        this.path = "peace-treaties/" + GUIPathContainer.translateEnumPath(this);
    }

    @Override
    public String getGUIPath() {
        return path;
    }
}
