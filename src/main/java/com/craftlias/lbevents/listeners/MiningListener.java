package com.craftlias.lbevents.listeners;

import com.craftlias.lbevents.LbEvents;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class MiningListener implements Listener {

    private static final Set<Material> ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!"mining".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent())) return;

        Material type = event.getBlock().getType();
        if (ORES.contains(type)) {
            event.setDropItems(false);
            for (ItemStack drop : event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
                ItemStack multiplied = drop.clone();
                multiplied.setAmount(drop.getAmount() * 2);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), multiplied);
            }
        }
    }
}