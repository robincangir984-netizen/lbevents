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
        
        if (colorKeys.isEmpty()) return;
        List<String> colorList = new ArrayList<>(colorKeys);
        Random random = new Random();

        String worldName = config.getString("locations.pos1.world", "BlockParty");
        World bpWorld = Bukkit.getWorld(worldName);

        gameTask = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent()) || !isGameActive) {
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    // 1. Yeni güvenli renk seç
                    currentSafeColorKey = colorList.get(random.nextInt(colorList.size()));
                    try {
                        currentSafeColor = Material.valueOf(currentSafeColorKey);
                    } catch (IllegalArgumentException e) {
                        currentSafeColor = Material.WHITE_TERRACOTTA;
                    }

                    String colorDisplayName = config.getString("colors." + currentSafeColorKey, "&f&lRENK");
                    String translatedColor = ChatColor.translateAlternateColorCodes('&', colorDisplayName);

                    // 2. Zemin taramasını yap ve yanlış renkleri kır (HAVAYA çevir)
                    updateArenaFloor(config, currentSafeColor);

                    // 3. Oyunculara bildir
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (bpWorld != null && player.getWorld().equals(bpWorld)) {
                            player.sendTitle(translatedColor, ChatColor.GRAY + "Bu renge bas!", 0, 40, 10);
                            player.sendMessage(Component.text("§e[BlockParty] §fGüvenli Renk: " + translatedColor));
                        }
                    }

                    countdown = 5; // Süreyi sıfırla
                } else {
                    // Sayaç akarken ekranda göster
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (bpWorld != null && player.getWorld().equals(bpWorld)) {
                            player.sendActionBar(Component.text("§bSıradaki Renk Değişimine: §e" + countdown + " §bsaniye"));
                        }
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L);
    }

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

        // Zemin yüksekliği tek kat veya birkaç kat olabilir, sınırları tam tarıyoruz
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    String blockName = block.getType().name();
                    if (blockName.endsWith("_TERRACOTTA") || blockName.endsWith("_WOOL")) {
                        if (block.getType() != safeMat) {
                            block.setType(Material.AIR);
                        }
                    }
                }
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
        String worldName = config.getString("locations.pos1.world", "BlockParty");
        
        if (!player.getWorld().getName().equals(worldName)) return;
        if (eliminatedPlayers.contains(player.getUniqueId())) return;

        Location loc = player.getLocation().subtract(0, 1, 0);
        Block block = loc.getBlock();
        String blockName = block.getType().name();

        // Eğer oyuncu boşluğa düştüyse VEYA güvenli renk dışındaki bir bloğa bastıysa elenir
        boolean isStandingOnInvalidBlock = block.getType() == Material.AIR || 
                ((blockName.endsWith("_TERRACOTTA") || blockName.endsWith("_WOOL")) && block.getType() != currentSafeColor);

        if (isStandingOnInvalidBlock) {
            eliminatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§c[BlockParty] Yanlış renkte kaldın veya düştün, elendin!"));
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }
}