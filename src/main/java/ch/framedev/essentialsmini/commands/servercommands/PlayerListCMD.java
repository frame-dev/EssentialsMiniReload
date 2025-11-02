package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 17.07.2020 22:52
 */
public class PlayerListCMD extends CommandBase {

    public PlayerListCMD(Main plugin) {
        super(plugin, "online", "offline");
        plugin.getCommands().put("online", this);
        plugin.getCommands().put("offline", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if ("online".equals(cmd)) {
            if (!sender.hasPermission("essentialsmini.online")) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }

            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }

            sender.sendMessage("§6== §cOnline Players §6==");
            sender.sendMessage(players.isEmpty() ? "§7None" : "§e" + String.join(", ", players));
            sender.sendMessage("§6Total Online Players: §e" + players.size());
            return true;
        }

        if ("offline".equals(cmd)) {
            if (!sender.hasPermission("essentialsmini.offline")) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }

            List<String> players = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (!offlinePlayer.isOnline()) {
                    String name = offlinePlayer.getName();
                    if (name != null && !name.isBlank()) players.add(name);
                }
            }

            sender.sendMessage("§6== §cOffline Players §6==");
            sender.sendMessage(players.isEmpty() ? "§7None" : "§e" + String.join(", ", players));
            sender.sendMessage("§6Total Offline Players: §e" + players.size());
            return true;
        }

        return false;
    }
}