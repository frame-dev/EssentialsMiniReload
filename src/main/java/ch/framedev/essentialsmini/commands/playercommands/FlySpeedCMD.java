package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.simplejavautils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission(plugin.getPermissionBase() + "flyspeed")) {
                player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }
            float flySpeed = Float.parseFloat(args[0]) / 10F;
            player.setFlySpeed(flySpeed);
            String flySpeedMessage = plugin.getLanguageConfig(player).getString("ChangeFlySpeed");
            flySpeedMessage = new TextUtils().replaceObject(flySpeedMessage, "%flyspeed%", String.valueOf(flySpeed * 10F));
            flySpeedMessage = new TextUtils().replaceAndWithParagraph(flySpeedMessage);
            player.sendMessage(flySpeedMessage);
            return true;
        } else if (args.length == 2) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "flyspeed.others")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }
            float flyspeed = Float.parseFloat(args[0]) / 10F;
            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                return true;
            }
            player.setFlySpeed(flyspeed);
            String flySpeed = plugin.getLanguageConfig(player).getString("ChangeFlySpeed");
            flySpeed = new TextUtils().replaceObject(flySpeed, "%flyspeed%", String.valueOf(flyspeed * 10F));
            flySpeed = new TextUtils().replaceAndWithParagraph(flySpeed);
            player.sendMessage(flySpeed);
            String other = plugin.getLanguageConfig(sender).getString("ChangeFlySpeedOther");
            other = new TextUtils().replaceAndWithParagraph(other);
            other = new TextUtils().replaceObject(other, "%player%", player.getName());
            other = new TextUtils().replaceObject(other, "%flyspeed%", String.valueOf(flyspeed * 10F));
            sender.sendMessage(other);
            return true;
        }
        return super.onCommand(sender, command, label, args);
    }
}
