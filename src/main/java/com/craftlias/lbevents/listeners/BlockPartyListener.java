package com.craftlias.lbevents.listeners;

import com.craftlias.lbevents.LbEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BlockPartyListener implements Listener {
    private static final Set<UUID> eliminatedPlayers = new HashSet<>();
    private static BukkitTask gameTask = null;
    private static Material currentSafeColor = Material.GREEN_WOOL;

    private static final List<Material> WOOL_COLORS = List.of(
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.CYAN_WOOL,
            Material.PURPLE_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,
            Material.RED_WOOL, Material.BLACK_WOOL
    );

    public static void startBlockParty() {
        reset();
        gameTask = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent())) {
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    // 1. Önceki turdan kalan kırılmış/havaya uçmuş blokları eski haline getirmek yerine 
                    // yeni renk seçip diğerlerini yok edeceğiz. 
                    // (Not: Gerçek bir BlockParty'de zemin ya tamamen yenilenir ya da yanlışlar yok olur)
                    
                    currentSafeColor = WOOL_COLORS.get(new Random().nextInt(WOOL_COLORS.size()));
                    String colorName = formatColorName(currentSafeColor);

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(ChatColor.YELLOW + "RENK: " + colorName, ChatColor.GRAY + "O bloğa hızlıca çık!", 0, 30, 10);
                        player.sendMessage(Component.text("§e[BlockParty] §fGüvenli Renk: " + colorName));
                        
                        // Oyuncunun etrafındaki (örneğin 15 blok yarıçapındaki alt zemindeki) yünleri tara ve sil
                        removeWrongBlocks(player.getLocation());
                    }

                    countdown = 5;
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(Component.text("§bSıradaki Renk Değişimine: §e" + countdown + " §bsaniye"));
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L);
    }

    // Oyuncunun altındaki zemini tarayıp güvenli renk olmayan yünleri yok eden fonksiyon
    private static void removeWrongBlocks(Location center) {
        int radius = 12; // Zemin tarama yarıçapı
        org.bukkit.World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Oyuncunun 1-2 blok altındaki alanı tarıyoruz
                for (int y = -3; y <= 1; y++) {
                    Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (block.getType().name().endsWith("_WOOL")) {
                        if (block.getType() != currentSafeColor) {
                            // Yanlış renkli yünleri havaya (boşluğa) çevirip düşmelerini sağlıyoruz
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    public static void stopBlockParty() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        eliminatedPlayers.clear();
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

        if (block.getType() == Material.AIR || (block.getType().name().endsWith("_WOOL") && block.getType() != currentSafeColor)) {
            eliminatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§c[BlockParty] Yanlış renkte kaldın veya boşluğa düştün, elendin!"));
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    private static String formatColorName(Material material) {
        String name = material.name().replace("_WOOL", "").replace("_", " ");
        return name.toUpperCase();
    }
}