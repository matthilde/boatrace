package de.matthil.boatRace;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Manages multiple races
 */
public class RaceController {
    private HashMap<String, BoatRaceGame> games;

    public RaceController() {
        games = new HashMap<>();
    }

    /**
     * Adds a game into the controller.
     *
     * @param name Map name
     * @param game BoatRace game object
     */
    public void addGame(String name, BoatRaceGame game) {
        if (!games.containsKey(name)) {
            games.put(name, game);
        }
    }

    /**
     * Kicks everyone.
     */
    public void kickAll() {
        for (BoatRaceGame game : games.values()) {
            game.removeAllPlayers();
        }
    }

    /**
     * Updates all games.
     */
    public void update() {
        for (BoatRaceGame game : games.values()) {
            game.update();
        }
    }

    /**
     * Updates all holograms.
     */
    public void updateHolograms() throws SQLException {
        for (BoatRaceGame game : games.values()) {
            game.updateLeaderboardHologram();
        }
    }

    /**
     * Gets a game by its worldguard region.
     *
     * @param regionName WorldGuard Region name
     * @return game object if race is found, null if not.
     */
    @Nullable
    public BoatRaceGame getGameByRegion(String regionName) {
        for (BoatRaceGame game : games.values()) {
            BoatRaceConfig config = game.config();

            if (config.checkpoints().contains(regionName) || config.segments().contains(regionName)) {
                return game;
            }
        }
        return null;
    }

    /**
     * Gets a game by its name.
     *
     * @return BoatRace game object if found, null if not found.
     */
    @Nullable
    public BoatRaceGame getGame(String name) {
        return games.get(name);
    }

    /**
     * Get a game by its player.
     *
     * @return game object if race is found, null if not.
     */
    @Nullable
    public BoatRaceGame getGameByPlayer(Player player) {
        for (BoatRaceGame game : games.values()) {
            if (game.isPlaying(player)) {
                return game;
            }
        }
        return null;
    }
}
