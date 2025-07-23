package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InvseeCMD extends CommandBase {

    private final Main plugin;
    private final boolean seeOwner;

    public InvseeCMD(Main plugin) {
        super(plugin, "invsee", "enderchest");
        this.plugin = plugin;
        this.seeOwner = plugin.getConfig().getBoolean("Invsee.Owner");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("invsee")) {
            if (args.length == 1) {
                if (!sender.hasPermission("essentialsmini.invsee")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                    return true;
                }
                if (!seeOwner && target.hasPermission(plugin.getPermissionBase() + "invsee.owner")) {
                    // If the target player has the invsee.owner permission, the player cannot see their inventory
                    player.sendMessage(plugin.getPrefix() + "§cYou can't see this Inventory!");
                    return true;
                }
                player.openInventory(target.getInventory());
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/invsee <PlayerName>"));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("enderchest")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }
            if (args.length == 0) {
                if (!sender.hasPermission("essentialsmini.enderchest")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                player.openInventory(player.getEnderChest());
                return true;
            } else if (args.length == 1) {
                if (!sender.hasPermission("essentialsmini.enderchest.others")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                    return true;
                }
                if (!seeOwner && target.hasPermission(plugin.getPermissionBase() + "enderchest.owner")) {
                    player.sendMessage(plugin.getPrefix() + "§cYou can't see this EnderChest!");
                    return true;
                }
                player.openInventory(target.getEnderChest());
                return true;
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/ec"));
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/ec <PlayerName>"));
                return true;
            }
        }
        return false;
    }
}