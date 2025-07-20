package org.kingdoms.services.maps;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.data.Pair;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.utils.fs.FolderRegistry;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class IconFolderRegistry extends FolderRegistry {
    private final String folderName;
    private final Map<String, Object> icons = new HashMap<>();
    private final MapAPI api;

    public IconFolderRegistry(@NonNull String displayName, @NonNull Path folder, String folderName, MapAPI api) {
        super(displayName, folder);
        this.folderName = folderName;
        this.api = api;
    }

    public Map<String, Object> getIcons() {
        return icons;
    }

    @Override
    protected Pair<String, URI> getDefaultsURI() {
        URI uri;
        try {
            uri = MapViewerAddon.class.getResource('/' + folderName).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return Pair.of(folderName, uri);
    }

    @Override
    protected void handle(@NonNull Entry entry) {
        Object registered = api.updateOrRegisterIcon(entry.getName(), entry.getPath());
        icons.put(entry.getName(), registered);
    }

    @Override
    public void register() {
        if (Files.exists(folder)) {
            visitPresent();
            if (useDefaults) visitDefaults();
        } else {
            visitDefaults();
        }
    }
}
