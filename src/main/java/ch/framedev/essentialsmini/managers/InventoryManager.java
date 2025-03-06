package ch.framedev.essentialsmini.managers;

/*
 * de.framedev.essentialsmini.managers
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 22.09.2020 19:27
 */

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryManager {

    private @NotNull String title;
    private int size = 0;
    private Inventory inventory;

    public InventoryManager() {
        title = "";
    }

    public InventoryManager(@NotNull String title) {
        this.title = title;
        this.size = 1;
    }

    public InventoryManager(@NotNull String title, int size) {
        this.title = title;
        this.size = size;
    }

    public InventoryManager(Inventory inventory) {
        this.inventory = inventory;
        title = "";
    }

    public @NotNull String getTitle() {
        return title;
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    public int getSize() {
        if (size == 0) size = 1;
        return size * 9;
    }

    public void setSize(int size) {
        if (size == 0) return;
        this.size = size;
    }

    public void setItem(int index, ItemStack itemStack) {
        if (inventory == null) return;
        inventory.setItem(index, itemStack);
    }

    public void setItem(int index, Material material) {
        if (inventory == null) return;
        inventory.setItem(index, new ItemStack(material));
    }

    public ItemStack getItem(int index) {
        if (inventory == null) return null;
        return inventory.getItem(index);
    }

    public void addItem(ItemStack... items) {
        if (inventory == null) return;
        this.inventory.addItem(items);
    }

    public void addItem(ItemStack itemStack) {
        if (inventory == null) return;
        this.inventory.addItem(itemStack);
    }

    public void addItem(Material material) {
        if (this.inventory == null) return;
        this.inventory.addItem(new ItemStack(material));
    }

    public InventoryManager create() {
        if (size == 0) size = 1;
        this.inventory = Bukkit.createInventory(null, getSize(), getTitle());
        return this;
    }

    public void fillNull() {
        int inventorySize = getSize();
        for (int i = 0; i < inventorySize; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ").build());
            }
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getFirstEmptySlot() {
        if (inventory == null) return -0;
        for (int i = 0; i < getSize(); i++) {
            if (inventory.getItem(i) == null)
                return i;
        }
        return -1;
    }
}
