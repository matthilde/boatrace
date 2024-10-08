package de.matthil.boatRace;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

/*
 * Main plugin class
 */
public final class BoatRace extends JavaPlugin {
    public FileConfiguration config;

    private RaceController races = null;
    private Events raceEvents = null;
    private int raceTaskId = -1;

    public RaceController games() { return this.races; }

    /*
     * Reloads the plugin YAML configuration.
     */
    public void reloadBoatConfig() {
        reloadConfig();

        // Global information
        double timeout = config.getDouble("global.timeout");

        // Leaderboard database information
        String username = config.getString("database.username");
        String password = config.getString("database.password");
        String database = config.getString("database.database");
        String host     = config.getString("database.host");
        int    port     = config.getInt("database.port");

        try {
            Leaderboard leaderboard = new Leaderboard("default", host, port, database, username, password);
            leaderboard.initDatabase();
        } catch (Exception e) {
            getLogger().warning(e.toString());
            getLogger().warning("La base de donnees n'a pas pu etre chargee.");
        }

        // Initiate the race game and event system
        races = new RaceController();

        ConfigurationSection racesList = config.getConfigurationSection("races");
        if (racesList != null) {
            // Get all races and initialise them
            for (String key : racesList.getKeys(false)) {
                ConfigurationSection raceConf = racesList.getConfigurationSection(key);
                if (raceConf != null) {
                    getLogger().warning(key);
                    List<String> segments = raceConf.getStringList("segments");
                    List<String> checkpoints = raceConf.getStringList("checkpoints");
                    String entryPoint = raceConf.getString("entryPoint");
                    List<Double> respawn = raceConf.getDoubleList("respawn");
                    String hologram = raceConf.getString("hologram");

                    BoatRaceConfig boatCfg = new BoatRaceConfig(segments, checkpoints, timeout, respawn, entryPoint, hologram);
                    Leaderboard leaderboard = new Leaderboard(key, host, port, database, username, password);

                    BoatRaceGame game = new BoatRaceGame(boatCfg, leaderboard);
                    races.addGame(key, game);

                    getLogger().info(String.format("Configured %s", key));
                }
            }
        }

        if (raceEvents == null) {
            raceEvents = new Events(races);
            this.getServer().getPluginManager().registerEvents(raceEvents, this);
            raceTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> raceEvents.scheduleFunction(), 0L, 2);
        }

        raceEvents.setGame(races);

        getLogger().info("Configuration chargee!");
    }

    @Override
    public void onEnable() {
        getLogger().info("BoatRace v1.0 par la eMDE !");
        saveDefaultConfig();

        config = getConfig();

        // Configuration
        reloadBoatConfig();
        Objects.requireNonNull(this.getCommand("br")).setExecutor(new CommandBr(this));
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTask(raceTaskId);
    }
}
