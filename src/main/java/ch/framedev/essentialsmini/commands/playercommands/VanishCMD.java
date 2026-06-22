package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 15.07.2020 11:59
 */
public class VanishCMD extends CommandListenerBase {

    private static final String COMMAND_NAME = "vanish";
    private static final String PERMISSION = "essentialsmini.vanish";
    private static final String SEE_PERMISSION = "essentialsmini.vanish.see";

    private final Main plugin;
    public final static ArrayList<String> hided = new ArrayList<>();

    public VanishCMD(Main plugin) {
        super(plugin, COMMAND_NAME);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND_NAME)) {
            return false;
        }

        if (!sender.hasPermission(PERMISSION)) {
            send(sender, plugin.getNoPerms());
            return true;
        }

        if (args.length == 0) {
            return handleSelfVanish(sender);
        }

        if (args.length == 1) {
            return handleOtherVanish(sender, args[0]);
        }

        send(sender, plugin.getWrongArgs("/vanish §coder §6/vanish <PlayerName>"));
        return true;
    }

    private boolean handleSelfVanish(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, plugin.getOnlyPlayer());
            return true;
        }

        boolean vanished = toggleVanish(player);
        send(player, vanishMessage(player, vanished, false, player.getName()));
        broadcastFakeJoinLeave(player, vanished);
        return true;
    }

    private boolean handleOtherVanish(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            send(sender, plugin.getVariables().getPlayerNameNotOnline(playerName));
            return true;
        }

        boolean vanished = toggleVanish(target);
        if (!Main.getSilent().contains(sender.getName())) {
            send(target, vanishMessage(target, vanished, false, target.getName()));
        }
        send(sender, vanishMessage(sender, vanished, true, target.getName()));
        return true;
    }

    private boolean toggleVanish(Player target) {
        boolean vanished = !isVanished(target);
        if (vanished) {
            vanish(target);
        } else {
            unVanish(target);
        }
        return vanished;
    }

    private void vanish(Player target) {
        if (!hided.contains(target.getName())) {
            hided.add(target.getName());
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission(SEE_PERMISSION)) {
                onlinePlayer.hidePlayer(plugin, target);
            }
        }
    }

    private void unVanish(Player target) {
        hided.remove(target.getName());
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, target);
        }
    }

    private boolean isVanished(Player player) {
        return hided.contains(player.getName());
    }

    private String vanishMessage(CommandSender receiver, boolean vanished, boolean multi, String playerName) {
        String key = vanished
                ? (multi ? "VanishOn.Multi" : "VanishOn.Single")
                : (multi ? "VanishOff.Multi" : "VanishOff.Single");
        String fallback = vanished
                ? (multi ? "&6%Player% &ais now in vanish" : "&aYou are now in vanish")
                : (multi ? "&6%Player% &cis not in vanish anymore" : "&cYou aren't in vanish anymore");
        return configuredMessage(receiver, key, fallback, playerName);
    }

    private void broadcastFakeJoinLeave(Player player, boolean vanished) {
        if (!plugin.getConfig().getBoolean("Vanish.Message")) {
            return;
        }

        String key = vanished ? "LeaveMessage" : "JoinMessage";
        String fallback = vanished ? "&6%Player% &ahas left the Server!" : "&aWelcome &6%Player% &aon this Server!";
        Bukkit.broadcastMessage(configMessage(key, fallback, player.getName()));
    }

    private String configuredMessage(CommandSender receiver, String key, String fallback, String playerName) {
        String message = plugin.getLanguageConfig(receiver).getString(key);
        if (message == null) {
            message = fallback;
        }
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", playerName);
        return ReplaceCharConfig.replaceParagraph(message);
    }

    private String configMessage(String key, String fallback, String playerName) {
        String message = plugin.getConfig().getString(key, fallback);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", playerName);
        return ReplaceCharConfig.replaceParagraph(message);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(SEE_PERMISSION)) {
            return;
        }

        for (String vanishedName : new ArrayList<>(hided)) {
            Player vanishedPlayer = Bukkit.getPlayer(vanishedName);
            if (vanishedPlayer != null) {
                player.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hided.remove(event.getPlayer().getName());
    }
}
