package ch.framedev.essentialsmini.commands.playercommands;



/*
 * ch.framedev.essentialsmini.commands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 06.03.2025 17:51
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

public class FlyCMD extends CommandBase {

    private static final String PERM_FLY = "fly";
    private static final String PERM_FLY_OTHERS = "fly.others";

    public FlyCMD(Main plugin) {
        super(plugin, "fly");
        setupTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
                return true;
            }

            if (!sender.hasPermission(getPlugin().getPermissionBase() + PERM_FLY)) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }

            toggleFlightAndNotify(player, sender, false);
            return true;
        }

        if (args.length == 1) {
            if (!sender.hasPermission(getPlugin().getPermissionBase() + PERM_FLY_OTHERS)) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }

            if (args[0].equalsIgnoreCase("**")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    toggleFlightAndNotify(online, sender, true);
                }
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[0]));
                return true;
            }

            toggleFlightAndNotify(target, sender, true);
            return true;
        }

        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/fly §cor §6/fly <PlayerName>"));
        return true;
    }

    private void toggleFlightAndNotify(Player target, CommandSender sender, boolean notifySender) {
        boolean enabling = !target.getAllowFlight();
        target.setAllowFlight(enabling);
        target.setFlying(enabling);

        if (!Main.getSilent().contains(sender.getName())) {
            String key = enabling ? "FlySelfOn" : "FlySelfOff";
            String fallback = enabling ? "§aYou can now fly!" : "§cYour flight has been disabled!";
            target.sendMessage(getPlugin().getPrefix() + getOrFallback(target, key, fallback));
        }

        if (notifySender) {
            String key = enabling ? "FlyOtherOn" : "FlyOtherOff";
            String fallback = enabling ? "§aEnabled flight for %Player%" : "§cDisabled flight for %Player%";
            String message = getOrFallback(sender, key, fallback).replace("%Player%", target.getName());
            sender.sendMessage(getPlugin().getPrefix() + message);
        }
    }

    private String getOrFallback(CommandSender viewer, String path, String fallback) {
        String fromConfig = getPlugin().getLanguageConfig(viewer).getString(path);
        if (fromConfig == null) {
            return fallback;
        }
        return colorize(fromConfig);
    }

    private String colorize(String input) {
        return input.replace('&', '§');
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1 && sender.hasPermission(getPlugin().getPermissionBase() + PERM_FLY_OTHERS)) {
            List<String> options = new ArrayList<>();
            options.add("**");
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
            return filterAndSort(options, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filterAndSort(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        Collections.sort(result);
        return result;
    }
}
