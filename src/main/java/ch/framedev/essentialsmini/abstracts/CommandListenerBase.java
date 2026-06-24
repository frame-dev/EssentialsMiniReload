package ch.framedev.essentialsmini.abstracts;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a base class for commands that also act as event listeners.
 * This class extends the functionality of the CommandBase class while
 * implementing the Listener interface to handle events.
 * <p>
 * The CommandListenerBase class provides multiple constructors to fit
 * various use cases, allowing for flexible initialization with different
 * combinations of command names, executors, tab completers, and listeners.
 * <p>
 * Features include:
 * - Automatic registration of the class as a listener with the plugin's event manager.
 * - Support for multiple command names and custom behavior via CommandExecutor and TabCompleter.
 * <p>
 * Subclasses should inherit this class to create commands capable of responding
 * to both command execution and event handling.
 */
@SuppressWarnings("unused")
public abstract class CommandListenerBase extends CommandBase implements Listener {

    public CommandListenerBase(Main plugin) {
        super(plugin);
        setupListener(this);
    }

    public CommandListenerBase(Main plugin,  String cmdName) {
        super(plugin, cmdName);
        setupListener(this);
    }

    public CommandListenerBase(Main plugin,  String cmdName, CommandExecutor executor) {
        super(plugin, executor, cmdName);
        setupListener(this);
    }

    public CommandListenerBase(Main plugin, @NotNull  String... cmdNames) {
        super(plugin,  cmdNames);
        setupListener(this);
    }

    public CommandListenerBase(Main plugin, CommandExecutor executor, TabCompleter completer, @NotNull String... cmdNames) {
        super(plugin, executor, completer, cmdNames);
        setupListener(this);
    }

    public CommandListenerBase(Main plugin, CommandExecutor executor, TabCompleter completer, @NotNull String cmdName, Listener listener) {
        super(plugin, executor, completer, cmdName);
        setupListener(listener);
    }

    public CommandListenerBase(Main plugin, CommandExecutor executor, TabCompleter completer, Listener listener, @NotNull String... cmdNames) {
        super(plugin, executor, completer, cmdNames);
        setupListener(listener);
    }

    public CommandListenerBase(Main plugin, CommandExecutor executor, Listener listener, @NotNull String... cmdNames) {
        super(plugin, executor, cmdNames);
        setupListener(listener);
    }

    public CommandListenerBase(Main plugin, CommandExecutor executor, Listener listener, @NotNull String cmdName) {
        super(plugin, executor, cmdName);
        setupListener(listener);
    }

    public void setupListener(Listener listener) {
        getPlugin().getListeners().add(listener);
    }
}
