package org.kingdoms.peacetreaties.terms;

import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.data.database.dataprovider.SectionableDataSetter;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.locale.provider.MessageBuilder;
import org.kingdoms.peacetreaties.data.PeaceTreaty;

public abstract class Term {
    public abstract TermProvider getProvider();

    public boolean apply(TermGroupingOptions options, PeaceTreaty peaceTreaty) {
        return true;
    }

    public Messenger canAccept(TermGroupingOptions options, PeaceTreaty peaceTreaty) {
        return null;
    }

    public void deserialize(DeserializationContext<SectionableDataGetter> context) {
    }

    public void serialize(SerializationContext<SectionableDataSetter> context) {
    }

    public void addEdits(MessageBuilder builder) {
    }

    public final MessageBuilder getEdits() {
        MessageBuilder edits = new MessageBuilder();
        addEdits(edits);
        return edits;
    }
}
