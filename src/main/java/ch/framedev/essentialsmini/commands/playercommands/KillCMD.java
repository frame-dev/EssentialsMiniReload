package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
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
            // use permission base for consistency
            if (sender.hasPermission(plugin.getPermissionBase() + "killall")) {
                if (args.length == 1) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                        return true;
                    }

                    String sub = args[0].toLowerCase();

                    switch (sub) {
                        case "animals" -> {
                            for (Entity entity : player.getWorld().getEntities()) {
                                if (entity instanceof Animals) {
                                    entity.remove();
                                }
                            }
                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Tiere in deiner Umgebung entfernt!");
                        }
                        case "mobs" -> {
                            for (Entity entity : player.getWorld().getEntities()) {
                                if (entity instanceof Mob) {
                                    entity.remove();
                                }
                            }
                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Monster in deiner Umgebung entfernt!");
                        }
                        case "players" -> {
                            // Kill players (set health to 0) but DON'T call Entity.remove() on Player
                            for (Player p : player.getWorld().getPlayers()) {
                                if (!p.getUniqueId().equals(player.getUniqueId())) { // exclude command sender
                                    try {
                                        p.setHealth(0);
                                        p.setFoodLevel(0);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Spieler in deiner Umgebung entfernt!");
                        }
                        case "items" -> {
                            for (Entity entity : player.getWorld().getEntities()) {
                                if (entity instanceof Item) {
                                    entity.remove();
                                }
                            }
                            player.sendMessage(plugin.getPrefix() + "§aEs wurden alle Items in deiner Umgebung entfernt!");
                        }
                        default -> player.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/killall <animals|mobs|players|items>"));
                    }
                    return true;
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/killall <animals|mobs|players|items>"));
                    return true;
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("suicid")) {
            if (args.length == 0) {
                if (sender.hasPermission(plugin.getPermissionBase() + "suicid")) {
                    if (sender instanceof Player player) {
                        if (!suicidPlayers.contains(player)) suicidPlayers.add(player);
                        try {
                            player.setHealth(0);
                            player.setFoodLevel(0);
                        } catch (Exception ignored) {
                        }
                        // broadcast to world players in player's world
                        player.getWorld().getPlayers().forEach(p -> p.sendMessage(plugin.getPrefix() + "§6" + player.getName() + " §ahat Suizid begangen!"));
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
                return true;
            } else if (args.length == 1) {
                if (sender.hasPermission(plugin.getPermissionBase() + "suicid.others")) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                        return true;
                    }
                    if (!suicidPlayers.contains(target)) suicidPlayers.add(target);
                    try {
                        target.setHealth(0);
                        target.setFoodLevel(0);
                    } catch (Exception ignored) {
                    }
                    // broadcast in target world
                    target.getWorld().getPlayers().forEach(p -> p.sendMessage(plugin.getPrefix() + "§6" + target.getName() + " §ahat Suizid begangen!"));
                    sender.sendMessage(plugin.getPrefix() + "§6" + target.getName() + " §ahat Suizid begangen!");
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
                return true;
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
