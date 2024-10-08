package de.matthil.boatRace;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Class storing the player's state in a race.
 */
public class PlayerState {
    private int checkpoint, segment;
    private boolean finished;
    private Boat boat = null;

    private final Instant[] records;
    private final Player player;
    private final int nbSegments;
    private final int nbCheckpoints;
    private final Instant start;
    private final BossBar chrono;
    private Instant leaveBoat;

    public PlayerState(Player player, Instant start, BossBar chrono, int nbSegments, int nbCheckpoints) {
        this.player = player;
        this.checkpoint = this.segment = 0;
        this.start = start;
        this.chrono = chrono;

        this.nbSegments = nbSegments;           // 0 = Finish Line
        this.nbCheckpoints = nbCheckpoints;     // 0 = Finish Line

        this.finished = false;

        this.leaveBoat = Instant.now();

        records = new Instant[nbSegments + 1];
        records[0] = start; // Temporaire, pour le bon fonctionnement
    }

    // Passe un checkpoint, renvoie false si le checkpoint est invalide
    public boolean passCheckpoint(int checkpoint) {
        if (checkpoint != (this.checkpoint + 1) % nbCheckpoints) {
            return false;
        }

        this.checkpoint = checkpoint;
        return true;
    }

    // Passe un segment, renvoie le temps si valide, null sinon
    // Le temps est renvoye en secondes
    @Nullable
    public Long passSegment(int segment) {
        if (segment == (this.segment)) {
            return 0L;
        }
        if (segment != (this.segment + 1) % nbSegments) {
            return null;
        }
        // Si on reviens au segment initial (l'arrivee), alors le joueur a termine la course.
        if (segment == 0) {
            this.finished = true;
        }

        Instant now = Instant.now();
        records[segment] = now;

        // Calcul du record.
        long intervalle = now.toEpochMilli() - records[this.segment].toEpochMilli();
        this.segment = segment;

        return intervalle;
    }

    // Temps total en millisecondes
    public long totalTime() {
        return records[0].toEpochMilli() - start.toEpochMilli();
    }

    // Temps de segment en millisecondes
    public long segmentTime(int segment) {
        segment = segment % nbSegments;
        int prevSegment = (segment + nbSegments - 1) % nbSegments;

        long cur  = records[segment].toEpochMilli();
        long prev = prevSegment == 0 ? start.toEpochMilli() : records[prevSegment].toEpochMilli();

        return cur - prev;
    }

    public boolean finished() { return finished; }
    public Player player() { return player; }
    public Instant start() { return start; }
    public BossBar chrono() { return chrono; }
    public int getNbSegments() { return nbSegments; }

    public long getLastLeaveAttempt() {
        return Instant.now().toEpochMilli() - leaveBoat.toEpochMilli();
    }

    public void updateLastLeaveAttempt() {
        leaveBoat = Instant.now();
    }

    public void setBoat(Boat boat) { this.boat = boat; }
    public Boat getBoat() { return boat; }

    /**
     * Respawns/spawns a boat for the player.
     * Takes the player location if no boat exists.
     */
    public void respawnBoat() {
        Location loc = player.getLocation();
        if (boat != null) {
            loc = boat.getLocation();
            removeBoat(false);
        }
        boat = (Boat)player.getWorld().spawnEntity(loc, EntityType.BOAT);
        boat.addPassenger(player);
    }

    /**
     * Removes the boat the player is riding.
     *
     * @param giveItem gives back a boat if true.
     */
    public void removeBoat(boolean giveItem) {
        if (boat != null) {
            player.leaveVehicle();
            getBoat().remove();
        }
        boat = null;

        if (giveItem) {
            ItemStack boatItem = new ItemStack(Material.OAK_BOAT);
            player.getInventory().addItem(boatItem);
        }
    }

    /**
     * Removes the boat the player is riding and gives back one as an item.
     */
    public void removeBoat() {
        removeBoat(true);
    }
}
