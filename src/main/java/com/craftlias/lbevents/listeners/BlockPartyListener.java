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
                    // Rastgele güvenli renk seç
                    currentSafeColor = WOOL_COLORS.get(new Random().nextInt(WOOL_COLORS.size()));
                    String colorName = formatColorName(currentSafeColor);

                    // Herkese Title ve Chat bildirimi gönder
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(ChatColor.YELLOW + "RENK: " + colorName, ChatColor.GRAY + "O bloğa hızlıca çık!", 0, 40, 10);
                        player.sendMessage(Component.text("§e[BlockParty] §fGüvenli Renk: " + colorName));
                    }

                    countdown = 5; // Sonraki tur için süreyi sıfırla
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(Component.text("§bSıradaki Renk Değişimine: §e" + countdown + " §bsaniye"));
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L); // Her 1 saniyede bir çalışır
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

        // Eğer oyuncu yün bir bloğun üzerindeyse ve güvenli renk değilse elenir
        if (block.getType().name().endsWith("_WOOL") && block.getType() != currentSafeColor) {
            eliminatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§c[BlockParty] Yanlış renkte kaldın ve elendin!"));
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    private static String formatColorName(Material material) {
        String name = material.name().replace("_WOOL", "").replace("_", " ");
        return name.toUpperCase();
    }
}