package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanFileManager;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BanCMD extends CommandBase {

    private static final String SUB_TYPE = "type";
    private static final String SUB_OWN = "own";
    private static final List<String> SUB_COMMANDS = Arrays.asList(SUB_TYPE, SUB_OWN);

    public BanCMD(Main plugin) {
        super(plugin, "eban");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission(getPlugin().getPermissionBase() + "ban")) {
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/eban <type|own> <Player> <Reason>"));
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        String targetName = args[1];
        if (targetName == null || targetName.isBlank()) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/eban <type|own> <Player> <Reason>"));
            return true;
        }

        if (mode.equals(SUB_TYPE)) {
            BanType type = parseBanType(args[2]);
            if (type == null) {
                sender.sendMessage(getPlugin().getPrefix() + "§cUnknown ban type: " + args[2]);
                return true;
            }
            applyBan(sender, targetName, type.getReason(), type);
            return true;
        }

        if (mode.equals(SUB_OWN)) {
            String reason = args[2];
            if (reason == null || reason.isBlank()) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/eban own <Player> <Reason>"));
                return true;
            }
            applyBan(sender, targetName, reason, null);
            return true;
        }

        sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/eban <type|own> <Player> <Reason>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(SUB_COMMANDS, args[0]);
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                String name = offlinePlayer.getName();
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
            return filterByPrefix(names, args[1]);
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase(SUB_TYPE)) {
                List<String> types = new ArrayList<>();
                for (BanType type : BanType.values()) {
                    types.add(type.name());
                }
                return filterByPrefix(types, args[2]);
            }
            if (args[0].equalsIgnoreCase(SUB_OWN)) {
                return new ArrayList<>(Collections.singleton("your_Message"));
            }
        }
        return super.onTabComplete(sender, command, label, args);
    }

    private void applyBan(CommandSender sender, String playerName, String reason, BanType type) {
        if (isDatabaseMode()) {
            OfflinePlayer offlinePlayer = PlayerUtils.getOfflinePlayerByName(playerName);
            if (type != null) {
                new BanMuteManager().setPermBan(offlinePlayer, type, true);
            } else {
                new BanMuteManager().setPermBan(offlinePlayer, reason, true);
            }
        } else {
            BanFileManager.banPlayer(playerName, reason);
        }

        kickIfOnlineOrNotify(sender, playerName, reason);
    }

    private boolean isDatabaseMode() {
        return getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB();
    }

    private void kickIfOnlineOrNotify(CommandSender sender, String playerName, String reason) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer == null) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNotOnline());
            return;
        }
        onlinePlayer.kickPlayer(ChatColor.RED + "You are Banned while " + ChatColor.GOLD + reason);
    }

    private BanType parseBanType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BanType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<String> filterByPrefix(List<String> source, String userInput) {
        List<String> results = new ArrayList<>();
        String prefix = userInput == null ? "" : userInput.toLowerCase(Locale.ROOT);
        for (String entry : source) {
            if (entry != null && entry.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                results.add(entry);
            }
        }
        Collections.sort(results);
        return results;
    }

    public enum BanType {

        HACKING("hacking"),
        GRIEFING("griefing"),
        SPAMMING("spamming"),
        ABUSING("abusing"),
        OFFENSIVE_LANGUAGE("using offensive language"),
        TRY_BYPASSING_BAN("try bypassing a Ban");

        private final String reason;

        BanType(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
