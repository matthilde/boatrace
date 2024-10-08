package de.matthil.boatRace;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// A noter que cette commande est seulement ici pour tester la leaderboard.

/**
 * BoardRace /br global command
 */
public class CommandBr implements CommandExecutor {
    private final BoatRace plugin;

    /**
     * @param plugin BoatRace plugin object to pass
     */
    public CommandBr(BoatRace plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the player has admin permission.
     *
     * @return the player has boatrace.admin
     */
    private boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("boatrace.admin");
    }

    public void cmdReload(CommandSender sender) {
        plugin.reloadBoatConfig();
        sender.sendMessage("Reloaded configuration!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String subCommand = args[0];
        BoatRaceGame game;

        switch (subCommand) {
            case "reload":
                if (isAdmin(sender)) {
                    cmdReload(sender);
                }
                break;
            case "leave":
                if (sender instanceof Player && (game = plugin.games().getGameByPlayer((Player) sender)) != null) {
                    game.disqualify((Player) sender, "commande");
                }
                break;
            case "restart":
                if (sender instanceof Player && (game = plugin.games().getGameByPlayer((Player) sender)) != null) {
                    game.restart((Player) sender);
                }
                break;
            default: return false;
        }

        return true;
    }
}
