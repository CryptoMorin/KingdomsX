package org.kingdoms.editor;

import org.kingdoms.ide.DontObfuscate;

@DontObfuscate("Used for JSON serialization.")
public final class SchematicLinkage {
    final String path;
    final String config;

    public SchematicLinkage(String path, String config) {
        this.path = path;
        this.config = config;
    }

    @Override
    public String toString() {
        return "SchematicLinkage{" +
                "path='" + path + '\'' +
                ", config='" + config + '\'' +
                '}';
    }
}
