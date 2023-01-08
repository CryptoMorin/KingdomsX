package org.kingdoms.peacetreaties.terms.types;

import com.google.gson.JsonObject;
import org.kingdoms.adapters.AdapterKingdom;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.model.logs.lands.LogKingdomInvader;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.gui.GUIAccessor;
import org.kingdoms.gui.InteractiveGUI;
import org.kingdoms.gui.ReusableOptionHandler;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.provider.MessageBuilder;
import org.kingdoms.peacetreaties.config.PeaceTreatyGUI;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;
import org.kingdoms.utils.LocationUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class KeepLandsTerm extends Term {
    private Set<SimpleChunkLocation> keptLands;

    public static Set<SimpleChunkLocation> getInvadedLands(Kingdom invader, UUID victimKingdomId) {
        return invader.getLogs().stream()
                .filter(x -> x instanceof LogKingdomInvader)
                .map(x -> (LogKingdomInvader) x)
                .filter(x -> x.getResult().isSuccessful())
                .filter(x -> x.correspondingKingdom.equals(victimKingdomId))
                .map(x -> x.affectedLands)
                .flatMap(Collection::stream)
                .filter(invader::isClaimed)
                .collect(Collectors.toSet());
    }

    public static final TermProvider PROVIDER = new StandardTermProvider(Namespace.kingdoms("KEEP_LANDS"), KeepLandsTerm::new, true, false) {
        @Override
        public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
            CompletableFuture<Term> completableFuture = new CompletableFuture<>();
            prompt(options, editor, completableFuture, new HashSet<>(10));
            return completableFuture;
        }

        private void prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor,
                            CompletableFuture<Term> completableFuture,
                            Set<SimpleChunkLocation> added) {
            InteractiveGUI gui = GUIAccessor.prepare(editor.getPlayer(), PeaceTreatyGUI.KEEP$LANDS);

            ReusableOptionHandler option = gui.getReusableOption("land");
            Kingdom kingdom = editor.getPeaceTreaty().getProposerKingdom();
            Set<SimpleChunkLocation> invadedLands = getInvadedLands(kingdom, editor.getPeaceTreaty().getVictimKingdomId());

            for (SimpleChunkLocation invadedLand : invadedLands) {
                option.setEdits("added", added.contains(invadedLand)).onNormalClicks(() -> {
                    if (!added.remove(invadedLand)) added.add(invadedLand);
                    prompt(options, editor);
                });
            }

            gui.option("back").onNormalClicks(editor::open).done();
            gui.openInventory();
        }
    };

    private KeepLandsTerm() {}

    public KeepLandsTerm(Set<SimpleChunkLocation> keptLands) {
        this.keptLands = keptLands;
    }

    public Set<SimpleChunkLocation> getKeptLands() {
        return keptLands;
    }

    @Override
    public void deserialize(DeserializationContext context) {
        JsonObject json = context.getJson();
        this.keptLands = context.getContext().deserialize(json.get("lands"), AdapterKingdom.CHUNKS);
    }

    @Override
    public void serialize(SerializationContext context) {
        JsonObject json = context.getJson();
        json.add("lands", context.getContext().serialize(keptLands, AdapterKingdom.CHUNKS));
    }

    @Override
    public void addEdits(MessageBuilder builder) {
        super.addEdits(builder);
        builder.raw("term_keep_lands_count", keptLands.size());
        builder.parse("term_keep_lands_kept_lands", "{$s}" + keptLands.stream()
                .map(x -> KingdomsLang.LOCATIONS_CHUNK.parse(LocationUtils.getChunkEdits(x)))
                .collect(Collectors.joining("{$sep}| {$s}")));
    }

    @Override
    public TermProvider getProvider() {
        return PROVIDER;
    }
}
