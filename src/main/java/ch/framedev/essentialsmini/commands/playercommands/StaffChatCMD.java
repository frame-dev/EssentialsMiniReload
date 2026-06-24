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

    private static final String COMMAND_NAME = "staffchat";
    private static final String PERMISSION = "staffchat";
    private static final String USAGE = "Usage: /staffchat <message>";
    private static final String FORMAT_CONFIG_KEY = "staffChatFormat";
    private static final String DEFAULT_FORMAT = "&7[&cStaff&7] &f%player%: &7%message%";

    public StaffChatCMD(Main plugin) {
        super(plugin, COMMAND_NAME);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND_NAME)) return false;

        if (!hasPermission(sender)) return true;

        if (args.length == 0) {
            send(sender, USAGE);
            return true;
        }

        broadcastStaffMessage(formatStaffMessage(sender, String.join(" ", args)));
        return true;
    }

    private void broadcastStaffMessage(String message) {
        for (Player staff : getPlugin().getServer().getOnlinePlayers()) {
            if (staff.hasPermission(getPlugin().getPermissionBase() + PERMISSION)) {
                send(staff, message);
            }
        }
    }

    private String formatStaffMessage(CommandSender sender, String message) {
        String format = getPlugin().getConfig().getString(FORMAT_CONFIG_KEY, DEFAULT_FORMAT);
        if (format.isBlank()) {
            format = DEFAULT_FORMAT;
        }

        return ChatColor.translateAlternateColorCodes('&', format
                .replace("%player%", senderName(sender))
                .replace("%message%", message));
    }

    private String senderName(CommandSender sender) {
        String name = sender.getName();
        return name.isBlank() ? "Console" : name;
    }

    private boolean hasPermission(CommandSender sender) {
        if (sender.hasPermission(getPlugin().getPermissionBase() + PERMISSION)) return true;

        send(sender, getPlugin().getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private void send(CommandSender sender, String message) {
        boolean usePrefix = getPlugin().getConfig().getBoolean("staffChat.usePrefix", false);
        sender.sendMessage(usePrefix ? getPlugin().getPrefix() + message : message);
    }
}
