package ch.framedev.essentialsmini.commands.worldcommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.simplejavautils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 25.07.2020 15:30
 */
public class DayNightCMD extends CommandBase {

    private final Main plugin;

    public DayNightCMD(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        setup("day", this);
        setup("night", this);
        setup("pltime", this);
        setup("resetpltime", this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (command.getName().equalsIgnoreCase("day")) {
                if (args.length == 0) {
                    Player player = (Player) sender;
                    if (player.hasPermission("essentialsmini.day")) {
                        player.getWorld().setTime(1000);
                        String message = plugin.getLanguageConfig(player).getString("Day");
                        if (message != null) {
                            message = new TextUtils().replaceAndWithParagraph(message);
                        }
                        player.sendMessage(plugin.getPrefix() + message);
                    } else {
                        player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/day"));
                }
                return true;
            }
            if (command.getName().equalsIgnoreCase("night")) {
                if (args.length == 0) {
                    Player player = (Player) sender;
                    if (player.hasPermission("essentialsmini.night")) {
                        String message = plugin.getLanguageConfig(player).getString("Night");
                        if (message != null) {
                            message = new TextUtils().replaceAndWithParagraph(message);
                        }
                        player.sendMessage(plugin.getPrefix() + message);
                        player.getWorld().setTime(13000);
                    } else {
                        player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/night"));
                }
                return true;
            }
            if (command.getName().equalsIgnoreCase("pltime")) {
                Player player = (Player) sender;
                if (player.hasPermission("essentialsmini.playertime")) {
                    try {
                        player.setPlayerTime(Integer.parseInt(args[0]), false);
                    } catch (Exception ex) {
                        switch (args[0]) {
                            case "day":
                                player.setPlayerTime(0, false);
                                break;
                            case "night":
                                player.setPlayerTime(13000, false);
                                break;
                            default:
                                player.setPlayerTime(1, false);
                        }
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
            if (command.getName().equalsIgnoreCase("resetpltime")) {
                Player player = (Player) sender;
                if (player.hasPermission("essentialsmini.playertime")) {
                    player.resetPlayerTime();
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
        } else {
            if (command.getName().equalsIgnoreCase("day")) {
                for (World world : Bukkit.getWorlds())
                    world.setTime(1000);
                String message = plugin.getLanguageConfig(sender).getString("Day");
                if (message != null) {
                    message = new TextUtils().replaceAndWithParagraph(message);
                }
                sender.sendMessage(plugin.getPrefix() + message);
                return true;
            }
            if (command.getName().equalsIgnoreCase("night")) {
                String message = plugin.getLanguageConfig(sender).getString("Night");
                if (message != null) {
                    message = new TextUtils().replaceAndWithParagraph(message);
                }
                sender.sendMessage(plugin.getPrefix() + message);
                for (World world : Bukkit.getWorlds())
                    world.setTime(13000);
                return true;
            }
        }
        return false;
    }
}
