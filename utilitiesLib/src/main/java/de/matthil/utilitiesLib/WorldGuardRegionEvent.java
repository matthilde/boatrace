package de.matthil.utilitiesLib;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

// Cette classe permet de gerer des events d'entree et sortie de regions WorldGuard.
public abstract class WorldGuardRegionEvent implements Listener {
    // Fonction permettant de gerer les fonctions abstraites d'event
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Eviter de calculer au moment d'un mouvement de tete.
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((to == null || from.getWorld() == null) ||
                (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ() &&
                from.getWorld().equals(to.getWorld()))) {
            return;
        }

        Player player = event.getPlayer();

        World world = BukkitAdapter.adapt(to.getWorld());
        BlockVector3 wgPosF = BukkitAdapter.adapt(from).toVector().toBlockPoint();
        BlockVector3 wgPosT = BukkitAdapter.adapt(to).toVector().toBlockPoint();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(to.getWorld()));

        ApplicableRegionSet setF = regions.getApplicableRegions(wgPosF);
        ApplicableRegionSet setT = regions.getApplicableRegions(wgPosT);

        // On verifie les regions dans lesquelles le joueur entre
        for (ProtectedRegion region : setT) {
            if (!setF.getRegions().contains(region)) {
                this.onRegionEnter(player, region);
            }
        }

        // Puis on verifie celles ou le joueur sort
        for (ProtectedRegion region : setF) {
            if (!setT.getRegions().contains(region)) {
                this.onRegionLeave(player, region);
            }
        }
    }

    public void onRegionEnter(Player ignoredPlayer, ProtectedRegion ignoredRegion) {  }
    public void onRegionLeave(Player ignoredPlayer, ProtectedRegion ignoredRegion) {  }
}
