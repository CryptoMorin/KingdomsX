package org.kingdoms.peacetreaties.terms;

import org.kingdoms.constants.namespace.Namespaced;
import org.kingdoms.locale.messenger.LanguageEntryMessenger;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.utils.string.Strings;

import java.util.concurrent.CompletionStage;

/**
 * A {@link Term} provider which provides an instance of it usually used to serialize data
 * into the class.
 */
public interface TermProvider extends Namespaced {
    /**
     * A new instance of the term must be provided.
     * This is usually implemented by simply calling a nullary constructor of the class.
     */
    Term construct();

    boolean requiresData(TermGroupingOptions options);

    boolean isLongTerm();

    CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor);

    default Messenger canApply(TermGroupingOptions options, PeaceTreaty peaceTreaty) {
        return null;
    }

    default Messenger getMessage() {
        return new LanguageEntryMessenger("peace-treaties", "terms", Strings.configOption(getNamespace().getKey()), "message");
    }
}