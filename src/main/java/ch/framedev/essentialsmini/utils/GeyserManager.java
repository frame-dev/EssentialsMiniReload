package ch.framedev.essentialsmini.utils;

public class GeyserManager {

    public static boolean isGeyserInstalled() {
        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
