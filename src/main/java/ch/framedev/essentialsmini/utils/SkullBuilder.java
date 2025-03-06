package ch.framedev.essentialsmini.utils;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 16.08.2020 22:17
 */

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SkullBuilder {

    private final String skullOwner;
    private String displayName;
    private final OfflinePlayer player;
    private final ItemStack itemStack;

    public SkullBuilder(String skullOwner) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        this.skullOwner = skullOwner;
        if (Bukkit.getServer().getOnlineMode()) {
            this.player = Bukkit.getOfflinePlayer(UUIDFetcher.getUUID(skullOwner));
        } else {
            //noinspection deprecation
            this.player = Bukkit.getOfflinePlayer(skullOwner);
        }
    }

    public SkullBuilder(UUID uuid) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        this.skullOwner = Bukkit.getOfflinePlayer(uuid).getName();
        this.player = Bukkit.getOfflinePlayer(uuid);
    }

    public SkullBuilder(OfflinePlayer player) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        this.player = player;
        this.skullOwner = player.getName();
    }

    public SkullBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public int getAmount() {
        return this.itemStack.getAmount();
    }

    public SkullBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ItemStack create() {
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        if (skullMeta != null) {
            if (displayName == null) {
                skullMeta.setDisplayName(skullOwner);
            } else {
                skullMeta.setDisplayName(displayName);
            }
            skullMeta.setOwningPlayer(player);
            itemStack.setItemMeta(skullMeta);
        }
        return itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public static ItemStack createSkull(String playerName) {
        return new SkullBuilder(playerName).setAmount(1).setDisplayName("§6" + playerName + "§a'Head").create();
    }

    public String getSkullOwner() {
        return skullOwner;
    }

    public String getDisplayName() {
        return displayName;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }
}