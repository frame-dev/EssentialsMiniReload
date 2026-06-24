package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanFileManager;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
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
            send(sender, getPlugin().getNoPerms(sender instanceof Player player ? player : null));
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
                sendMessage(sender, "EBan.Errors.UnknownType", "§cUnknown ban type: §6%Type%",
                        "%Type%", args[2]);
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

        sendMessage(sender, "EBan.Errors.InvalidMode", getPlugin().getWrongArgs(sender instanceof Player player ? player : null, "/eban <type|own> <Player> <Reason>"),
                "%Mode%", args[0]);
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
            sendMessage(sender, "EBan.TargetOffline", "§6%Player% §awas banned, but is not online.",
                    "%Player%", playerName,
                    "%Reason%", reason);
            return;
        }
        onlinePlayer.kickPlayer(message(onlinePlayer, "EBan.Kick", ChatColor.RED + "You are Banned while " + ChatColor.GOLD + "%Reason%",
                "%Player%", playerName,
                "%Reason%", reason));
        sendMessage(sender, "EBan.Success", "§6%Player% §ahas been banned while §6%Reason%!",
                "%Player%", playerName,
                "%Reason%", reason);
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

    private void send(CommandSender sender, String message) {
        sender.sendMessage(getPlugin().getPrefix() + message);
    }

    private void sendMessage(CommandSender sender, String key, String defaultMessage, String... replacements) {
        send(sender, message(sender, key, defaultMessage, replacements));
    }

    private String message(CommandSender receiver, String key, String defaultMessage, String... replacements) {
        String message = getPlugin().getLanguageConfig(receiver).getString(key, defaultMessage);
        if (message == null) message = defaultMessage;
        message = ReplaceCharConfig.replaceParagraph(message);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = ReplaceCharConfig.replaceObjectWithData(message, replacements[i], replacements[i + 1] == null ? "" : replacements[i + 1]);
        }
        return message;
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
