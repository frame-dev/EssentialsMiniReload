package ch.framedev.essentialsmini.api.events;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityHitByProjectileEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity hitEntity;
    private final Entity shooter;

    public EntityHitByProjectileEvent(Entity hitEntity, Entity shooter) {
        this.hitEntity = hitEntity;
        this.shooter = shooter;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Entity getHitEntity() {
        return hitEntity;
    }

    public Entity getShooter() {
        return shooter;
    }
}
