package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 11.08.2020 23:04
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemCMD extends CommandBase {

    private final Main plugin;

    public ItemCMD(Main plugin) {
        super(plugin, "item");
        setupTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission(new Permission("essentialsmini.item", PermissionDefault.OP))) {
            if (args.length == 1) {
                if (sender instanceof Player player) {
                    if (player.hasPermission(new Permission("essentialsmini.item", PermissionDefault.OP))) {
                        String name = args[0];
                        Material material = Material.getMaterial(name.toUpperCase());
                        if (material != null) {
                            player.getInventory().addItem(new ItemStack(material));
                            String message = plugin.getLanguageConfig(player).getString("Item.Get");
                            if (message == null) {
                                player.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Get' not found! Please contact the Admin!");
                                return true;
                            }
                            message = ReplaceCharConfig.replaceParagraph(message);
                            message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                            message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", "" + 1);
                            player.sendMessage(plugin.getPrefix() + message);
                        } else {
                            String message = plugin.getLanguageConfig(player).getString("Item.notFound");
                            if(message == null) {
                                player.sendMessage(plugin.getPrefix() + "§cConfig 'Item.notFound' not found! Please contact the Admin!");
                                return true;
                            }
                            message = message.replace("&", "§");
                            message = message.replace("%Item%", name);
                            player.sendMessage(plugin.getPrefix() + message);
                        }
                    } else {
                        player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            } else if (args.length == 2) {
                try {
                    if (args[1].equalsIgnoreCase(String.valueOf(Integer.parseInt(args[1])))) {
                        if (sender instanceof Player player) {
                            if (player.hasPermission(new Permission("essentialsmini.item", PermissionDefault.OP))) {
                                String name = args[0];
                                Material material = Material.getMaterial(name.toUpperCase());
                                if (material != null) {
                                    int amount = Integer.parseInt(args[1]);
                                    player.getInventory().addItem(new ItemStack(material, amount));
                                    if (!Main.getSilent().contains(sender.getName())) {
                                        String message = plugin.getLanguageConfig(player).getString("Item.Get");
                                        if (message == null) {
                                            player.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Get' not found! Please contact the Admin!");
                                            return true;
                                        }
                                        message = ReplaceCharConfig.replaceParagraph(message);
                                        message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                                        message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", "" + amount);
                                        player.sendMessage(plugin.getPrefix() + message);
                                    }
                                } else {
                                    player.sendMessage(plugin.getPrefix() + "§cDieses Item existiert nicht! §6" + name);
                                }
                            } else {
                                player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                            }
                        } else {
                            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                        }
                    }
                } catch (NumberFormatException ignored) {
                    if (sender.hasPermission(new Permission("essentialsmini.item", PermissionDefault.OP))) {
                        String name = args[0];
                        Material material = Material.getMaterial(name.toUpperCase());
                        if (material != null) {
                            Player player1 = Bukkit.getPlayer(args[1]);
                            if (player1 != null) {
                                player1.getInventory().addItem(new ItemStack(material));
                                String message = plugin.getLanguageConfig(sender).getString("Item.Other");
                                if (message == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Other' not found! Please contact the Admin!");
                                    return true;
                                }
                                message = ReplaceCharConfig.replaceParagraph(message);
                                message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                                message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", player1.getName());
                                message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", "" + 1);
                                sender.sendMessage(plugin.getPrefix() + message);
                            } else {
                                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                            }
                        } else {
                            sender.sendMessage(plugin.getPrefix() + "§cDieses Item existiert nicht! §6" + name);
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                }
            } else if (args.length == 3) {
                if (sender.hasPermission(new Permission("essentialsmini.item", PermissionDefault.OP))) {
                    String name = args[0];
                    Material material = Material.getMaterial(name.toUpperCase());
                    if (material != null) {
                        int amount = Integer.parseInt(args[1]);
                        Player player1 = Bukkit.getPlayer(args[2]);
                        if (player1 != null) {
                            player1.getInventory().addItem(new ItemStack(material, amount));
                            String message = plugin.getLanguageConfig(sender).getString("Item.Other");
                            if (message == null) {
                                sender.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Other' not found! Please contact the Admin!");
                                return true;
                            }
                            message = ReplaceCharConfig.replaceParagraph(message);
                            message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                            message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", player1.getName());
                            message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", "" + amount);
                            sender.sendMessage(plugin.getPrefix() + message);
                        } else {
                            sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[2]));
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + "§cDieses Item existiert nicht! §6" + name);
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item>"));
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <SpielerName>"));
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl>"));
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl> <SpielerName>"));
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("essentialsmini.item")) {
                ArrayList<String> empty = new ArrayList<>();
                List<Material> materials = Arrays.stream(Material.values()).toList();
                for (Material material : materials) {
                    if (material.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                        empty.add(material.name());
                    }
                }
                Collections.sort(empty);
                return empty;
            }
        }
        return null;
    }
}
