package ch.framedev.essentialsmini.abstracts;


/*
 * de.framedev.essentialsmini.abstracts
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 23.09.2020 19:13
 */

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.event.Listener;

public abstract class ListenerBase implements Listener {

    /**
     * The main instance of the plugin.
     */
    private final Main plugin;

    /**
     * Register a Listener.
     *
     * @param listener the Listener for registering
     */
    public void setupListener(Listener listener) {
        plugin.getListeners().add(listener);
    }

    /**
     * Constructor for ListenerBase.
     *
     * @param plugin the main instance of the plugin
     */
    public ListenerBase(Main plugin) {
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

    public Main getPlugin() {
        return plugin;
    }
}
