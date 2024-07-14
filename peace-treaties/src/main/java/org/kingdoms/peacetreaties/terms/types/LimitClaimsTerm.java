package org.kingdoms.peacetreaties.terms.types;

import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.data.database.dataprovider.SectionableDataSetter;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

import java.util.concurrent.CompletionStage;

public class LimitClaimsTerm extends Term {
    private int maxClaims;

    public static final TermProvider PROVIDER =
            new StandardTermProvider(Namespace.kingdoms("LIMIT_CLAIMS"), LimitClaimsTerm::new, true, true) {
                @Override
                public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
                    return standardAmountPrompt(this, options, editor, (amount) -> new LimitClaimsTerm(amount.intValue()));
                }
            };

    public LimitClaimsTerm(int maxClaims) {
        this.maxClaims = maxClaims;
    }

    public LimitClaimsTerm() {
    }

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean apply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        return true;
    }

    public int getMaxClaims() {
        return maxClaims;
    }

    @Override
    public void deserialize(DeserializationContext<SectionableDataGetter> context) {
        SectionableDataGetter json = context.getDataProvider();
        this.maxClaims = json.get("maxClaims").asInt();
    }

    @Override
    public void serialize(SerializationContext<SectionableDataSetter> context) {
        SectionableDataSetter json = context.getDataProvider();
        json.setInt("maxClaims", maxClaims);
    }

    @Override
    public void addEdits(MessagePlaceholderProvider builder) {
        super.addEdits(builder);
        builder.raw("term_limit_claims_amount", maxClaims);
    }


    @Override
    public Messenger canAccept(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        return null;
    }
}
