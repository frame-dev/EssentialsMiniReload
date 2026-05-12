package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TextUtils;
import ch.framedev.essentialsmini.utils.Variables;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * / This Plugin was Created by FrameDev
 * / Package : de.framedev.essentialsmini.commands.playercommands
 * / ClassName ExperienceCMD
 * / Date: 03.10.21
 * / Project: EssentialsMini
 * / Copyrighted by FrameDev
 */
public class ExperienceCMD extends CommandBase {

    private static final String SUB_SET = "set";
    private static final String SUB_ADD = "add";
    private static final String SUB_REMOVE = "remove";
    private static final String SUB_SEND = "send";

    private static final String MODE_LEVEL = "level";
    private static final String MODE_XP = "xp";

    private final TextUtils textUtils;

    public ExperienceCMD(Main plugin) {
        super(plugin, "xp");
        setupTabCompleter(this);
        this.textUtils = new TextUtils();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(getPlugin().getPermissionBase() + "xp")) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }

        if (args.length != 4) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String amountStr = args[1];
        String targetName = args[2];
        String mode = args[3].toLowerCase(Locale.ROOT);

        return switch (sub) {
            case SUB_SET -> handleSet(sender, amountStr, targetName, mode);
            case SUB_ADD -> handleAdd(sender, amountStr, targetName, mode);
            case SUB_REMOVE -> handleRemove(sender, amountStr, targetName, mode);
            case SUB_SEND -> handleSend(sender, amountStr, targetName, mode);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleSet(CommandSender sender, String amountStr, String targetName, String mode) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(targetName));
            return true;
        }

        if (MODE_LEVEL.equals(mode)) {
            Integer amount = parseInt(amountStr, sender);
            if (amount == null) {
                return true;
            }
            int level = Math.max(0, amount);
            target.setLevel(level);
            sendLevelMessage(target, level);
            return true;
        }

        if (MODE_XP.equals(mode)) {
            Float amount = parseFloat(amountStr, sender);
            if (amount == null) {
                return true;
            }
            // setExp expects progress to next level in [0,1]. Keep old division behavior and clamp.
            float progress = Math.max(0.0f, Math.min(1.0f, amount / 10.0f));
            target.setExp(progress);
            sendXpMessage(target, String.valueOf(amount));
            return true;
        }

        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp set <Amount> <Player> <level|xp>"));
        return true;
    }

    private boolean handleAdd(CommandSender sender, String amountStr, String targetName, String mode) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(targetName));
            return true;
        }

        Integer amount = parseInt(amountStr, sender);
        if (amount == null) {
            return true;
        }

        if (MODE_LEVEL.equals(mode)) {
            int newLevel = Math.max(0, target.getLevel() + amount);
            target.setLevel(newLevel);
            sendLevelMessage(target, newLevel);
            return true;
        }

        if (MODE_XP.equals(mode)) {
            target.giveExp(amount);
            sendXpMessage(target, String.valueOf(target.getTotalExperience()));
            return true;
        }

        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp add <Amount> <Player> <level|xp>"));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String amountStr, String targetName, String mode) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(targetName));
            return true;
        }

        Integer amount = parseInt(amountStr, sender);
        if (amount == null) {
            return true;
        }

        if (MODE_LEVEL.equals(mode)) {
            int newLevel = Math.max(0, target.getLevel() - amount);
            target.setLevel(newLevel);
            sendLevelMessage(target, newLevel);
            return true;
        }

        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp remove <Amount> <Player> <level>"));
        return true;
    }

    private boolean handleSend(CommandSender sender, String amountStr, String targetName, String mode) {
        if (!(sender instanceof Player source)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(targetName));
            return true;
        }

        Integer amount = parseInt(amountStr, sender);
        if (amount == null) {
            return true;
        }

        if (MODE_LEVEL.equals(mode)) {
            int transferable = Math.max(0, Math.min(amount, source.getLevel()));
            source.setLevel(source.getLevel() - transferable);
            int targetNewLevel = target.getLevel() + transferable;
            target.setLevel(targetNewLevel);
            sendLevelMessage(target, targetNewLevel);
            return true;
        }

        if (MODE_XP.equals(mode)) {
            int sourceXp = source.getTotalExperience();
            int transferable = Math.max(0, Math.min(amount, sourceXp));
            source.setTotalExperience(sourceXp - transferable);
            int targetNewXp = target.getTotalExperience() + transferable;
            target.setTotalExperience(targetNewXp);
            sendXpMessage(target, String.valueOf(targetNewXp));
            return true;
        }

        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/xp send <Amount> <Player> <level|xp>"));
        return true;
    }

    private Integer parseInt(String raw, CommandSender sender) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(getPlugin().getPrefix() + "§cInvalid number: " + raw);
            return null;
        }
    }

    private Float parseFloat(String raw, CommandSender sender) {
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(getPlugin().getPrefix() + "§cInvalid number: " + raw);
            return null;
        }
    }

    private void sendLevelMessage(Player player, int level) {
        String levelMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.Level");
        if (levelMessage == null) {
            player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.Level' not found! Please contact the Admin!");
            return;
        }
        levelMessage = textUtils.replaceAndWithParagraph(levelMessage);
        levelMessage = textUtils.replaceObject(levelMessage, "%Level%", String.valueOf(level));
        player.sendMessage(getPlugin().getPrefix() + levelMessage);
    }

    private void sendXpMessage(Player player, String xp) {
        String xpMessage = getPlugin().getLanguageConfig(player).getString(Variables.EXPERIENCE + ".Self.XP");
        if (xpMessage == null) {
            player.sendMessage(getPlugin().getPrefix() + "§cConfig 'Experience.Self.XP' not found! Please contact the Admin!");
            return;
        }
        xpMessage = textUtils.replaceAndWithParagraph(xpMessage);
        xpMessage = textUtils.replaceObject(xpMessage, "%XP%", xp);
        player.sendMessage(getPlugin().getPrefix() + xpMessage);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(getPlugin().getPrefix() + "§cUsage: /xp <set|add|remove|send> <Amount> <Player> <xp|level>");
        sender.sendMessage(getPlugin().getPrefix() + "§cExample: /xp set 100 PlayerName level");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of(SUB_SET, SUB_ADD, SUB_REMOVE, SUB_SEND), args[0]);
        }

        if (args.length == 3) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[2]);
        }

        if (args.length == 4) {
            List<String> modes = new ArrayList<>();
            modes.add(MODE_LEVEL);
            if (!args[0].equalsIgnoreCase(SUB_REMOVE)) {
                modes.add(MODE_XP);
            }
            return filter(modes, args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String startsWith) {
        String prefix = startsWith.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(value);
            }
        }
        Collections.sort(out);
        return out;
    }
}
