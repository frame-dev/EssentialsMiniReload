package ch.framedev.essentialsmini.api.events;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * / This Plugin was Created by FrameDev
 * / Package : de.framedev.essentialsmini.api.events
 * / ClassName PlayerKillPlayerEvent
 * / Date: 16.07.21
 * / Project: EssentialsMini
 * / Copyrighted by FrameDev
 */

public class PlayerKillPlayerEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String playerName;
    private final Player player;
    private final World world;
    private final Player killer;
    private final EntityDamageEvent.DamageCause deathCause;
    private List<ItemStack> drops;
    private double droppedExp;

    public PlayerKillPlayerEvent(Player player, Player killer, List<ItemStack> drops, double droppedExp) {
        this.player = player;
        this.playerName = player.getName();
        this.world = player.getWorld();
        this.killer = killer;
        if (player.getLastDamageCause() != null) {
            this.deathCause = player.getLastDamageCause().getCause();
        } else {
            deathCause = null;
        }

        this.drops = drops;
        this.droppedExp = droppedExp;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public void setDrops(List<ItemStack> drops) {
        this.drops = drops;
    }

    public void setDroppedExp(double droppedExp) {
        this.droppedExp = droppedExp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    public Player getKiller() {
        return killer;
    }

    public EntityDamageEvent.DamageCause getDeathCause() {
        return deathCause;
    }

    public List<ItemStack> getDrops() {
        return drops;
    }

    public double getDroppedExp() {
        return droppedExp;
    }
}
