package com.craftlias.lbevents;

import com.craftlias.lbevents.commands.EventCommand;
import com.craftlias.lbevents.listeners.BlockPartyListener;
import com.craftlias.lbevents.listeners.MiningListener;
import com.craftlias.lbevents.listeners.SabotageListener;
import com.craftlias.lbevents.listeners.WandListener;
import com.craftlias.lbevents.managers.EventManager;
import com.craftlias.lbevents.managers.LocationManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class LbEvents extends JavaPlugin {

    private static LbEvents instance;
    private EventManager eventManager;
    private LocationManager locationManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.eventManager = new EventManager();
        this.locationManager = new LocationManager(this);

        // Listener Kayıtları
        getServer().getPluginManager().registerEvents(new BlockPartyListener(), this);
        getServer().getPluginManager().registerEvents(new MiningListener(), this);
        getServer().getPluginManager().registerEvents(new SabotageListener(), this);
        getServer().getPluginManager().registerEvents(new WandListener(), this);

        // Komut Kayıtları
        if (getCommand("lbevents") != null) {
            getCommand("lbevents").setExecutor(new EventCommand());
            getCommand("lbevents").setTabCompleter(new EventCommand());
        }

        getLogger().info("LbEvents başarıyla aktif edildi!");
    }

    @Override
    public void onDisable() {
        getLogger().info("LbEvents deaktif edildi.");
    }

    public static LbEvents getInstance() {
        return instance;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }
}