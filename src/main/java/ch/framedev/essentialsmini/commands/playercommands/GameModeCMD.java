package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 14.07.2020 22:52
 */
public class GameModeCMD extends CommandBase {

    private final Main plugin;

    public GameModeCMD(Main plugin) {
        super(plugin, "gamemode");
        setupTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gamemode")) {
            if (args.length == 1) {
                if (sender.hasPermission("essentialsmini.gamemode")) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                        return true;
                    }
                    String gameModeChanged = plugin.getLanguageConfig(player).getString("GameModeChanged");
                    try {
                        switch (Integer.parseInt(args[0])) {
                            case 0:
                                player.setGameMode(GameMode.SURVIVAL);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SURVIVAL.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            case 1:
                                player.setGameMode(GameMode.CREATIVE);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.CREATIVE.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            case 2:
                                player.setGameMode(GameMode.ADVENTURE);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.ADVENTURE.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            case 3:
                                player.setGameMode(GameMode.SPECTATOR);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SPECTATOR.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            default:
                                player.sendMessage(plugin.getPrefix() + "§cKein gültigen GameMode gefunden mit der Nummer §6" + args[0] + "§c!");
                        }
                    } catch (NumberFormatException ex) {
                        switch (args[0]) {
                            case "survival", "s":
                                player.setGameMode(GameMode.SURVIVAL);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SURVIVAL.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            case "creative", "c":
                                player.setGameMode(GameMode.CREATIVE);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.CREATIVE.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            case "adventure", "a":
                                player.setGameMode(GameMode.ADVENTURE);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.ADVENTURE.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            case "spectator", "sp":
                                player.setGameMode(GameMode.SPECTATOR);
                                if (gameModeChanged == null) {
                                    player.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeChanged.contains("%GameMode%"))
                                    gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SPECTATOR.name());
                                gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                player.sendMessage(plugin.getPrefix() + gameModeChanged);
                                break;
                            default:
                                player.sendMessage(plugin.getPrefix() + "§cNo valid GameMode found §6" + args[0] + "§c!");
                        }
                    }
                } else {
                    sender.sendMessage(Main.getInstance().getPrefix() + plugin.getNoPerms());
                }
            } else if (args.length == 2) {
                if (sender.hasPermission("essentialsmini.gamemode.others")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                        return true;
                    }
                    String gameModeChanged = plugin.getLanguageConfig(target).getString("GameModeChanged");
                    String gameModeOtherChanged = plugin.getLanguageConfig(sender).getString("GameModeOtherChanged");
                    try {
                        switch (Integer.parseInt(args[0])) {
                            case 0:
                                target.setGameMode(GameMode.SURVIVAL);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SURVIVAL.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.SURVIVAL.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            case 1:
                                target.setGameMode(GameMode.CREATIVE);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.CREATIVE.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.CREATIVE.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            case 2:
                                target.setGameMode(GameMode.ADVENTURE);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.ADVENTURE.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.ADVENTURE.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            case 3:
                                target.setGameMode(GameMode.SPECTATOR);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SPECTATOR.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.SPECTATOR.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            default:
                                sender.sendMessage(plugin.getPrefix() + "§cNo valid GameMode found with the number §6" + args[0] + "§c!");
                        }
                    } catch (NumberFormatException ex) {
                        switch (args[0]) {
                            case "survival", "s":
                                target.setGameMode(GameMode.SURVIVAL);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SURVIVAL.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.SURVIVAL.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            case "creative", "c":
                                target.setGameMode(GameMode.CREATIVE);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.CREATIVE.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.CREATIVE.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            case "adventure", "a":
                                target.setGameMode(GameMode.ADVENTURE);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.ADVENTURE.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.ADVENTURE.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            case "spectator", "sp":
                                target.setGameMode(GameMode.SPECTATOR);
                                if (!Main.getSilent().contains(sender.getName())) {
                                    if (gameModeChanged == null) {
                                        sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeChanged' not found! Please contact the Admin!");
                                        return true;
                                    }
                                    if (gameModeChanged.contains("%GameMode%"))
                                        gameModeChanged = gameModeChanged.replace("%GameMode%", GameMode.SPECTATOR.name());
                                    gameModeChanged = ReplaceCharConfig.replaceParagraph(gameModeChanged);
                                    target.sendMessage(plugin.getPrefix() + gameModeChanged);
                                }
                                if (gameModeOtherChanged == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'GameModeOtherChanged' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (gameModeOtherChanged.contains("%Player%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%Player%", target.getName());
                                if (gameModeOtherChanged.contains("%GameMode%"))
                                    gameModeOtherChanged = gameModeOtherChanged.replace("%GameMode%", GameMode.SPECTATOR.name());
                                gameModeOtherChanged = ReplaceCharConfig.replaceParagraph(gameModeOtherChanged);
                                sender.sendMessage(plugin.getPrefix() + gameModeOtherChanged);
                                break;
                            default:
                                sender.sendMessage(plugin.getPrefix() + "§cNo valid GameMode found §6" + args[0] + "§c!");
                        }
                    }
                } else {
                    sender.sendMessage(Main.getInstance().getPrefix() + plugin.getNoPerms());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/gamemode <Gamemode (Name oder Zahl)> §coder §6/gamemode <Gamemode (Name oder Zahl)> <Spieler Name>"));
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static GameMode getGameModeByQualifier(String qualifier) {
        return switch (qualifier) {
            case "0", "s", "su" -> GameMode.SURVIVAL;
            case "1", "c", "cr" -> GameMode.CREATIVE;
            case "2", "a", "ad" -> GameMode.ADVENTURE;
            case "3", "sp", "spe" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    @SuppressWarnings("unused")
    public static GameMode getGameModeById(int id) {
        return switch (id) {
            case 0 -> GameMode.SURVIVAL;
            case 1 -> GameMode.CREATIVE;
            case 2 -> GameMode.ADVENTURE;
            case 3 -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            ArrayList<String> gameModes = new ArrayList<>();
            ArrayList<String> empty = new ArrayList<>();
            gameModes.add("creative");
            gameModes.add("survival");
            gameModes.add("adventure");
            gameModes.add("spectator");
            gameModes.add("c");
            gameModes.add("s");
            gameModes.add("a");
            gameModes.add("sp");
            gameModes.add("0");
            gameModes.add("1");
            gameModes.add("2");
            gameModes.add("3");
            for (String s : gameModes) {
                if (s.toLowerCase().startsWith(args[0])) {
                    empty.add(s);
                }
            }
            Collections.sort(empty);
            return empty;
        }
        return null;
    }
}
