package org.kingdoms.outposts;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OutpostParticipant {
    private final Set<UUID> membersInArena = new HashSet<>();
    private final UUID requested;
    private int score;

    public OutpostParticipant(UUID requested) {
        this.requested = requested;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int score() {
        return ++this.score;
    }

    public UUID getRequested() {
        return requested;
    }

    public Set<UUID> getMembersInArena() {
        return membersInArena;
    }
}
