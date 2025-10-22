package ch.framedev.essentialsmini.listeners;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 18.08.2020 22:47
 */

import ch.framedev.essentialsmini.api.events.*;
import ch.framedev.essentialsmini.commands.playercommands.KillCMD;
import ch.framedev.essentialsmini.commands.playercommands.SpawnCMD;
import ch.framedev.essentialsmini.commands.playercommands.VanishCMD;
import ch.framedev.essentialsmini.database.mongodb.BackendManager;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.LocationsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class PlayerListeners implements Listener {

    private final Main plugin;
    private BukkitTask spawnTask;

    public PlayerListeners(Main plugin) {
        this.plugin = plugin;
        plugin.getListeners().add(this);
    }

    @EventHandler
    public void onColorChat(AsyncPlayerChatEvent event) {
        if (plugin.getConfig().getBoolean("ColoredChat")) {
            String message = event.getMessage();
            if (message.contains("&"))
                message = message.replace('&', '§');
            for(String color : colorList) {
                if (message.contains("%" + color + "%")) {
                    message = message.replace("%" + color + "%", ChatColor.valueOf(color.toUpperCase()) + "");
                }
            }
            event.setMessage(message);
        }
    }

    private final List<String> colorList = List.of(
            "aqua",
            "black",
            "blue",
            "fuchsia",
            "gray",
            "green",
            "lime",
            "maroon",
            "navy",
            "olive",
            "orange",
            "purple",
            "red",
            "silver",
            "teal",
            "white",
            "yellow"
    );

    @EventHandler
    public void onSignColor(SignChangeEvent event) {
        if (plugin.getConfig().getBoolean("ColoredSigns")) {
            for (int i = 0; i < event.getLines().length; i++) {
                if (event.getLines()[i].contains("&")) {
                    String line = event.getLines()[i];
                    line = line.replace('&', '§');
                    event.setLine(i, line);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        System.out.println("join");
        if (!VanishCMD.hided.contains(event.getPlayer().getName())) {
            if (plugin.getConfig().getBoolean("JoinBoolean")) {
                if (plugin.getConfig().getBoolean("IgnoreJoinLeave")) {
                    if (event.getPlayer().hasPermission("essentialsmini.ignorejoin")) {
                        event.setJoinMessage(null);
                    } else {
                        String joinMessage = plugin.getConfig().getString("JoinMessage");
                        if (joinMessage == null) return;
                        if (joinMessage.contains("&"))
                            joinMessage = joinMessage.replace('&', '§');
                        if (joinMessage.contains("%Player%"))
                            joinMessage = joinMessage.replace("%Player%", event.getPlayer().getName());
                        event.setJoinMessage(joinMessage);
                    }
                } else {
                    String joinMessage = plugin.getConfig().getString("JoinMessage");
                    if (joinMessage == null) return;
                    if (joinMessage.contains("&"))
                        joinMessage = joinMessage.replace('&', '§');
                    if (joinMessage.contains("%Player%"))
                        joinMessage = joinMessage.replace("%Player%", event.getPlayer().getName());
                    event.setJoinMessage(joinMessage);
                }
            }
        } else {
            event.setJoinMessage(null);
        }
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getConfig().getBoolean("SpawnTP")) {
                    List<String> ignoredWorlds = plugin.getConfig().getStringList("spawnTpIgnoreWorlds");
                    for(String world : ignoredWorlds) {
                        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(world)) {
                            return;
                        }
                    }
                    LocationsManager spawnLocation = new LocationsManager("spawn");
                    try {
                        event.getPlayer().teleport(spawnLocation.getLocation());
                    } catch (IllegalArgumentException ex) {
                        event.getPlayer().teleport(event.getPlayer().getWorld().getSpawnLocation());
                    }
                }
                cancel();

            }
        }.runTaskLater(plugin, 20);
        if (plugin.getVaultManager() != null && plugin.getVaultManager().getEco() != null) {
            if (plugin.isMongoDB()) {
                if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                    if (plugin.getVaultManager().getEco().hasAccount(event.getPlayer())) {
                        String collection = "essentialsmini_data";
                        plugin.getDatabaseManager().getBackendManager().updateUser(event.getPlayer(), BackendManager.DATA.MONEY.getName(), plugin.getVaultManager().getEco().getBalance(event.getPlayer()), collection);
                    }
                }
            }
            plugin.getVaultManager().getEco().createPlayerAccount(event.getPlayer());
        }
        if (!event.getPlayer().hasPlayedBefore()) {
            if (plugin.getConfig().getBoolean("StartBalance.Boolean")) {
                double startBalance = plugin.getConfig().getDouble("StartBalance.Amount");
                plugin.getVaultManager().getEco().depositPlayer(event.getPlayer(), startBalance);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!VanishCMD.hided.contains(event.getPlayer().getName())) {
            if (plugin.getConfig().getBoolean("LeaveBoolean")) {
                if (plugin.getConfig().getBoolean("IgnoreJoinLeave")) {
                    if (event.getPlayer().hasPermission("essentialsmini.ignoreleave")) {
                        event.setQuitMessage(null);
                    } else {
                        String joinMessage = plugin.getConfig().getString("LeaveMessage");
                        if (joinMessage == null) return;
                        if (joinMessage.contains("&"))
                            joinMessage = joinMessage.replace('&', '§');
                        if (joinMessage.contains("%Player%"))
                            joinMessage = joinMessage.replace("%Player%", event.getPlayer().getName());
                        event.setQuitMessage(joinMessage);
                    }
                } else {
                    String joinMessage = plugin.getConfig().getString("LeaveMessage");
                    if (joinMessage == null) return;
                    if (joinMessage.contains("&"))
                        joinMessage = joinMessage.replace('&', '§');
                    if (joinMessage.contains("%Player%"))
                        joinMessage = joinMessage.replace("%Player%", event.getPlayer().getName());
                    event.setQuitMessage(joinMessage);
                }
            }
        } else {
            event.setQuitMessage(null);
        }
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        if (plugin.getConfig().getBoolean("PlayerEvents")) {
            if (event.getEntity().getKiller() != null) {
                event.getEntity();// getServer().getPluginManager().callEvent(new PlayerKillPlayerEvent((Player) event.getEntity(), event.getEntity().getKiller(), event.getDrops(), event.getDroppedExp()));
                Bukkit.getPluginManager().callEvent(new PlayerKillEntityEvent(event.getEntity().getKiller(), event.getEntity(), event.getDrops(), event.getDroppedExp()));
            }
        }
    }

    /**
     * @param event Respawn event {@link SpawnCMD}
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        try {
            if (event.getPlayer().getRespawnLocation() == null || event.getPlayer().getRespawnLocation().equals(new LocationsManager("spawn").getLocation()) && !event.isBedSpawn())
                event.setRespawnLocation(new LocationsManager("spawn").getLocation());
        } catch (Exception ignored) {
            event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (KillCMD.suicidPlayers.contains(event.getEntity())) {
            event.setDeathMessage(null);
            KillCMD.suicidPlayers.remove(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.getConfig().getBoolean("PlayerEvents"))
            if (event.getMessage().contains("/clear")) {
                getServer().getPluginManager().callEvent(new PlayerInventoryClearEvent(event.getPlayer(), event.getPlayer().getInventory()));
            }
    }

    @EventHandler
    public void onHitByArrow(ProjectileHitEvent event) {
        if (plugin.getConfig().getBoolean("PlayerEvents")) {
            if (event.getHitBlock() != null) return;
            if (event.getHitEntity() == null) return;
            if (event.getEntity().getShooter() == null) return;
            if (event.getHitEntity() != null && event.getHitEntity() instanceof Player && event.getEntity().getShooter() != null) {
                if (event.getEntity().getShooter() instanceof Entity)
                    getServer().getPluginManager().callEvent(new PlayerHitByProjectileEvent((Player) event.getHitEntity(), (Entity) event.getEntity().getShooter()));
            }
            if (event.getHitEntity() != null && event.getEntity().getShooter() != null) {
                getServer().getPluginManager().callEvent(new EntityHitByProjectileEvent(event.getHitEntity(), (Entity) event.getEntity().getShooter()));
            }
        }
    }

    @SuppressWarnings("unused")
    public BukkitTask getSpawnTask() {
        return spawnTask;
    }
}
