package org.kingdoms.outposts;

import org.bukkit.Bukkit;
import org.kingdoms.constants.group.model.logs.AuditLog;
import org.kingdoms.constants.group.model.logs.AuditLogProvider;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.data.database.dataprovider.SectionableDataSetter;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;

import java.util.UUID;

public class LogKingdomOutpostJoin extends AuditLog {
    private UUID player;
    private String outpostName;

    protected LogKingdomOutpostJoin() {
    }

    public LogKingdomOutpostJoin(UUID player, String outpostName) {
        this.player = player;
        this.outpostName = outpostName;
    }

    private static final Namespace NS = new Namespace("Outposts", "JOIN");
    public static final AuditLogProvider PROVIDER = new AuditLogProvider() {
        @Override
        public AuditLog construct() {
            return new LogKingdomOutpostJoin();
        }

        @Override
        public Namespace getNamespace() {
            return NS;
        }
    };

    @Override
    public AuditLogProvider getProvider() {
        return PROVIDER;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getOutpostName() {
        return outpostName;
    }

    @Override
    public void deserialize(DeserializationContext<SectionableDataGetter> context) {
        SectionableDataGetter json = context.getDataProvider();
        this.player = json.get("player").asUUID();
        this.outpostName = json.get("outpostName").asString();
    }

    @Override
    public void serialize(SerializationContext<SectionableDataSetter> context) {
        SectionableDataSetter json = context.getDataProvider();
        json.setUUID("player", player);
        json.setString("outpostName", this.outpostName);
    }

    @Override
    public void addEdits(MessagePlaceholderProvider builder) {
        builder.withContext(Bukkit.getOfflinePlayer(player));
        builder.raw("outpost_name", outpostName);
    }
}
