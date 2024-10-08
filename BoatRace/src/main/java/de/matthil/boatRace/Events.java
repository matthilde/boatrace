package de.matthil.boatRace;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.matthil.utilitiesLib.WorldGuardRegionEvent;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;

/**
 * Global event handler of boat races.
 * It is planned to expand it to handle multiple race tracks.
 */
public class Events extends WorldGuardRegionEvent implements Listener {
    private RaceController games;

    public RaceController games() { return this.games; }

    /**
     * Schedule routine that updates the game state.
     */
    public void scheduleFunction() {
        games.update();
    }

    /**
     * @param games Game object controller
     */
    public Events(RaceController games) {
        super();
        this.games = games;

        try {
            games.updateHolograms();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Changes the current game controller object. Used mostly for reloading.
     * @param games game controller to replace
     */
    public void setGame(RaceController games) {
        games.kickAll();
        this.games = games;
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        BoatRaceGame game = games.getGameByPlayer(event.getPlayer());
        if (game == null) return;

        PlayerState state = game.getPlayer(event.getPlayer());
        if (state == null) return;

        state.removeBoat();
        game.removePlayer(state.player());
    }


    @Override
    public void onRegionEnter(Player player, ProtectedRegion region) {
        String regionName = region.getId().trim();
        BoatRaceGame game = games.getGameByPlayer(player);

        // Dans le cas ou le joueur n'a pas commence de game
        if (game == null) {
            game = games.getGameByRegion(regionName);
            if (game == null) return;

            String start = game.config().segments().getFirst().toLowerCase();

            // Commencer une nouvelle partie
            if (regionName.equals(start) && player.getVehicle() instanceof Boat) {
                game.startRace(player);
            }
        }

        // Dans le cas ou le joueur est en train de jouer
        else {
            int segment = game.config().segments().indexOf(regionName);
            if (segment >= 0) {
                game.passSegment(player, segment);
            }

            int checkpoint = game.config().checkpoints().indexOf(regionName);
            if (checkpoint >= 0) {
                game.passCheckpoint(player, checkpoint);
            }

            game.finishRaceHook(player);
        }
    }
}
