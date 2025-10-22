package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.main.Main;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;

import java.util.Optional;
import java.util.UUID;

public class GeyserManager {

    public static boolean isGeyserInstalled() {
        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static GeyserConnection getGeyserConnection(UUID player) {
        if (!isGeyserInstalled()) {
            return null;
        }
        try {
            return GeyserApi.api().connectionByUuid(player);
        } catch (Exception e) {
            Main.getInstance().getLogger4J().error("Error while getting Geyser connection: " + e.getMessage());
            return null;
        }
    }

    public static Optional<GeyserPlayerEntity> getGeyserPlayerEntity(UUID player) {
        if (!isGeyserInstalled()) {
            return Optional.empty();
        }
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(player);
            if (connection == null) {
                return Optional.empty();
            }
            return Optional.of(connection.entities().playerEntity());
        } catch (Exception e) {
            Main.getInstance().getLogger4J().error("Error while getting Geyser player entity: " + e.getMessage());
            return Optional.empty();
        }
    }
}
