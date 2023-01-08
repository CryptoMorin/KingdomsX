package org.kingdoms.peacetreaties.terms.types;

import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermProvider;

public class MiscUpgradesTerm extends Term {
    public static final TermProvider PROVIDER =
            new StandardTermProvider(Namespace.kingdoms("MISC_UPGRADES"), MiscUpgradesTerm::new, false, true);

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }
}
