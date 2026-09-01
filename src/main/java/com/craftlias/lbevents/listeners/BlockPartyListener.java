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
    private static boolean isEliminationPhase = false; // Koşma ve düşme aşamalarını ayırır

    private static final Map<Location, Material> originalFloorBlocks = new HashMap<>();
    private static int floorMinY = 0; // Oyuncuların düşüp düşmediğini kontrol edeceğimiz sınır Y seviyesi

    public static void startBlockParty() {
        reset();
        isGameActive = true;
        isEliminationPhase = false;
        originalFloorBlocks.clear();

        FileConfiguration config = LbEvents.getInstance().getConfig();
        String worldName = config.getString("locations.pos1.world", config.getString("events.blockparty.world", "BlockParty"));
        World bpWorld = Bukkit.getWorld(worldName);
        if (bpWorld == null) bpWorld = Bukkit.getWorlds().get(0);
        final World finalWorld = bpWorld;

        // Oyun başlarken arenanın fotoğrafını çekip hafızaya alıyoruz
        saveArenaFloor(config, finalWorld);

        if (originalFloorBlocks.isEmpty()) {
            Bukkit.broadcast(Component.text("§c[HATA] BlockParty zemini bulunamadı! /lbevent setarea koordinatlarını kontrol et."));
            isGameActive = false;
            return;
        }

        // Renkleri config'den uydurmak yerine, DİREKT olarak zemindeki var olan renklerden seçtiriyoruz (Asla olmayan renk gelmez)
        List<Material> availableColors = new ArrayList<>(new HashSet<>(originalFloorBlocks.values()));
        Random random = new Random();

        gameTask = new BukkitRunnable() {
            int timer = 4; // İlk döngüde 3'e düşürüp başlatması için 4'ten başlatıyoruz

            @Override
            public void run() {
                if (!"blockparty".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent()) || !isGameActive) {
                    cancel();
                    return;
                }

                if (!isEliminationPhase) {
                    // AŞAMA 1: KOŞMA SÜRESİ
                    if (timer == 4) {
                        restoreArenaFloor(); // Kırılan tüm zeminleri eski haline (desenine) getir
                        currentSafeColor = availableColors.get(random.nextInt(availableColors.size()));
                        
                        String baseName = currentSafeColor.name().replace("_TERRACOTTA", "").replace("_WOOL", "");
                        String translatedColor = ChatColor.translateAlternateColorCodes('&', config.getString("colors." + currentSafeColor.name(), "&f&l" + baseName));

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getWorld().equals(finalWorld) && !eliminatedPlayers.contains(player.getUniqueId())) {
                                player.sendTitle(translatedColor, ChatColor.GRAY + "Bu renge koş!", 0, 40, 10);
                                player.sendMessage(Component.text("§e[BlockParty] §fHedef Renk: " + translatedColor));
                            }
                        }
                        timer = 3; // 3 Saniyelik koşma süresi
                    }

                    if (timer > 0) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getWorld().equals(finalWorld)) {
                                player.sendActionBar(Component.text("§bSıradaki Renk: §e" + timer + " §bsaniye"));
                            }
                        }
                        timer--;
                    } else {
                        // Süre doldu, eleme aşamasına geç
                        destroyWrongBlocks(currentSafeColor); // Yanlış blokları sil (Hava yap)
                        isEliminationPhase = true; // Artık düşenler elenecek
                        timer = 2; // Düşmeleri ve elenmeleri için 2 saniye bekleme süresi
                    }
                } else {
                    // AŞAMA 2: ELEME SÜRESİ (2 saniye boyunca zemin boştur)
                    if (timer > 0) {
                        timer--;
                    } else {
                        isEliminationPhase = false; // Eleme bitti
                        timer = 4; // Sonraki tur için döngüyü başa sar
                    }
                }
            }
        }.runTaskTimer(LbEvents.getInstance(), 0L, 20L); // Saniyede 1 kez çalışır
    }

    private static void saveArenaFloor(FileConfiguration config, World world) {
        String[] pathPrefixes = {"locations", "events.blockparty", "arenas.blockparty"};
        int lowestY = 300;

        for (String prefix : pathPrefixes) {
            if (config.contains(prefix + ".pos1") && config.contains(prefix + ".pos2")) {
                double x1 = config.getDouble(prefix + ".pos1.x");
                double y1 = config.getDouble(prefix + ".pos1.y");
                double z1 = config.getDouble(prefix + ".pos1.z");

                double x2 = config.getDouble(prefix + ".pos2.x");
                double y2 = config.getDouble(prefix + ".pos2.y");
                double z2 = config.getDouble(prefix + ".pos2.z");

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
                                if (y < lowestY) lowestY = y;
                            }
                        }
                    }
                }
                break;
            }
        }
        if (!originalFloorBlocks.isEmpty()) {
            floorMinY = lowestY; // Zeminin en alt noktasını belirle (düşüş kontrolü için)
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
        restoreArenaFloor(); // Oyun kapatılınca zemin havada kalmasın, geri gelsin
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

        // SADECE eleme aşamasındayken (bloklar silindiğinde) ve oyuncu zeminin ALTINA düştüyse ele.
        // Bu sayede koşma süresinde zıplamak veya yanlış bloğa basmak kimseyi öldürmez.
        if (isEliminationPhase) {
            if (player.getLocation().getY() < floorMinY) {
                eliminatedPlayers.add(player.getUniqueId());
                player.sendMessage(Component.text("§c[BlockParty] Sürede doğru renge ulaşamadın ve elendin!"));
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }
    }
}