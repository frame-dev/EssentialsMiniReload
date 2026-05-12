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
import java.util.Locale;

public class FeedCMD extends CommandBase {

    private static final String PERM_FEED = "feed";
    private static final String PERM_FEED_OTHERS = "feed.others";

    private final Main plugin;

    public FeedCMD(Main plugin) {
        super(plugin, "feed");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }
            if (!sender.hasPermission(plugin.getPermissionBase() + PERM_FEED)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }
            feedPlayerAndNotify(player, sender, false);
            return true;
        }

        if (args.length == 1) {
            if (!sender.hasPermission(plugin.getPermissionBase() + PERM_FEED_OTHERS)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }

            if (args[0].equalsIgnoreCase("**")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    feedPlayerAndNotify(online, sender, true);
                }
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                return true;
            }

            feedPlayerAndNotify(target, sender, true);
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/feed §cor §6/feed <PlayerName>"));
        return true;
    }

    private void feedPlayerAndNotify(Player target, CommandSender sender, boolean notifySender) {
        target.setFoodLevel(20);

        if (!Main.getSilent().contains(sender.getName())) {
            String feedSet = colorize(plugin.getLanguageConfig(target).getString("FeedSet"));
            if (feedSet == null) {
                target.sendMessage(plugin.getPrefix() + "§aYour saturation has been filled!");
            } else {
                target.sendMessage(plugin.getPrefix() + feedSet);
            }
        }

        if (notifySender) {
            String feedOther = colorize(plugin.getLanguageConfig(sender).getString("FeedOtherSet"));
            if (feedOther != null) {
                sender.sendMessage(plugin.getPrefix() + feedOther.replace("%Player%", target.getName()));
            }
        }
    }

    private String colorize(String message) {
        if (message == null) {
            return null;
        }
        return message.replace('&', '§');
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1 && sender.hasPermission(plugin.getPermissionBase() + PERM_FEED_OTHERS)) {
            List<String> candidates = new ArrayList<>();
            candidates.add("**");
            for (Player player : Bukkit.getOnlinePlayers()) {
                candidates.add(player.getName());
            }
            return filterAndSort(candidates, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filterAndSort(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                out.add(value);
            }
        }
        Collections.sort(out);
        return out;
    }
}
