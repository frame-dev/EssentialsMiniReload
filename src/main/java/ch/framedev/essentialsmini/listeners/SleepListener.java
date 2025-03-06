package ch.framedev.essentialsmini.listeners;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 10.08.2020 12:41
 */

import ch.framedev.essentialsmini.main.Main;
import ch.framedev.simplejavautils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SleepListener implements Listener {

    private final Main plugin;
    private boolean sleep;

    public SleepListener(Main plugin) {
        this.plugin = plugin;
        plugin.getListeners().add(this);
    }

    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        if (plugin.getConfig().getBoolean("SkipNight")) {
            if (event.getPlayer().getWorld().getTime() >= 12542 && event.getPlayer().getWorld().getTime() <= 23460 || event.getPlayer().getWorld().isThundering()) {
                if (!sleep) {
                    sleep = true;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            String message = plugin.getLanguageConfig(event.getPlayer()).getString("SkipNight");
                            message = new TextUtils().replaceAndWithParagraph(message);
                            message = new TextUtils().replaceObject(message, "%Player%", event.getPlayer().getName());
                            Bukkit.broadcastMessage(message);
                            event.getPlayer().getWorld().setTime(0);
                            event.getPlayer().getWorld().setThundering(false);
                            event.getPlayer().getWorld().setStorm(false);
                            event.setCancelled(true);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    sleep = false;
                                }
                            }.runTaskLater(plugin, 320);
                        }
                    }.runTaskLater(plugin, 120);
                } else {
                    event.setUseBed(Event.Result.DENY);
                }
            }
        }
    }
}
