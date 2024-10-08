package de.matthil.boatRace;

import de.matthil.utilitiesLib.Functions;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * This class covers all the functionalities and internal mechanics of the game
 */
public class BoatRaceGame {
    final private HashMap<UUID, PlayerState> playing;
    final private BoatRaceConfig config;
    final private Leaderboard leaderboard;

    /**
     * constructor
     *
     * @param config configuration of the game
     */
    public BoatRaceGame(BoatRaceConfig config, Leaderboard leaderboard) {
        playing = new HashMap<>();
        this.config = config;
        this.leaderboard = leaderboard;
    }

    /**
     * Adds a player in the race. Does nothing if the player exists.
     *
     * @param player player
     * @return state object after adding the player
     */
    public PlayerState addPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerState state = playing.get(uuid);
        if (state == null) {
            state = new PlayerState(
                    player, Instant.now(),
                    Bukkit.createBossBar("", BarColor.PINK, BarStyle.SOLID),
                    config.segments().size(), config.checkpoints().size());

            state.chrono().addPlayer(player);
            state.chrono().setVisible(true);
            playing.put(uuid, state);
        }

        return state;
    }

    /**
     * Removes a player from the race. Does nothing if the player does not exist
     *
     * @param player player
     */
    public void removePlayer(Player player) {
        if (playing.containsKey(player.getUniqueId())) {
            PlayerState state = playing.get(player.getUniqueId());
            state.chrono().removePlayer(player);
            state.chrono().setVisible(false);

            playing.remove(player.getUniqueId());
        }
    }

    public void resetPlayer(Player player) {
        PlayerState state = getPlayer(player);
        if (state != null) {
            removePlayer(player);
            state = addPlayer(player);

            Entity vehicle = player.getVehicle();
            if (vehicle instanceof Boat) {
                state.setBoat((Boat) vehicle);
            }
        }
    }

    /**
     * Kick all players from the race.
     */
    public void removeAllPlayers() {
        for (Map.Entry<UUID, PlayerState> entry : playing.entrySet()) {
            PlayerState state = entry.getValue();
            state.removeBoat();
            disqualify(state.player(), "kick all");

            playing.remove(entry.getKey());
        }
    }

    /**
     * Get the PlayerState object from a given player.
     *
     * @param player player
     * @return player state, null if the player does not exist
     */
    @Nullable
    public PlayerState getPlayer(Player player) {
        return playing.get(player.getUniqueId());
    }

    /**
     * Updates boss bars to give the proper time.
     */
    private void updateBars() {
        if (playing.isEmpty()) { return; }

        Instant now = Instant.now();
        for (PlayerState state : playing.values()) {
            BossBar bar = state.chrono();
            long seconds = (now.toEpochMilli() - state.start().toEpochMilli());
            bar.setTitle(Functions.formatTime(seconds));
        }
    }

    /**
     * Verify that everyone is riding their boat. Warns players who leaves it.
     */
    private void checkRiders() {
        for (PlayerState state : playing.values()) {
            Player player = state.player();
            if (!(player.getVehicle() instanceof Boat) && state.getBoat() != null) {
                if (state.getLastLeaveAttempt() > 1000) {
                    player.sendMessage("Requitte le bateau pour abandonner la course!");
                    state.updateLastLeaveAttempt();
                    state.respawnBoat();
                } else {
                    disqualify(player, "abandon");
                }
            }
        }
    }

    /**
     * Update routine (must be every 2 ticks)
     */
    public void update() {
        checkRiders();
        updateBars();
    }

    /**
     * Checks if a player is playing.
     *
     * @param player player
     * @return true if the player exists within the race, false otherwise
     */
    public boolean isPlaying(Player player) {
        return playing.containsKey(player.getUniqueId());
    }

    /**
     * Teleports back the player at the starting point.
     */
    public void teleportBack(Entity entity) {
        List<Double> teleportPos = config().respawn();
        Location playerLoc = entity.getLocation();
        playerLoc.setX(teleportPos.get(0));
        playerLoc.setY(teleportPos.get(1));
        playerLoc.setZ(teleportPos.get(2));

        entity.teleport(playerLoc);
    }

    /**
     * Disqualify a player with a given reason.
     *
     * @param player player to disqualify
     * @param cause reason why they're disqualified
     */
    public void disqualify(Player player, String cause) {
        if (!isPlaying(player)) {
            return;
        }

        PlayerState state = getPlayer(player);
        if (state != null) {
            player.sendMessage(ChatColor.RED + "Tu as ete disqualifie " + ChatColor.RESET + "car " + cause);

            state.removeBoat();
            teleportBack(player);
            removePlayer(player);
        }
    }

    /**
     * Restarts race for player.
     */
    public void restart(Player player) {
        PlayerState state = getPlayer(player);
        if (state != null) {
            state.removeBoat();
            removePlayer(player);

            teleportBack(player);
            state.respawnBoat();
        }
    }

    /**
     * Starts a race for player
     */
    public void startRace(Player player) {
        player.sendMessage(ChatColor.YELLOW + "C'est parti!");
        addPlayer(player);
        getPlayer(player).setBoat((Boat)player.getVehicle());
    }

    /**
     * Passes a segment.
     *
     * @param segment segment ID
     */
    public void passSegment(Player player, int segment) {
        PlayerState state = getPlayer(player);
        if (state == null) return;

        Long temps = state.passSegment(segment);
        if (temps == null) {
            disqualify(player, "tu as saute un segment");
        } else if (temps == 0) {
            if (segment == 0) {
                removePlayer(player);
                addPlayer(player);
            }
        } else {
            int actualSegment = segment == 0 ? config().segments().size() : segment;
            long topRecord = 0;
            try {
                if (leaderboard != null) {
                    Long t = leaderboard.getBestRecord(player, actualSegment);
                    topRecord = t == null ? 0L : t;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            player.sendMessage(String.format("Segment %d: %s [Top: %s]",
                    actualSegment,
                    Functions.formatTime(temps),
                    Functions.formatTime(topRecord)));
        }
    }

    /**
     * Passes a checkpoint
     *
     * @param checkpoint Checkpoint ID
     */
    public void passCheckpoint(Player player, int checkpoint) {
        PlayerState state = getPlayer(player);
        if (state != null && !state.passCheckpoint(checkpoint)) {
            disqualify(player, "tu as saute un checkpoint");
        }
    }

    /**
     * Updates the holographic leaderboard.
     *
     * @throws SQLException when the database yields an SQL error.
     */
    public void updateLeaderboardHologram() throws SQLException {
        List<LeaderboardEntry> lb;
        ArrayList<String> lbString = new ArrayList<>();
        lbString.add(ChatColor.BOLD + "" + ChatColor.DARK_GREEN + "-- LEADERBOARD --");

        lb = leaderboard.getLeaderboard();
        for (LeaderboardEntry entry : lb) {
            String msg = String.format(ChatColor.BOLD + "" + ChatColor.GREEN + "[%s] " + ChatColor.RESET + " %s",
                    entry.nom(),
                    Functions.formatTime(entry.temps())
            );
            lbString.add(msg);
        }

        Hologram hg = DHAPI.getHologram(config().leaderboard());
        if (hg != null) {
            DHAPI.setHologramLines(hg, lbString);
        }
    }

    /**
     * Checks if the race is finished and acts appropriately.
     */
    public void finishRaceHook(Player player) {
        PlayerState state = getPlayer(player);
        if (state == null || !state.finished()) return;

        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "-- COURSE TERMINEE! --");
        for (int i = 1; i <= nbSegments(); ++i) {
            String fmt = String.format(
                    ChatColor.DARK_GREEN + "Segment %d" +
                            ChatColor.RESET + ": %s", i,
                    Functions.formatTime(state.segmentTime(i)));
            player.sendMessage(fmt);
        }

        player.sendMessage(String.format("Ton record total est " + ChatColor.YELLOW + "%s",
                Functions.formatTime(state.totalTime())));

        if (leaderboard != null) {
            try {
                Long record = leaderboard.getBestRecord(player, 0);
                if (record == null || record > state.totalTime()) {
                    if (record == null) record = 0L;
                    player.sendMessage(String.format(
                            ChatColor.GREEN + "" + ChatColor.BOLD + "NOUVEAU RECORD!" + ChatColor.RESET +
                                    " Ton ancien record etait " + ChatColor.YELLOW + "%s!",
                            Functions.formatTime(record)));
                }

                leaderboard.newRecord(state, null);
                updateLeaderboardHologram();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Recommence la partie
        resetPlayer(player);
    }

    /**
     * @return boat race configuration
     */
    public BoatRaceConfig config() { return this.config; }

    /**
     * @return number of segments within the race
     */
    public int nbSegments() { return config.segments().size(); }

    /**
     * @return number of checkpoints within the race
     */
    public int nbCheckpoints() { return config.checkpoints().size(); }

    /**
     * @return Leaderboard object
     */
    public Leaderboard leaderboard() { return leaderboard; }
}
