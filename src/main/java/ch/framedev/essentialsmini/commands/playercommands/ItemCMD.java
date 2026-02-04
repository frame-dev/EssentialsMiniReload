package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ItemCMD extends CommandBase {

    private final Main plugin;

    public ItemCMD(Main plugin) {
        super(plugin, "item");
        setupTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // use permission base for consistency (e.g. essentialsmini.)
        if (!sender.hasPermission(plugin.getPermissionBase() + "item")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }
            String name = args[0].trim();
            Material material = Material.matchMaterial(name);
            if (material == null) {
                String message = plugin.getLanguageConfig(player).getString("Item.notFound");
                if (message == null) {
                    player.sendMessage(plugin.getPrefix() + "§cConfig 'Item.notFound' not found! Please contact the Admin!");
                } else {
                    message = ReplaceCharConfig.replaceParagraph(message);
                    message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                    player.sendMessage(plugin.getPrefix() + message);
                }
                return true;
            }
            player.getInventory().addItem(new ItemStack(material));
            String message = plugin.getLanguageConfig(player).getString("Item.Get");
            if (message == null) {
                player.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Get' not found! Please contact the Admin!");
            } else {
                message = ReplaceCharConfig.replaceParagraph(message);
                message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", "1");
                player.sendMessage(plugin.getPrefix() + message);
            }
            return true;
        }

        if (args.length == 2) {
            String name = args[0].trim();
            Material material = Material.matchMaterial(name);
            if (material == null) {
                sender.sendMessage(plugin.getPrefix() + "§cDieses Item existiert nicht! §6" + name);
                return true;
            }
            try {
                int amount = Integer.parseInt(args[1]);
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                ItemStack stack = new ItemStack(material, Math.max(1, Math.min(amount, 64)));
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
                if (!leftover.isEmpty()) {
                    // Drop leftovers at player's location
                    leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                    player.sendMessage(plugin.getPrefix() + "§cYour inventory was full; extra items were dropped on the ground.");
                }
                if (!Main.getSilent().contains(sender.getName())) {
                    String message = plugin.getLanguageConfig(player).getString("Item.Get");
                    if (message == null) {
                        player.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Get' not found! Please contact the Admin!");
                    } else {
                        message = ReplaceCharConfig.replaceParagraph(message);
                        message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                        message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", String.valueOf(amount));
                        player.sendMessage(plugin.getPrefix() + message);
                    }
                }
            } catch (NumberFormatException e) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                    return true;
                }
                Map<Integer, ItemStack> leftoverTarget = target.getInventory().addItem(new ItemStack(material));
                if (!leftoverTarget.isEmpty()) {
                    leftoverTarget.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                    sender.sendMessage(plugin.getPrefix() + "§cTarget's inventory was full; extra items were dropped on the ground.");
                }
                String message = plugin.getLanguageConfig(sender).getString("Item.Other");
                if (message == null) {
                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Other' not found! Please contact the Admin!");
                } else {
                    message = ReplaceCharConfig.replaceParagraph(message);
                    message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                    message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", target.getName());
                    message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", "1");
                    sender.sendMessage(plugin.getPrefix() + message);
                }
            }
            return true;
        }

        if (args.length == 3) {
            String name = args[0].trim();
            Material material = Material.matchMaterial(name);
            if (material == null) {
                sender.sendMessage(plugin.getPrefix() + "§cDieses Item existiert nicht! §6" + name);
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl> <SpielerName>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[2]));
                return true;
            }
            ItemStack stack3 = new ItemStack(material, Math.max(1, Math.min(amount, 64)));
            Map<Integer, ItemStack> leftover3 = target.getInventory().addItem(stack3);
            if (!leftover3.isEmpty()) {
                leftover3.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                sender.sendMessage(plugin.getPrefix() + "§cTarget's inventory was full; extra items were dropped on the ground.");
            }
            String message = plugin.getLanguageConfig(sender).getString("Item.Other");
            if (message == null) {
                sender.sendMessage(plugin.getPrefix() + "§cConfig 'Item.Other' not found! Please contact the Admin!");
            } else {
                message = ReplaceCharConfig.replaceParagraph(message);
                message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", name);
                message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", target.getName());
                message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", String.valueOf(amount));
                sender.sendMessage(plugin.getPrefix() + message);
            }
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item>"));
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <SpielerName>"));
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl>"));
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl> <SpielerName>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1 && sender.hasPermission(plugin.getPermissionBase() + "item")) {
            List<String> matches = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                    matches.add(material.name());
                }
            }
            Collections.sort(matches);
            return matches;
        }
        return null;
    }
}