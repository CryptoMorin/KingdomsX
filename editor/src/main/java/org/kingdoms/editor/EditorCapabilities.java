package org.kingdoms.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Map;

public final class EditorCapabilities {
    final Map<String, SchematicLinkage> schematicDirectories;
    final Map<String, String> outpostPageDirectories;

    public EditorCapabilities(
            Map<String, SchematicLinkage> schematicDirectories,
            Map<String, String> outpostPageDirectories) {
        this.schematicDirectories = schematicDirectories;
        this.outpostPageDirectories = outpostPageDirectories;
    }
}
