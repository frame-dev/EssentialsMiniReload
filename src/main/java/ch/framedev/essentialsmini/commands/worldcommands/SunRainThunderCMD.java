package ch.framedev.essentialsmini.commands.worldcommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 14.08.2020 20:52
 */

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public record SunRainThunderCMD(Main plugin) implements CommandExecutor {

    public SunRainThunderCMD(Main plugin) {
        this.plugin = plugin;
        plugin.getCommands().put("sun", this);
        plugin.getCommands().put("rain", this);
        plugin.getCommands().put("thunder", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("sun")) {
                String message = plugin.getLanguageConfig(player).getString("WeatherSun");
                if (message == null) {
                    player.sendMessage(plugin.getPrefix() + "§cConfig 'WeatherSun' not found! Please contact the Admin!");
                    return true;
                }
                if (message.contains("&"))
                    message = message.replace('&', '§');
                if (message.contains("%World%")) {
                    message = message.replace("%World%", player.getWorld().getName());
                }
                if (player.hasPermission(plugin.getPermissionBase() + "sun")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.getWorld().setStorm(false);
                            player.getWorld().setThundering(false);
                        }
                    }.runTaskLater(plugin, 60);
                    player.sendMessage(plugin.getPrefix() + message);
                } else {
                    player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
            if (command.getName().equalsIgnoreCase("rain")) {
                if (player.hasPermission(plugin.getPermissionBase() + "rain")) {
                    String message = plugin.getLanguageConfig(player).getString("WeatherRain");
                    if (message == null) {
                        player.sendMessage(plugin.getPrefix() + "§cConfig 'WeatherRain' not found! Please contact the Admin!");
                        return true;
                    }
                    if (message.contains("%World%"))
                        message = message.replace("%World%", player.getWorld().getName());
                    if (message.contains("&"))
                        message = message.replace('&', '§');

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.getWorld().setStorm(true);
                        }
                    }.runTaskLater(plugin, 60);
                    player.sendMessage(plugin.getPrefix() + message);
                } else {
                    player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
            if (command.getName().equalsIgnoreCase("thunder")) {
                if (player.hasPermission(plugin.getPermissionBase() + "thunder")) {
                    String message = plugin.getLanguageConfig(player).getString("WeatherThunder");
                    if (message == null) {
                        player.sendMessage(plugin.getPrefix() + "§cConfig 'WeatherThunder' not found! Please contact the Admin!");
                        return true;
                    }
                    if (message.contains("%World%"))
                        message = message.replace("%World%", player.getWorld().getName());
                    if (message.contains("&"))
                        message = message.replace('&', '§');

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.getWorld().setStorm(true);
                            player.getWorld().setThundering(true);
                        }
                    }.runTaskLater(plugin, 60);
                    player.sendMessage(plugin.getPrefix() + message);
                } else {
                    player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
        } else {
            if (command.getName().equalsIgnoreCase("sun")) {
                String message = "sun";
                if (sender.hasPermission(plugin.getPermissionBase() + "sun")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getWorlds().forEach(world -> {
                                if (world.getEnvironment() == World.Environment.NORMAL) {
                                    world.setStorm(false);
                                    world.setThundering(false);
                                }
                            });
                        }
                    }.runTaskLater(plugin, 60);
                    sender.sendMessage(plugin.getPrefix() + message);
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
            if (command.getName().equalsIgnoreCase("rain")) {
                if (sender.hasPermission(plugin.getPermissionBase() + "rain")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Bukkit.getWorlds().forEach(world -> {
                                        if (world.getEnvironment() == World.Environment.NORMAL)
                                            world.setStorm(true);
                                    });
                                }
                            }.runTaskLater(plugin, 60);
                        }
                    }.runTaskLater(plugin, 60);
                    sender.sendMessage(plugin.getPrefix() + "Rain");
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
            if (command.getName().equalsIgnoreCase("thunder")) {
                if (sender.hasPermission(plugin.getPermissionBase() + "thunder")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getWorlds().forEach(world -> {
                                if (world.getEnvironment() == World.Environment.NORMAL) {
                                    world.setStorm(true);
                                    world.setThundering(true);
                                }
                            });
                        }
                    }.runTaskLater(plugin, 60);
                    sender.sendMessage(plugin.getPrefix() + "Thunder");
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
        }
        return false;
    }
}
