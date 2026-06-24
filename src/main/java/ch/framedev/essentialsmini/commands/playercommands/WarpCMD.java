package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.InventoryManager;
import ch.framedev.essentialsmini.managers.ItemBuilder;
import ch.framedev.essentialsmini.managers.LocationManager;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import ch.framedev.essentialsmini.utils.Variables;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 15.07.2020 19:28
 */
public class WarpCMD extends CommandListenerBase {

    private static final String SET_WARP = "setwarp";
    private static final String WARP = "warp";
    private static final String WARPS = "warps";
    private static final String DEL_WARP = "delwarp";
    private static final String INVENTORY_TITLE = "§aWarps";

    private final Main plugin;

    public WarpCMD(Main plugin) {
        super(plugin, SET_WARP, WARP, WARPS, DEL_WARP);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case SET_WARP -> handleSetWarp(sender, args);
            case WARP -> handleWarp(sender, args);
            case WARPS -> handleWarps(sender);
            case DEL_WARP -> handleDeleteWarp(sender, args);
            default -> false;
        };
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "essentialsmini.setwarp")) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            send(sender, plugin.getOnlyPlayer());
            return true;
        }

        if (args.length != 1 && args.length != 2) {
            send(sender, plugin.getWrongArgs("/setwarp <Name> §cor §6/setwarp <Name> <Cost>"));
            return true;
        }

        String name = normalizeWarpName(args[0]);
        if (name.isEmpty()) {
            send(sender, plugin.getWrongArgs("/setwarp <Name> §cor §6/setwarp <Name> <Cost>"));
            return true;
        }

        LocationManager locationManager = new LocationManager();
        if (args.length == 1) {
            locationManager.setLocation("warps." + name, player.getLocation());
        } else {
            Double cost = parseCost(sender, args[1]);
            if (cost == null) {
                return true;
            }
            locationManager.setWarp(name, player.getLocation(), cost);
        }

        send(player, warpMessage(player, "Created", "&aWarp has been created with the name &6%WarpName%&c!", name, null));
        if (args.length == 2) {
            send(player, warpMessage(player, "Cost", "&aThis Warp costs &6%Cost%&c!", name, locationManager.getWarpCost(name)));
        }
        return true;
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "essentialsmini.warp")) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            send(sender, plugin.getOnlyPlayer());
            return true;
        }

        if (args.length == 0) {
            if (plugin.getConfig().getBoolean("WarpGUI", false)) {
                openWarpGui(player);
            } else {
                send(player, plugin.getWrongArgs("/warp <Name>"));
            }
            return true;
        }

        if (args.length != 1) {
            send(player, plugin.getWrongArgs("/warp <Name>"));
            return true;
        }

        teleportToWarp(player, args[0]);
        return true;
    }

    private boolean handleWarps(CommandSender sender) {
        if (!hasPermission(sender, "essentialsmini.warps")) {
            return true;
        }

        List<String> warps = getWarpNames();
        if (warps.isEmpty()) {
            send(sender, warpMessage(sender, "NotExist", "&cThis Warp doesn't exist!", null, null));
            return true;
        }

        send(sender, "§a==Alle Aktuellen Warps==");
        for (String warp : warps) {
            TextComponent textComponent = new TextComponent("§6" + warp);
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click me to add as Warp Command §6(/warp " + warp + ")")));
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/warp " + warp));
            sender.spigot().sendMessage(textComponent);
        }
        return true;
    }

    private boolean handleDeleteWarp(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "essentialsmini.delwarp")) {
            return true;
        }

        if (args.length != 1) {
            send(sender, plugin.getWrongArgs("/delwarp <Name>"));
            return true;
        }

        String warp = normalizeWarpName(args[0]);
        if (!warpExists(warp)) {
            send(sender, warpMessage(sender, "NotExist", "&cThis Warp doesn't exist!", warp, null));
            return true;
        }

        new LocationManager().removeLocation("warps." + warp);
        send(sender, warpMessage(sender, "Delete", "&aThe Warp with the name &6%WarpName% &ahas been &cdeleted!", warp, null));
        return true;
    }

    private void teleportToWarp(Player player, String inputName) {
        String name = normalizeWarpName(inputName);
        LocationManager locationManager = new LocationManager();
        Location location = locationManager.getWarp(name);
        if (location == null) {
            send(player, warpMessage(player, "NotExist", "&cThis Warp doesn't exist!", name, null));
            return;
        }

        if (!chargeWarpCost(player, name, locationManager)) {
            return;
        }

        player.teleport(location);
        send(player, warpMessage(player, "Teleport", "&aYou've teleported to &6%WarpName%!", name, null));
    }

    private boolean chargeWarpCost(Player player, String warpName, LocationManager locationManager) {
        if (!locationManager.costWarp(warpName) || plugin.getVaultManager() == null || plugin.getVaultManager().getEco() == null) {
            return true;
        }

        double cost = locationManager.getWarpCost(warpName);
        if (cost <= 0) {
            return true;
        }

        Economy economy = plugin.getVaultManager().getEco();
        if (economy.has(player, cost)) {
            economy.withdrawPlayer(player, cost);
            return true;
        }

        send(player, "§cNot enough §6" + economy.currencyNamePlural());
        return false;
    }

    private void openWarpGui(Player player) {
        List<String> warps = getWarpNames();
        if (warps.isEmpty()) {
            send(player, warpMessage(player, "NotExist", "&cThis Warp doesn't exist!", null, null));
            return;
        }

        int rows = Math.min(6, Math.max(1, (warps.size() + 8) / 9));
        InventoryManager inventoryManager = new InventoryManager();
        inventoryManager.setTitle(INVENTORY_TITLE);
        inventoryManager.setSize(rows);
        inventoryManager.create();

        LocationManager locationManager = new LocationManager();
        int maxItems = Math.min(warps.size(), inventoryManager.getSize());
        for (int i = 0; i < maxItems; i++) {
            String warp = warps.get(i);
            ItemBuilder builder = new ItemBuilder(Material.ENDER_PEARL).setDisplayName("§6" + warp);
            if (locationManager.costWarp(warp)) {
                builder.setLore("§aCost : §6" + locationManager.getWarpCost(warp), "§aTeleport to this Warp");
            } else {
                builder.setLore("§aTeleport to this Warp");
            }
            inventoryManager.setItem(i, builder.build());
        }

        inventoryManager.fillNull();
        player.openInventory(inventoryManager.getInventory());
    }

    private List<String> getWarpNames() {
        LocationManager locationManager = new LocationManager();
        List<String> warps = new ArrayList<>();

        ConfigurationSection section = locationManager.getCfg().getConfigurationSection("warps");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (isActiveWarp(locationManager, key)) {
                    warps.add(key);
                }
            }
            return warps;
        }

        for (String name : locationManager.getWarpNames()) {
            String cleaned = name == null ? "" : name.replace("warps.", "");
            if (!cleaned.isEmpty() && isActiveWarp(locationManager, cleaned)) {
                warps.add(cleaned);
            }
        }
        return warps;
    }

    private boolean isActiveWarp(LocationManager locationManager, String warpName) {
        if (warpName == null || warpName.isBlank()) {
            return false;
        }

        Object value = locationManager.getCfg().get("warps." + warpName);
        return value != null && !" ".equals(String.valueOf(value));
    }

    private boolean warpExists(String warpName) {
        return getWarpNames().contains(warpName);
    }

    private Double parseCost(CommandSender sender, String rawCost) {
        try {
            double cost = Double.parseDouble(rawCost);
            if (cost < 0) {
                send(sender, "§cCost must be 0 or higher.");
                return null;
            }
            return cost;
        } catch (NumberFormatException ex) {
            send(sender, "§cInvalid cost: " + rawCost);
            return null;
        }
    }

    private String warpMessage(CommandSender sender, String key, String fallback, String warpName, Double cost) {
        String message = plugin.getLanguageConfig(sender).getString(Variables.WARP_MESSAGE + "." + key);
        if (message == null) {
            message = fallback;
        }
        if (warpName != null) {
            message = ReplaceCharConfig.replaceObjectWithData(message, "%WarpName%", warpName);
        }
        if (cost != null) {
            message = ReplaceCharConfig.replaceObjectWithData(message, "%Cost%", cost + plugin.getCurrencySymbol());
        }
        return ReplaceCharConfig.replaceParagraph(message);
    }

    private String normalizeWarpName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        send(sender, plugin.getNoPerms());
        return false;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if ((commandName.equals(WARP) && sender.hasPermission("essentialsmini.warp"))
                || (commandName.equals(DEL_WARP) && sender.hasPermission("essentialsmini.delwarp"))) {
            if (args.length == 1) {
                return TabCompleteUtils.matchingStrings(getWarpNames(), args[0]);
            }
        }
        return List.of();
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(INVENTORY_TITLE)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || !currentItem.hasItemMeta()) {
            return;
        }

        ItemMeta itemMeta = currentItem.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return;
        }

        String displayName = itemMeta.getDisplayName();
        if (!displayName.startsWith("§6")) {
            return;
        }

        String warpName = normalizeWarpName(displayName.substring(2));
        if (warpExists(warpName)) {
            teleportToWarp(player, warpName);
        }
    }
}
