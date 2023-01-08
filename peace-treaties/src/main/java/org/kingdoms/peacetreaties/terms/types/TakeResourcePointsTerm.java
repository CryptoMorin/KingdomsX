package org.kingdoms.peacetreaties.terms.types;

import com.google.gson.JsonObject;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.locale.provider.MessageBuilder;
import org.kingdoms.peacetreaties.config.PeaceTreatyLang;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

import java.util.concurrent.CompletionStage;

public class TakeResourcePointsTerm extends Term {
    private long amount;

    public static final TermProvider PROVIDER = new StandardTermProvider(Namespace.kingdoms("TAKE_RESOURCE_POINTS"), TakeResourcePointsTerm::new, true, false) {
        @Override
        public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
            return standardAmountPrompt(this, options, editor, (amount) -> new TakeResourcePointsTerm(amount.longValue()));
        }
    };

    TakeResourcePointsTerm() {}

    public TakeResourcePointsTerm(long amount) {
        this.amount = amount;
    }

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public void deserialize(DeserializationContext context) {
        JsonObject json = context.getJson();
        this.amount = json.get("amount").getAsLong();
    }

    @Override
    public void serialize(SerializationContext context) {
        JsonObject json = context.getJson();
        json.addProperty("amount", amount);
    }

    @Override
    public boolean apply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        Kingdom kingdom = peaceTreaty.getVictimKingdom();
        if (!kingdom.hasResourcePoints(amount)) return false;
        kingdom.addResourcePoints(-amount);
        return true;
    }

    @Override
    public void addEdits(MessageBuilder builder) {
        super.addEdits(builder);
        builder.raw("term_take_resource_points_amount", amount);
    }

    @Override
    public Messenger canAccept(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        Kingdom kingdom = peaceTreaty.getVictimKingdom();
        if (!kingdom.hasResourcePoints(amount)) return PeaceTreatyLang.TERMS_TAKE_RESOURCE_POINTS_NOT_ENOUGH;
        return null;
    }
}
