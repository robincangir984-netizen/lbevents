package com.craftlias.lbevents.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {
    public static final Map<UUID, Location> pos1Map = new HashMap<>();
    public static final Map<UUID, Location> pos2Map = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.WOODEN_AXE) return;
        if (!item.hasItemMeta() || !item.getItemMeta().getDisplayName().contains("LbEvents Wand")) return;

        if (event.getClickedBlock() == null) return;
        
        event.setCancelled(true); // Bloğu kırmasını önle
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            pos1Map.put(player.getUniqueId(), loc);
            player.sendMessage(Component.text("§a[LbEvents] 1. Pos ayarlandı: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            pos2Map.put(player.getUniqueId(), loc);
            player.sendMessage(Component.text("§a[LbEvents] 2. Pos ayarlandı: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        }
    }
}