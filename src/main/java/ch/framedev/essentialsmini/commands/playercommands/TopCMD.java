package ch.framedev.essentialsmini.commands.playercommands;



/*
 * ch.framedev.essentialsmini.commands.playercommands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 19.07.2025 16:20
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TopCMD extends CommandBase {

    private final Main plugin;

    public TopCMD(Main plugin) {
        super(plugin, "top");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "top")) {
            player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return true;
        }

        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = player.getWorld().getHighestBlockYAt(x, z);
        Location topLoc = new Location(player.getWorld(), x + 0.5, y + 1, z + 0.5);

        player.teleport(topLoc);
        player.sendMessage(plugin.getPrefix() + "Teleported to the highest block at your location.");
        return true;
    }
}