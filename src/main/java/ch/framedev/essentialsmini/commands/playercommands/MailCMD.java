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
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MailCMD extends CommandBase {

    private static final String SEND = "send";
    private static final String READ = "read";
    private static final String CLEAR = "clear";
    private static final List<String> SUB_COMMANDS = Arrays.asList(SEND, READ, CLEAR);
    private static final String USAGE = "Usage: /mail <send|read|clear> [args]";
    private static final String SEND_USAGE = "Usage: /mail send <player> <message>";
    private static final SimpleDateFormat MAIL_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Main plugin;
    private final File mailFile;

    public MailCMD(Main plugin) {
        super(plugin, "mail");
        this.plugin = plugin;
        this.mailFile = new File(plugin.getDataFolder(), "mail.yml");
        ensureMailFile();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!hasPermission(sender, "mail")) return true;

        if (args.length == 0) {
            send(sender, USAGE);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case SEND -> handleSend(sender, args);
            case READ -> handleRead(sender, args);
            case CLEAR -> handleClear(sender, args);
            default -> {
                send(sender, "Unknown sub-command. Use /mail <send|read|clear> [args]");
                yield true;
            }
        };
    }

    private boolean handleSend(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, SEND_USAGE);
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        if (message.isEmpty()) {
            send(sender, SEND_USAGE);
            return true;
        }

        MailTarget target = resolveTarget(args[1]);
        FileConfiguration config = loadConfig();
        List<String> messages = new ArrayList<>(config.getStringList(target.storageKey()));
        messages.add(formatStoredMessage(sender, message));
        config.set(target.storageKey(), messages);

        if (!saveConfig(config, sender)) return true;

        send(sender, "Mail sent to " + target.displayName() + ": " + message);
        notifyRecipient(target, senderName(sender));
        return true;
    }

    private boolean handleRead(CommandSender sender, String[] args) {
        String targetName = args.length >= 2 ? args[1] : senderName(sender);
        if (!canAccessTarget(sender, targetName, "mail.read.others")) return true;

        MailTarget target = resolveTarget(targetName);
        List<String> messages = getMessages(loadConfig(), target);
        if (messages.isEmpty()) {
            send(sender, "No mail found for " + targetName);
            return true;
        }

        send(sender, "Mail for " + target.displayName() + ":");
        for (String message : messages) {
            sender.sendMessage(" - " + message);
        }
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        String targetName = args.length >= 2 ? args[1] : senderName(sender);
        if (!canAccessTarget(sender, targetName, "mail.clear.others")) return true;

        MailTarget target = resolveTarget(targetName);
        FileConfiguration config = loadConfig();
        if (getMessages(config, target).isEmpty()) {
            send(sender, "No mail found for " + targetName);
            return true;
        }

        config.set(target.storageKey(), new ArrayList<>());
        config.set(target.legacyKey(), new ArrayList<>());

        if (saveConfig(config, sender)) {
            send(sender, "Mail cleared for " + target.displayName());
        }
        return true;
    }

    private List<String> getMessages(FileConfiguration config, MailTarget target) {
        List<String> messages = new ArrayList<>(config.getStringList(target.storageKey()));
        if (messages.isEmpty() && !target.storageKey().equalsIgnoreCase(target.legacyKey())) {
            messages.addAll(config.getStringList(target.legacyKey()));
        }
        return messages;
    }

    private boolean canAccessTarget(CommandSender sender, String targetName, String othersPermission) {
        if (targetName.equalsIgnoreCase(senderName(sender))) return true;
        return hasPermission(sender, othersPermission);
    }

    private boolean hasPermission(CommandSender sender, String permissionSuffix) {
        if (sender.hasPermission(plugin.getPermissionBase() + permissionSuffix)) return true;

        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private MailTarget resolveTarget(String playerName) {
        String safeName = playerName == null || playerName.isBlank() ? "unknown" : playerName;
        String legacyKey = safeName.toLowerCase(Locale.ROOT);

        Player online = Bukkit.getPlayerExact(safeName);
        if (online != null) {
            return new MailTarget(online.getUniqueId().toString(), legacyKey, online.getName(), online);
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String offlineName = offlinePlayer.getName();
            if (offlineName != null && offlineName.equalsIgnoreCase(safeName)) {
                Player onlinePlayer = offlinePlayer.isOnline() ? offlinePlayer.getPlayer() : null;
                return new MailTarget(offlinePlayer.getUniqueId().toString(), legacyKey, offlineName, onlinePlayer);
            }
        }

        return new MailTarget(legacyKey, legacyKey, safeName, Bukkit.getPlayer(safeName));
    }

    private void notifyRecipient(MailTarget target, String senderName) {
        Player onlineRecipient = target.onlinePlayer();
        if (onlineRecipient != null) {
            plugin.sendConfiguredNotification(onlineRecipient, "mailReceived", "mail",
                    "You have new mail from " + senderName,
                    Collections.singletonMap("%Sender%", senderName));
        }
    }

    private String formatStoredMessage(CommandSender sender, String message) {
        return String.format("[%s] %s: %s", MAIL_TIME_FORMAT.format(new Date()), senderName(sender), message);
    }

    private String senderName(CommandSender sender) {
        String name = sender.getName();
        return name.isBlank() ? "Console" : name;
    }

    private FileConfiguration loadConfig() {
        return YamlConfiguration.loadConfiguration(mailFile);
    }

    private boolean saveConfig(FileConfiguration config, CommandSender sender) {
        try {
            config.save(mailFile);
            return true;
        } catch (IOException e) {
            send(sender, "Error saving mail: " + e.getMessage());
            return false;
        }
    }

    private void ensureMailFile() {
        File parent = mailFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger4J().warn("Could not create data folder for mail.yml");
        }

        try {
            if (!mailFile.exists() && !mailFile.createNewFile()) {
                plugin.getLogger4J().warn("Could not create mail.yml");
            }
        } catch (IOException e) {
            plugin.getLogger4J().error("Failed to ensure mail.yml exists", e);
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return TabCompleteUtils.matchingStrings(SUB_COMMANDS, args[0]);
        }

        if (args.length == 2 && SUB_COMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            return TabCompleteUtils.matchingOnlinePlayers(args[1]);
        }

        return Collections.emptyList();
    }

    private record MailTarget(String storageKey, String legacyKey, String displayName, Player onlinePlayer) {
    }
}
