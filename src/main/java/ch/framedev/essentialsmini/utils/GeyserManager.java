package ch.framedev.essentialsmini.utils;

import java.lang.reflect.Method;
import java.util.UUID;

public final class GeyserManager {

    private GeyserManager() { /* utility class */ }

    /**
     * Returns true if Geyser or Floodgate API is present on the server classpath.
     */
    public static boolean isGeyserInstalled() {
        return isClassAvailable("org.geysermc.geyser.api.GeyserApi") ||
                isClassAvailable("org.geysermc.floodgate.api.FloodgateApi");
    }

    /**
     * Returns true if Floodgate API is present (Floodgate is the common Floodgate plugin for Geyser).
     */
    public static boolean isFloodgateInstalled() {
        return isClassAvailable("org.geysermc.floodgate.api.FloodgateApi");
    }

    /**
     * Uses FloodgateApi.getInstance().isFloodgatePlayer(UUID) via reflection.
     * Returns true only if Floodgate is installed and the UUID belongs to a Floodgate (Bedrock) player.
     */
    public static boolean isFloodgatePlayer(UUID uuid) {
        if (uuid == null) return false;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            Method isFgPlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            Object result = isFgPlayer.invoke(api, uuid);
            return result instanceof Boolean && (Boolean) result;
        } catch (ClassNotFoundException e) {
            // Floodgate not present
            return false;
        } catch (Exception e) {
            // Any reflection failure => treat as not a floodgate player
            return false;
        }
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
