package org.kingdoms.services.maps.abstraction.markersets;

import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.constants.namespace.Namespaced;
import org.kingdoms.data.Pair;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.utils.config.ConfigSection;
import org.kingdoms.utils.config.adapters.YamlContainer;
import org.kingdoms.utils.config.adapters.YamlResource;
import org.kingdoms.utils.config.importer.YamlGlobalImporter;
import org.kingdoms.utils.internal.enumeration.QuickEnumMap;
import org.snakeyaml.nodes.MappingNode;
import org.snakeyaml.validation.NodeValidator;

import java.util.*;

public class MarkerType implements Namespaced {
    public static final MarkerType
            KINGDOMS = new MarkerType("kingdoms"),
            NATIONS = new MarkerType("nations"),
            POWERCELL = new MarkerType("POWERCELL", MarkerType.KINGDOMS, KINGDOMS.getConfig().getSection("powercell"));

    private final Namespace namespace;
    private final String id;
    private final YamlResource adapter;
    private final ConfigSection config;

    @SuppressWarnings("MapReplaceableByEnumMap")
    private final Map<MarkerListenerType, MarkerListenerType.Settings> listenerTypes = new QuickEnumMap<>(MarkerListenerType.values());

    static {
        MarkerTypeRegistry.INSTANCE.register(KINGDOMS);
        MarkerTypeRegistry.INSTANCE.register(NATIONS);
        MarkerTypeRegistry.INSTANCE.register(POWERCELL);
    }

    public static Collection<MarkerType> getMarkerTypes() {
        return MarkerTypeRegistry.INSTANCE.getRegistry().values();
    }

    @SuppressWarnings("unchecked")
    public static <T extends MarkerListenerType.Settings> Collection<Pair<MarkerType, T>> getMarkerTypes(MarkerListenerType type) {
        List<Pair<MarkerType, T>> list = new ArrayList<>(6);

        for (MarkerType markerType : MarkerTypeRegistry.INSTANCE.getRegistry().values()) {
            MarkerListenerType.Settings settings = markerType.listenerTypes.get(type);
            if (settings != null) list.add(Pair.of(markerType, (T) settings));
        }

        return list;
    }

    public MarkerType listen(MarkerListenerType.Settings listener) {
        if (this.listenerTypes.put(listener.type(), listener) != null) {
            throw new IllegalArgumentException("Lisetener " + listener + " was already registered for " + this);
        }
        return this;
    }

    private final MarkerSettings markerSettings = new MarkerSettings();

    public MarkerType(String name) {
        String id = name.replace('_', '-');
        this.namespace = Namespace.kingdoms(name.toUpperCase(Locale.ENGLISH));
        this.id = "kingdoms_" + id;
        this.adapter = new YamlResource(
                getPlugin(),
                Kingdoms.getFolder().resolve("maps").resolve(id + ".yml").toFile(),
                "maps/" + id + ".yml"
        );
        adapter.setResolveAliases(false).load();
        adapter.setImporter(YamlGlobalImporter.INSTANCE).importDeclarations();

        this.config = null;
        this.adapter.setSchema(Data.VALIDATOR);
    }

    public MarkerType(String name, MarkerType parent, ConfigSection config) {
        String id = name.toUpperCase(Locale.ENGLISH).replace('_', '-');
        this.namespace = Namespace.kingdoms(name);
        this.id = "kingdoms_" + id;
        this.adapter = null;

        Objects.requireNonNull(config, () -> "Marker type '" + name + "' config is null");
        MappingNode node = config.getNode().clone();
        node.copyIfDoesntExist(parent.getConfig().getNode(), parent.namespace.asNormalizedString());
        this.config = new ConfigSection(node);
    }

    static Plugin getPlugin() {
        try {
            return (Plugin) Class.forName("org.kingdoms.services.maps.MapViewerAddon").getDeclaredMethod("get").invoke(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() {
        return id;
    }

    public MarkerSettings getMarkerSettings() {
        return markerSettings;
    }

    public Optional<YamlResource> getConfigAdapter() {
        return Optional.ofNullable(adapter);
    }

    public ConfigSection getConfig() {
        return adapter == null ? config : adapter.getConfig();
    }

    public KeyedYamlConfigAccessor config(MarkerConfig config) {
        return config.getManager(this);
    }

    public boolean isEnabled() {
        return !config(MarkerConfig.DISABLED).getBoolean();
    }

    @Override
    public @NonNull Namespace getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + namespace.asNormalizedString() + ')';
    }

    private static final class Data {
        private static final NodeValidator VALIDATOR = YamlContainer.parseValidator(getPlugin().getResource("schemas/maps/marker.yml"), "Marker Schema");
    }
}
