package ch.framedev.essentialsmini.commands.playercommands;


/*
 * de.framedev.essentialsmini.commands
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 20.09.2020 18:26
 */

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class GodCMD implements CommandExecutor {

    private final Main plugin;

    public GodCMD(Main plugin) {
        this.plugin = plugin;
        plugin.getCommands().put("godmode", this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission(new Permission(plugin.getPermissionBase() + "god", PermissionDefault.OP))) {
                player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }
            if (player.isInvulnerable()) {
                player.setInvulnerable(false);
                String godSelfOff = plugin.getLanguageConfig(player).getString("God.Self.Deactivated");
                if(godSelfOff == null) {
                    player.sendMessage(plugin.getPrefix() + "§cConfig 'God.Self.Deactivated' not found! Please contact the Admin!");
                    return true;
                }
                if (godSelfOff.contains("&"))
                    godSelfOff = godSelfOff.replace('&', '§');
                player.sendMessage(plugin.getPrefix() + godSelfOff);
            } else {
                player.setInvulnerable(true);
                String godSelfOn = plugin.getLanguageConfig(player).getString("God.Self.Activated");
                if(godSelfOn == null) {
                    player.sendMessage(plugin.getPrefix() + "§cConfig 'God.Self.Activated' not found! Please contact the Admin!");
                    return true;
                }
                if (godSelfOn.contains("&"))
                    godSelfOn = godSelfOn.replace('&', '§');
                player.sendMessage(plugin.getPrefix() + godSelfOn);
            }
            return true;
        } else if (args.length == 1) {
            Player player = Bukkit.getPlayer(args[0]);
            if (player != null) {
                if (!sender.hasPermission(new Permission(plugin.getPermissionBase() + "god.others", PermissionDefault.OP))) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                if (player.isInvulnerable()) {
                    player.setInvulnerable(false);
                    if (!Main.getSilent().contains(sender.getName())) {
                        String godSelfOff = plugin.getLanguageConfig(player).getString("God.Self.Deactivated");
                        if(godSelfOff == null) {
                            player.sendMessage(plugin.getPrefix() + "§cConfig 'God.Self.Deactivated' not found! Please contact the Admin!");
                            return true;
                        }
                        if (godSelfOff.contains("&"))
                            godSelfOff = godSelfOff.replace('&', '§');
                        player.sendMessage(plugin.getPrefix() + godSelfOff);
                    }
                    String godOtherOff = plugin.getLanguageConfig(sender).getString("God.Other.Deactivated");
                    if(godOtherOff == null) {
                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'God.Other.Deactivated' not found! Please contact the Admin!");
                        return true;
                    }
                    if (godOtherOff.contains("%Player%"))
                        godOtherOff = godOtherOff.replace("%Player%", player.getName());
                    if (godOtherOff.contains("&"))
                        godOtherOff = godOtherOff.replace('&', '§');
                    sender.sendMessage(plugin.getPrefix() + godOtherOff);
                } else {
                    player.setInvulnerable(true);
                    if (!Main.getSilent().contains(sender.getName())) {
                        String godSelfOn = plugin.getLanguageConfig(player).getString("God.Self.Activated");
                        if(godSelfOn == null) {
                            player.sendMessage(plugin.getPrefix() + "§cConfig 'God.Self.Activated' not found! Please contact the Admin!");
                            return true;
                        }
                        if (godSelfOn.contains("&"))
                            godSelfOn = godSelfOn.replace('&', '§');
                        player.sendMessage(plugin.getPrefix() + godSelfOn);
                    }
                    String godOtherOff = plugin.getLanguageConfig(sender).getString("God.Other.Activated");
                    if(godOtherOff == null) {
                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'God.Other.Activated' not found! Please contact the Admin!");
                        return true;
                    }
                    if (godOtherOff.contains("%Player%"))
                        godOtherOff = godOtherOff.replace("%Player%", player.getName());
                    if (godOtherOff.contains("&"))
                        godOtherOff = godOtherOff.replace('&', '§');
                    sender.sendMessage(plugin.getPrefix() + godOtherOff);
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
            }
            return true;
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/god"));
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/god <SpielerName>"));
            return true;
        }
    }
}
