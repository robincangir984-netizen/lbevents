package com.craftlias.lbevents.managers;

import com.craftlias.lbevents.LbEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LocationManager {
    private final LbEvents plugin;
    private File file;
    private FileConfiguration config;

    public LocationManager(LbEvents plugin) {
        this.plugin = plugin;
        createConfig();
    }

    private void createConfig() {
        file = new File(plugin.getDataFolder(), "locations.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource("locations.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveArea(String eventName, Location pos1, Location pos2) {
        String path = "events." + eventName.toLowerCase();
        config.set(path + ".world", pos1.getWorld().getName());
        config.set(path + ".pos1.x", pos1.getBlockX());
        config.set(path + ".pos1.y", pos1.getBlockY());
        config.set(path + ".pos1.z", pos1.getBlockZ());

        config.set(path + ".pos2.x", pos2.getBlockX());
        config.set(path + ".pos2.y", pos2.getBlockY());
        config.set(path + ".pos2.z", pos2.getBlockZ());

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}