package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.LocationManager;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LegacyUtilityCMD extends CommandBase {

    private static final List<String> COMMANDS = List.of(
            "book", "copybook", "firework", "mysql", "position", "renameitem", "signitem", "spawnmob"
    );
    private static final int MAX_SPAWN_AMOUNT = 20;

    private final Main plugin;

    public LegacyUtilityCMD(Main plugin) {
        super(plugin, COMMANDS.toArray(String[]::new));
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        return switch (commandName) {
            case "book" -> handleBook(sender);
            case "copybook" -> handleCopyBook(sender);
            case "firework" -> handleFirework(sender);
            case "mysql" -> handleMysql(sender);
            case "position" -> handlePosition(sender);
            case "renameitem" -> handleRenameItem(sender, args);
            case "signitem" -> handleSignItem(sender);
            case "spawnmob" -> handleSpawnMob(sender, args);
            default -> false;
        };
    }

    private boolean handleBook(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "book")) return true;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isBook(item)) {
            send(player, "§cHold a written or writable book first.");
            return true;
        }

        player.openBook(item);
        return true;
    }

    private boolean handleCopyBook(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "copybook")) return true;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isBook(item)) {
            send(player, "§cHold a written or writable book first.");
            return true;
        }

        ItemStack copy = item.clone();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(copy);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        send(player, leftovers.isEmpty() ? "§aBook copied." : "§aBook copied. §eYour inventory was full, so the copy was dropped.");
        return true;
    }

    private boolean handleFirework(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "firework")) return true;

        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.AQUA, Color.LIME)
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        send(player, "§aLaunched a firework.");
        return true;
    }

    private boolean handleMysql(CommandSender sender) {
        if (!hasPermission(sender, "mysql")) return true;

        String backend;
        if (plugin.isMysql()) {
            backend = "MySQL";
        } else if (plugin.isSQL()) {
            backend = "SQLite";
        } else if (plugin.isMongoDB()) {
            backend = "MongoDB";
        } else {
            backend = "file storage";
        }

        send(sender, "§7Database backend: §a" + backend);
        send(sender, "§7Economy: " + (plugin.isEconomyEnabled() ? "§aenabled" : "§cdisabled"));
        return true;
    }

    private boolean handlePosition(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "position")) return true;

        Location location = player.getLocation();
        new LocationManager().setLocation("position." + player.getName(), location);
        String message = "§6" + player.getName() + " §amarked position §e"
                + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()
                + " §ain §e" + location.getWorld().getName();
        Bukkit.broadcastMessage(plugin.getPrefix() + message);
        return true;
    }

    private boolean handleRenameItem(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "renameitem")) return true;

        if (args.length == 0) {
            send(player, plugin.getWrongArgs(player, "/renameitem <name>"));
            return true;
        }

        ItemStack item = getHeldItem(player);
        if (item == null) return true;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            send(player, "§cThis item cannot be renamed.");
            return true;
        }

        meta.setDisplayName(String.join(" ", args).replace('&', '§'));
        item.setItemMeta(meta);
        send(player, "§aItem renamed.");
        return true;
    }

    private boolean handleSignItem(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "signitem")) return true;

        ItemStack item = getHeldItem(player);
        if (item == null) return true;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            send(player, "§cThis item cannot be signed.");
            return true;
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String signature = "§7Signed by §6" + player.getName();
        if (!lore.contains(signature)) {
            lore.add(signature);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        send(player, "§aItem signed.");
        return true;
    }

    private boolean handleSpawnMob(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "spawnmob")) return true;

        if (args.length < 1 || args.length > 2) {
            send(player, plugin.getWrongArgs(player, "/spawnmob <type> [amount]"));
            return true;
        }

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            send(player, "§cUnknown entity type: §6" + args[0]);
            return true;
        }

        if (!entityType.isSpawnable() || !entityType.isAlive()) {
            send(player, "§cThat entity type cannot be spawned as a mob.");
            return true;
        }

        Integer amount = parseAmount(args.length == 2 ? args[1] : "1");
        if (amount == null) {
            send(player, "§cAmount must be between 1 and " + MAX_SPAWN_AMOUNT + ".");
            return true;
        }

        for (int i = 0; i < amount; i++) {
            player.getWorld().spawnEntity(player.getLocation(), entityType).playEffect(EntityEffect.ENTITY_POOF);
        }
        send(player, "§aSpawned §6" + amount + "x " + entityType.name().toLowerCase(Locale.ROOT) + "§a.");
        return true;
    }

    private boolean isBook(ItemStack item) {
        return item != null
                && (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.WRITABLE_BOOK)
                && item.getItemMeta() instanceof BookMeta;
    }

    private ItemStack getHeldItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            send(player, "§cHold an item first.");
            return null;
        }
        return item;
    }

    private Integer parseAmount(String value) {
        try {
            int amount = Integer.parseInt(value);
            return amount >= 1 && amount <= MAX_SPAWN_AMOUNT ? amount : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        send(sender, plugin.getOnlyPlayer(null));
        return null;
    }

    private boolean hasPermission(CommandSender sender, String permissionSuffix) {
        if (sender.hasPermission(plugin.getPermissionBase() + permissionSuffix)) {
            return true;
        }
        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawnmob") && args.length == 1) {
            List<String> entityTypes = Arrays.stream(EntityType.values())
                    .filter(type -> type.isSpawnable() && type.isAlive())
                    .map(Enum::name)
                    .toList();
            return TabCompleteUtils.matchingStrings(entityTypes, args[0]);
        }

        if (command.getName().equalsIgnoreCase("spawnmob") && args.length == 2) {
            return TabCompleteUtils.matchingStrings(List.of("1", "2", "3", "5", "10"), args[1]);
        }

        return List.of();
    }
}
