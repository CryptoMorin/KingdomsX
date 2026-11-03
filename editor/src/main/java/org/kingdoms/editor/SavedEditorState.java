package org.kingdoms.editor;

import java.time.LocalDateTime;

public class SavedEditorState {
    public String savedDate;
    public String id, key, serverToken, linkSeed;
    public int protocol;

    public static SavedEditorState from(KingdomsEditorSession.Session session) {
        SavedEditorState state = new SavedEditorState();
        state.savedDate = LocalDateTime.now().toString();
        state.id = session.id;
        state.key = session.key;
        state.serverToken = session.serverToken;
        state.linkSeed = session.linkSeed;
        state.protocol = session.remoteContract.protocol;
        return state;
    }
}
