package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.SkinChanger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NickCMD extends CommandBase {

    private final File file;
    private final FileConfiguration fileConfiguration;
    private final List<String> nickList;

    public NickCMD(Main plugin) {
        super(plugin, "nick");

        this.file = new File(plugin.getDataFolder(), "nicks.yml");
        this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
        this.nickList = fileConfiguration.getStringList("nicks");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPrefix() + getPlugin().getOnlyPlayer(null));
            return true;
        }

        if (!player.hasPermission(getPlugin().getPermissionBase() + "nick")) {
            player.sendMessage(getPrefix() + getPlugin().getNoPerms(player));
            return true;
        }

        if (args.length == 1 && isResetArgument(args[0])) {
            resetNick(player);
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(getPrefix() + "Usage: /nick <nickname> <skin>");
            player.sendMessage(getPrefix() + "Usage: /nick reset");
            return true;
        }

        String displayName = ChatColor.translateAlternateColorCodes('&', args[0]);
        String profileName = ChatColor.stripColor(displayName);
        if (!profileName.matches("[A-Za-z0-9_]{1,16}")) {
            player.sendMessage(getPrefix() + "§cNickname must be 1-16 characters and only contain letters, numbers, or underscores.");
            return true;
        }

        String skinName = args[1];
        debug("Command from " + player.getName() + ": displayName='" + displayName + "', profileName='" + profileName + "', skinName='" + skinName + "'");

        if (getPlugin().getSkinService() == null) {
            debug("ProtocolLib SkinService is not available. Applying nickname only for " + player.getName());
            markNicked(player);
            applyDisplayNick(player, displayName);
            saveNickConfiguration(player);
            player.sendMessage(getPrefix() + "§aNickname applied.");
            player.sendMessage(getPrefix() + "§cSkin change requires ProtocolLib.");
            return true;
        }

        SkinChanger.fetchByUsername(getPlugin(), skinName).whenComplete((tex, err) ->
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    if (!player.isOnline()) return;

                    if (err != null) {
                        debug("Skin fetch failed for skinName='" + skinName + "': " + getRootMessage(err));
                        player.sendMessage(getPrefix() + "§cFailed to fetch skin: " + getRootMessage(err));
                        return;
                    }

                    try {
                        debug("Skin fetch succeeded for skinName='" + skinName + "' valueLength=" + tex.value().length() + ", signatureLength=" + tex.signature().length());
                        getPlugin().getSkinService().apply(player, profileName, tex.value(), tex.signature());
                        applyDisplayNick(player, displayName);
                        markNicked(player);
                        saveNickConfiguration(player);
                        debug("Nickname and skin apply completed for " + player.getName());
                        player.sendMessage(getPrefix() + "§aNickname and skin applied. Re-log if the skin does not refresh immediately.");
                    } catch (Exception e) {
                        debug("Skin apply failed for " + player.getName() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        player.sendMessage(getPrefix() + "§cApply failed: " + e.getMessage());
                    }
                }));
        return true;
    }

    private void resetNick(Player player) {
        resetDisplayNick(player);
        if (getPlugin().getSkinService() != null) {
            getPlugin().getSkinService().clear(player);
        }
        nickList.remove(player.getName());
        saveNickConfiguration(player);
        player.sendMessage(getPrefix() + "§aYour nickname and skin have been reset.");
    }

    private void applyDisplayNick(Player player, String displayName) {
        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
        player.setCustomName(displayName);
        player.setCustomNameVisible(true);
    }

    private void resetDisplayNick(Player player) {
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        player.setCustomName(null);
        player.setCustomNameVisible(false);
    }

    private void markNicked(Player player) {
        if (!nickList.contains(player.getName())) {
            nickList.add(player.getName());
        }
    }

    private void saveNickConfiguration(Player player) {
        fileConfiguration.set("nicks", nickList);
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            player.sendMessage(getPrefix() + "§cFailed to save nick configuration.");
        }
    }

    private boolean isResetArgument(String argument) {
        String normalized = argument.toLowerCase(Locale.ROOT);
        return normalized.equals("reset") || normalized.equals("off") || normalized.equals("clear");
    }

    private String getRootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private void debug(String message) {
        if (getPlugin().getConfig().getBoolean("skinDebug", false) || getPlugin().getConfig().getBoolean("debug", false)) {
            getPlugin().getLogger().info("[SkinDebug] " + message);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("reset");
            return completions;
        }

        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                completions.add(onlinePlayer.getName());
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
