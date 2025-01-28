package org.kingdoms.outposts;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OutpostParticipant {
    private final Set<UUID> membersInArena = new HashSet<>();
    private final UUID requested;
    private double score;

    public OutpostParticipant(UUID requested) {
        this.requested = requested;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double score() {
        return ++this.score;
    }

    public double score(double plus) {
        this.score += plus;
        return this.score;
    }

    public UUID getRequested() {
        return requested;
    }

    public Set<UUID> getMembersInArena() {
        return membersInArena;
    }
}
