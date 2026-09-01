package com.craftlias.lbevents.listeners;

import com.craftlias.lbevents.LbEvents;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BlockPartyListener implements Listener {
    private static final Set<UUID> eliminatedPlayers = new HashSet<>();
    private static Material safeColor = Material.GREEN_WOOL;

    public static void setSafeColor(Material material) {
        safeColor = material;
    }

    public static void reset() {
        eliminatedPlayers.clear();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent())) return;
        
        Player player = event.getPlayer();
        if (eliminatedPlayers.contains(player.getUniqueId())) return;

        Location loc = player.getLocation().subtract(0, 1, 0);
        Block block = loc.getBlock();

        if (block.getType().name().endsWith("_WOOL") && block.getType() != safeColor) {
            eliminatedPlayers.add(player.getUniqueId());
            player.sendMessage("§cBlockParty'de yanlış renkte kaldın ve elendin!");
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }
}