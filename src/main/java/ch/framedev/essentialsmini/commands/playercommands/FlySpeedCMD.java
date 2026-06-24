package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands.playercommands
 * ClassName FlySpeedCMD
 * Date: 14.05.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

public class FlySpeedCMD extends CommandBase {

    private static final float MIN_SPEED = 0F;
    private static final float MAX_SPEED = 10F;
    private static final float DEFAULT_SPEED = 1F;
    private static final String COMMAND_USAGE = "/flyspeed <0-10|reset> [player]";
    private static final String PERMISSION = "flyspeed";
    private static final String PERMISSION_OTHERS = "flyspeed.others";
    private static final String CHANGE_FLY_SPEED = "ChangeFlySpeed";
    private static final String CHANGE_FLY_SPEED_OTHER = "ChangeFlySpeedOther";
    private static final String DEFAULT_CHANGE_FLY_SPEED = "§aYour fly speed has been set to %flyspeed%";
    private static final String DEFAULT_CHANGE_FLY_SPEED_OTHER = "§aSet fly speed for %player% to %flyspeed%";

    private final Main plugin;
    private final TextUtils textUtils;

    public FlySpeedCMD(Main plugin) {
        super(plugin, "flyspeed");
        this.plugin = plugin;
        this.textUtils = new TextUtils();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player target)) {
                send(sender, plugin.getOnlyPlayer(null));
                return true;
            }

            if (!hasPermission(target, PERMISSION)) {
                return true;
            }

            applyFlySpeed(sender, target, args[0], false);
            return true;
        }

        if (args.length == 2) {
            if (!hasPermission(sender, PERMISSION_OTHERS)) {
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                send(sender, plugin.getVariables().getPlayerNameNotOnline(args[1]));
                return true;
            }

            applyFlySpeed(sender, target, args[0], true);
            return true;
        }

        sendWrongArgs(sender);
        return true;
    }

    private void applyFlySpeed(CommandSender sender, Player target, String speedArgument, boolean notifySender) {
        Float rawSpeed = parseSpeed(sender, speedArgument);
        if (rawSpeed == null) {
            return;
        }

        target.setFlySpeed(toBukkitFlySpeed(rawSpeed));
        sendSpeedChangedMessage(target, rawSpeed);

        if (notifySender && !sender.equals(target)) {
            sendOtherSpeedChangedMessage(sender, target, rawSpeed);
        }
    }

    private Float parseSpeed(CommandSender sender, String argument) {
        if (isDefaultSpeedArgument(argument)) {
            return DEFAULT_SPEED;
        }

        float rawSpeed;
        try {
            rawSpeed = Float.parseFloat(argument);
        } catch (NumberFormatException ex) {
            send(sender, "§cInvalid number: " + argument);
            return null;
        }

        if (!Float.isFinite(rawSpeed) || rawSpeed < MIN_SPEED || rawSpeed > MAX_SPEED) {
            send(sender, "§cFly speed must be between 0 and 10!");
            return null;
        }
        return rawSpeed;
    }

    private float toBukkitFlySpeed(float rawSpeed) {
        return rawSpeed / MAX_SPEED;
    }

    private void sendSpeedChangedMessage(Player target, float rawSpeed) {
        String message = plugin.getLanguageConfig(target).getString(CHANGE_FLY_SPEED, DEFAULT_CHANGE_FLY_SPEED);
        target.sendMessage(plugin.getPrefix() + formatMessage(message, target, rawSpeed));
    }

    private void sendOtherSpeedChangedMessage(CommandSender sender, Player target, float rawSpeed) {
        String message = plugin.getLanguageConfig(sender).getString(CHANGE_FLY_SPEED_OTHER, DEFAULT_CHANGE_FLY_SPEED_OTHER);
        sender.sendMessage(plugin.getPrefix() + formatMessage(message, target, rawSpeed));
    }

    private String formatMessage(String message, Player target, float rawSpeed) {
        message = textUtils.replaceObject(message, "%player%", target.getName());
        message = textUtils.replaceObject(message, "%flyspeed%", String.valueOf(rawSpeed));
        return textUtils.replaceAndWithParagraph(message);
    }

    private boolean isDefaultSpeedArgument(String argument) {
        return argument.equalsIgnoreCase("default")
                || argument.equalsIgnoreCase("reset")
                || argument.equalsIgnoreCase("normal");
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(plugin.getPermissionBase() + permission)) {
            return true;
        }

        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private void sendWrongArgs(CommandSender sender) {
        send(sender, plugin.getWrongArgs(sender instanceof Player player ? player : null, COMMAND_USAGE));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (!sender.hasPermission(plugin.getPermissionBase() + PERMISSION)
                    && !sender.hasPermission(plugin.getPermissionBase() + PERMISSION_OTHERS)) {
                return List.of();
            }

            List<String> values = new ArrayList<>(List.of("0", "1", "2", "3", "5", "10", "default", "normal", "reset"));
            return TabCompleteUtils.matchingStrings(values, args[0]);
        }

        if (args.length == 2 && sender.hasPermission(plugin.getPermissionBase() + PERMISSION_OTHERS)) {
            return TabCompleteUtils.matchingOnlinePlayers(args[1]);
        }

        return List.of();
    }
}
