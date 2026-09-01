package com.craftlias.lbevents.listeners;

import com.craftlias.lbevents.LbEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SabotageListener implements Listener {
    private static final Map<UUID, UUID> sabotageTargets = new HashMap<>();
    private static final Set<UUID> saboteurs = new HashSet<>();

    public static void initializeSabotageGame() {
        sabotageTargets.clear();
        saboteurs.clear();

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() < 2) return;

        Collections.shuffle(players);
        int saboteurCount = Math.max(1, players.size() / 4);

        for (int i = 0; i < saboteurCount; i++) {
            Player saboteur = players.get(i);
            saboteurs.add(saboteur.getUniqueId());
            saboteur.sendMessage(ChatColor.RED + "⚠ Sen bir SABOTAJCINSIN! Sana atanan hedefi bul ve engelle!");
        }

        List<Player> workers = new ArrayList<>(players.subList(saboteurCount, players.size()));
        int workerIndex = 0;

        for (UUID sabID : saboteurs) {
            Player sab = Bukkit.getPlayer(sabID);
            if (sab == null) continue;
            
            Player target = workers.get(workerIndex % workers.size());
            sabotageTargets.put(sabID, target.getUniqueId());
            
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
            sab.sendMessage(ChatColor.YELLOW + "Hedefin: " + ChatColor.GOLD + target.getName());
            workerIndex++;
        }
    }

    public static void clearSabotage() {
        for (UUID targetID : sabotageTargets.values()) {
            Player target = Bukkit.getPlayer(targetID);
            if (target != null) {
                target.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
        sabotageTargets.clear();
        saboteurs.clear();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!"sabotage".equalsIgnoreCase(LbEvents.getInstance().getEventManager().getActiveEvent())) return;
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        if (killer != null && saboteurs.contains(killer.getUniqueId())) {
            UUID assignedTarget = sabotageTargets.get(killer.getUniqueId());
            if (assignedTarget != null && assignedTarget.equals(victim.getUniqueId())) {
                killer.sendMessage(ChatColor.GREEN + "Tebrikler! Atandığın hedefi başarıyla etkisiz hale getirdin.");
                Bukkit.broadcastMessage(ChatColor.RED + "⚠ Sabotajcı " + killer.getName() + ", hedefi olan " + victim.getName() + "'i avladı!");
            }
        }
    }
}