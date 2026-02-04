package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InvseeCMD extends CommandBase {

    private final Main plugin;
    // If true, admins are allowed to see inventories/enderchests of players who hold the
    // respective 'owner' permission. If false, such targets are protected from being viewed.
    private final boolean seeOwner;

    public InvseeCMD(Main plugin) {
        super(plugin, "invsee", "enderchest");
        setupTabCompleter(this);
        this.plugin = plugin;
        this.seeOwner = plugin.getConfig().getBoolean("Invsee.Owner");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("invsee")) {
            if (args.length == 1) {
                if (!sender.hasPermission(plugin.getPermissionBase() + "invsee")) {
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
                if (!seeOwner && target.hasPermission(plugin.getPermissionBase() + "invsee.owner") && !target.getUniqueId().equals(player.getUniqueId())) {
                    // If the target player has the invsee.owner permission, the player cannot see their inventory
                    player.sendMessage(plugin.getPrefix() + "§cYou can't see this Inventory!");
                    return true;
                }

                // Open the target's inventory for the command sender
                player.openInventory(target.getInventory());

                // Notify the sender and log the action if viewing someone else's inventory
                if (!player.getUniqueId().equals(target.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix() + "§aNow viewing inventory of §6" + target.getName());
                    try {
                        plugin.getLogger4J().info(player.getName() + " viewed inventory of " + target.getName());
                    } catch (Exception ignored) {
                    }
                }
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
                if (!sender.hasPermission(plugin.getPermissionBase() + "enderchest")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                player.openInventory(player.getEnderChest());
                return true;
            } else if (args.length == 1) {
                if (!sender.hasPermission(plugin.getPermissionBase() + "enderchest.others")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                    return true;
                }
                if (!seeOwner && target.hasPermission(plugin.getPermissionBase() + "enderchest.owner") && !target.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix() + "§cYou can't see this EnderChest!");
                    return true;
                }
                player.openInventory(target.getEnderChest());
                if (!player.getUniqueId().equals(target.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix() + "§aNow viewing EnderChest of §6" + target.getName());
                    try {
                        plugin.getLogger4J().info(player.getName() + " viewed enderchest of " + target.getName());
                    } catch (Exception ignored) {
                    }
                }
                return true;
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/ec"));
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/ec <PlayerName>"));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            ArrayList<String> matches = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) matches.add(p.getName());
            }
            Collections.sort(matches);
            return matches;
        }
        return super.onTabComplete(sender, command, label, args);
    }
}