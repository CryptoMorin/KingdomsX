package org.kingdoms.services.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.skills.conditions.IEntityComparisonCondition;
import io.lumine.mythic.core.skills.SkillCondition;

/**
 * SkillCondition is an abstract class that handles many ISkillConditions that the class implements.
 */
public class RelationalMythicMobSkillCondition extends SkillCondition implements IEntityComparisonCondition {
    private final SimpleRelationalChecker checker;

    public RelationalMythicMobSkillCondition(String line, SimpleRelationalChecker checker) {
        super(line);
        this.checker = checker;
    }

    @Override
    public boolean check(AbstractEntity caster, AbstractEntity target) {
        // Caster must be a player and be in a kingdom.
        return checker.check(caster.getBukkitEntity(), target.getBukkitEntity());
    }
}
