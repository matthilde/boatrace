package de.matthil.utilitiesLib;

import org.bukkit.plugin.java.JavaPlugin;

public final class UtilitiesLib extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("MDE UtilitiesLib enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
