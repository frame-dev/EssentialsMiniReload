package ch.framedev.essentialsmini.commands.playercommands;


/*
 * EssentialsMini
 * de.framedev.essentialsmin.commands
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 15.10.2020 20:02
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpeedCMD extends CommandBase {

    private static final float MIN_SPEED = 0F;
    private static final float MAX_SPEED = 10F;
    private static final String COMMAND_NAME = "walkspeed";
    private static final String PERMISSION = "speed";
    private static final String USAGE = "/walkspeed <0-10>";
    private static final String MESSAGE_KEY = "WalkSpeed";
    private static final String DEFAULT_MESSAGE = "§aYour walk speed has been changed to §6%WalkSpeed%";

    private final Main plugin;

    public SpeedCMD(Main plugin) {
        super(plugin, COMMAND_NAME);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND_NAME)) return false;

        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (!hasPermission(player)) return true;

        if (args.length != 1) {
            sendWrongArgs(player);
            return true;
        }

        Float speed = parseSpeed(player, args[0]);
        if (speed == null) return true;

        player.setWalkSpeed(speed / MAX_SPEED);
        sendSpeedMessage(player, speed);
        return true;
    }

    private Float parseSpeed(CommandSender sender, String value) {
        float speed;
        try {
            speed = Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            send(sender, "§cInvalid number!");
            return null;
        }

        if (!Float.isFinite(speed) || speed < MIN_SPEED || speed > MAX_SPEED) {
            send(sender, "§cWalk speed must be between 0 and 10!");
            return null;
        }
        return speed;
    }

    private void sendSpeedMessage(Player player, float speed) {
        String message = plugin.getLanguageConfig(player).getString(MESSAGE_KEY, DEFAULT_MESSAGE);
        if (message == null) message = DEFAULT_MESSAGE;
        message = ReplaceCharConfig.replaceParagraph(message);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%WalkSpeed%", String.valueOf(speed));
        send(player, message);
    }

    private boolean hasPermission(Player player) {
        if (player.hasPermission(plugin.getPermissionBase() + PERMISSION)) return true;

        send(player, plugin.getNoPerms(player));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        send(sender, plugin.getOnlyPlayer(null));
        return null;
    }

    private void sendWrongArgs(CommandSender sender) {
        send(sender, plugin.getWrongArgs(sender instanceof Player player ? player : null, USAGE));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }
}
