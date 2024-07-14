package org.kingdoms.peacetreaties.terms.types;

import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.data.database.dataprovider.SectionableDataSetter;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

import java.util.concurrent.CompletionStage;

public class ScutageTerm extends Term {
    private double percent;

    public static final TermProvider PROVIDER = new StandardTermProvider(Namespace.kingdoms("SCUTAGE"), ScutageTerm::new, true, true) {
        @Override
        public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
            return standardAmountPrompt(this, options, editor, (amount) -> new ScutageTerm(amount.doubleValue()));
        }
    };

    private ScutageTerm() {
    }

    public ScutageTerm(double money) {
        this.percent = money;
    }

    public double getPercent() {
        return percent;
    }

    @Override
    public void deserialize(DeserializationContext<SectionableDataGetter> context) {
        SectionableDataGetter json = context.getDataProvider();
        this.percent = json.get("percent").asDouble();
    }

    @Override
    public void serialize(SerializationContext<SectionableDataSetter> context) {
        SectionableDataSetter json = context.getDataProvider();
        json.setDouble("percent", percent);
    }

    @Override
    public void addEdits(MessagePlaceholderProvider builder) {
        super.addEdits(builder);
        builder.raw("term_scutage_percent", percent);
    }

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }
}
