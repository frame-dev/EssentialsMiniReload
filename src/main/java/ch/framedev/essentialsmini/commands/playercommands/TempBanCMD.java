package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import ch.framedev.essentialsmini.utils.DateUnit;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands.playercommands
 * ClassName TempBanCMD
 * Date: 15.05.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

@SuppressWarnings("deprecation")
public class TempBanCMD extends CommandBase {

    private static final String TEMPBAN = "tempban";
    private static final String REMOVE_TEMPBAN = "removetempban";
    private static final String TEMPBAN_USAGE = "/tempban <type|own> <Player> <Reason> <Time> <SEC|MIN|DAY|WEEK|MONTH|YEAR>";
    private static final String REMOVE_TEMPBAN_USAGE = "/removetempban <Player>";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss");

    public TempBanCMD(Main plugin) {
        super(plugin, TEMPBAN, REMOVE_TEMPBAN);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case TEMPBAN -> handleTempBan(sender, args);
            case REMOVE_TEMPBAN -> handleRemoveTempBan(sender, args);
            default -> super.onCommand(sender, cmd, label, args);
        };
    }

    private boolean handleTempBan(CommandSender sender, String[] args) {
        if (!hasTempBanPermission(sender)) {
            return true;
        }

        if (args.length != 5) {
            sendWrongArgs(sender, TEMPBAN_USAGE);
            return true;
        }

        BanMode mode = parseMode(args[0]);
        if (mode == null) {
            sendMessage(sender, "TempBan.Errors.InvalidMode", "§cPlease use §6type §cor §6own§c.");
            return true;
        }

        OfflinePlayer target = findPlayer(sender, args[1]);
        if (target == null) {
            return true;
        }

        DateUnit unit = parseDateUnit(sender, args[4]);
        if (unit == null) {
            return true;
        }

        Long value = parsePositiveLong(sender, args[3]);
        if (value == null) {
            return true;
        }

        String reason = parseReason(sender, mode, args[2]);
        if (reason == null) {
            return true;
        }

        Date expireDate = createExpireDate(sender, value, unit);
        if (expireDate == null) {
            return true;
        }

        applyTempBan(target, reason, expireDate);
        kickIfOnline(target, reason, value, unit, expireDate);
        sendMessage(sender, "TempBan.Success", "§6%Player% §ahas been banned while §6%Reason% §afor §6%Time% %Unit%!",
                "%Player%", safeName(target),
                "%Reason%", reason,
                "%Time%", String.valueOf(value),
                "%Unit%", unit.getOutput(),
                "%Expire%", DATE_FORMAT.format(expireDate));
        return true;
    }

    private boolean handleRemoveTempBan(CommandSender sender, String[] args) {
        if (!hasTempBanPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            sendWrongArgs(sender, REMOVE_TEMPBAN_USAGE);
            return true;
        }

        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) {
            return true;
        }

        if (hasExternalBanStorage()) {
            new BanMuteManager().removeTempBan(target);
        }
        Bukkit.getServer().getBanList(BanList.Type.NAME).pardon(safeName(target));
        sendMessage(sender, "TempBan.Remove.Success", "§6%Player% §ahas been unbanned!",
                "%Player%", safeName(target));
        return true;
    }

    private boolean hasTempBanPermission(CommandSender sender) {
        if (sender.hasPermission(getPlugin().getPermissionBase() + "tempban")) {
            return true;
        }

        send(sender, getPlugin().getNoPerms());
        return false;
    }

    private BanMode parseMode(String value) {
        if (value == null) {
            return null;
        }

        for (BanMode mode : BanMode.values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }

    private OfflinePlayer findPlayer(CommandSender sender, String name) {
        OfflinePlayer target = PlayerUtils.getOfflinePlayerByName(name);
        if (target == null || target.getName() == null) {
            sendMessage(sender, "TempBan.Errors.PlayerNotFound", "§cPlayer name not found!",
                    "%Player%", name);
            return null;
        }
        return target;
    }

    private DateUnit parseDateUnit(CommandSender sender, String value) {
        try {
            return DateUnit.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sendMessage(sender, "TempBan.Errors.InvalidUnit", "§cInvalid time unit. Use SEC, MIN, DAY, WEEK, MONTH or YEAR.",
                    "%Unit%", value);
            return null;
        }
    }

    private Long parsePositiveLong(CommandSender sender, String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                sendMessage(sender, "TempBan.Errors.TimeGreaterThanZero", "§cTime must be greater than 0.",
                        "%Time%", value);
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            sendMessage(sender, "TempBan.Errors.InvalidTime", "§cInvalid time: §6%Time%",
                    "%Time%", value);
            return null;
        }
    }

    private String parseReason(CommandSender sender, BanMode mode, String value) {
        if (mode == BanMode.OWN) {
            return value;
        }

        try {
            return Ban.valueOf(value.toUpperCase(Locale.ROOT)).getReason();
        } catch (IllegalArgumentException ex) {
            sendMessage(sender, "TempBan.Errors.InvalidType", "§cInvalid ban type: §6%Type%",
                    "%Type%", value);
            return null;
        }
    }

    private Date createExpireDate(CommandSender sender, long value, DateUnit unit) {
        try {
            long seconds = Math.multiplyExact(value, unit.getToSec());
            long millis = Math.multiplyExact(seconds, 1000L);
            return new Date(Math.addExact(System.currentTimeMillis(), millis));
        } catch (ArithmeticException ex) {
            sendMessage(sender, "TempBan.Errors.DurationTooLarge", "§cThat ban duration is too large.");
            return null;
        }
    }

    private void applyTempBan(OfflinePlayer target, String reason, Date expireDate) {
        if (hasExternalBanStorage()) {
            new BanMuteManager().setTempBan(target, reason, DATE_FORMAT.format(expireDate));
            return;
        }

        Bukkit.getServer().getBanList(BanList.Type.NAME)
                .addBan(safeName(target), message(null, "TempBan.BanListReason", "§aYou are Banned. Reason:§c %Reason%",
                        "%Player%", safeName(target),
                        "%Reason%", reason,
                        "%Expire%", DATE_FORMAT.format(expireDate)), expireDate, "true");
    }

    private void kickIfOnline(OfflinePlayer target, String reason, long value, DateUnit unit, Date expireDate) {
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().kickPlayer(message(target.getPlayer(), "TempBan.Kick", "§bBan while §c%Reason%§b for §a%Time% %Unit%!",
                    "%Player%", safeName(target),
                    "%Reason%", reason,
                    "%Time%", String.valueOf(value),
                    "%Unit%", unit.getOutput(),
                    "%Expire%", DATE_FORMAT.format(expireDate)));
        }
    }

    private boolean hasExternalBanStorage() {
        return getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB();
    }

    private String safeName(OfflinePlayer player) {
        String name = player.getName();
        return name == null ? player.getUniqueId().toString() : name;
    }

    private void sendWrongArgs(CommandSender sender, String usage) {
        send(sender, getPlugin().getWrongArgs(usage));
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

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase(TEMPBAN)) {
            return completeTempBan(args);
        }

        if (command.getName().equalsIgnoreCase(REMOVE_TEMPBAN) && args.length == 1) {
            return TabCompleteUtils.matchingStrings(tempBannedPlayerNames(), args[0]);
        }

        return super.onTabComplete(sender, command, label, args);
    }

    private List<String> completeTempBan(String[] args) {
        return switch (args.length) {
            case 1 -> TabCompleteUtils.matchingStrings(List.of("type", "own"), args[0]);
            case 2 -> TabCompleteUtils.matchingStrings(offlinePlayerNames(), args[1]);
            case 3 -> completeReason(args);
            case 4 -> List.of("Time");
            case 5 -> TabCompleteUtils.matchingStrings(dateUnitNames(), args[4]);
            default -> List.of();
        };
    }

    private List<String> completeReason(String[] args) {
        BanMode mode = parseMode(args[0]);
        if (mode == BanMode.TYPE) {
            return TabCompleteUtils.matchingStrings(banReasonNames(), args[2]);
        }

        if (mode == BanMode.OWN) {
            return List.of("your_Message");
        }

        return List.of();
    }

    private List<String> offlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer != null && offlinePlayer.getName() != null) {
                names.add(offlinePlayer.getName());
            }
        }
        return names;
    }

    private List<String> banReasonNames() {
        List<String> reasons = new ArrayList<>();
        Arrays.stream(Ban.values()).forEach(ban -> reasons.add(ban.name()));
        return reasons;
    }

    private List<String> dateUnitNames() {
        List<String> dateUnits = new ArrayList<>();
        Arrays.stream(DateUnit.values()).forEach(dateUnit -> dateUnits.add(dateUnit.name()));
        return dateUnits;
    }

    private @NotNull List<String> tempBannedPlayerNames() {
        List<String> playerNames = new ArrayList<>();
        if (hasExternalBanStorage()) {
            List<String> bannedPlayers = new BanMuteManager().getAllTempBannedPlayers();
            return bannedPlayers == null ? playerNames : bannedPlayers;
        }

        for (OfflinePlayer player : getPlugin().getServer().getBannedPlayers()) {
            if (player != null && player.getName() != null) {
                playerNames.add(player.getName());
            }
        }
        return playerNames;
    }

    private enum BanMode {
        TYPE,
        OWN
    }

    public enum Ban {

        CLIENT_MODIFICATIONS("client modifications"),
        BUG_USING("exploit bugs"),
        FORBIDDEN_SKIN("forbidden skin/name"),
        DESTROY_BUILDINGS("destroy other buildings"),
        TROLLING("trolling"),
        TEAMING("teaming"),
        GRIEFING("griefing"),
        OFFENSIVE_INAPPROPROATE_BUILDING("Offensive / inappropriate building");

        private final String reason;

        Ban(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
