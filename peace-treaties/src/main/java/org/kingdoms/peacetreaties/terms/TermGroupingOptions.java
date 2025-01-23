package org.kingdoms.peacetreaties.terms;

import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.Pair;
import org.kingdoms.locale.LanguageEntry;
import org.kingdoms.peacetreaties.data.PeaceTreaty;
import org.kingdoms.utils.MathUtils;
import org.kingdoms.utils.compilers.expressions.ConditionalExpression;
import org.kingdoms.utils.compilers.expressions.MathExpression;
import org.kingdoms.utils.config.ConfigSection;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class TermGroupingOptions {
    private final ConfigSection config;
    private final Collection<Pair<ConditionalExpression, LanguageEntry>> conditions;
    private final MathExpression requiredWarPoints;
    private final Map<Namespace, TermProvider> terms;
    private final String name;

    public TermGroupingOptions(String name, ConfigSection config,
                               Collection<Pair<ConditionalExpression, LanguageEntry>> condition,
                               MathExpression requiredWarPoints,
                               Map<Namespace, TermProvider> terms) {
        this.name = Objects.requireNonNull(name);
        this.config = config;
        this.conditions = Objects.requireNonNull(condition);
        this.requiredWarPoints = Objects.requireNonNull(requiredWarPoints);
        this.terms = Objects.requireNonNull(terms);
    }

    public double getRequiredWarPoints(PeaceTreaty peaceTreaty) {
        return MathUtils.eval(requiredWarPoints, peaceTreaty.getPlaceholderContextProvider());
    }

    public Map<Namespace, TermProvider> getTerms() {
        return Collections.unmodifiableMap(terms);
    }

    public ConfigSection getConfigOf(TermProvider term) {
        String option = term.getNamespace().getConfigOptionName();
        return config.getSection("term-options", option);
    }

    public ConfigSection getConfig() {
        return config;
    }

    public Collection<Pair<ConditionalExpression, LanguageEntry>> getConditions() {
        return conditions;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TermGroupingOptions{" +
                "name='" + name + '\'' +
                ", terms=" + terms.keySet() +
                '}';
    }
}
