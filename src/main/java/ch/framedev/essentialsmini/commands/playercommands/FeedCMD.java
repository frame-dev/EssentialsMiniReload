package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 18.08.2020 19:36
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedCMD extends CommandBase {

    private final Main plugin;

    public FeedCMD(Main plugin) {
        super(plugin, "feed");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (sender.hasPermission("essentialsmini.feed")) {
                    feedPlayerAndNotify(player, sender, false);
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            }
            return true;
        } else if (args.length == 1) {
            if (sender.hasPermission("essentialsmini.feed.others")) {
                if (args[0].equalsIgnoreCase("**")) {
                    Bukkit.getOnlinePlayers().forEach(player -> feedPlayerAndNotify(player, sender, true));
                } else {
                    Player player = Bukkit.getPlayer(args[0]);
                    if (player != null) {
                        feedPlayerAndNotify(player, sender, true);
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                    }
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
            return true;
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/feed §cor §6/feed <PlayerName>"));
            return true;
        }
    }

    // Helper to feed a player and notify both the target and the command sender (if requested).
    private void feedPlayerAndNotify(Player target, CommandSender sender, boolean notifySender) {
        // Refill food level
        target.setFoodLevel(20);

        // Notify target unless sender is silent
        if (!Main.getSilent().contains(sender.getName())) {
            String feedSet = plugin.getLanguageConfig(target).getString("FeedSet");
            if (feedSet == null) {
                target.sendMessage(plugin.getPrefix() + "§aYour saturation has been filled!");
            } else {
                if (feedSet.contains("&")) feedSet = feedSet.replace('&', '§');
                target.sendMessage(plugin.getPrefix() + feedSet);
            }
        }

        // Optionally notify command sender about the action
        if (notifySender) {
            String feedOther = plugin.getLanguageConfig(sender).getString("FeedOtherSet");
            if (feedOther != null) {
                if (feedOther.contains("&")) feedOther = feedOther.replace('&', '§');
                if (feedOther.contains("%Player%")) feedOther = feedOther.replace("%Player%", target.getName());
                sender.sendMessage(plugin.getPrefix() + feedOther);
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission(plugin.getPermissionBase() + "feed.others")) {
                ArrayList<String> players = new ArrayList<>();
                ArrayList<String> empty = new ArrayList<>();
                players.add("**");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                for (String s : players) {
                    if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                        empty.add(s);
                    }
                }
                Collections.sort(empty);
                return empty;
            }
        }
        return super.onTabComplete(sender, command, label, args);
    }
}
