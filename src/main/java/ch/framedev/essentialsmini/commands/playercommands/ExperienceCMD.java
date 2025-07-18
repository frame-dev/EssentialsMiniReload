package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.Variables;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * / This Plugin was Created by FrameDev
 * / Package : de.framedev.essentialsmini.commands.playercommands
 * / ClassName ExperienceCMD
 * / Date: 03.10.21
 * / Project: EssentialsMini
 * / Copyrighted by FrameDev
 */

public class ExperienceCMD extends CommandBase {

    private final TextUtils textUtils;

    public ExperienceCMD(Main plugin) {
        super(plugin, "xp");
        setupTabCompleter(this);
        this.textUtils = new TextUtils();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 4) {
            if (!sender.hasPermission(getPlugin().getPermissionBase() + "xp")) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }
            if (args[0].equalsIgnoreCase("set")) {
                Object amount;
                if (args[1].contains(".")) {
                    amount = Float.parseFloat(args[1]);
                } else {
                    amount = Integer.parseInt(args[1]);
                }
                Player player = Bukkit.getPlayer(args[2]);
                if (player == null) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[2]));
                    return true;
                }
                String xpMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.XP");
                if(xpMessage == null) {
                    player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.XP' not found! Please contact the Admin!");
                    return true;
                }
                xpMessage = textUtils.replaceAndWithParagraph(xpMessage);
                xpMessage = textUtils.replaceObject(xpMessage, "%XP%", amount + "");
                String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
                if(levelMessage == null) {
                    player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                    return true;
                }
                levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                levelMessage = textUtils.replaceObject(levelMessage, "%Level%", amount + "");
                if (args[3].equalsIgnoreCase("level")) {
                    assert amount instanceof Integer;
                    player.setLevel((Integer) amount);
                    player.sendMessage(getPlugin().getPrefix() + levelMessage);
                } else if (args[3].equalsIgnoreCase("xp")) {
                    assert amount instanceof Float;
                    player.setExp((Float) amount / 10);
                    player.sendMessage(getPlugin().getPrefix() + xpMessage);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("add")) {
                int amount = Integer.parseInt(args[1]);
                Player player = Bukkit.getPlayer(args[2]);
                if (player == null) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[2]));
                    return true;
                }
                int level = player.getLevel();
                level += amount;
                String xpMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.XP");
                if(xpMessage == null) {
                    player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.XP' not found! Please contact the Admin!");
                    return true;
                }
                xpMessage = textUtils.replaceAndWithParagraph(xpMessage);
                xpMessage = textUtils.replaceObject(xpMessage, "%XP%", player.getTotalExperience() + amount + "");
                String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
                if(levelMessage == null) {
                    player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                    return true;
                }
                levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                levelMessage = textUtils.replaceObject(levelMessage, "%Level%", level + "");
                if (args[3].equalsIgnoreCase("level")) {
                    player.setLevel(level);
                    player.sendMessage(getPlugin().getPrefix() + levelMessage);
                } else if (args[3].equalsIgnoreCase("xp")) {
                    player.giveExp(amount);
                    player.sendMessage(getPlugin().getPrefix() + xpMessage);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                int amount = Integer.parseInt(args[1]);
                Player player = Bukkit.getPlayer(args[2]);
                if (player == null) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[2]));
                    return true;
                }
                int level = player.getLevel();
                level -= amount;
                String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
                if(levelMessage == null) {
                    player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                    return true;
                }
                levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                levelMessage = textUtils.replaceObject(levelMessage, "%Level%", level + "");
                if (args[3].equalsIgnoreCase("level")) {
                    player.setLevel(level);
                    player.sendMessage(getPlugin().getPrefix() + levelMessage);
                }
                return true;
            }
        } else if (args.length == 5) {
            // xp send <Amount> <Player> <xp/level>
            if (args[0].equalsIgnoreCase("send")) {
                int amount = Integer.parseInt(args[1]);
                Player player = (Player) sender;
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[2]));
                    return true;
                }
                int level = target.getLevel();
                level += amount;
                int xp = target.getTotalExperience();
                xp += amount;
                String xpMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.XP");
                if(xpMessage == null) {
                    player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.XP' not found! Please contact the Admin!");
                    return true;
                }
                xpMessage = textUtils.replaceAndWithParagraph(xpMessage);
                xpMessage = textUtils.replaceObject(xpMessage, "%XP%", xp + "");
                String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
                if(levelMessage == null) {
                    player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                    return true;
                }
                levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                levelMessage = textUtils.replaceObject(levelMessage, "%Level%", level + "");
                if (args[3].equalsIgnoreCase("level")) {
                    target.setLevel(level);
                    player.setLevel(player.getLevel() - amount);
                    target.sendMessage(getPlugin().getPrefix() + levelMessage);
                } else if (args[3].equalsIgnoreCase("xp")) {
                    target.setTotalExperience(xp);
                    player.setTotalExperience(player.getTotalExperience() - amount);
                    target.sendMessage(getPlugin().getPrefix() + xpMessage);
                }
            }
        } else {
            sender.sendMessage(getPlugin().getPrefix() + "§cUsage: /xp <set/add/remove/send> <Amount> <Player> <xp/level>");
            sender.sendMessage(getPlugin().getPrefix() + "§cExample: /xp set 100 PlayerName level");
            sender.sendMessage(getPlugin().getPrefix() + "§cExample: /xp add 50 PlayerName xp");
            sender.sendMessage(getPlugin().getPrefix() + "§cExample: /xp remove 20 PlayerName level");
            sender.sendMessage(getPlugin().getPrefix() + "§cExample: /xp send 10 PlayerName xp");
            sender.sendMessage(getPlugin().getPrefix() + "§cExample: /xp send 5 PlayerName level");
            sender.sendMessage(getPlugin().getPrefix() + "§cYou can use 'level' or 'xp' as the last argument.");
            return true;
        }
        return super.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> commands = new ArrayList<>();
        commands.add("set");
        commands.add("add");
        commands.add("remove");
        commands.add("send");
        if (args.length == 1) {
            List<String> empty = new ArrayList<>();
            for (String s : commands) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase()))
                    empty.add(s);
            }
            Collections.sort(empty);
            return empty;
        } else if (args.length == 4) {
            List<String> argFourCommands = new ArrayList<>();
            argFourCommands.add("level");
            if (!args[0].equalsIgnoreCase("remove"))
                argFourCommands.add("xp");
            List<String> empty = new ArrayList<>();
            for (String s : argFourCommands) {
                if (s.toLowerCase().startsWith(args[3].toLowerCase()))
                    empty.add(s);
            }
            Collections.sort(empty);
            return empty;
        }
        return super.onTabComplete(sender, command, label, args);
    }
}
