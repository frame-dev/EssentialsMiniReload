package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UtilityStationCMD extends CommandBase {

    private final Main plugin;
    private final Map<String, Station> stations = new HashMap<>();

    public UtilityStationCMD(Main plugin) {
        super(plugin, "workbench", "anvil", "grindstone", "smithingtable", "cartographytable", "loom", "stonecutter");
        this.plugin = plugin;

        stations.put("workbench", new Station("workbench", InventoryType.WORKBENCH, "Workbench"));
        stations.put("craft", stations.get("workbench"));
        stations.put("anvil", new Station("anvil", InventoryType.ANVIL, "Anvil"));
        stations.put("grindstone", new Station("grindstone", InventoryType.GRINDSTONE, "Grindstone"));
        stations.put("smithingtable", new Station("smithingtable", InventoryType.SMITHING, "Smithing Table"));
        stations.put("cartographytable", new Station("cartographytable", InventoryType.CARTOGRAPHY, "Cartography Table"));
        stations.put("loom", new Station("loom", InventoryType.LOOM, "Loom"));
        stations.put("stonecutter", new Station("stonecutter", InventoryType.STONECUTTER, "Stonecutter"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer(null));
            return true;
        }

        Station station = stations.getOrDefault(label.toLowerCase(Locale.ROOT), stations.get(command.getName().toLowerCase(Locale.ROOT)));
        if (station == null) {
            return false;
        }

        if (!player.hasPermission(plugin.getPermissionBase() + station.permission())) {
            player.sendMessage(plugin.getPrefix() + plugin.getNoPerms(player));
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(plugin.getPrefix() + plugin.getWrongArgs(player, "/" + label));
            return true;
        }

        player.openInventory(Bukkit.createInventory(player, station.inventoryType()));
        sendMessage(player, "UtilityStation.Opened", "&aOpened &6%Station%&a.", station.displayName());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return Collections.emptyList();
    }

    private void sendMessage(Player player, String key, String fallback, String stationName) {
        String message = plugin.getLanguageConfig(player).getString(key, fallback);
        if (message == null) message = fallback;

        message = ReplaceCharConfig.replaceParagraph(message);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Station%", stationName);
        player.sendMessage(plugin.getPrefix() + message);
    }

    private record Station(String permission, InventoryType inventoryType, String displayName) {
    }
}
