package com.craftlias.lbevents.listeners;

import com.craftlias.lbevents.LbEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
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
    private static Material currentSafeColor = Material.WHITE_TERRACOTTA;
    private static String currentSafeColorKey = "WHITE_TERRACOTTA";

    public static void startBlockParty() {
        reset();
        FileConfiguration config = LbEvents.getInstance().getConfig();
        
        // Config'deki renkleri al
        Set<String> colorKeys = config.getConfigurationSection("colors") != null ? 
                config.getConfigurationSection("colors").getKeys(false) : Collections.emptySet();
        
        if (colorKeys.isEmpty()) return;
        List<String> colorList = new ArrayList<>(colorKeys);
        Random random = new Random();

        gameTask = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent())) {
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    // 1. Rastgele güvenli renk seç
                    currentSafeColorKey = colorList.get(random.nextInt(colorList.size()));
                    try {
                        currentSafeColor = Material.valueOf(currentSafeColorKey);
                    } catch (IllegalArgumentException e) {
                        currentSafeColor = Material.WHITE_TERRACOTTA;
                    }

                    String colorDisplayName = config.getString("colors." + currentSafeColorKey, "&f&lRENK");

                    // 2. Config'deki pos1 ve pos2 arasındaki zemini tara: Güvenli olmayanları havaya çevir!
                    updateArenaFloor(config, currentSafeColor);

                    // 3. Herkese bildir
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(ChatColor.translateAlternateColorCodes('&', colorDisplayName), ChatColor.GRAY + "Bu renge bas!", 0, 40, 10);
                        player.sendMessage(Component.text("§e[BlockParty] §fGüvenli Renk: " + ChatColor.translateAlternateColorCodes('&', colorDisplayName)));
                    }

                    countdown = 5;
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(Component.text("§bSıradaki Renk: §e" + countdown + " §bsaniye"));
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L);
    }

    // Config'deki pos1 ve pos2 koordinatları arasındaki alanı tarayıp yanlış renkleri silen fonksiyon
    private static void updateArenaFloor(FileConfiguration config, Material safeMat) {
        if (!config.contains("locations.pos1") || !config.contains("locations.pos2")) return;

        String worldName = config.getString("locations.pos1.world", "BlockParty");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        double x1 = config.getDouble("locations.pos1.x");
        double y1 = config.getDouble("locations.pos1.y");
        double z1 = config.getDouble("locations.pos1.z");

        double x2 = config.getDouble("locations.pos2.x");
        double y2 = config.getDouble("locations.pos2.y");
        double z2 = config.getDouble("locations.pos2.z");

        int minX = (int) Math.min(x1, x2);
        int maxX = (int) Math.max(x1, x2);
        int minY = (int) Math.min(y1, y2);
        int maxY = (int) Math.max(y1, y2);
        int minZ = (int) Math.min(z1, z2);
        int maxZ = (int) Math.max(z1, z2);

        // Bölge içindeki tüm blokları tara
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    String blockName = block.getType().name();
                    // Eğer blok bir terracotta (pişmiş toprak) ise ve güvenli renk değilse havaya çevir
                    if (blockName.endsWith("_TERRACOTTA")) {
                        if (block.getType() != safeMat) {
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
        String blockName = block.getType().name();

        // Eğer oyuncu boşluğa düştüyse VEYA yanlış bir terracotta/blok üzerindeyse elenir
        boolean isStandingOnInvalidBlock = block.getType() == Material.AIR || 
                (blockName.endsWith("_TERRACOTTA") && block.getType() != currentSafeColor);

        if (isStandingOnInvalidBlock) {
            eliminatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§c[BlockParty] Yanlış renkte kaldın ve elendin!"));
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }
}