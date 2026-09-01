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
    private static boolean isGameActive = false;
    private static boolean isEliminationPhase = false;

    private static final Map<Location, Material> originalFloorBlocks = new HashMap<>();
    private static int floorMinY = 0;

    public static void startBlockParty() {
        reset();
        isGameActive = true;
        isEliminationPhase = false;
        originalFloorBlocks.clear();

        if (LbEvents.getInstance().getLocationManager() == null) {
            Bukkit.broadcast(Component.text("§c[HATA] LocationManager yüklenemedi!"));
            isGameActive = false;
            return;
        }

        FileConfiguration locConfig = LbEvents.getInstance().getLocationManager().getConfig();
        String path = "events.blockparty";

        String worldName = locConfig.getString(path + ".world", "world");
        World bpWorld = Bukkit.getWorld(worldName);
        if (bpWorld == null) bpWorld = Bukkit.getWorlds().get(0);
        final World finalWorld = bpWorld;

        saveArenaFloor(locConfig, path, finalWorld);

        if (originalFloorBlocks.isEmpty()) {
            Bukkit.broadcast(Component.text("§c[HATA] BlockParty zemini bulunamadı! /lbevent wand ile seçip /lbevent setarea blockparty yapmalısın."));
            isGameActive = false;
            return;
        }

        List<Material> availableColors = new ArrayList<>(new HashSet<>(originalFloorBlocks.values()));
        if (availableColors.isEmpty()) {
            availableColors.add(Material.WHITE_TERRACOTTA);
        }
        Random random = new Random();

        gameTask = new BukkitRunnable() {
            int timer = 4;

            @Override
            public void run() {
                if (!isGameActive) {
                    cancel();
                    return;
                }

                // EventManager null kontrolü güvenli hale getirildi
                try {
                    Object eventMgr = LbEvents.getInstance().getClass().getMethod("getEventManager").invoke(LbEvents.getInstance());
                    if (eventMgr != null) {
                        Object activeEvent = eventMgr.getClass().getMethod("getActiveEvent").invoke(eventMgr);
                        if (activeEvent != null && !"blockparty".equalsIgnoreCase(activeEvent.toString())) {
                            cancel();
                            return;
                        }
                    }
                } catch (Exception ignored) {}

                if (!isEliminationPhase) {
                    if (timer == 4) {
                        restoreArenaFloor();
                        currentSafeColor = availableColors.get(random.nextInt(availableColors.size()));
                        
                        FileConfiguration mainConfig = LbEvents.getInstance().getConfig();
                        String baseName = currentSafeColor.name().replace("_TERRACOTTA", "").replace("_WOOL", "").replace("_CONCRETE", "").replace("_CARPET", "");
                        String translatedColor = ChatColor.translateAlternateColorCodes('&', mainConfig != null ? mainConfig.getString("colors." + currentSafeColor.name(), "&f&l" + baseName) : "&f&l" + baseName);

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getWorld().equals(finalWorld) && !eliminatedPlayers.contains(player.getUniqueId())) {
                                player.sendTitle(translatedColor, ChatColor.GRAY + "Bu renge koş!", 0, 40, 10);
                                player.sendMessage(Component.text("§e[BlockParty] §fHedef Renk: " + translatedColor));
                            }
                        }
                        timer = 3;
                    }

                    if (timer > 0) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getWorld().equals(finalWorld)) {
                                player.sendActionBar(Component.text("§bSıradaki Renk: §e" + timer + " §bsaniye"));
                            }
                        }
                        timer--;
                    } else {
                        destroyWrongBlocks(currentSafeColor);
                        isEliminationPhase = true;
                        timer = 2;
                    }
                } else {
                    if (timer > 0) {
                        timer--;
                    } else {
                        isEliminationPhase = false;
                        timer = 4;
                    }
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L);
    }

    private static void saveArenaFloor(FileConfiguration locConfig, String path, World world) {
        if (!locConfig.contains(path + ".pos1") || !locConfig.contains(path + ".pos2")) {
            return;
        }

        int x1 = locConfig.getInt(path + ".pos1.x");
        int y1 = locConfig.getInt(path + ".pos1.y");
        int z1 = locConfig.getInt(path + ".pos1.z");

        int x2 = locConfig.getInt(path + ".pos2.x");
        int y2 = locConfig.getInt(path + ".pos2.y");
        int z2 = locConfig.getInt(path + ".pos2.z");

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int lowestY = 300;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    String name = block.getType().name();
                    if (name.endsWith("_TERRACOTTA") || name.endsWith("_WOOL") || name.endsWith("_CONCRETE") || name.endsWith("_CARPET")) {
                        originalFloorBlocks.put(block.getLocation(), block.getType());
                        if (y < lowestY) lowestY = y;
                    }
                }
            }
        }
        if (!originalFloorBlocks.isEmpty()) {
            floorMinY = lowestY;
        }
    }

    private static void restoreArenaFloor() {
        for (Map.Entry<Location, Material> entry : originalFloorBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
    }

    private static void destroyWrongBlocks(Material safeMat) {
        for (Map.Entry<Location, Material> entry : originalFloorBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            if (entry.getValue() != safeMat) {
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
        restoreArenaFloor();
        eliminatedPlayers.clear();
    }

    public static void reset() {
        eliminatedPlayers.clear();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isGameActive) return;
        
        Player player = event.getPlayer();
        if (LbEvents.getInstance().getLocationManager() == null) return;
        
        FileConfiguration locConfig = LbEvents.getInstance().getLocationManager().getConfig();
        String worldName = locConfig.getString("events.blockparty.world", "world");
        
        if (!player.getWorld().getName().equals(worldName)) return;
        if (eliminatedPlayers.contains(player.getUniqueId())) return;

        if (isEliminationPhase) {
            if (player.getLocation().getY() < floorMinY) {
                eliminatedPlayers.add(player.getUniqueId());
                player.sendMessage(Component.text("§c[BlockParty] Sürede doğru renge ulaşamadın ve elendin!"));
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }
    }
}