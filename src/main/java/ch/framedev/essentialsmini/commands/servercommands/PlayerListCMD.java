package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 17.07.2020 22:52
 */
public class PlayerListCMD implements CommandExecutor {

    private final Main plugin;

    private final ArrayList<String> players = new ArrayList<>();

    public PlayerListCMD(Main plugin) {
        this.plugin = plugin;
        plugin.getCommands().put("online", this);
        plugin.getCommands().put("offline", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("online")) {
            if (sender.hasPermission("essentialsmini.online")) {
                /* Online Spieler */
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                sender.sendMessage("§6==§cOnlinePlayers§6==");
                sender.sendMessage(Arrays.toString(players.toArray()));
                players.clear();
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        }
        if (command.getName().equalsIgnoreCase("offline")) {
            if (sender.hasPermission("essentialsmini.online")) {
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (!offlinePlayer.isOnline())
                        players.add(offlinePlayer.getName());
                }
                sender.sendMessage("§6==§cOfflinePlayers§6==");
                sender.sendMessage(Arrays.toString(players.toArray()));
                players.clear();
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        }
        return false;
    }
}
