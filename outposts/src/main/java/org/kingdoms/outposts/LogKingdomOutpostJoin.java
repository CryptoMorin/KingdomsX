package org.kingdoms.outposts;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.kingdoms.constants.group.model.logs.AuditLog;
import org.kingdoms.constants.group.model.logs.AuditLogProvider;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.main.locale.messager.MessageBuilder;
import org.kingdoms.utils.internal.FastUUID;

import java.util.UUID;

public class LogKingdomOutpostJoin extends AuditLog {
    private UUID player;
    private String outpostName;

    protected LogKingdomOutpostJoin() {}

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
    public void deserialize(DeserializationContext context) {
        JsonObject json = context.getJson();
        this.player = FastUUID.fromString(json.get("player").getAsString());
        this.outpostName = json.get("outpostName").getAsString();
    }

    @Override
    public void serialize(SerializationContext context) {
        JsonObject json = context.getJson();
        json.addProperty("player", FastUUID.toString(player));
        json.addProperty("outpostName", this.outpostName);
    }

    @Override
    protected void addEdits(MessageBuilder builder) {
        builder.withContext(Bukkit.getOfflinePlayer(player));
        builder.raw("outpost_name", outpostName);
    }
}
