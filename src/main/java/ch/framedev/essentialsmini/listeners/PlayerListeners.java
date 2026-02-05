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

@SuppressWarnings("ConstantValue")
public class PlayerListeners implements Listener {

    private final Main plugin;
    private BukkitTask spawnTask;

    public PlayerListeners(Main plugin) {
        this.plugin = plugin;
        plugin.getListeners().add(this);
    }

    @EventHandler
    public void onColorChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("ColoredChat")) return;
        String message = event.getMessage();
        if (message == null || message.isEmpty()) return;

        if (message.contains("&"))
            message = message.replace('&', '§');

        for (String color : colorList) {
            if (message.contains("%" + color + "%")) {
                try {
                    ChatColor chatColor = ChatColor.valueOf(color.toUpperCase());
                    message = message.replace("%" + color + "%", chatColor + "");
                } catch (IllegalArgumentException ignored) {
                    // Skip invalid color names
                }
            }
        }
        event.setMessage(message);
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
        if (!plugin.getConfig().getBoolean("ColoredSigns")) return;

        String[] lines = event.getLines();
        if (lines == null) return;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line != null && line.contains("&")) {
                line = line.replace('&', '§');
                event.setLine(i, line);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (!VanishCMD.hided.contains(player.getName())) {
            if (plugin.getConfig().getBoolean("JoinBoolean")) {
                if (plugin.getConfig().getBoolean("IgnoreJoinLeave")) {
                    if (player.hasPermission("essentialsmini.ignorejoin")) {
                        event.setJoinMessage(null);
                    } else {
                        String joinMessage = plugin.getConfig().getString("JoinMessage");
                        if (joinMessage != null) {
                            if (joinMessage.contains("&"))
                                joinMessage = joinMessage.replace('&', '§');
                            if (joinMessage.contains("%Player%"))
                                joinMessage = joinMessage.replace("%Player%", player.getName());
                            event.setJoinMessage(joinMessage);
                        }
                    }
                } else {
                    String joinMessage = plugin.getConfig().getString("JoinMessage");
                    if (joinMessage != null) {
                        if (joinMessage.contains("&"))
                            joinMessage = joinMessage.replace('&', '§');
                        if (joinMessage.contains("%Player%"))
                            joinMessage = joinMessage.replace("%Player%", player.getName());
                        event.setJoinMessage(joinMessage);
                    }
                }
            }
        } else {
            event.setJoinMessage(null);
        }

        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (plugin.getConfig().getBoolean("SpawnTP")) {
                    if (player.getWorld() == null) {
                        cancel();
                        return;
                    }

                    List<String> ignoredWorlds = plugin.getConfig().getStringList("spawnTpIgnoreWorlds");
                    for (String world : ignoredWorlds) {
                        if (player.getWorld().getName().equalsIgnoreCase(world)) {
                            cancel();
                            return;
                        }
                    }

                    try {
                        LocationsManager spawnLocation = new LocationsManager("spawn");
                        player.teleport(spawnLocation.getLocation());
                    } catch (IllegalArgumentException | NullPointerException ex) {
                        if (player.getWorld() != null) {
                            player.teleport(player.getWorld().getSpawnLocation());
                        }
                    }
                }
                cancel();
            }
        }.runTaskLater(plugin, 20);

        if (plugin.getVaultManager() != null && plugin.getVaultManager().getEco() != null) {
            if (plugin.isMongoDB()) {
                if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                    if (plugin.getVaultManager().getEco().hasAccount(player)) {
                        String collection = "essentialsmini_data";
                        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().getBackendManager() != null) {
                            plugin.getDatabaseManager().getBackendManager().updateUser(
                                player,
                                BackendManager.DATA.MONEY.getName(),
                                plugin.getVaultManager().getEco().getBalance(player),
                                collection
                            );
                        }
                    }
                }
            }
            plugin.getVaultManager().getEco().createPlayerAccount(player);

            if (!player.hasPlayedBefore()) {
                if (plugin.getConfig().getBoolean("StartBalance.Boolean")) {
                    double startBalance = plugin.getConfig().getDouble("StartBalance.Amount");
                    plugin.getVaultManager().getEco().depositPlayer(player, startBalance);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (!VanishCMD.hided.contains(player.getName())) {
            if (plugin.getConfig().getBoolean("LeaveBoolean")) {
                if (plugin.getConfig().getBoolean("IgnoreJoinLeave")) {
                    if (player.hasPermission("essentialsmini.ignoreleave")) {
                        event.setQuitMessage(null);
                    } else {
                        String leaveMessage = plugin.getConfig().getString("LeaveMessage");
                        if (leaveMessage != null) {
                            if (leaveMessage.contains("&"))
                                leaveMessage = leaveMessage.replace('&', '§');
                            if (leaveMessage.contains("%Player%"))
                                leaveMessage = leaveMessage.replace("%Player%", player.getName());
                            event.setQuitMessage(leaveMessage);
                        }
                    }
                } else {
                    String leaveMessage = plugin.getConfig().getString("LeaveMessage");
                    if (leaveMessage != null) {
                        if (leaveMessage.contains("&"))
                            leaveMessage = leaveMessage.replace('&', '§');
                        if (leaveMessage.contains("%Player%"))
                            leaveMessage = leaveMessage.replace("%Player%", player.getName());
                        event.setQuitMessage(leaveMessage);
                    }
                }
            }
        } else {
            event.setQuitMessage(null);
        }
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("PlayerEvents")) return;
        if (event == null || event.getEntity() == null) return;

        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            Bukkit.getPluginManager().callEvent(
                new PlayerKillEntityEvent(killer, event.getEntity(), event.getDrops(), event.getDroppedExp())
            );
        }
    }

    /**
     * @param event Respawn event {@link SpawnCMD}
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event == null || event.getPlayer() == null) return;

        try {
            LocationsManager spawnManager = new LocationsManager("spawn");
            if (event.getPlayer().getRespawnLocation() == null ||
                (event.getPlayer().getRespawnLocation().equals(spawnManager.getLocation()) && !event.isBedSpawn())) {
                event.setRespawnLocation(spawnManager.getLocation());
            }
        } catch (Exception ex) {
            if (event.getPlayer().getWorld() != null) {
                event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event == null || event.getEntity() == null) return;

        if (KillCMD.suicidPlayers.contains(event.getEntity())) {
            event.setDeathMessage(null);
            KillCMD.suicidPlayers.remove(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("PlayerEvents")) return;
        if (event == null || event.getMessage() == null) return;

        if (event.getMessage().contains("/clear")) {
            getServer().getPluginManager().callEvent(
                new PlayerInventoryClearEvent(event.getPlayer(), event.getPlayer().getInventory())
            );
        }
    }

    @EventHandler
    public void onHitByArrow(ProjectileHitEvent event) {
        if (!plugin.getConfig().getBoolean("PlayerEvents")) return;
        if (event == null || event.getEntity() == null) return;
        if (event.getHitBlock() != null) return;
        if (event.getHitEntity() == null) return;
        if (event.getEntity().getShooter() == null) return;

        // Player hit by projectile event
        if (event.getHitEntity() instanceof Player && event.getEntity().getShooter() instanceof Entity) {
            getServer().getPluginManager().callEvent(
                new PlayerHitByProjectileEvent((Player) event.getHitEntity(), (Entity) event.getEntity().getShooter())
            );
        }

        // General entity hit by projectile event
        if (event.getEntity().getShooter() instanceof Entity) {
            getServer().getPluginManager().callEvent(
                new EntityHitByProjectileEvent(event.getHitEntity(), (Entity) event.getEntity().getShooter())
            );
        }
    }

    @SuppressWarnings("unused")
    public BukkitTask getSpawnTask() {
        return spawnTask;
    }
}
