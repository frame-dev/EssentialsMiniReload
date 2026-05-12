package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
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

    private static final int DEFAULT_AMOUNT = 1;
    private static final String MESSAGE_ITEM_GET = "Item.Get";
    private static final String MESSAGE_ITEM_OTHER = "Item.Other";
    private static final String MESSAGE_ITEM_NOT_FOUND = "Item.notFound";
    private static final String DEFAULT_ITEM_GET = "§aYou got the Item §6%Item%§a! Amount: §6%Amount%";
    private static final String DEFAULT_ITEM_OTHER = "§6%Player% §agot the Item §6%Item%§a! Amount: §6%Amount%";
    private static final String DEFAULT_ITEM_NOT_FOUND = "§cThis item does not exist! §6%Item%";

    private final Main plugin;

    public ItemCMD(Main plugin) {
        super(plugin, "item");
        setupTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!hasPermission(sender)) return true;

        if (args.length < 1 || args.length > 3) {
            sendUsage(sender);
            return true;
        }

        Material material = matchItem(sender, args[0]);
        if (material == null) return true;

        ItemRequest request = parseRequest(sender, args, material);
        if (request == null) return true;

        giveItem(sender, request.target(), material, request.amount());

        if (request.givingToSelf()) {
            sendSelfMessage(request.target(), material, request.amount());
        } else {
            sendOtherMessage(sender, request.target(), material, request.amount());
        }
        return true;
    }

    private ItemRequest parseRequest(CommandSender sender, String[] args, Material material) {
        if (args.length == 1) {
            Player player = requirePlayer(sender);
            return player == null ? null : new ItemRequest(player, DEFAULT_AMOUNT, true);
        }

        if (args.length == 2) {
            Integer amount = parseAmount(args[1], material);
            if (amount != null) {
                Player player = requirePlayer(sender);
                return player == null ? null : new ItemRequest(player, amount, true);
            }

            Player target = findTarget(sender, args[1]);
            return target == null ? null : new ItemRequest(target, DEFAULT_AMOUNT, false);
        }

        Integer amount = parseAmount(args[1], material);
        if (amount == null) {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl> <SpielerName>"));
            return null;
        }

        Player target = findTarget(sender, args[2]);
        return target == null ? null : new ItemRequest(target, amount, false);
    }

    private Material matchItem(CommandSender sender, String rawName) {
        String itemName = rawName.trim();
        if (itemName.isEmpty()) {
            sendItemNotFound(sender, rawName);
            return null;
        }

        Material material = Material.matchMaterial(itemName);
        if (material == null || !material.isItem() || material.isAir()) {
            sendItemNotFound(sender, itemName);
            return null;
        }
        return material;
    }

    private Integer parseAmount(String argument, Material material) {
        try {
            int amount = Integer.parseInt(argument);
            return Math.max(DEFAULT_AMOUNT, Math.min(amount, material.getMaxStackSize()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void giveItem(CommandSender sender, Player target, Material material, int amount) {
        Map<Integer, ItemStack> leftovers = target.getInventory().addItem(new ItemStack(material, amount));
        if (leftovers.isEmpty()) return;

        leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        if (sender.equals(target)) {
            sender.sendMessage(plugin.getPrefix() + "§cYour inventory was full; extra items were dropped on the ground.");
        } else {
            sender.sendMessage(plugin.getPrefix() + "§cTarget's inventory was full; extra items were dropped on the ground.");
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer(null));
        return null;
    }

    private Player findTarget(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) return target;

        sender.sendMessage(plugin.getPrefix() + playerNotOnline(playerName));
        return null;
    }

    private boolean hasPermission(CommandSender sender) {
        if (sender.hasPermission(plugin.getPermissionBase() + "item")) return true;

        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private void sendItemNotFound(CommandSender sender, String itemName) {
        String message = message(sender, MESSAGE_ITEM_NOT_FOUND, DEFAULT_ITEM_NOT_FOUND);
        sender.sendMessage(plugin.getPrefix() + format(message, itemName, null, DEFAULT_AMOUNT));
    }

    private void sendSelfMessage(Player player, Material material, int amount) {
        if (Main.getSilent() != null && Main.getSilent().contains(player.getName())) return;

        String message = message(player, MESSAGE_ITEM_GET, DEFAULT_ITEM_GET);
        player.sendMessage(plugin.getPrefix() + format(message, material.name(), player, amount));
    }

    private void sendOtherMessage(CommandSender sender, Player target, Material material, int amount) {
        String message = message(sender, MESSAGE_ITEM_OTHER, DEFAULT_ITEM_OTHER);
        sender.sendMessage(plugin.getPrefix() + format(message, material.name(), target, amount));
    }

    private String message(CommandSender sender, String key, String defaultMessage) {
        String message = plugin.getLanguageConfig(sender).getString(key, defaultMessage);
        if (message == null) message = defaultMessage;
        return ReplaceCharConfig.replaceParagraph(message);
    }

    private String format(String message, String itemName, Player target, int amount) {
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", itemName);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", String.valueOf(amount));
        if (target != null) {
            message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", target.getName());
        }
        return message;
    }

    private String playerNotOnline(String playerName) {
        if (plugin.getVariables() == null) {
            return "§cPlayer §6" + playerName + " §cis not online!";
        }
        return plugin.getVariables().getPlayerNameNotOnline(playerName);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item>"));
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <SpielerName>"));
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl>"));
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/item <Item> <Anzahl> <SpielerName>"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(plugin.getPermissionBase() + "item")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return matchingMaterials(args[0]);
        }

        if (args.length == 2 || args.length == 3) {
            return matchingPlayers(args[args.length - 1]);
        }

        return Collections.emptyList();
    }

    private List<String> matchingMaterials(String prefix) {
        List<String> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) {
                materials.add(material.name());
            }
        }
        return TabCompleteUtils.matchingStrings(materials, prefix);
    }

    private List<String> matchingPlayers(String prefix) {
        return TabCompleteUtils.matchingOnlinePlayers(prefix);
    }

    private record ItemRequest(Player target, int amount, boolean givingToSelf) {
    }
}
