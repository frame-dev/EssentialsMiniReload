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
            String sub = args[0].toLowerCase();
            String amountStr = args[1];
            String targetName = args[2];
            String mode = args[3].toLowerCase(); // "level" or "xp"

            switch (sub) {
                case "set" -> {
                    Player player = Bukkit.getPlayer(targetName);
                    if (player == null) {
                        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(targetName));
                        return true;
                    }

                    if (mode.equals("level")) {
                        int amount;
                        try {
                            amount = Integer.parseInt(amountStr);
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(getPlugin().getPrefix() + "§cInvalid number: " + amountStr);
                            return true;
                        }
                        player.setLevel(amount);
                        String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
                        if (levelMessage == null) {
                            player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                            return true;
                        }
                        levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                        levelMessage = textUtils.replaceObject(levelMessage, "%Level%", String.valueOf(amount));
                        player.sendMessage(getPlugin().getPrefix() + levelMessage);
                        return true;
                    } else if (mode.equals("xp")) {
                        float amount;
                        try {
                            amount = Float.parseFloat(amountStr);
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(getPlugin().getPrefix() + "§cInvalid number: " + amountStr);
                            return true;
                        }
                        // The Bukkit API setExp() expects a value between 0.0 and 1.0 representing progress to next level.
                        // The original code divided by 10; preserve that behavior but clamp to [0,1].
                        float progress = amount / 10.0f;
                        if (progress < 0f) progress = 0f;
                        if (progress > 1f) progress = 1f;
                        player.setExp(progress);
                        String xpMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.XP");
                        if (xpMessage == null) {
                            player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.XP' not found! Please contact the Admin!");
                            return true;
                        }
                        xpMessage = textUtils.replaceAndWithParagraph(xpMessage);
                        xpMessage = textUtils.replaceObject(xpMessage, "%XP%", String.valueOf(amount));
                        player.sendMessage(getPlugin().getPrefix() + xpMessage);
                        return true;
                    } else {
                        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp set <Amount> <Player> <level|xp>"));
                        return true;
                    }
                }
                case "add" -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(amountStr);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(getPlugin().getPrefix() + "§cInvalid number: " + amountStr);
                        return true;
                    }
                    Player player = Bukkit.getPlayer(targetName);
                    if (player == null) {
                        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(targetName));
                        return true;
                    }
                    if (mode.equals("level")) {
                        int newLevel = player.getLevel() + amount;
                        player.setLevel(newLevel);
                        String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
                        if (levelMessage == null) {
                            player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                            return true;
                        }
                        levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                        levelMessage = textUtils.replaceObject(levelMessage, "%Level%", String.valueOf(newLevel));
                        player.sendMessage(getPlugin().getPrefix() + levelMessage);
                    } else if (mode.equals("xp")) {
                        player.giveExp(amount);
                        String xpMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.XP");
                        if (xpMessage == null) {
                            player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.XP' not found! Please contact the Admin!");
                            return true;
                        }
                        xpMessage = textUtils.replaceAndWithParagraph(xpMessage);
                        xpMessage = textUtils.replaceObject(xpMessage, "%XP%", String.valueOf(player.getTotalExperience()));
                        player.sendMessage(getPlugin().getPrefix() + xpMessage);
                    } else {
                        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp add <Amount> <Player> <level|xp>"));
                        return true;
                    }
                    return true;
                }
                case "remove" -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(amountStr);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(getPlugin().getPrefix() + "§cInvalid number: " + amountStr);
                        return true;
                    }
                    Player player = Bukkit.getPlayer(targetName);
                    if (player == null) {
                        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(targetName));
                        return true;
                    }
                    if (mode.equals("level")) {
                        int newLevel = Math.max(0, player.getLevel() - amount);
                        player.setLevel(newLevel);
                        String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
                        if (levelMessage == null) {
                            player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                            return true;
                        }
                        levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                        levelMessage = textUtils.replaceObject(levelMessage, "%Level%", String.valueOf(newLevel));
                        player.sendMessage(getPlugin().getPrefix() + levelMessage);
                    } else {
                        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp remove <Amount> <Player> <level>"));
                        return true;
                    }
                }
                default -> {
                    // unknown subcommand -- fall through to showing usage below
                }
            }
        } else if (args.length == 5) {
            // xp send <Amount> <Player> <xp/level>
            if (args[0].equalsIgnoreCase("send")) {
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(getPlugin().getPrefix() + "§cInvalid number: " + args[1]);
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[2]));
                    return true;
                }
                String mode = args[3].toLowerCase();
                if (mode.equals("level")) {
                    int targetNewLevel = target.getLevel() + amount;
                    target.setLevel(targetNewLevel);
                    player.setLevel(Math.max(0, player.getLevel() - amount));
                    String levelMessage = getPlugin().getLanguageConfig(target).getString(Variables.EXPERIENCE + ".Self.Level");
                    if (levelMessage == null) {
                        target.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
                        return true;
                    }
                    levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
                    levelMessage = textUtils.replaceObject(levelMessage, "%Level%", String.valueOf(targetNewLevel));
                    target.sendMessage(getPlugin().getPrefix() + levelMessage);
                } else if (mode.equals("xp")) {
                    int targetNewXp = target.getTotalExperience() + amount;
                    target.setTotalExperience(targetNewXp);
                    player.setTotalExperience(Math.max(0, player.getTotalExperience() - amount));
                    String xpMessage = getPlugin().getLanguageConfig(target).getString(Variables.EXPERIENCE + ".Self.XP");
                    if (xpMessage == null) {
                        target.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.XP' not found! Please contact the Admin!");
                        return true;
                    }
                    xpMessage = textUtils.replaceAndWithParagraph(xpMessage);
                    xpMessage = textUtils.replaceObject(xpMessage, "%XP%", String.valueOf(targetNewXp));
                    target.sendMessage(getPlugin().getPrefix() + xpMessage);
                } else {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp send <Amount> <Player> <level|xp>"));
                    return true;
                }
                return true;
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
