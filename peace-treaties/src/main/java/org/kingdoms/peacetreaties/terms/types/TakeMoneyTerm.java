package org.kingdoms.peacetreaties.terms.types;

import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.data.database.dataprovider.SectionableDataSetter;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.peacetreaties.config.PeaceTreatyLang;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

import java.util.concurrent.CompletionStage;

public class TakeMoneyTerm extends Term {
    private double amount;

    public static final TermProvider PROVIDER = new StandardTermProvider(Namespace.kingdoms("TAKE_MONEY"), TakeMoneyTerm::new, true, false) {
        @Override
        public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
            return standardAmountPrompt(this, options, editor, (amount) -> new TakeMoneyTerm(amount.doubleValue()));
        }
    };

    private TakeMoneyTerm() {
    }

    public TakeMoneyTerm(double money) {
        this.amount = money;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public void deserialize(DeserializationContext<SectionableDataGetter> context) {
        SectionableDataGetter json = context.getDataProvider();
        this.amount = json.get("amount").asDouble();
    }

    @Override
    public void serialize(SerializationContext<SectionableDataSetter> context) {
        SectionableDataSetter json = context.getDataProvider();
        json.setDouble("amount", amount);
    }

    @Override
    public void addEdits(MessagePlaceholderProvider builder) {
        super.addEdits(builder);
        builder.raw("term_take_money_amount", amount);
    }

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean apply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        Kingdom kingdom = peaceTreaty.getVictimKingdom();
        if (!kingdom.getBank().has(amount)) return false;
        kingdom.getBank().add(-amount);
        return true;
    }

    @Override
    public Messenger canAccept(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        Kingdom kingdom = peaceTreaty.getVictimKingdom();
        if (!kingdom.getBank().has(amount)) return PeaceTreatyLang.TERMS_TAKE_MONEY_NOT_ENOUGH;
        return null;
    }
}
