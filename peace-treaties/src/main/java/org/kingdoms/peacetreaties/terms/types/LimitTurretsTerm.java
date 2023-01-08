package org.kingdoms.peacetreaties.terms.types;

import com.google.gson.JsonObject;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.locale.provider.MessageBuilder;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

import java.util.concurrent.CompletionStage;

public class LimitTurretsTerm extends Term {
    private int maxTurrets;

    public static final TermProvider PROVIDER =
            new StandardTermProvider(Namespace.kingdoms("LIMIT_TURRETS"), LimitTurretsTerm::new, true, true) {
                @Override
                public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
                    return standardAmountPrompt(this, options, editor, (amount) -> new LimitTurretsTerm(amount.intValue()));
                }
            };

    public LimitTurretsTerm(int maxTurrets) {
        this.maxTurrets = maxTurrets;
    }

    public LimitTurretsTerm() {}

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    public int getMaxTurrets() {
        return maxTurrets;
    }

    @Override
    public void deserialize(DeserializationContext context) {
        JsonObject json = context.getJson();
        this.maxTurrets = json.get("maxTurrets").getAsInt();
    }

    @Override
    public void serialize(SerializationContext context) {
        JsonObject json = context.getJson();
        json.addProperty("maxTurrets", maxTurrets);
    }

    @Override
    public void addEdits(MessageBuilder builder) {
        super.addEdits(builder);
        builder.raw("term_limit_turrets_amount", maxTurrets);
    }
}
