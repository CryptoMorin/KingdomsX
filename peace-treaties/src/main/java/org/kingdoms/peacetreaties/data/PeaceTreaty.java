package org.kingdoms.peacetreaties.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.abstraction.PlayerOperator;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.model.relationships.KingdomRelation;
import org.kingdoms.constants.metadata.KingdomMetadata;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.events.general.GroupRelationshipChangeEvent;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.locale.placeholders.context.PlaceholderContextBuilder;
import org.kingdoms.peacetreaties.PeaceTreatiesAddon;
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig;
import org.kingdoms.peacetreaties.config.PeaceTreatyLang;
import org.kingdoms.peacetreaties.managers.RelationshipListener;
import org.kingdoms.peacetreaties.terms.*;
import org.kingdoms.utils.MathUtils;
import org.kingdoms.utils.conditions.ConditionProcessor;
import org.kingdoms.utils.config.NodeInterpreter;
import org.kingdoms.utils.internal.functional.Fn;

import java.time.Duration;
import java.util.*;

public class PeaceTreaty implements PlayerOperator {
    private final UUID proposerKingdom, victimKingdom;
    private long started;
    private final long sentTime;
    private final Duration duration;
    private final UUID requesterPlayer;
    private final Map<String, TermGrouping> terms = new HashMap<>();

    public PeaceTreaty(UUID proposerKingdom, UUID victimKingdom, long started, long sentTime, Duration duration, UUID requesterPlayer) {
        this.proposerKingdom = Objects.requireNonNull(proposerKingdom);
        this.victimKingdom = Objects.requireNonNull(victimKingdom);
        this.started = started;
        this.sentTime = sentTime;
        this.duration = Objects.requireNonNull(duration);
        this.requesterPlayer = requesterPlayer;
    }

    public MessagePlaceholderProvider getPlaceholderContextProvider() {
        return getPlaceholderContextProvider(new MessagePlaceholderProvider());
    }

    public <T extends PlaceholderContextBuilder> T getPlaceholderContextProvider(T settings) {
        settings
                .withContext(getVictimKingdom())
                .other(getProposerKingdom())
                .raw("peacetreaty_duration", duration.toMillis())
                .raw("peacetreaty_started", started)
                .raw("peacetreaty_requested_time", sentTime)
                .raw("peacetreaty_requester_player", Bukkit.getOfflinePlayer(requesterPlayer).getName())
                .raw("peacetreaty_count_terms", terms.size())
                .raw("peacetreaty_can_enforce_acceptance", Fn.supply(this::canEnforceAcceptance))
                .raw("peacetreaty_accepted", isAccepted())
                .raw("peacetreaty_war_points", Fn.supply(this::getTotalRequiredWarPoints));

        settings.raw("peacetreaty_force_acceptance_war_points",
                MathUtils.eval(PeaceTreatyConfig.FORCE_ACCEPT_WAR_POINTS.getManager().getMathExpression(), settings));

        settings.addChild("term", x -> {
            x = x.replace('_', '-');
            if (!TermRegistry.getTermGroupings().containsKey(x)) return null;
            return terms.containsKey(x);
        });

        settings.addChild("subterm", x -> {
            Namespace ns = Namespace.fromConfigString(x);
            if (!PeaceTreatiesAddon.get().getTermRegistry().isRegisetered(ns)) return null;
            return terms.values().stream()
                    .map(y -> y.getTerms().values())
                    .flatMap(Collection::stream)
                    .anyMatch(y -> y.getProvider().getNamespace().equals(ns));
        });

        return settings;
    }

    @SuppressWarnings("unchecked")
    public void revoke() {
        Kingdom proposer = getProposerKingdom();
        Kingdom victim = getVictimKingdom();

        if (victim != null) {
            KingdomMetadata victimMeta = victim.getMetadata().get(PeaceTreatyReceiverMetaHandler.INSTANCE);
            if (victimMeta != null) ((Map<UUID, PeaceTreaty>) victimMeta.getValue()).remove(proposerKingdom);
        }

        if (proposer != null) {
            KingdomMetadata proposerMeta = proposer.getMetadata().get(PeaceTreatyProposerMetaHandler.INSTANCE);
            if (proposerMeta != null) ((Set<UUID>) proposerMeta.getValue()).remove(victimKingdom);
        }
    }

    public boolean canEnforceAcceptance() {
        return ConditionProcessor.process(
                PeaceTreatyConfig.FORCE_ACCEPT_CONDITION.getManager().get(NodeInterpreter.CONDITION),
                new MessagePlaceholderProvider()
                        .withContext(getVictimKingdom())
                        .other(getProposerKingdom())
        );
    }

    public void propose() {
        Kingdom proposer = getProposerKingdom();
        Kingdom victim = getVictimKingdom();

        {
            Map<UUID, PeaceTreaty> victimMeta = PeaceTreaties.initializeMeta(victim, PeaceTreatyReceiverMetaHandler.INSTANCE,
                    () -> new PeaceTreatyReceiverMeta(new HashMap<>()));
            victimMeta.put(proposer.getId(), this);
        }
        {
            Set<UUID> proposerMeta = PeaceTreaties.initializeMeta(proposer, PeaceTreatyProposerMetaHandler.INSTANCE,
                    () -> new PeaceTreatyProposedMeta(new HashSet<>()));
            proposerMeta.add(victim.getId());
        }

        MessagePlaceholderProvider settings = getPlaceholderContextProvider(new MessagePlaceholderProvider());
        for (Player online : proposer.getOnlineMembers()) {
            PeaceTreatyLang.NOTIFICATION_SENDERS.sendMessage(online, settings);
        }

        for (Player online : victim.getOnlineMembers()) {
            PeaceTreatyLang.NOTIFICATION_RECEIVERS.sendMessage(online, settings);
        }

        proposer.log(new LogPeaceTreatySent(this));
        victim.log(new LogPeaceTreatyReceived(this));
    }

    @SuppressWarnings("unchecked")
    public void removeContract() {
        Kingdom proposer = getProposerKingdom();
        Kingdom victim = getVictimKingdom();

        // Initialize if not set
        PeaceTreaties.getReceivedPeaceTreaties(victim);
        PeaceTreaties.getProposedPeaceTreaties(proposer);

        KingdomMetadata victimMeta = victim.getMetadata().get(PeaceTreatyReceiverMetaHandler.INSTANCE);
        ((Map<UUID, PeaceTreaty>) victimMeta.getValue()).remove(proposer.getId());

        KingdomMetadata proposerMeta = proposer.getMetadata().get(PeaceTreatyProposerMetaHandler.INSTANCE);
        ((Set<UUID>) proposerMeta.getValue()).remove(victim.getId());
    }

    public boolean isAccepted() {
        return started != 0;
    }

    public long getSentTime() {
        return sentTime;
    }

    public GroupRelationshipChangeEvent accept() {
        Kingdom victim = getVictimKingdom();
        Kingdom proposer = getProposerKingdom();

        GroupRelationshipChangeEvent event = new GroupRelationshipChangeEvent(KingdomPlayer.getKingdomPlayer(requesterPlayer), victim, proposer, KingdomRelation.NEUTRAL);
        event.getMetadata().put(RelationshipListener.RELATION_CHANGE_NS, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return event;

        victim.setRelationShipWith(proposer, event.getNewRelation());
        this.started = System.currentTimeMillis();
        for (TermGrouping termGrouping : terms.values()) {
            termGrouping.apply(this);
        }

        return event;
    }

    public Term getSubTerm(TermProvider provider) {
        for (TermGrouping grouping : this.terms.values()) {
            for (Term term : grouping.getTerms().values()) {
                if (term.getProvider() == provider) return term;
            }
        }

        return null;
    }

    public double getTotalRequiredWarPoints() {
        double warPoints = 0;

        for (TermGrouping grouping : terms.values()) {
            warPoints += grouping.getOptions().getRequiredWarPoints(this);
        }

        return warPoints;
    }

    @Override
    public KingdomPlayer getPlayer() {
        return KingdomPlayer.getKingdomPlayer(requesterPlayer);
    }

    public UUID getRequesterPlayerID() {
        return requesterPlayer;
    }

    public long getStarted() {
        return started;
    }

    public Kingdom getVictimKingdom() {
        return Kingdom.getKingdom(victimKingdom);
    }

    public Kingdom getProposerKingdom() {
        return Kingdom.getKingdom(proposerKingdom);
    }

    @NonNull
    public UUID getVictimKingdomId() {
        return victimKingdom;
    }

    @NonNull
    public UUID getProposerKingdomId() {
        return proposerKingdom;
    }

    @NonNull
    public Map<String, TermGrouping> getTerms() {
        return terms;
    }

    public void addOrCreateTerm(TermGroupingOptions options, Term term) {
        terms.compute(options.getName(), (k, v) -> {
            if (v == null) v = new TermGrouping(options, new HashMap<>());
            v.getTerms().put(term.getProvider().getNamespace(), term);
            return v;
        });
    }

    public Term getOrCreateTerm(TermGroupingOptions grouping, TermProvider provider) {
        return getTerm(grouping, provider).orElseGet(provider::construct);
    }

    public Optional<Term> getTerm(TermGroupingOptions grouping, TermProvider provider) {
        TermGrouping constructedTerms = terms.get(grouping.getName());
        if (constructedTerms == null) return Optional.empty();
        return Optional.ofNullable(constructedTerms.getTerms().get(provider.getNamespace()));
    }

    public Duration getDuration() {
        return duration;
    }
}
