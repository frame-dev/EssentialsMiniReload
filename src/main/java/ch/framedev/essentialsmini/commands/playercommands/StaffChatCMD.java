package ch.framedev.essentialsmini.commands.playercommands;



/*
 * ch.framedev.essentialsmini.commands.playercommands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 19.07.2025 11:32
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StaffChatCMD extends CommandBase {

    public StaffChatCMD(Main plugin) {
        super(plugin, "staffchat");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("essentialsmini.staffchat")) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getPlugin().getPrefix() + "Usage: /staffchat <message>");
            return true;
        }

        String message = String.join(" ", args);
        String format = getPlugin().getConfig().getString("staffChatFormat", "&7[&cStaff&7] &f%player%: &7%message%");
        format = format.replace("%player%", sender.getName()).replace("%message%", message);
        for (Player staff : getPlugin().getServer().getOnlinePlayers()) {
            if (staff.hasPermission("essentialsmini.staffchat")) {
                staff.sendMessage(getPlugin().getPrefix() + ChatColor.translateAlternateColorCodes('&', format));
            }
        }
        return true;
    }
}
