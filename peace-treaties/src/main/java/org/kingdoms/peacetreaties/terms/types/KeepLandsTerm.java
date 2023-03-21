package org.kingdoms.peacetreaties.terms.types;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.model.logs.lands.LogKingdomInvader;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.Pair;
import org.kingdoms.data.database.dataprovider.DataSetter;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.data.database.dataprovider.SectionableDataSetter;
import org.kingdoms.events.lands.ClaimLandEvent;
import org.kingdoms.events.lands.UnclaimLandEvent;
import org.kingdoms.gui.GUIAccessor;
import org.kingdoms.gui.GUIPagination;
import org.kingdoms.gui.InteractiveGUI;
import org.kingdoms.gui.ReusableOptionHandler;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.provider.MessageBuilder;
import org.kingdoms.peacetreaties.config.PeaceTreatyGUI;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor;
import org.kingdoms.peacetreaties.terms.StandardTermProvider;
import org.kingdoms.peacetreaties.terms.Term;
import org.kingdoms.peacetreaties.terms.TermGroupingOptions;
import org.kingdoms.peacetreaties.terms.TermProvider;
import org.kingdoms.utils.LocationUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class KeepLandsTerm extends Term {
    @NonNull
    private Set<SimpleChunkLocation> keptLands = new HashSet<>();
    private transient Set<LogKingdomInvader> groupedKeptLands = new HashSet<>();

    public static List<LogKingdomInvader> getInvadedLandsSimple(Kingdom invader, UUID victimKingdomId) {
        return invader.getLogs().stream()
                .filter(x -> x instanceof LogKingdomInvader)
                .map(x -> (LogKingdomInvader) x)
                .filter(x -> x.getResult().isSuccessful())
                .filter(x -> x.correspondingKingdom.equals(victimKingdomId))
                .collect(Collectors.toList());
    }

    public static Map<SimpleChunkLocation, LogKingdomInvader> getInvadedLands(Kingdom invader, UUID victimKingdomId) {
        return getInvadedLandsSimple(invader, victimKingdomId).stream()
                .flatMap(x -> x.affectedLands.stream().map(y -> Pair.of(y, x)))
                .filter(x -> invader.isClaimed(x.getKey()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (prev, next) -> next, HashMap::new));
    }

    public Set<SimpleChunkLocation> getLandsGivingBack(Kingdom invader, UUID victimKingdomId) {
        return invader.getLogs().stream()
                .filter(x -> x instanceof LogKingdomInvader)
                .map(x -> (LogKingdomInvader) x)
                .filter(x -> x.getResult().isSuccessful())
                .filter(x -> x.correspondingKingdom.equals(victimKingdomId))
                .flatMap(x -> x.affectedLands.stream())
                .filter(invader::isClaimed)
                .filter(x -> !keptLands.contains(x))
                .collect(Collectors.toSet());
    }

    public static final TermProvider PROVIDER = new StandardTermProvider(Namespace.kingdoms("KEEP_LANDS"), KeepLandsTerm::new, true, false) {
        @Override
        public CompletionStage<Term> prompt(TermGroupingOptions options, StandardPeaceTreatyEditor editor) {
            CompletableFuture<Term> completableFuture = new CompletableFuture<>();
            Optional<Term> term = editor.getPeaceTreaty().getTerm(options, this);
            Set<SimpleChunkLocation> added = term
                    .map(value -> ((KeepLandsTerm) value).keptLands)
                    .orElseGet(() -> new HashSet<>(10));
            Set<LogKingdomInvader> addedGrouped = term
                    .map(value -> ((KeepLandsTerm) value).groupedKeptLands)
                    .orElseGet(() -> new HashSet<>(10));

            prompt(editor, completableFuture, added, addedGrouped, true, 0);
            return completableFuture;
        }

        private void prompt(StandardPeaceTreatyEditor editor,
                            CompletableFuture<Term> completableFuture,
                            Set<SimpleChunkLocation> added, Set<LogKingdomInvader> addedLogs, boolean displayModeGrouped, int page) {
            InteractiveGUI gui = GUIAccessor.prepare(editor.getPlayer(), PeaceTreatyGUI.KEEP$LANDS);
            gui.getSettings().raw("displaymode_grouped", displayModeGrouped);

            Kingdom kingdom = editor.getPeaceTreaty().getProposerKingdom();

            if (displayModeGrouped) {
                List<LogKingdomInvader> invadedLands = getInvadedLandsSimple(kingdom, editor.getPeaceTreaty().getVictimKingdomId());

                Pair<ReusableOptionHandler, Collection<LogKingdomInvader>> pagination =
                        GUIPagination.paginate(gui, invadedLands, "land", page,
                                (newPage) -> prompt(editor, completableFuture, added, addedLogs, displayModeGrouped, newPage));

                ReusableOptionHandler option = pagination.getKey();
                for (LogKingdomInvader log : pagination.getValue()) {
                    option.setEdits("added", addedLogs.contains(log)).onNormalClicks(() -> {
                        if (!addedLogs.remove(log)) addedLogs.add(log);
                        prompt(editor, completableFuture, added, addedLogs, displayModeGrouped, page);
                    });
                    log.addEdits(option.getSettings());
                    option.done();
                }
            } else {
                List<Pair<SimpleChunkLocation, LogKingdomInvader>> invadedLands =
                        getInvadedLands(kingdom, editor.getPeaceTreaty().getVictimKingdomId())
                                .entrySet().stream()
                                .map(x -> Pair.of(x.getKey(), x.getValue()))
                                .collect(Collectors.toList());
                Pair<ReusableOptionHandler, Collection<Pair<SimpleChunkLocation, LogKingdomInvader>>> pagination =
                        GUIPagination.paginate(gui, invadedLands, "land", page,
                                (newPage) -> prompt(editor, completableFuture, added, addedLogs, displayModeGrouped, newPage));

                ReusableOptionHandler option = pagination.getKey();
                for (Pair<SimpleChunkLocation, LogKingdomInvader> pair : pagination.getValue()) {
                    SimpleChunkLocation invadedLand = pair.getKey();
                    option.setEdits("added", added.contains(invadedLand)).onNormalClicks(() -> {
                        if (!added.remove(invadedLand)) added.add(invadedLand);
                        prompt(editor, completableFuture, added, addedLogs, displayModeGrouped, page);
                    });
                    pair.getValue().addEdits(option.getSettings());
                    LocationUtils.getChunkEdits(option.getSettings(), invadedLand, "");
                    option.done();
                }
            }

            gui.option("display-mode").onNormalClicks((ctx) ->
                    prompt(editor, completableFuture, added, addedLogs, !displayModeGrouped, page)
            ).done();

            gui.option("cancel").onNormalClicks(editor::open).done();
            gui.option("done").onNormalClicks(() -> {
                KeepLandsTerm term = new KeepLandsTerm(displayModeGrouped ?
                        addedLogs.stream().flatMap(x -> x.affectedLands.stream()).collect(Collectors.toSet())
                        : added
                );
                term.groupedKeptLands = addedLogs;
                completableFuture.complete(term);
            }).done();
            gui.onClose(() -> {
                if (gui.wasSwitched()) return;
                editor.pause();
            });
            gui.open();
        }
    };

    private KeepLandsTerm() {}

    public KeepLandsTerm(@NonNull Set<SimpleChunkLocation> keptLands) {
        this.keptLands = Objects.requireNonNull(keptLands);
    }

    @NonNull
    public Set<SimpleChunkLocation> getKeptLands() {
        return Objects.requireNonNull(keptLands, "Not initialized yet");
    }

    @Override
    public void deserialize(DeserializationContext<SectionableDataGetter> context) {
        SectionableDataGetter json = context.getDataProvider();
        this.keptLands = json.asCollection(new HashSet<>(), (c, e) -> c.add(e.asSimpleChunkLocation()));
    }

    @Override
    public void serialize(SerializationContext<SectionableDataSetter> context) {
        SectionableDataSetter json = context.getDataProvider();
        json.setCollection(keptLands, DataSetter::setSimpleChunkLocation);
    }

    @Override
    public boolean apply(TermGroupingOptions options, PeaceTreaty peaceTreaty) {
        if (!super.apply(options, peaceTreaty)) return false;

        Kingdom invader = peaceTreaty.getProposerKingdom();
        Kingdom victim = peaceTreaty.getVictimKingdom();

        Set<SimpleChunkLocation> givingBackChunks = getLandsGivingBack(invader, victim.getId());
        if (givingBackChunks.isEmpty()) return true;

        invader.unclaim(givingBackChunks, peaceTreaty.getPlayer(), UnclaimLandEvent.Reason.CUSTOM, false);
        victim.claim(givingBackChunks, null, ClaimLandEvent.Reason.CUSTOM, false);

        return true;
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
