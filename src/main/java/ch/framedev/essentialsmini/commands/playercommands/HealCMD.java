package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 18.08.2020 19:30
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HealCMD extends CommandBase {

    private final Main plugin;

    public HealCMD(Main plugin) {
        super(plugin, "heal");
        setupTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (sender.hasPermission("essentialsmini.heal")) {
                    healPlayerAndNotify(player, sender, false);
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            }
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("**")) {
                if (sender.hasPermission("essentialsmini.heal.others")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player != null) {
                            healPlayerAndNotify(player, sender, true);
                        }
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                if (sender.hasPermission("essentialsmini.heal.others")) {
                    Player player = Bukkit.getPlayer(args[0]);
                    if (player != null) {
                        healPlayerAndNotify(player, sender, true);
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
            return true;
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/heal §coder §6/heal <PlayerName>"));
            return true;
        }
    }

    // Centralized heal + notify logic. notifySender controls whether the command sender
    // receives the "other" message (Heal.Other) and whether the target gets notified
    // depending on Main.getSilent().
    private void healPlayerAndNotify(Player target, CommandSender sender, boolean notifySender) {
        // Heal the player
        target.setHealth(20);
        target.setFireTicks(0);
        target.setFoodLevel(20);

        // Message to target (respect silent)
        if (!Main.getSilent().contains(sender.getName())) {
            String selfMsg = plugin.getLanguageConfig(target).getString("Heal.Self");
            if (selfMsg == null) selfMsg = "§aYou have been healed!";
            if (selfMsg.contains("&")) selfMsg = selfMsg.replace('&', '§');
            target.sendMessage(plugin.getPrefix() + selfMsg);
        }

        // Message to sender (notify about other player healed)
        if (notifySender) {
            // Heal.Other should come from sender's language, not target's
            String otherMsg = plugin.getLanguageConfig(sender).getString("Heal.Other");
            if (otherMsg == null) otherMsg = "§aHealed %Player%";
            otherMsg = otherMsg.replace("%Player%", target.getName());
            if (otherMsg.contains("&")) otherMsg = otherMsg.replace('&', '§');
            sender.sendMessage(plugin.getPrefix() + otherMsg);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission(plugin.getPermissionBase() + "heal.others")) {
                ArrayList<String> players = new ArrayList<>();
                ArrayList<String> empty = new ArrayList<>();

                if (Bukkit.getOnlinePlayers().size() == 1) {
                    return List.of(sender.getName());
                }

                players.add("**"); // Add a wildcard option first

                // Add all online player names to the player list
                Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));

                // Filter player names based on the argument (case-insensitive)
                players.stream()
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .sorted()
                        .forEach(empty::add);
                return empty;
            }
        }
        return super.onTabComplete(sender, command, label, args);
    }
}
