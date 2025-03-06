package ch.framedev.essentialsmini.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * / This Plugin was Created by FrameDev
 * / Package : de.framedev.essentialsmini.api.events
 * / ClassName PlayerInventoryClearEvent
 * / Date: 16.07.21
 * / Project: EssentialsMini
 * / Copyrighted by FrameDev
 */

public class PlayerInventoryClearEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Inventory inventory;

    private boolean isCancelled;

    public PlayerInventoryClearEvent(Player player, Inventory inventory) {
        this.player = player;
        this.inventory = inventory;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}