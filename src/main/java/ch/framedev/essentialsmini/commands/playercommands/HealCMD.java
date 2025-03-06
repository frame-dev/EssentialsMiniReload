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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                if (sender.hasPermission("essentialsmini.heal")) {
                    Player player = (Player) sender;
                    player.setHealth(20);
                    player.setFireTicks(0);
                    player.setFoodLevel(20);

                    // Heal messages from the selected Language Message File
                    String heal = plugin.getLanguageConfig(player).getString("Heal.Self");
                    if (heal == null) return true;
                    if (heal.contains("&"))
                        heal = heal.replace('&', '§');
                    player.sendMessage(plugin.getPrefix() + heal);
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
                            player.setHealth(20);
                            player.setFireTicks(0);
                            player.setFoodLevel(20);
                            if (!Main.getSilent().contains(sender.getName())) {
                                String heal = plugin.getLanguageConfig(player).getString("Heal.Self");
                                if (heal == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'Heal.Self' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (heal.contains("&"))
                                    heal = heal.replace('&', '§');
                                player.sendMessage(plugin.getPrefix() + heal);
                            }
                            String healOther = plugin.getLanguageConfig(player).getString("Heal.Other");
                            if (healOther == null) {
                                player.sendMessage(plugin.getPrefix() + "§cConfig 'Heal.Other' not found! Please contact the Admin!");
                                return true;
                            }
                            if (healOther.contains("&"))
                                healOther = healOther.replace('&', '§');
                            if (healOther.contains("%Player%"))
                                healOther = healOther.replace("%Player%", player.getName());
                            sender.sendMessage(plugin.getPrefix() + healOther);
                        }
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                if (sender.hasPermission("essentialsmini.heal.others")) {
                    Player player = Bukkit.getPlayer(args[0]);
                    if (player != null) {
                        player.setHealth(20);
                        player.setFireTicks(0);
                        player.setFoodLevel(20);
                        if (!Main.getSilent().contains(sender.getName())) {
                            String heal = plugin.getLanguageConfig(player).getString("Heal.Self");
                            if (heal == null) {
                                player.sendMessage(plugin.getPrefix() + "§cConfig 'Heal.Self' not found! Please contact the Admin!");
                                return true;
                            }
                            if (heal.contains("&"))
                                heal = heal.replace('&', '§');
                            player.sendMessage(plugin.getPrefix() + heal);
                        }
                        String healOther = plugin.getLanguageConfig(player).getString("Heal.Other");
                        if (healOther == null) {
                            player.sendMessage(plugin.getPrefix() + "§cConfig 'Heal.Other' not found! Please contact the Admin!");
                            return true;
                        }
                        if (healOther.contains("&"))
                            healOther = healOther.replace('&', '§');
                        if (healOther.contains("%Player%"))
                            healOther = healOther.replace("%Player%", player.getName());
                        sender.sendMessage(plugin.getPrefix() + healOther);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
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
