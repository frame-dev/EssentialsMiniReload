package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.simplejavautils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 15.07.2020 11:59
 */
public class VanishCMD extends CommandListenerBase {

    private final Main plugin;
    public static ArrayList<String> hided = new ArrayList<>();

    public VanishCMD(Main plugin) {
        super(plugin, "vanish");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("vanish")) {
            if (sender.hasPermission("essentialsmini.vanish")) {
                if (args.length == 0) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (hided.contains(player.getName())) {
                            Bukkit.getOnlinePlayers().forEach(o -> {
                                o.showPlayer(this.plugin, player);
                            });
                            hided.remove(player.getName());
                            String message = plugin.getLanguageConfig(player).getString("VanishOff.Single");
                            if(message == null) {
                                player.sendMessage(plugin.getPrefix() + "§cConfig 'VanishOff.Single' not found!");
                                return true;
                            }
                            if (message.contains("&"))
                                message = new TextUtils().replaceAndWithParagraph(message);
                            player.sendMessage(plugin.getPrefix() + message);
                            if (plugin.getConfig().getBoolean("Vanish.Message")) {
                                String joinMessage = plugin.getConfig().getString("JoinMessage");
                                if(joinMessage == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'JoinMessage' not found!");
                                    return true;
                                }
                                if (joinMessage.contains("&"))
                                    joinMessage = joinMessage.replace('&', '§');
                                if (joinMessage.contains("%Player%"))
                                    joinMessage = joinMessage.replace("%Player%", player.getName());
                                Bukkit.broadcastMessage(joinMessage);
                            }
                            return true;
                        } else {
                            Bukkit.getOnlinePlayers().forEach(o -> {
                                if (!o.hasPermission("essentialsmini.vanish.see")) {
                                    o.hidePlayer(this.plugin, player);
                                }
                            });
                            hided.add(player.getName());
                            String message = plugin.getLanguageConfig(player).getString("VanishOn.Single");
                            if(message == null) {
                                player.sendMessage(plugin.getPrefix() + "§cConfig 'VanishOn.Single' not found!");
                                return true;
                            }
                            if (message.contains("&"))
                                message = new TextUtils().replaceAndWithParagraph(message);
                            player.sendMessage(plugin.getPrefix() + message);
                            if (plugin.getConfig().getBoolean("Vanish.Message")) {
                                String leaveMessage = plugin.getConfig().getString("LeaveMessage");
                                if(leaveMessage == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'LeaveMessage' not found!");
                                    return true;
                                }
                                if (leaveMessage.contains("&"))
                                    leaveMessage = leaveMessage.replace('&', '§');
                                if (leaveMessage.contains("%Player%"))
                                    leaveMessage = leaveMessage.replace("%Player%", player.getName());
                                Bukkit.broadcastMessage(leaveMessage);
                            }
                            return true;
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                } else if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        if (hided.contains(target.getName())) {
                            Bukkit.getOnlinePlayers().forEach(o -> {
                                o.showPlayer(this.plugin, target);
                            });
                            hided.remove(target.getName());
                            String message = plugin.getLanguageConfig(sender).getString("VanishOff.Single");
                            if(message == null) {
                                sender.sendMessage(plugin.getPrefix() + "§cConfig 'VanishOff.Single' not found!");
                                return true;
                            }
                            if (message.contains("&"))
                                message = new TextUtils().replaceAndWithParagraph(message);
                            String playerMessage = plugin.getLanguageConfig(sender).getString("VanishOff.Multi");
                            if(playerMessage == null) {
                                sender.sendMessage(plugin.getPrefix() + "§cConfig 'VanishOff.Multi' not found!");
                                return true;
                            }
                            if (playerMessage.contains("%Player%"))
                                playerMessage = playerMessage.replace("%Player%", target.getName());
                            if (playerMessage.contains("&")) playerMessage = playerMessage.replace('&', '§');
                            if (!Main.getSilent().contains(sender.getName()))
                                target.sendMessage(plugin.getPrefix() + message);
                            sender.sendMessage(plugin.getPrefix() + playerMessage);
                            return true;

                        } else {
                            Bukkit.getOnlinePlayers().forEach(o -> {
                                if (!o.hasPermission("essentialsmini.vanish.see")) {
                                    o.hidePlayer(this.plugin, target);
                                }
                            });
                            hided.add(target.getName());
                            String message = plugin.getLanguageConfig(sender).getString("VanishOn.Single");
                            if(message == null) {
                                sender.sendMessage(plugin.getPrefix() + "§cConfig 'VanishOn.Single' not found!");
                                return true;
                            }
                            if (message.contains("&"))
                                message = new TextUtils().replaceAndWithParagraph(message);
                            String playerMessage = plugin.getLanguageConfig(sender).getString("VanishOn.Multi");
                            if(playerMessage == null) {
                                sender.sendMessage(plugin.getPrefix() + "§cConfig 'VanishOn.Multi' not found!");
                                return true;
                            }
                            if (playerMessage.contains("%Player%"))
                                playerMessage = playerMessage.replace("%Player%", target.getName());
                            if (playerMessage.contains("&")) playerMessage = playerMessage.replace('&', '§');
                            if (!Main.getSilent().contains(sender.getName()))
                                target.sendMessage(plugin.getPrefix() + message);
                            sender.sendMessage(plugin.getPrefix() + playerMessage);
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                    }
                    return true;
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/vanish §coder §6/vanish <PlayerName>"));
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (String vanish : hided) {
            Player vanishPlayer = Bukkit.getPlayer(vanish);
            if(vanishPlayer == null) return;
            if (!player.hasPermission("essentialsmini.vanish.see")) {
                player.hidePlayer(plugin, vanishPlayer);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hided.remove(event.getPlayer().getName());
    }
}
