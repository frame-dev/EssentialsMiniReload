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
import java.util.*;

// TODO: Require Testing
public class MailCMD extends CommandBase {

    private final File mailFile;

    public MailCMD(Main plugin) {
        super(plugin, "mail");
        this.mailFile = new File(plugin.getDataFolder(), "mail.yml");
        if (!this.mailFile.getParentFile().exists()) {
            boolean ok = this.mailFile.getParentFile().mkdirs();
            if (!ok) plugin.getLogger4J().warn("Could not create data folder for mail.yml");
        }
        try {
            if (!this.mailFile.exists()) {
                boolean created = this.mailFile.createNewFile();
                if (!created) plugin.getLogger4J().warn("Could not create mail.yml");
            }
        } catch (IOException e) {
            plugin.getLogger4J().error("Failed to ensure mail.yml exists", e);
        }
    }

    private FileConfiguration loadCfg() {
        return YamlConfiguration.loadConfiguration(mailFile);
    }

    private void saveCfg(FileConfiguration cfg, CommandSender sender) {
        try {
            cfg.save(mailFile);
        } catch (IOException e) {
            sender.sendMessage(getPlugin().getPrefix() + "Error saving mail: " + e.getMessage());
        }
    }

    // Resolve storage key: prefer online player's UUID, else try offline players (name match), else fallback to lowercase name
    private String resolveKey(String playerName) {
        if (playerName == null) return "unknown";
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) return online.getUniqueId().toString();

        // try offline players (case-insensitive match)
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(playerName)) {
                return op.getUniqueId().toString();
            }
        }

        // fallback to legacy lowercase name key
        return playerName.toLowerCase(Locale.ROOT);
    }

    private String displayNameForKey(String key) {
        try {
            UUID id = UUID.fromString(key);
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String n = op.getName();
            if (n != null) return n;
        } catch (Exception ignored) {
        }
        return key;
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
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "send":
                if (args.length < 3) {
                    sender.sendMessage(getPlugin().getPrefix() + "Usage: /mail send <player> <message>");
                    return true;
                }
                String recip = args[1];
                String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                sendMail(sender, recip, msg);
                return true;
            case "read":
                String readTarget = args.length >= 2 ? args[1] : sender.getName();
                if (!readTarget.equalsIgnoreCase(sender.getName()) && !sender.hasPermission(getPlugin().getPermissionBase() + "mail.read.others")) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                    return true;
                }
                readMail(sender, readTarget);
                return true;
            case "clear":
                String clearTarget = args.length >= 2 ? args[1] : sender.getName();
                if (!clearTarget.equalsIgnoreCase(sender.getName()) && !sender.hasPermission(getPlugin().getPermissionBase() + "mail.clear.others")) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                    return true;
                }
                clearMail(sender, clearTarget);
                return true;
            default:
                sender.sendMessage(getPlugin().getPrefix() + "Unknown sub-command. Use /mail <send|read|clear> [args]");
                return true;
        }
    }

    private void sendMail(CommandSender sender, String recipientStr, String message) {
        FileConfiguration cfg = loadCfg();
        String key = resolveKey(recipientStr);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        String stored = String.format("[%s] %s: %s", time, sender.getName(), message);

        List<String> list = cfg.getStringList(key);
        list.add(stored);
        cfg.set(key, list);
        saveCfg(cfg, sender);

        sender.sendMessage(getPlugin().getPrefix() + "Mail sent to " + displayNameForKey(key) + ": " + message);

        // Notify online recipient
        try {
            UUID id = UUID.fromString(key);
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            if (op.isOnline()) {
                ((Player) op).sendMessage(getPlugin().getPrefix() + "You have new mail from " + sender.getName());
            }
        } catch (Exception ignored) {
            Player online = Bukkit.getPlayer(recipientStr);
            if (online != null) online.sendMessage(getPlugin().getPrefix() + "You have new mail from " + sender.getName());
        }
    }

    private void readMail(CommandSender sender, String recipientStr) {
        FileConfiguration cfg = loadCfg();
        String key = resolveKey(recipientStr);
        List<String> messages = cfg.getStringList(key);
        if (messages.isEmpty() && !key.equalsIgnoreCase(recipientStr.toLowerCase(Locale.ROOT))) {
            messages = cfg.getStringList(recipientStr.toLowerCase(Locale.ROOT));
        }
        if (messages.isEmpty()) {
            sender.sendMessage(getPlugin().getPrefix() + "No mail found for " + recipientStr);
            return;
        }
        sender.sendMessage(getPlugin().getPrefix() + "Mail for " + displayNameForKey(key) + ":");
        for (String m : messages) sender.sendMessage(" - " + m);
    }

    private void clearMail(CommandSender sender, String recipientStr) {
        FileConfiguration cfg = loadCfg();
        String key = resolveKey(recipientStr);
        if (!cfg.contains(key) && cfg.getStringList(recipientStr.toLowerCase(Locale.ROOT)).isEmpty()) {
            sender.sendMessage(getPlugin().getPrefix() + "No mail found for " + recipientStr);
            return;
        }
        cfg.set(key, new ArrayList<>());
        cfg.set(recipientStr.toLowerCase(Locale.ROOT), new ArrayList<>());
        saveCfg(cfg, sender);
        sender.sendMessage(getPlugin().getPrefix() + "Mail cleared for " + displayNameForKey(key));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("send", "read", "clear");
            List<String> out = new ArrayList<>();
            String pref = args[0].toLowerCase(Locale.ROOT);
            for (String s : subs) if (s.startsWith(pref)) out.add(s);
            Collections.sort(out);
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("read") || args[0].equalsIgnoreCase("clear"))) {
            String pref = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(pref)) out.add(p.getName());
            Collections.sort(out);
            return out;
        }
        return null;
    }
}