package org.kingdoms.peacetreaties.config;

import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.peacetreaties.PeaceTreatiesAddon;
import org.kingdoms.peacetreaties.terms.TermProvider;
import org.snakeyaml.nodes.ScalarNode;
import org.snakeyaml.nodes.Tag;
import org.snakeyaml.validation.NodeValidator;
import org.snakeyaml.validation.ValidationContext;
import org.snakeyaml.validation.ValidationFailure;

public final class CustomConfigValidators {
    private static final Tag TERM = org.kingdoms.utils.config.CustomConfigValidators.register("Term", new Term());

    public static void init() {
    }

    private static final class Term implements NodeValidator {
        @Override
        public ValidationFailure validate(ValidationContext context) {
            Tag tag = context.getNode().getTag();
            if (tag != Tag.STR) return context.err("Expected a term name, instead got " + tag);
            ScalarNode scalarNode = (ScalarNode) context.getNode();

            Namespace ns = Namespace.fromConfigString(scalarNode.getValue());
            TermProvider term = PeaceTreatiesAddon.get().getTermRegistry().getRegistered(ns);
            if (term == null) return context.err("Unknown term: " + scalarNode.getValue() + " (" + ns.asString() + ')');

            return null;
        }
    }
}
