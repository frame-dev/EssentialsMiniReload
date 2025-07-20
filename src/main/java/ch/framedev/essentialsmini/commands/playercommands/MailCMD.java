package ch.framedev.essentialsmini.commands.playercommands;



/*
 * ch.framedev.essentialsmini.commands.playercommands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 19.07.2025 16:24
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class MailCMD extends CommandBase {

    private final File mailFile = new File(Main.getInstance().getDataFolder(), "mail.yml");
    private final FileConfiguration mailConfig = YamlConfiguration.loadConfiguration(mailFile);

    public MailCMD(Main plugin) {
        super(plugin, "mail");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(getPlugin().getPermissionBase() + "mail")) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getPlugin().getPrefix() + "Usage: /mail <send|read|clear> [args]");
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "send":
                if (args.length < 3) {
                    sender.sendMessage(getPlugin().getPrefix() + "Usage: /mail send <player> <message>");
                    return true;
                }
                String recipient = args[1];
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                sendMail(sender, recipient, message);
                break;
            case "read":
                String readTarget = args.length >= 2 ? args[1] : sender.getName();
                readMail(sender, readTarget);
                break;
            case "clear":
                String clearTarget = args.length >= 2 ? args[1] : sender.getName();
                clearMail(sender, clearTarget);
                break;
            default:
                sender.sendMessage(getPlugin().getPrefix() + "Unknown sub-command. Use /mail <send|read|clear> [args]");
                return true;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void sendMail(CommandSender sender, String recipient, String message) {
        if (!mailConfig.contains(recipient)) {
            mailConfig.set(recipient, new ArrayList<String>());
        }
        ArrayList<String> messages = (ArrayList<String>) mailConfig.getList(recipient);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        mailConfig.set(recipient, messages);
        try {
            mailConfig.save(mailFile);
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "Error saving mail: " + e.getMessage());
            return;
        }
        sender.sendMessage(getPlugin().getPrefix() + "Mail sent to " + recipient + ": " + message);
    }

    @SuppressWarnings("unchecked")
    private void readMail(CommandSender sender, String recipient) {
        if (!mailConfig.contains(recipient)) {
            sender.sendMessage(getPlugin().getPrefix() + "No mail found for " + recipient);
            return;
        }
        ArrayList<String> messages = (ArrayList<String>) mailConfig.getList(recipient);
        if (messages == null || messages.isEmpty()) {
            sender.sendMessage(getPlugin().getPrefix() + "No mail found for " + recipient);
            return;
        }
        sender.sendMessage(getPlugin().getPrefix() + "Mail for " + recipient + ":");
        for (String message : messages) {
            sender.sendMessage(" - " + message);
        }
    }

    private void clearMail(CommandSender sender, String recipient) {
        if (!mailConfig.contains(recipient)) {
            sender.sendMessage(getPlugin().getPrefix() + "No mail found for " + recipient);
            return;
        }
        mailConfig.set(recipient, new ArrayList<String>());
        try {
            mailConfig.save(mailFile);
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "Error clearing mail: " + e.getMessage());
            return;
        }
        sender.sendMessage(getPlugin().getPrefix() + "Mail cleared for " + recipient);
    }
}