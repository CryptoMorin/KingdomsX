package org.kingdoms.services.mythicmobs.conditions;

public final class MythicMobConditionRegistry {
    public static void register(String name, SimpleRelationalChecker checker) {
        String id = "kingdoms_" + name;
        KingdomsMythicMobConditionListener.CONDITIONS.put(id, new RelationalMythicMobSkillCondition(id, checker));
    }
}
