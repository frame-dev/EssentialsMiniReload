package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 14.07.2020 16:35
 */
public class InvseeCMD extends CommandBase {

    private final Main plugin;
    private final boolean seeOwner;

    public InvseeCMD(Main plugin) {
        super(plugin, "invsee", "enderchest");
        this.plugin = plugin;
        this.seeOwner = plugin.getConfig().getBoolean("Invsee.Owner");
        //plugin.getCommands().put("resethealth", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("invsee")) {
            if (args.length == 1) {
                if (sender.hasPermission("essentialsmini.invsee")) {
                    if (sender instanceof Player player) {
                        Player target = Bukkit.getPlayer(args[0]);
                        if (target != null) {
                            if (!seeOwner) {
                                if (!target.hasPermission(plugin.getPermissionBase() + "invsee.owner")) {
                                    player.openInventory(target.getInventory());
                                } else {
                                    player.sendMessage(plugin.getPrefix() + "§cYou can't see this Inventory!");
                                }
                            } else {
                                player.openInventory(target.getInventory());
                            }
                        } else {
                            player.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/invsee <PlayerName>"));
            }
        }
        if (command.getName().equalsIgnoreCase("enderchest")) {
            if (args.length == 0) {
                if (sender.hasPermission("essentialsmini.enderchest")) {
                    if (sender instanceof Player player) {
                        player.openInventory(player.getEnderChest());
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else if (args.length == 1) {
                if (sender instanceof Player player) {
                    if (sender.hasPermission("essentialsmini.enderchest.others")) {
                        Player target = Bukkit.getPlayer(args[0]);
                        if (target != null) {
                            if (!target.hasPermission(plugin.getPermissionBase() + "enderchest.owner")) {
                                player.openInventory(target.getEnderChest());
                            }
                        } else {
                            player.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/ec"));
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/ec <PlayerName>"));
            }
        }
        return false;
    }
}
