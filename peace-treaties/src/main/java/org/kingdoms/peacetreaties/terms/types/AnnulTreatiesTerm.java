package org.kingdoms.peacetreaties.terms.types;

import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.model.relationships.KingdomRelation;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;

import java.util.Map;
import java.util.UUID;

public class AnnulTreatiesTerm extends Term {
    public static final TermProvider PROVIDER =
            new StandardTermProvider(Namespace.kingdoms("ANNUL_TREATIES"), AnnulTreatiesTerm::new, false, true);

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean apply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        if (canAccept(config, peaceTreaty) != null) return false;

        Kingdom kingdom = peaceTreaty.getVictimKingdom();
        kingdom.getRelations().entrySet().stream()
                .filter(x -> x.getValue() == KingdomRelation.ALLY || x.getValue() == KingdomRelation.TRUCE)
                .forEach(x -> {
                    Kingdom other = Kingdom.getKingdom(x.getKey());
                    if (other == null) return;
                    kingdom.setRelationShipWith(kingdom, null);
                });

        Kingdom proposer = peaceTreaty.getProposerKingdom();
        for (Map.Entry<UUID, KingdomRelation> relation : proposer.getRelations().entrySet()) {
            kingdom.getRelations().put(relation.getKey(), relation.getValue());
        }

        return true;
    }

    @Override
    public Messenger canAccept(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        return null;
    }
}
