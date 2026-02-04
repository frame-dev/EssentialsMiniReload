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
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpeedCMD extends CommandBase {

    private final Main plugin;

    public SpeedCMD(Main plugin) {
        super(plugin, "walkspeed");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                if (sender.hasPermission(plugin.getPermissionBase() + "speed")) {
                    try {
                        float walkSpeed = Float.parseFloat(args[0]);
                        if(walkSpeed > 10F) {
                            sender.sendMessage(plugin.getPrefix() + "§cWalk speed must be lower than 10!");
                            return true;
                        }
                        ((Player) sender).setWalkSpeed(walkSpeed / 10F);
                        String message = plugin.getLanguageConfig(sender).getString("WalkSpeed");
                        message = new TextUtils().replaceAndWithParagraph(message);
                        message = new TextUtils().replaceObject(message, "%WalkSpeed%", String.valueOf(walkSpeed));
                        sender.sendMessage(plugin.getPrefix() + message);
                    } catch (Exception ex) {
                        sender.sendMessage(plugin.getPrefix() + "§cInvalid number!");
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
        }
        return super.onCommand(sender, command, label, args);
    }
}
