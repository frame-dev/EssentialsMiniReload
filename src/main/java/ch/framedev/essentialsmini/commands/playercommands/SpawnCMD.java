/**
 * Dies ist ein Plugin von FrameDev
 * Bitte nichts §ndern, @Copyright by FrameDev
 */
package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.LocationManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * @author DHZoc
 */
public record SpawnCMD(Main plugin) implements CommandExecutor {

    private static final String SPAWN = "spawn";
    private static final String SET_SPAWN = "setspawn";

    public SpawnCMD(Main plugin) {
        this.plugin = plugin;
        plugin.getCommands().put(SPAWN, this);
        plugin.getCommands().put(SET_SPAWN, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case SET_SPAWN -> handleSetSpawn(sender);
            case SPAWN -> handleSpawn(sender);
            default -> false;
        };
    }

    private boolean handleSetSpawn(CommandSender sender) {
        if (!hasPermission(sender, SET_SPAWN)) return true;

        Player player = requirePlayer(sender);
        if (player == null) return true;

        new LocationManager(SPAWN).setLocation(player.getLocation());
        send(sender, "§6Spawn §aset!");
        return true;
    }

    private boolean handleSpawn(CommandSender sender) {
        if (!hasPermission(sender, SPAWN)) return true;

        Player player = requirePlayer(sender);
        if (player == null) return true;

        Location location = new LocationManager(SPAWN).getLocation();
        if (location == null) {
            location = player.getWorld().getSpawnLocation();
        }

        player.teleport(location);
        send(sender, "§aTeleport to Spawn!");
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permissionSuffix) {
        if (sender.hasPermission(plugin.getPermissionBase() + permissionSuffix)) return true;

        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }

        send(sender, plugin.getOnlyPlayer(null));
        return null;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

}
