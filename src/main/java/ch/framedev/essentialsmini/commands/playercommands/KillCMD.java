package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 18.07.2020 13:32
 */
public class KillCMD extends CommandBase {

    private final Main plugin;
    public static final List<Player> suicidPlayers = new ArrayList<>();

    public KillCMD(Main plugin) {
        super(plugin, "killall", "suicid");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("killall")) {
            if (sender.hasPermission(new Permission(plugin.getPermissionBase() + "killall", PermissionDefault.OP))) {
                if (args.length == 1) {
                    if (sender instanceof Player player) {
                        if (args[0].equalsIgnoreCase("animals")) {
                            for (Entity entity : player.getWorld().getEntities()) {
                                if (entity instanceof Animals) {
                                    entity.remove();
                                }
                            }
                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Tiere in deiner Umgebung entfernt!");
                        } else if (args[0].equalsIgnoreCase("mobs")) {
                            for (Entity entity : player.getWorld().getEntities()) {
                                if (entity instanceof Mob) {
                                    entity.remove();
                                }
                            }
                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Monster in deiner Umgebung entfernt!");
                        } else if (args[0].equalsIgnoreCase("players")) {
                            for (Entity entity : player.getWorld().getEntities()) {
                                if (entity instanceof Player) {
                                    entity.remove();
                                }
                            }

                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Spieler in deiner Umgebung entfernt!");
                        } else if (args[0].equalsIgnoreCase("items")) {
                            for (Entity entity : player.getWorld().getEntities()) {
                                if (entity instanceof Item) {
                                    entity.remove();
                                }
                            }
                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Items in deiner Umgebung entfernt!");
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        }
        if (command.getName().equalsIgnoreCase("suicid")) {
            if(args.length == 0) {
                if (sender.hasPermission(plugin.getPermissionBase() + "suicid")) {
                    if (sender instanceof Player) {
                        if(!suicidPlayers.contains((Player) sender))
                            suicidPlayers.add((Player) sender);
                        ((Player) sender).setHealth(0);
                        ((Player) sender).setFoodLevel(0);
                        ((Player) sender).getWorld().getPlayers().forEach(players -> players.sendMessage("§6" + sender.getName() + " §ahat Suicid begangen!"));
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else if(args.length == 1) {
                if(sender.hasPermission(plugin.getPermissionBase() + "suicid.others")) {
                    Player player = Bukkit.getPlayer(args[0]);
                    if(player == null) {
                        sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                        return true;
                    }
                    if(!suicidPlayers.contains(player))
                        suicidPlayers.add(player);
                    player.setHealth(0);
                    player.setFoodLevel(0);
                    player.getWorld().getPlayers().forEach(players -> players.sendMessage("§6" + player.getName() + " §ahat Suicid begangen!"));
                    sender.sendMessage(plugin.getPrefix() + "§6" + player.getName() + " §ahat Suicid begangen!");
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("killall")) {
            if (sender.hasPermission(plugin.getPermissionBase() + "killall")) {
                if (args.length == 1) {
                    ArrayList<String> empty = getCommands(args);
                    Collections.sort(empty);
                    return empty;
                }
            }
        }
        return null;
    }

    private static @NotNull ArrayList<String> getCommands(String[] args) {
        ArrayList<String> commands = new ArrayList<>();
        ArrayList<String> empty = new ArrayList<>();
        commands.add("items");
        commands.add("players");
        commands.add("mobs");
        commands.add("animals");
        for (String s : commands) {
            if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                empty.add(s);
            }
        }
        return empty;
    }
}
