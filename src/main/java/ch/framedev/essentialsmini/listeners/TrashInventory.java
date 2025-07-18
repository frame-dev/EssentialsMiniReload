package ch.framedev.essentialsmini.listeners;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 18.08.2020 21:49
 */

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class TrashInventory implements CommandExecutor, Listener {

    private final Inventory inventory = Bukkit.createInventory(null, 9 * 6, "Trash");
    private final Main plugin;

    public TrashInventory(Main plugin) {
        this.plugin = plugin;
        plugin.getListeners().add(this);
        // Trash Command Register
        plugin.getCommands().put("trash", this);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission("essentialsmini.trash")) {
            Player player = (Player) sender;
            player.openInventory(inventory);
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
        }
        return false;
    }

    @EventHandler
    public void onSwitchItem(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.getInventory().clear();
        }
    }

    @EventHandler
    public void onMoveItem(InventoryEvent event) {
        if(event.getView().getTitle().equalsIgnoreCase("Trash")) {
            Inventory inventory1 = event.getInventory();
            inventory1.clear();
        }
    }
}
