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
    private static boolean isGameActive = false;
    
    // Orijinal zemin bloklarını tur bittiğinde geri yükleyebilmek için hafızada tutuyoruz (Konum -> Materyal)
    private static final Map<Location, Material> originalFloorBlocks = new HashMap<>();
    private static boolean floorSaved = false;

    public static void startBlockParty() {
        reset();
        isGameActive = true;
        floorSaved = false;
        originalFloorBlocks.clear();

        FileConfiguration config = LbEvents.getInstance().getConfig();
        
        Set<String> colorKeys = config.getConfigurationSection("colors") != null ? 
                config.getConfigurationSection("colors").getKeys(false) : Collections.emptySet();
        
        if (colorKeys.isEmpty()) {
            colorKeys = Set.of("WHITE_TERRACOTTA", "ORANGE_TERRACOTTA", "MAGENTA_TERRACOTTA", "YELLOW_TERRACOTTA", "LIME_TERRACOTTA", "PINK_TERRACOTTA", "GREEN_TERRACOTTA", "RED_TERRACOTTA", "BLACK_TERRACOTTA");
        }
        
        List<String> colorList = new ArrayList<>(colorKeys);
        Random random = new Random();

        String worldName = config.getString("locations.pos1.world", config.getString("events.blockparty.world", "BlockParty"));
        World bpWorld = Bukkit.getWorld(worldName);
        if (bpWorld == null) bpWorld = Bukkit.getWorlds().get(0);
        final World finalWorld = bpWorld;

        // Oyun başladığında zemini hafızaya al
        saveArenaFloor(config, finalWorld);

        gameTask = new BukkitRunnable() {
            int countdown = 3; // İstediğin gibi 3 saniye

            @Override
            public void run() {
                if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent()) || !isGameActive) {
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    // 1. Önceki turdan silinen blokları komple orijinal haline geri getir
                    restoreArenaFloor();

                    // 2. Rastgele bir güvenli renk seç
                    currentSafeColorKey = colorList.get(random.nextInt(colorList.size()));
                    try {
                        currentSafeColor = Material.valueOf(currentSafeColorKey);
                    } catch (IllegalArgumentException e) {
                        currentSafeColor = Material.WHITE_TERRACOTTA;
                    }

                    String colorDisplayName = config.getString("colors." + currentSafeColorKey, "&f&l" + currentSafeColorKey);
                    String translatedColor = ChatColor.translateAlternateColorCodes('&', colorDisplayName);

                    // 3. Güvenli renk DIŞINDAKİ tüm zemin bloklarını havaya (AIR) uçur!
                    destroyWrongBlocks(currentSafeColor);

                    // 4. Herkese bildir
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(finalWorld)) {
                            player.sendTitle(translatedColor, ChatColor.GRAY + "Bu renge yetiş!", 0, 35, 10);
                            player.sendMessage(Component.text("§e[BlockParty] §fGüvenli Renk: " + translatedColor));
                        }
                    }

                    countdown = 3; // Her tur arası 3 saniye
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(finalWorld)) {
                            player.sendActionBar(Component.text("§bSıradaki Renk Değişimine: §e" + countdown + " §bsaniye"));
                        }
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L);
    }

    private static void saveArenaFloor(FileConfiguration config, World world) {
        if (floorSaved) return;
        
        if (config.contains("locations.pos1") && config.contains("locations.pos2")) {
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

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        String name = block.getType().name();
                        if (name.endsWith("_TERRACOTTA") || name.endsWith("_WOOL")) {
                            originalFloorBlocks.put(block.getLocation(), block.getType());
                        }
                    }
                }
            }
        }
        floorSaved = true;
    }

    private static void restoreArenaFloor() {
        for (Map.Entry<Location, Material> entry : originalFloorBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
    }

    private static void destroyWrongBlocks(Material safeMat) {
        for (Map.Entry<Location, Material> entry : originalFloorBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            // Eğer bloğun orijinali güvenli renk DEĞİLSE havaya uçur
            if (entry.getValue() != safeMat) {
                block.setType(Material.AIR);
            } else {
                block.setType(safeMat); // Güvenli olanlar yerinde kalır
            }
        }
    }

    public static void stopBlockParty() {
        isGameActive = false;
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        restoreArenaFloor(); // Oyun bitince zemini eski haline getir
        eliminatedPlayers.clear();
    }

    public static void reset() {
        eliminatedPlayers.clear();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent()) || !isGameActive) return;
        
        Player player = event.getPlayer();
        FileConfiguration config = LbEvents.getInstance().getConfig();
        String worldName = config.getString("locations.pos1.world", config.getString("events.blockparty.world", "BlockParty"));
        
        if (!player.getWorld().getName().equals(worldName)) return;
        if (eliminatedPlayers.contains(player.getUniqueId())) return;

        Location loc = player.getLocation().subtract(0, 1, 0);
        Block block = loc.getBlock();
        String blockName = block.getType().name();

        // Boşluğa düşen veya yanlış renkte kalan direkt elenir
        boolean isStandingOnInvalidBlock = block.getType() == Material.AIR || 
                ((blockName.endsWith("_TERRACOTTA") || blockName.endsWith("_WOOL")) && block.getType() != currentSafeColor);

        if (isStandingOnInvalidBlock) {
            eliminatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§c[BlockParty] Sürede doğru renge ulaşamadın ve elendin!"));
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }
}