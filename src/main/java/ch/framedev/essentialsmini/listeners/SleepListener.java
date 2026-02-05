package ch.framedev.essentialsmini.listeners;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 10.08.2020 12:41
 */

import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TextUtils;
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
        if (event == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("SkipNight")) return;

        long time = event.getPlayer().getWorld().getTime();
        boolean isNight = time >= 12542 && time <= 23460;
        boolean isThundering = event.getPlayer().getWorld().isThundering();

        if (!isNight && !isThundering) {
            return;
        }

        if (sleep) {
            event.setUseBed(Event.Result.DENY);
            return;
        }
        sleep = true;
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new RunSkipNight(event), 120);
    }

    private class RunSkipNight implements Runnable {

        private final PlayerBedEnterEvent event;

        public RunSkipNight(PlayerBedEnterEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            if (event == null) {
                sleep = false;
                return;
            }

            String message = plugin.getLanguageConfig(event.getPlayer()).getString("SkipNight");
            if (message != null) {
                message = new TextUtils().replaceAndWithParagraph(message);
                message = new TextUtils().replaceObject(message, "%Player%", event.getPlayer().getName());
                Bukkit.broadcastMessage(message);
            }

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
    }
}
