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

    public static void startBlockParty() {
        reset();
        isGameActive = true;
        FileConfiguration config = LbEvents.getInstance().getConfig();
        
        Set<String> colorKeys = config.getConfigurationSection("colors") != null ? 
                config.getConfigurationSection("colors").getKeys(false) : Collections.emptySet();
        
        if (colorKeys.isEmpty()) {
            // Yedek renk havuzu (config boşsa çökmesin)
            colorKeys = Set.of("WHITE_TERRACOTTA", "RED_TERRACOTTA", "GREEN_TERRACOTTA", "BLUE_TERRACOTTA", "YELLOW_TERRACOTTA");
        }
        
        List<String> colorList = new ArrayList<>(colorKeys);
        Random random = new Random();

        String worldName = config.getString("locations.pos1.world", config.getString("events.blockparty.world", "BlockParty"));
        World bpWorld = Bukkit.getWorld(worldName);
        if (bpWorld == null) {
            bpWorld = Bukkit.getWorlds().get(0); // Dünya bulunamazsa ana dünya
        }

        final World finalWorld = bpWorld;

        gameTask = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent()) || !isGameActive) {
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    currentSafeColorKey = colorList.get(random.nextInt(colorList.size()));
                    try {
                        currentSafeColor = Material.valueOf(currentSafeColorKey);
                    } catch (IllegalArgumentException e) {
                        currentSafeColor = Material.WHITE_TERRACOTTA;
                    }

                    String colorDisplayName = config.getString("colors." + currentSafeColorKey, "&f&l" + currentSafeColorKey);
                    String translatedColor = ChatColor.translateAlternateColorCodes('&', colorDisplayName);

                    // 1. Zemin kırma işlemini çalıştır (Config koordinatları veya oyuncu altı akıllı tarama)
                    updateArenaFloor(config, finalWorld, currentSafeColor);

                    // 2. Oyunculara başlık ve mesaj gönder
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(finalWorld)) {
                            player.sendTitle(translatedColor, ChatColor.GRAY + "Bu renge bas!", 0, 40, 10);
                            player.sendMessage(Component.text("§e[BlockParty] §fGüvenli Renk: " + translatedColor));
                        }
                    }

                    countdown = 5;
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(finalWorld)) {
                            player.sendActionBar(Component.text("§bSıradaki Renk: §e" + countdown + " §bsaniye"));
                        }
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L);
    }

    private static void updateArenaFloor(FileConfiguration config, World world, Material safeMat) {
        boolean usedConfigBounds = false;

        // Farklı olası config yollarını kontrol et (/lbevent setarea nereye kaydediyorsa yakala)
        String[] pathPrefixes = {"locations", "events.blockparty", "arenas.blockparty"};
        
        for (String prefix : pathPrefixes) {
            if (config.contains(prefix + ".pos1") && config.contains(prefix + ".pos2")) {
                double x1 = config.getDouble(prefix + ".pos1.x");
                double y1 = config.getDouble(prefix + ".pos1.y");
                double z1 = config.getDouble(prefix + ".pos1.z");

                double x2 = config.getDouble(prefix + ".pos2.x");
                double y2 = config.getDouble(prefix + ".pos2.y");
                double z2 = config.getDouble(prefix + ".pos2.z");

                // Eğer Y koordinatları 0.0 veya tanımsız/hatalı girilmişse sütun taramasına geç
                if (y1 != 0.0 || y2 != 0.0) {
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
                                breakBlockIfInvalid(block, safeMat);
                            }
                        }
                    }
                    usedConfigBounds = true;
                    break;
                }
            }
        }

        // Eğer config koordinatları yoksa veya Y=0 hatası içeriyorsa, 
        // dünyadaki tüm aktif oyuncuların altındaki zemini (15 blok yarıçapında) otomatik kırarak asla hata vermesini engelle!
        if (!usedConfigBounds) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(world)) {
                    Location loc = player.getLocation();
                    int px = loc.getBlockX();
                    int pz = loc.getBlockZ();
                    int py = loc.getBlockY();

                    for (int x = -15; x <= 15; x++) {
                        for (int z = -15; z <= 15; z++) {
                            for (int y = -3; y <= 2; y++) {
                                Block block = world.getBlockAt(px + x, py + y, pz + z);
                                breakBlockIfInvalid(block, safeMat);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void breakBlockIfInvalid(Block block, Material safeMat) {
        String name = block.getType().name();
        if (name.endsWith("_TERRACOTTA") || name.endsWith("_WOOL")) {
            if (block.getType() != safeMat) {
                block.setType(Material.AIR);
            }
        }
    }

    public static void stopBlockParty() {
        isGameActive = false;
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
        if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent()) || !isGameActive) return;
        
        Player player = event.getPlayer();
        FileConfiguration config = LbEvents.getInstance().getConfig();
        String worldName = config.getString("locations.pos1.world", config.getString("events.blockparty.world", "BlockParty"));
        
        if (!player.getWorld().getName().equals(worldName)) return;
        if (eliminatedPlayers.contains(player.getUniqueId())) return;

        Location loc = player.getLocation().subtract(0, 1, 0);
        Block block = loc.getBlock();
        String blockName = block.getType().name();

        boolean isStandingOnInvalidBlock = block.getType() == Material.AIR || 
                ((blockName.endsWith("_TERRACOTTA") || blockName.endsWith("_WOOL")) && block.getType() != currentSafeColor);

        if (isStandingOnInvalidBlock) {
            eliminatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§c[BlockParty] Yanlış renkte kaldın veya düştün, elendin!"));
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }
}