package ch.framedev.essentialsmini.managers;


/*
 * de.framedev.essentialsmini.utils
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 22.09.2020 17:31
 */

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ItemMeta getItemMeta() {
        return itemMeta;
    }

    public ItemBuilder setDisplayName(String displayName) {
        if (itemMeta == null) {
            return null;
        }
        this.itemMeta.setDisplayName(displayName);
        return this;
    }

    public String getDisplayName() {
        if (itemMeta == null) {
            return null;
        }
        return this.itemMeta.getDisplayName();
    }

    public ItemBuilder setLore(String... lore) {
        if (itemMeta == null) return null;
        this.itemMeta.setLore(Arrays.asList(lore));
        return this;
    }

    public List<String> getLore() {
        if (itemMeta == null) return null;
        return this.itemMeta.getLore();
    }

    public ItemBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public int getAmount() {
        if (itemStack == null) return 0;
        return itemStack.getAmount();
    }

    public ItemBuilder addLore(String stringLore) {
        if (this.itemMeta == null) return null;
        if (!this.itemMeta.hasLore()) {
            List<String> lore = new ArrayList<>();
            lore.add(stringLore);
            this.itemMeta.setLore(lore);
        } else {
            List<String> lore = this.itemMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(stringLore);
            this.itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder addLore(String... stringLore) {
        if (this.itemMeta == null) return null;
        if (!this.itemMeta.hasLore()) {
            List<String> lore = new ArrayList<>(Arrays.asList(stringLore));
            this.itemMeta.setLore(lore);
        } else {
            List<String> lore = this.itemMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.addAll(Arrays.asList(stringLore));
            this.itemMeta.setLore(lore);
        }
        return this;
    }

    public Material getMaterial() {
        if (this.itemStack == null) return null;
        return this.itemStack.getType();
    }

    public @Nullable String getLore(int index) {
        if (this.itemMeta == null) return null;
        if (this.itemMeta.hasLore() && this.itemMeta.getLore() != null) {
            return this.itemMeta.getLore().get(index);
        }
        return null;
    }

    public @Nullable ItemBuilder addEnchantment(Enchantment enchantment, int level, boolean ignore) {
        if (this.itemMeta == null) return null;
        itemMeta.addEnchant(enchantment, level, ignore);
        return this;
    }

    public @Nullable ItemBuilder removeEnchantment(Enchantment enchantment) {
        if (this.itemMeta == null) return null;
        if (itemMeta.hasEnchant(enchantment))
            itemMeta.removeEnchant(enchantment);
        return this;
    }

    public ItemStack build() {
        if (this.itemMeta == null) {
            return itemStack;
        }
        this.itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
