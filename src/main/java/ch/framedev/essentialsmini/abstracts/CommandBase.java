package ch.framedev.essentialsmini.abstracts;

/*
 * de.framedev.essentialsmini.managers
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 17.09.2020 20:54
 */

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class CommandBase implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private String cmdName;
    @Nullable
    private final String[] cmdNames;

    /**
     * Register a Command
     *
     * @param cmdName  the CommandName for registering
     * @param executor the Executor who executes the Command
     */
    public void setup(String cmdName, CommandExecutor executor) {
        plugin.getCommands().put(cmdName, executor);
    }

    /**
     * Register a TabCompleter
     *
     * @param cmdName      the CommandName for registering
     * @param tabCompleter the TabCompleter who used for the Command
     */
    public void setupTabCompleter(String cmdName, TabCompleter tabCompleter) {
        plugin.getTabCompleters().put(cmdName, tabCompleter);
    }

    public CommandBase(Main plugin) {
        this.plugin = plugin;
        this.cmdName = null;
        this.cmdNames = null;
    }

    public CommandBase(Main plugin, @NotNull String... cmdNames) {
        this.plugin = plugin;
        this.cmdName = cmdNames[0];
        this.cmdNames = cmdNames;
        for (String cmd : cmdNames) {
            setup(cmd, this);
            setupTabCompleter(cmd, this);
        }
    }

    public CommandBase(Main plugin, String cmdName) {
        this.plugin = plugin;
        this.cmdName = cmdName;
        this.cmdNames = new String[]{cmdName};
        setup(this);
        setupTabCompleter(this);
    }

    public CommandBase(Main plugin, String cmdName, CommandExecutor executor) {
        this.plugin = plugin;
        this.cmdName = cmdName;
        this.cmdNames = new String[]{cmdName};
        setup(executor);
        setupTabCompleter(this);
    }

    public CommandBase(Main plugin, CommandExecutor executor, @NotNull String... cmdNames) {
        this.plugin = plugin;
        this.cmdNames = cmdNames;
        for (String cmd : cmdNames) {
            setup(cmd, executor);
            setupTabCompleter(cmd, this);
        }
    }

    public CommandBase(Main plugin, CommandExecutor executor, TabCompleter completer, @NotNull String... cmdNames) {
        this.plugin = plugin;
        this.cmdNames = cmdNames;
        for (String cmd : cmdNames) {
            setup(cmd, executor);
            setupTabCompleter(cmd, completer);
        }
    }

    public String[] getCmdNames() {
        return cmdNames;
    }

    public Main getPlugin() {
        return plugin;
    }

    /**
     * Register a Command
     *
     * @param executor the Executor who executes the Command
     */
    public void setup(CommandExecutor executor) {
        if (cmdName == null) throw new IllegalArgumentException("Command name cannot be null");
        plugin.getCommands().put(cmdName, executor);
    }

    /**
     * Register an TabCompleter
     *
     * @param tabCompleter the TabCompleter who used for the Command
     */
    public void setupTabCompleter(TabCompleter tabCompleter) {
        if (cmdName == null) throw new IllegalArgumentException("Command name cannot be null");
        plugin.getTabCompleters().put(cmdName, tabCompleter);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return null;
    }

    public String getPrefix() {
        return plugin.getPrefix();
    }
}
