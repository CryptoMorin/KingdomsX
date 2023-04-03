package org.kingdoms.peacetreaties.terms.types;

import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.Nation;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.events.general.GroupDisband;
import org.kingdoms.events.members.LeaveReason;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.peacetreaties.config.PeaceTreatyLang;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

public class LeaveDisbandNationTerm extends Term {
    public static final TermProvider PROVIDER =
            new StandardTermProvider(Namespace.kingdoms("LEAVE_NATION"), LeaveDisbandNationTerm::new, false, true) {
                @Override
                public Messenger canApply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
                    Kingdom kingdom = peaceTreaty.getVictimKingdom();
                    Nation nation = kingdom.getNation();
                    if (nation == null) return KingdomsLang.NO_NATION_OTHER;
                    if (nation.isPermanent() && nation.getCapitalId().equals(kingdom.getId()))
                        return PeaceTreatyLang.TERMS_LEAVE_NATION_PERMANENT;
                    return null;
                }
            };

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean apply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        if (canAccept(config, peaceTreaty) != null) return false;

        Kingdom kingdom = peaceTreaty.getVictimKingdom();
        Nation nation = kingdom.getNation();

        if (nation.getCapitalId().equals(kingdom.getId())) {
            return !nation.disband(GroupDisband.Reason.CUSTOM).isCancelled();
        } else {
            return !kingdom.leaveNation(LeaveReason.CUSTOM).isCancelled();
        }
    }
}
