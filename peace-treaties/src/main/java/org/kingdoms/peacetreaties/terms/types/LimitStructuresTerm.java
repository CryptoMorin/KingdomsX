package org.kingdoms.peacetreaties.terms.types;

import com.google.gson.JsonObject;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.locale.provider.MessageBuilder;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

import java.util.concurrent.CompletionStage;

public class LimitStructuresTerm extends Term {
    private int maxStructures;

    public static final TermProvider PROVIDER =
            new StandardTermProvider(Namespace.kingdoms("LIMIT_STRUCTURES"), LimitStructuresTerm::new, true, true) {
                @Override
                public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
                    return standardAmountPrompt(this, options, editor, (amount) -> new LimitStructuresTerm(amount.intValue()));
                }
            };


    public LimitStructuresTerm(int maxStructures) {
        this.maxStructures = maxStructures;
    }

    public LimitStructuresTerm() {}

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean apply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        return true;
    }

    public int getMaxStructures() {
        return maxStructures;
    }

    @Override
    public void deserialize(DeserializationContext context) {
        JsonObject json = context.getJson();
        this.maxStructures = json.get("maxStructures").getAsInt();
    }

    @Override
    public void serialize(SerializationContext context) {
        JsonObject json = context.getJson();
        json.addProperty("maxStructures", maxStructures);
    }

    @Override
    public void addEdits(MessageBuilder builder) {
        super.addEdits(builder);
        builder.raw("term_limit_structures_amount", maxStructures);
    }


    @Override
    public Messenger canAccept(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        return null;
    }
}
