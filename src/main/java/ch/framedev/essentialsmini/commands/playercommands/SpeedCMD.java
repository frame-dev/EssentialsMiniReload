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
import ch.framedev.simplejavautils.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpeedCMD extends CommandBase {

    private final Main plugin;

    public SpeedCMD(Main plugin) {
        super(plugin, "speed");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                if (sender.hasPermission(plugin.getPermissionBase() + "speed")) {
                    ((Player) sender).setWalkSpeed(Integer.parseInt(args[0]) / 10F);
                    int walkSpeed = Integer.parseInt(args[0]);
                    String message = plugin.getLanguageConfig(sender).getString("WalkSpeed");
                    message = new TextUtils().replaceAndWithParagraph(message);
                    message = new TextUtils().replaceObject(message, "%WalkSpeed%", String.valueOf(walkSpeed));
                    sender.sendMessage(plugin.getPrefix() + message);
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
        }
        return super.onCommand(sender, command, label, args);
    }
}
