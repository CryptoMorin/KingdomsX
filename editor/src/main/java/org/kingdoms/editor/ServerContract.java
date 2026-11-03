package org.kingdoms.editor;

import java.util.Map;

public final class ServerContract {
    final int protocol;
    final String pluginVersion;
    final Map<String, String> files;
    final Map<String, String> yamlFiles;
    final Map<String, String> schematicFiles;
    final EditorCapabilities capabilities;
    final byte[] manifestBytes;

    public ServerContract(
            int protocol,
            String pluginVersion,
            Map<String, String> files,
            Map<String, String> yamlFiles,
            Map<String, String> schematicFiles,
            EditorCapabilities capabilities,
            byte[] manifestBytes) {
        this.protocol = protocol;
        this.pluginVersion = pluginVersion;
        this.files = files;
        this.yamlFiles = yamlFiles;
        this.schematicFiles = schematicFiles;
        this.capabilities = capabilities;
        this.manifestBytes = manifestBytes;
    }
}
