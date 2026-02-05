package ch.framedev.essentialsmini.listeners;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trash inventory system that allows players to delete items
 * Each player gets their own unique trash inventory
 */
public class TrashInventory extends CommandListenerBase {

    private static final String TRASH_TITLE = "Trash";
    private static final int TRASH_SIZE = 9 * 6; // 6 rows

    // Track which inventories belong to which players
    private final Map<UUID, Inventory> playerTrashInventories = new ConcurrentHashMap<>();
    private final Main plugin;

    public TrashInventory(Main plugin) {
        super(plugin, "trash");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("essentialsmini.trash")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            return true;
        }

        // Create or get existing trash inventory for this player
        Inventory trashInventory = getOrCreateTrashInventory(player);
        player.openInventory(trashInventory);

        return true;
    }

    /**
     * Get or create a unique trash inventory for the player
     * @param player the player
     * @return the player's trash inventory
     */
    private Inventory getOrCreateTrashInventory(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player already has a trash inventory
        Inventory existing = playerTrashInventories.get(playerId);
        if (existing != null) {
            return existing;
        }

        // Create new trash inventory for this player
        Inventory newTrash = Bukkit.createInventory(null, TRASH_SIZE, TRASH_TITLE);
        playerTrashInventories.put(playerId, newTrash);

        return newTrash;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // Check if this is a trash inventory by title
        String title = event.getView().getTitle();
        if (!title.equals(TRASH_TITLE)) {
            return;
        }

        // Get player UUID
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Inventory playerTrash = playerTrashInventories.get(playerId);

        // Verify this is the player's trash inventory
        if (playerTrash != null && event.getInventory().equals(playerTrash)) {
            // Clear the inventory contents (delete items)
            event.getInventory().clear();

            // Remove from the tracking map to prevent memory leaks
            playerTrashInventories.remove(playerId);
        }
    }

    /**
     * Clean up all trash inventories (called on plugin disable)
     */
    public void cleanup() {
        playerTrashInventories.values().forEach(Inventory::clear);
        playerTrashInventories.clear();
    }
}