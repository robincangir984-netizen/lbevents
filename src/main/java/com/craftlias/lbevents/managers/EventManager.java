package com.craftlias.lbevents.managers;

import com.craftlias.lbevents.listeners.BlockPartyListener;
import com.craftlias.lbevents.listeners.SabotageListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class EventManager {
    private String activeEvent = null;

    public boolean startEvent(String type) {
        if (activeEvent != null) return false;
        this.activeEvent = type.toLowerCase();

        if (this.activeEvent.equals("blockparty")) {
            BlockPartyListener.reset();
        } else if (this.activeEvent.equals("sabotage")) {
            SabotageListener.initializeSabotageGame();
        }

        Bukkit.broadcast(Component.text("§a" + type.toUpperCase() + " etkinliği başlatıldı!"));
        return true;
    }

    public boolean stopEvent() {
        if (activeEvent == null) return false;

        if (this.activeEvent.equals("sabotage")) {
            SabotageListener.clearSabotage();
        }

        Bukkit.broadcast(Component.text("§cAktif etkinlik sonlandırıldı!"));
        this.activeEvent = null;
        return true;
    }

    public String getActiveEvent() {
        return activeEvent;
    }
}