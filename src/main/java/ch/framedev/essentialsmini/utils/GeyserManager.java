package ch.framedev.essentialsmini.utils;

import org.geysermc.geyser.api.GeyserApi;

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

    public static boolean isBedrockPlayer(UUID uuid) {
        if (!isGeyserInstalled()) {
            return false;
        }
        try {
            return GeyserApi.api().isBedrockPlayer(uuid);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
