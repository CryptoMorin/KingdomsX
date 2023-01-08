package org.kingdoms.peacetreaties.terms;

import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.peacetreaties.data.PeaceTreaty;

import java.util.Map;

public class TermGrouping {
    private final TermGroupingOptions options;
    private final Map<Namespace, Term> terms;

    public TermGrouping(TermGroupingOptions options, Map<Namespace, Term> terms) {
        this.options = options;
        this.terms = terms;
    }

    public Map<Namespace, Term> getTerms() {
        return terms;
    }

    public TermGroupingOptions getOptions() {
        return options;
    }

    public boolean apply(PeaceTreaty peaceTreaty) {
        for (Term term : terms.values()) {
            if (!term.apply(options, peaceTreaty)) return false;
        }

        return true;
    }
}
