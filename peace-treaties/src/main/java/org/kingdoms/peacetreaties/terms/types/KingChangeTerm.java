package org.kingdoms.peacetreaties.terms.types;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.data.database.dataprovider.SectionableDataSetter;
import org.kingdoms.events.general.KingdomKingChangeEvent;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.messenger.Messenger;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.locale.placeholders.target.PlaceholderTarget;
import org.kingdoms.managers.chat.ChatInputManager;
import org.kingdoms.peacetreaties.config.PeaceTreatyLang;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;
import org.kingdoms.utils.PlayerUtils;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class KingChangeTerm extends Term {
    private UUID newKing;

    public static final TermProvider PROVIDER = new StandardTermProvider(
            Namespace.kingdoms("LEADER_CHANGE"),
            KingChangeTerm::new, true, false
    ) {
        @Override
        public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
            Player viewer = editor.getPlayer();
            return ChatInputManager.awaitInput(viewer, input -> {
                OfflinePlayer player = PlayerUtils.getOfflinePlayer(input);
                if (player == null) {
                    KingdomsLang.NOT_FOUND_PLAYER.sendError(viewer);
                    return null;
                }

                UUID newKing = player.getUniqueId();
                PeaceTreaty peaceTreaty = editor.getPeaceTreaty();
                if (!peaceTreaty.getVictimKingdom().isMember(newKing) &&
                        !peaceTreaty.getProposerKingdom().isMember(newKing)) {
                    PeaceTreatyLang.TERMS_KING_CHANGE_NOT_APPLICABLE.sendError(viewer);
                    return null;
                }

                return new KingChangeTerm(player.getUniqueId());
            });
        }
    };

    private KingChangeTerm() {
    }

    public KingChangeTerm(UUID newKing) {
        this.newKing = newKing;
    }

    public UUID getNewKing() {
        return newKing;
    }

    public OfflinePlayer getNewKingPlayer() {
        return Bukkit.getOfflinePlayer(newKing);
    }

    @Override
    public void deserialize(DeserializationContext<SectionableDataGetter> context) {
        SectionableDataGetter json = context.getDataProvider();
        this.newKing = json.get("amount").asUUID();
    }

    @Override
    public void serialize(SerializationContext<SectionableDataSetter> context) {
        SectionableDataSetter json = context.getDataProvider();
        json.setUUID("amount", newKing);
    }

    @Override
    public void addEdits(MessagePlaceholderProvider builder) {
        super.addEdits(builder);
        builder.other(PlaceholderTarget.of(getNewKingPlayer()));
    }

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean apply(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        return !peaceTreaty.getVictimKingdom()
                .setKing(KingdomPlayer.getKingdomPlayer(newKing), KingdomKingChangeEvent.Reason.OTHER).isCancelled();
    }

    @Override
    public Messenger canAccept(TermGroupingOptions config, PeaceTreaty peaceTreaty) {
        if (!peaceTreaty.getVictimKingdom().isMember(newKing) &&
                !peaceTreaty.getProposerKingdom().isMember(newKing)) {
            return PeaceTreatyLang.TERMS_KING_CHANGE_NOT_APPLICABLE;
        }
        return null;
    }
}
