package de.matthil.boatRace;

import java.util.List;

/**
 * BoatRace plugin configuration record.
 *
 * @param segments      lists WorldGuard regions for segments
 * @param checkpoints   lists WorldGuard regions for checkpoints
 * @param timeout       max timeout after leaving a boat before being kicked out
 * @param respawn       3d vector where the player should be teleported once kicked out
 * @param entryPoint    WorldGuard region where the player is given a boat and starts the game
 * @param leaderboard   DecentHolograms hologram name to display the leaderboard
 */
public record BoatRaceConfig(
        List<String> segments,
        List<String> checkpoints,
        double timeout,
        List<Double> respawn,
        String entryPoint,
        String leaderboard
) {
}
