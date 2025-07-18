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

public class FlyCMD extends CommandBase {
    public FlyCMD(Main plugin) {
        super(plugin, "fly");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
                return true;
            }
            if (player.hasPermission("essentialsmini.fly")) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    String flySelfOn = getPlugin().getLanguageConfig(player).getString("FlySelfOn");
                    if (flySelfOn == null) {
                        player.sendMessage(getPlugin().getPrefix() + "§cConfig 'FlySelfOn' not found! Please contact the Admin!");
                        return true;
                    }
                    if (flySelfOn.contains("&"))
                        flySelfOn = flySelfOn.replace('&', '§');
                    player.sendMessage(getPlugin().getPrefix() + flySelfOn);
                } else {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    String flySelfOff = getPlugin().getLanguageConfig(player).getString("FlySelfOff");
                    if (flySelfOff == null) {
                        player.sendMessage(getPlugin().getPrefix() + "§cConfig 'FlySelfOff' not found! Please contact the Admin!");
                        return true;
                    }
                    if (flySelfOff.contains("&"))
                        flySelfOff = flySelfOff.replace('&', '§');
                    player.sendMessage(getPlugin().getPrefix() + flySelfOff);
                }
            } else {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            }
        } else if (args.length == 1) {
            if (sender.hasPermission("essentialsmini.fly")) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    if (!target.getAllowFlight()) {
                        target.setAllowFlight(true);
                        target.setFlying(true);
                        if (!Main.getSilent().contains(sender.getName())) {
                            String flySelfOn = getPlugin().getLanguageConfig(target).getString("FlySelfOn");
                            if (flySelfOn == null) {
                                target.sendMessage(getPlugin().getPrefix() + "§cConfig 'FlySelfOn' not found! Please contact the Admin!");
                                return true;
                            }
                            if (flySelfOn.contains("&"))
                                flySelfOn = flySelfOn.replace('&', '§');
                            target.sendMessage(getPlugin().getPrefix() + flySelfOn);
                        }
                        String flyOtherOn = getPlugin().getLanguageConfig(sender).getString("FlyOtherOn");
                        if (flyOtherOn == null) {
                            sender.sendMessage(getPlugin().getPrefix() + "§cConfig 'FlyOtherOn' not found! Please contact the Admin!");
                            return true;
                        }
                        if (flyOtherOn.contains("&"))
                            flyOtherOn = flyOtherOn.replace('&', '§');
                        if (flyOtherOn.contains("%Player%"))
                            flyOtherOn = flyOtherOn.replace("%Player%", target.getName());
                        sender.sendMessage(getPlugin().getPrefix() + flyOtherOn);
                    } else {
                        target.setAllowFlight(false);
                        target.setFlying(false);
                        if (!Main.getSilent().contains(sender.getName())) {
                            String flySelfOff = getPlugin().getLanguageConfig(target).getString("FlySelfOff");
                            if (flySelfOff == null) {
                                target.sendMessage(getPlugin().getPrefix() + "§cConfig 'FlySelfOff' not found! Please contact the Admin!");
                                return true;
                            }
                            if (flySelfOff.contains("&"))
                                flySelfOff = flySelfOff.replace('&', '§');
                            target.sendMessage(getPlugin().getPrefix() + flySelfOff);
                        }
                        String flyOtherOff = getPlugin().getLanguageConfig(sender).getString("FlyOtherOff");
                        if (flyOtherOff == null) {
                            sender.sendMessage(getPlugin().getPrefix() + "§cConfig 'FlyOtherOff' not found! Please contact the Admin!");
                            return true;
                        }
                        if (flyOtherOff.contains("&"))
                            flyOtherOff = flyOtherOff.replace('&', '§');
                        if (flyOtherOff.contains("%Player%"))
                            flyOtherOff = flyOtherOff.replace("%Player%", target.getName());
                        sender.sendMessage(getPlugin().getPrefix() + flyOtherOff);
                    }
                } else {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[0]));
                }
            } else {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            }
        }
        return false;
    }
}
