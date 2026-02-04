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

public class FlyCMD extends CommandBase {
    public FlyCMD(Main plugin) {
        super(plugin, "fly");
        // enable tab completion for player names and the "**" wildcard
        setupTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
                return true;
            }
            if (player.hasPermission("essentialsmini.fly")) {
                toggleFlightAndNotify(player, sender, false);
            } else {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            }
            return true;
        } else if (args.length == 1) {
            if (sender.hasPermission("essentialsmini.fly")) {
                if (args[0].equalsIgnoreCase("**")) {
                    // Toggle flight for all online players
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        toggleFlightAndNotify(online, sender, true);
                    }
                } else {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        toggleFlightAndNotify(target, sender, true);
                    } else {
                        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[0]));
                    }
                }
            } else {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            }
            return true;
        }
        return true;
    }

    // Helper toggles flight for target and sends messages to target and sender (if notifySender=true).
    private void toggleFlightAndNotify(Player target, CommandSender sender, boolean notifySender) {
        boolean enabling = !target.getAllowFlight();
        if (enabling) {
            target.setAllowFlight(true);
            target.setFlying(true);
            // Notify target unless sender is silent
            if (!Main.getSilent().contains(sender.getName())) {
                String flySelfOn = getPlugin().getLanguageConfig(target).getString("FlySelfOn");
                if (flySelfOn == null) {
                    target.sendMessage(getPlugin().getPrefix() + "§aYou can now fly!");
                } else {
                    if (flySelfOn.contains("&")) flySelfOn = flySelfOn.replace('&', '§');
                    target.sendMessage(getPlugin().getPrefix() + flySelfOn);
                }
            }
            // Notify sender about the change
            if (notifySender) {
                String flyOtherOn = getPlugin().getLanguageConfig(sender).getString("FlyOtherOn");
                if (flyOtherOn == null) {
                    sender.sendMessage(getPlugin().getPrefix() + "§aEnabled flight for %Player%".replace("%Player%", target.getName()));
                } else {
                    if (flyOtherOn.contains("&")) flyOtherOn = flyOtherOn.replace('&', '§');
                    if (flyOtherOn.contains("%Player%")) flyOtherOn = flyOtherOn.replace("%Player%", target.getName());
                    sender.sendMessage(getPlugin().getPrefix() + flyOtherOn);
                }
            }
        } else {
            target.setAllowFlight(false);
            target.setFlying(false);
            if (!Main.getSilent().contains(sender.getName())) {
                String flySelfOff = getPlugin().getLanguageConfig(target).getString("FlySelfOff");
                if (flySelfOff == null) {
                    target.sendMessage(getPlugin().getPrefix() + "§cYour flight has been disabled!");
                } else {
                    if (flySelfOff.contains("&")) flySelfOff = flySelfOff.replace('&', '§');
                    target.sendMessage(getPlugin().getPrefix() + flySelfOff);
                }
            }
            if (notifySender) {
                String flyOtherOff = getPlugin().getLanguageConfig(sender).getString("FlyOtherOff");
                if (flyOtherOff == null) {
                    sender.sendMessage(getPlugin().getPrefix() + "§cDisabled flight for %Player%".replace("%Player%", target.getName()));
                } else {
                    if (flyOtherOff.contains("&")) flyOtherOff = flyOtherOff.replace('&', '§');
                    if (flyOtherOff.contains("%Player%")) flyOtherOff = flyOtherOff.replace("%Player%", target.getName());
                    sender.sendMessage(getPlugin().getPrefix() + flyOtherOff);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("**");
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
            List<String> result = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String s : options) {
                if (s.toLowerCase().startsWith(prefix)) result.add(s);
            }
            Collections.sort(result);
            return result;
        }
        return super.onTabComplete(sender, command, label, args);
    }
}
