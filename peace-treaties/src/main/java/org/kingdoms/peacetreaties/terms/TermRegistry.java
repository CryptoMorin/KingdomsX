package org.kingdoms.peacetreaties.terms;

import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.constants.namespace.NamespacedRegistry;
import org.kingdoms.constants.namespace.UnregistrableNamespaceRegistry;
import org.kingdoms.data.Pair;
import org.kingdoms.locale.LanguageEntry;
import org.kingdoms.main.KLogger;
import org.kingdoms.peacetreaties.PeaceTreatiesAddon;
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig;
import org.kingdoms.utils.compilers.ConditionalCompiler;
import org.kingdoms.utils.compilers.expressions.ConditionalExpression;
import org.kingdoms.utils.compilers.expressions.MathExpression;
import org.kingdoms.utils.config.ConfigSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class TermRegistry extends NamespacedRegistry<TermProvider> implements UnregistrableNamespaceRegistry<TermProvider> {
    private static final Map<String, TermGroupingOptions> TERM_GROUPINGS = new HashMap<>();

    @Override
    public TermProvider unregister(Namespace namespace) {
        return super.registry.remove(namespace);
    }

    public static Map<String, TermGroupingOptions> getTermGroupings() {
        return TERM_GROUPINGS;
    }

    public static void loadTermGroupings() {
        PeaceTreatiesAddon.get().getLogger().info("Loading term grouping settings...");
        Map<Namespace, TermProvider> registry = PeaceTreatiesAddon.get().getTermRegistry().registry;
        ConfigSection terms = PeaceTreatyConfig.getConfig().getConfig().getSection("terms");

        for (Map.Entry<String, ConfigSection> term : terms.getSections().entrySet()) {
            ConfigSection section = term.getValue();
            MathExpression warPoints = section.getMathExpression("war-points");

            ConfigSection conditionsSection = section.getSection("conditions");
            Collection<Pair<ConditionalExpression, LanguageEntry>> conditions = new ArrayList<>();

            if (conditionsSection != null) {
                for (String condition : conditionsSection.getKeys()) {
                    String messagePath = conditionsSection.getString(condition);
                    conditions.add(Pair.of(ConditionalCompiler.compile(condition).evaluate(), LanguageEntry.fromString(messagePath)));
                }
            }

            Map<Namespace, TermProvider> termsList = new HashMap<>();
            for (String termName : section.getStringList("terms")) {
                Namespace ns = Namespace.fromConfigString(termName);
                TermProvider realTerm = registry.get(ns);
                if (realTerm != null) termsList.put(ns, realTerm);
                else KLogger.error("Unknown peace treaty term: " + ns + " <- " + termName);
            }

            TermGroupingOptions grouping = new TermGroupingOptions(term.getKey(), section, conditions, warPoints, termsList);
            TERM_GROUPINGS.put(term.getKey(), grouping);
        }
    }
}
