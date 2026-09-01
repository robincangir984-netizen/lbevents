package com.craftlias.lbevents.commands;

import com.craftlias.lbevents.LbEvents;
import com.craftlias.lbevents.managers.EventManager;
import com.craftlias.lbevents.managers.WebhookManager;
import com.craftlias.lbevents.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        EventManager em = LbEvents.getInstance().getEventManager();

        if (args.length == 0) {
            sender.sendMessage(ColorUtil.colorize(LbEvents.getInstance().getConfig().getString("messages.prefix", "&eLbEvents ▸ ") + "&fKullanım: /lbevents <start|stop|reload|help|setarea>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("lbevents.reload") && !sender.hasPermission("lbevents.admin")) {
                    sender.sendMessage(ColorUtil.colorize(LbEvents.getInstance().getConfig().getString("messages.no-permission", "&cYetkiniz yok.")));
                    return true;
                }
                LbEvents.getInstance().reloadConfig();
                sender.sendMessage(ColorUtil.colorize(LbEvents.getInstance().getConfig().getString("messages.reload", "&aKonfigürasyon yenilendi!")));
                break;

            case "start":
                if (!sender.hasPermission("lbevents.start") && !sender.hasPermission("lbevents.admin")) {
                    sender.sendMessage(ColorUtil.colorize("&cYetkiniz yok."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.colorize("&cKullanım: /lbevents start <blockparty|mining|sabotage>"));
                    return true;
                }
                String eventType = args[1].toLowerCase();
                if (em.startEvent(eventType)) {
                    WebhookManager.sendWebhook(eventType, "başlatıldı");
                    sender.sendMessage(ColorUtil.colorize("&aEtkinlik başarıyla başlatıldı."));
                } else {
                    sender.sendMessage(ColorUtil.colorize("&cZaten devam eden bir etkinlik var!"));
                }
                break;

            case "stop":
                if (!sender.hasPermission("lbevents.stop") && !sender.hasPermission("lbevents.admin")) {
                    sender.sendMessage(ColorUtil.colorize("&cYetkiniz yok."));
                    return true;
                }
                String active = em.getActiveEvent();
                if (em.stopEvent()) {
                    if (active != null) WebhookManager.sendWebhook(active, "durduruldu");
                    sender.sendMessage(ColorUtil.colorize("&aAktif etkinlik durduruldu."));
                } else {
                    sender.sendMessage(ColorUtil.colorize("&cŞu an çalışan bir etkinlik yok!"));
                }
                break;

            case "setarea":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir.");
                    return true;
                }
                if (!sender.hasPermission("lbevents.admin")) {
                    sender.sendMessage(ColorUtil.colorize("&cYetkiniz yok."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.colorize("&cKullanım: /lbevents setarea <blockparty|sabotage>"));
                    return true;
                }
                sender.sendMessage(ColorUtil.colorize("&a" + args[1].toUpperCase() + " için bölge koordinatları kaydedildi!"));
                break;

            default:
                sender.sendMessage(ColorUtil.colorize("&cBöyle bir komut yok. /lbevents yazarak bilgi alın."));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("start");
            completions.add("stop");
            completions.add("reload");
            completions.add("help");
            completions.add("setarea");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("setarea"))) {
            completions.add("blockparty");
            completions.add("mining");
            completions.add("sabotage");
        }
        return completions;
    }
}