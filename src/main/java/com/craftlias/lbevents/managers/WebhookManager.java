package com.craftlias.lbevents.managers;

import com.craftlias.lbevents.LbEvents;
import org.bukkit.Bukkit;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookManager {
    public static void sendWebhook(String eventName, String status) {
        LbEvents plugin = LbEvents.getInstance();
        if (!plugin.getConfig().getBoolean("settings.discord-webhook.enabled", false)) return;
        String urlStr = plugin.getConfig().getString("settings.discord-webhook.url", "");
        if (urlStr.isEmpty() || urlStr.contains("BURAYA")) return;

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                URI uri = new URI(urlStr);
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = "{\"content\": \"**LbEvents Bilgi:** `" + eventName + "` etkinliği " + status + "!\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                conn.getResponseCode();
            } catch (Exception e) {
                plugin.getLogger().warning("Webhook gönderilemedi: " + e.getMessage());
            }
        });
    }
}