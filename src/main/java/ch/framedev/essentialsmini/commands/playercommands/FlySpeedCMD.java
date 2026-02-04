package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands.playercommands
 * ClassName FlySpeedCMD
 * Date: 14.05.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

public class FlySpeedCMD extends CommandBase {

    private final Main plugin;

    public FlySpeedCMD(Main plugin) {
        super(plugin, "flyspeed");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }
            if (!player.hasPermission(plugin.getPermissionBase() + "flyspeed")) {
                player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }

            float raw;
            try {
                raw = Float.parseFloat(args[0]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.getPrefix() + "§cInvalid number: " + args[0]);
                return true;
            }
            float flySpeed = raw / 10F;
            if (raw < 0 || raw > 10 || flySpeed > 1.0f) {
                sender.sendMessage(plugin.getPrefix() + "§cFly speed must be between 0 and 10!");
                return true;
            }

            // Apply speed
            player.setFlySpeed(flySpeed);

            String flySpeedMessage = plugin.getLanguageConfig(player).getString("ChangeFlySpeed");
            if (flySpeedMessage == null) {
                // default message when missing
                flySpeedMessage = "§aYour fly speed has been set to %flyspeed%";
            }
            flySpeedMessage = new TextUtils().replaceObject(flySpeedMessage, "%flyspeed%", String.valueOf(raw));
            flySpeedMessage = new TextUtils().replaceAndWithParagraph(flySpeedMessage);
            player.sendMessage(plugin.getPrefix() + flySpeedMessage);
            return true;
        } else if (args.length == 2) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "flyspeed.others")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }

            float raw;
            try {
                raw = Float.parseFloat(args[0]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.getPrefix() + "§cInvalid number: " + args[0]);
                return true;
            }
            float flySpeed = raw / 10F;
            if (raw < 0 || raw > 10 || flySpeed > 1.0f) {
                sender.sendMessage(plugin.getPrefix() + "§cFly speed must be between 0 and 10!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                return true;
            }

            target.setFlySpeed(flySpeed);

            String flySpeedText = plugin.getLanguageConfig(target).getString("ChangeFlySpeed");
            if (flySpeedText == null) {
                flySpeedText = "§aYour fly speed has been set to %flyspeed%";
            }
            flySpeedText = new TextUtils().replaceObject(flySpeedText, "%flyspeed%", String.valueOf(raw));
            flySpeedText = new TextUtils().replaceAndWithParagraph(flySpeedText);
            target.sendMessage(plugin.getPrefix() + flySpeedText);

            String other = plugin.getLanguageConfig(sender).getString("ChangeFlySpeedOther");
            if (other == null) {
                other = "§aSet fly speed for %player% to %flyspeed%";
            }
            other = new TextUtils().replaceAndWithParagraph(other);
            other = new TextUtils().replaceObject(other, "%player%", target.getName());
            other = new TextUtils().replaceObject(other, "%flyspeed%", String.valueOf(raw));
            sender.sendMessage(plugin.getPrefix() + other);
            return true;
        }
        return super.onCommand(sender, command, label, args);
    }
}
