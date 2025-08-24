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

public class TrashInventory extends CommandListenerBase {

    private final Inventory inventory = Bukkit.createInventory(null, 9 * 6, "Trash");
    private final Main plugin;

    public TrashInventory(Main plugin) {
        super(plugin, "trash");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("essentialsmini.trash")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            return true;
        }
        player.openInventory(inventory);
        return true;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.getInventory().clear();
        }
    }
}