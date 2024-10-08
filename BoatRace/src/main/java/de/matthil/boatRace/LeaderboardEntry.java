package de.matthil.boatRace;

import java.time.Instant;

public record LeaderboardEntry(long temps, Instant quand, String course, String nom) {
}

// LeaderboardEntry entry = new LeaderboardEntry(10000, Instant.now(), "default", "matthilde0");
// System.out.println(entry.nom());