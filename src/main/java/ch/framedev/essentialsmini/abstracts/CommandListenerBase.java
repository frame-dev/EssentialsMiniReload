package ch.framedev.essentialsmini.abstracts;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * / This Plugin was Created by FrameDev
 * / Package: de.framedev.essentialsmini.managers
 * / ClassName CommandListenerBase
 * / Date: 24.08.21
 * / Project: EssentialsMini
 * / Copyrighted by FrameDev
 */

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
