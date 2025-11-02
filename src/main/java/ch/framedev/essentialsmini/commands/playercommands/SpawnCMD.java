/**
 * Dies ist ein Plugin von FrameDev
 * Bitte nichts §ndern, @Copyright by FrameDev
 */
package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.LocationsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author DHZoc
 */
public record SpawnCMD(Main plugin) implements CommandExecutor {

    public SpawnCMD(Main plugin) {
        this.plugin = plugin;
        plugin.getCommands().put("spawn", this);
        plugin.getCommands().put("setspawn", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (sender.hasPermission("essentialsmini.setspawn")) {
                if (sender instanceof Player) {
                    new LocationsManager("spawn").setLocation(((Player) sender).getLocation());
                    sender.sendMessage(plugin.getPrefix() + "§6Spawn §aset!");
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        }
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (sender.hasPermission(plugin.getPermissionBase() + "spawn")) {
                if (sender instanceof Player) {
                    try {
                        ((Player) sender).teleport(new LocationsManager("spawn").getLocation());
                        sender.sendMessage(plugin.getPrefix() + "§aTeleport to Spawn!");
                    } catch (Exception ignored) {
                        ((Player) sender).teleport(((Player) sender).getWorld().getSpawnLocation());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        }
        return false;
    }

}
