package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.commands.playercommands.*;
import ch.framedev.essentialsmini.commands.servercommands.ClearChatCMD;
import ch.framedev.essentialsmini.commands.servercommands.GlobalMuteCMD;
import ch.framedev.essentialsmini.commands.servercommands.MaintenanceCMD;
import ch.framedev.essentialsmini.commands.servercommands.PlayerListCMD;
import ch.framedev.essentialsmini.commands.worldcommands.DayNightCMD;
import ch.framedev.essentialsmini.commands.worldcommands.LightningStrikeCMD;
import ch.framedev.essentialsmini.commands.worldcommands.SunRainThunderCMD;
import ch.framedev.essentialsmini.listeners.*;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import java.util.Map;
import java.util.Objects;

public class RegisterManager {

    private final Main plugin;

    // MuteCMD Var
    private MuteCMD muteCMD;

    private PlayerListeners playerListeners;

    /**
     * Constructor of RegisterManager
     * Register all Events and Commands
     *
     * @param plugin the Main Plugin
     */
    public RegisterManager(Main plugin) {
        this.plugin = plugin;
        plugin.getLogger4J().info("Registering Events and Commands starting!");

        // Register Commands
        registerCommands();

        // Register Listeners
        registerListeners();

        // Register TabCompleters
        registerTabCompleters();
        plugin.getLogger4J().info("Registering Events and Commands finished!");
    }

    /**
     * Register all TabCompleters
     */
    private void registerTabCompleters() {
        for (Map.Entry<String, TabCompleter> completer : plugin.getTabCompleters().entrySet()) {
            PluginCommand command = Objects.requireNonNull(plugin.getCommand(completer.getKey()),
                    "Command not found for tab completer: " + completer.getKey());
           command.setTabCompleter(completer.getValue());
        }
    }

    /**
     * Register all Listeners
     */
    private void registerListeners() {
        new DisallowCommands(plugin);
        new SleepListener(plugin);
        this.playerListeners = new PlayerListeners(plugin);
        new BanListener(plugin);
        new WarpSigns(plugin);
        plugin.getListeners().forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));
    }

    /**
     * Register all Commands
     */
    private void registerCommands() {
        new SpawnCMD(plugin);
        if (plugin.getConfig().getBoolean("HomeTP")) {
            new HomeCMD(plugin);
        }
        new TeleportCMD(plugin);
        new FlyCMD(plugin);
        new InvseeCMD(plugin);
        if (plugin.getConfig().getBoolean("Back"))
            new BackCMD(plugin);
        new GameModeCMD(plugin);
        new VanishCMD(plugin);
        new WarpCMD(plugin);
        new PlayerListCMD(plugin);
        new DayNightCMD(plugin);
        new BackpackCMD(plugin);
        new SleepCMD(plugin);
        new ItemCMD(plugin);
        new KillCMD(plugin);
        new PlayerHeadsCMD(plugin);
        new MessageCMD(plugin);
        new EnchantCMD(plugin);
        new SunRainThunderCMD(plugin);
        new RepairCMD(plugin);
        new HealCMD(plugin);
        new FeedCMD(plugin);
        new TrashInventory(plugin);
        new KitCMD(plugin);
        new GodCMD(plugin);
        new SpeedCMD(plugin);
        new LightningStrikeCMD(plugin);
        new ClearChatCMD(plugin);
        if (plugin.getConfig().getBoolean("Economy.Activate")) {
            new EcoCMDs(plugin);
            new BankCMD(plugin);
            new MoneySignListeners(plugin);
        }
        if (plugin.getConfig().getBoolean("AFK.Boolean"))
            new AFKCMD(plugin);
        new SilentCMD(plugin);
        new FlySpeedCMD(plugin);
        this.muteCMD = new MuteCMD(plugin);
        new TempBanCMD(plugin);
        new BanCMD(plugin);
        new UnBanCMD(plugin);
        new ExperienceCMD(plugin);
        new TimePlayedCMD(plugin);
        new MuteForPlayerCMD(plugin);

        new NickCMD(plugin);

        new PlayerWeatherCMD(plugin);
        new MaintenanceCMD(plugin);
        new GlobalMuteCMD(plugin);
        new StaffChatCMD(plugin);

        new TopCMD(plugin);
        new MailCMD(plugin);

        for (Map.Entry<String, CommandExecutor> commands : plugin.getCommands().entrySet()) {
            if (commands.getKey() == null) continue;
            if (commands.getValue() == null) continue;
            if (plugin.getCommand(commands.getKey()) == null) continue;
            Objects.requireNonNull(plugin.getCommand(commands.getKey()), "Command not found for Executor: " + commands.getKey()).setExecutor(commands.getValue());
        }
    }

    @SuppressWarnings("unused")
    public PlayerListeners getPlayerListeners() {
        return playerListeners;
    }

    @SuppressWarnings("unused")
    public MuteCMD getMuteCMD() {
        return muteCMD;
    }
}
