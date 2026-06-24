package ch.framedev.essentialsmini.abstracts;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for handling listeners in a plugin environment.
 * <p>
 * This class provides a structure for managing listeners in plugins by
 * automatically registering the listener upon instantiation and providing
 * helper methods to interact with the plugin instance.
 */
public abstract class ListenerBase implements Listener {

    /**
     * The main instance of the plugin.
     */
    @NotNull
    private final Main plugin;

    /**
     * Register a Listener.
     *
     * @param listener the Listener for registering
     */
    public void setupListener(Listener listener) {
        plugin.getListeners().add(listener);
        if (plugin.isDebug())
            plugin.getLogger4J().info("[ListenerBase] " + listener + " has been setup");
    }

    /**
     * Constructor for ListenerBase.
     *
     * @param plugin the main instance of the plugin
     */
    public ListenerBase(@NotNull Main plugin) {
        this.plugin = plugin;
        setupListener(this);
    }

    /**
     * Get the prefix of the plugin.
     *
     * @return the prefix of the plugin
     */
    public String getPrefix() {
        return plugin.getPrefix();
    }

    public @NotNull Main getPlugin() {
        return plugin;
    }
}
