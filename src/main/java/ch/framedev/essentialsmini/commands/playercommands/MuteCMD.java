package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import ch.framedev.essentialsmini.utils.DateUnit;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands.playercommands
 * ClassName MuteCMD
 * Date: 15.05.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
@SuppressWarnings("deprecation")
public class MuteCMD extends CommandBase implements Listener {

    private static final String MUTE = "mute";
    private static final String TEMP_MUTE = "tempmute";
    private static final String MUTE_INFO = "muteinfo";
    private static final String REMOVE_TEMP_MUTE = "removetempmute";
    private static final String DATE_FORMAT = "dd.MM.yyyy | HH:mm:ss";
    private static final String TEMP_MUTE_USAGE = "/tempmute <type|own> <Player> <Reason> <Time> <Unit>";
    private static final String MUTE_USAGE = "/mute <Player>";
    private static final String REMOVE_TEMP_MUTE_USAGE = "/removetempmute <Player>";

    private final Main plugin;
    private final List<OfflinePlayer> muted;

    public static File file;
    public static FileConfiguration cfg;

    public MuteCMD(Main plugin) {
        super(plugin);
        setup(MUTE, this);
        setup(TEMP_MUTE, this);
        setup(MUTE_INFO, this);
        setup(REMOVE_TEMP_MUTE, this);
        setupTabCompleter(TEMP_MUTE, this);
        this.plugin = plugin;
        plugin.getListeners().add(this);
        this.muted = plugin.getVariables() == null ? new ArrayList<>() : plugin.getVariables().getMutedPlayers();
        file = new File(plugin.getDataFolder(), "tempMutes.yml");
        ensureMuteFile();
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case MUTE -> handleMute(sender, args);
            case TEMP_MUTE -> handleTempMute(sender, args);
            case REMOVE_TEMP_MUTE -> handleRemoveTempMute(sender, args);
            case MUTE_INFO -> handleMuteInfo(sender);
            default -> false;
        };
    }

    private boolean handleMute(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "mute")) return true;
        if (args.length != 1) {
            sendWrongArgs(sender, MUTE_USAGE);
            return true;
        }

        OfflinePlayer target = resolvePlayer(sender, args[0]);
        if (target == null) return true;

        boolean mutedNow;
        if (isPermanentlyMuted(target)) {
            muted.removeIf(mutedPlayer -> samePlayer(mutedPlayer, target));
            mutedNow = false;
        } else {
            muted.add(target);
            mutedNow = true;
        }

        notifyMuteState(sender, target, mutedNow);
        return true;
    }

    private boolean handleTempMute(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "tempmute")) return true;
        if (args.length != 5) {
            sendWrongArgs(sender, TEMP_MUTE_USAGE);
            return true;
        }

        TempMuteRequest request = parseTempMuteRequest(sender, args);
        if (request == null) return true;

        setTempMute(request.target(), request.reason(), request.expiresAt());
        notifyTempMuteState(sender, request, true);
        return true;
    }

    private boolean handleRemoveTempMute(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "tempmute")) return true;
        if (args.length != 1) {
            sendWrongArgs(sender, REMOVE_TEMP_MUTE_USAGE);
            return true;
        }

        OfflinePlayer target = resolvePlayer(sender, args[0]);
        if (target == null) return true;

        removeTempMute(target);
        notifyTempMuteState(sender, new TempMuteRequest(target, "", null, 0L, null), false);
        return true;
    }

    private boolean handleMuteInfo(CommandSender sender) {
        if (!hasPermission(sender, "muteinfo")) return true;

        if (usesDatabaseStorage()) {
            sendDatabaseMuteInfo(sender);
        } else {
            sendFileMuteInfo(sender);
        }
        return true;
    }

    private TempMuteRequest parseTempMuteRequest(CommandSender sender, String[] args) {
        String mode = args[0].toLowerCase(Locale.ROOT);
        if (!mode.equals("type") && !mode.equals("own")) {
            sendMessage(sender, "TempMute.Errors.InvalidMode", "§cPlease use §6type §cor §6own§c.",
                    "%Mode%", args[0]);
            return null;
        }

        OfflinePlayer target = resolvePlayer(sender, args[1]);
        if (target == null) return null;

        String reason;
        if (mode.equals("type")) {
            MuteReason muteReason = parseMuteReason(sender, args[2]);
            if (muteReason == null) return null;
            reason = muteReason.getReason();
        } else {
            reason = args[2];
        }

        Long amount = parsePositiveLong(sender, args[3]);
        DateUnit unit = parseDateUnit(sender, args[4]);
        if (amount == null || unit == null) return null;

        long durationMillis;
        try {
            durationMillis = Math.multiplyExact(Math.multiplyExact(amount, unit.getToSec()), 1000L);
        } catch (ArithmeticException ex) {
            sendMessage(sender, "TempMute.Errors.DurationTooLarge", "§cMute duration is too large.");
            return null;
        }

        return new TempMuteRequest(target, reason, new Date(System.currentTimeMillis() + durationMillis), amount, unit);
    }

    private void setTempMute(OfflinePlayer target, String reason, Date expiresAt) {
        if (usesDatabaseStorage()) {
            new BanMuteManager().setTempMute(target, reason, formatDate(expiresAt));
            return;
        }

        String targetName = safePlayerName(target);
        cfg.set(targetName + ".reason", reason);
        cfg.set(targetName + ".expire", expiresAt);
        saveConfig();
    }

    private void removeTempMute(OfflinePlayer target) {
        if (usesDatabaseStorage()) {
            new BanMuteManager().removeTempMute(target);
            return;
        }

        cfg.set(safePlayerName(target), null);
        saveConfig();
    }

    public CompletableFuture<Boolean> isExpiredAsync(OfflinePlayer player) {
        if (player == null) return CompletableFuture.completedFuture(true);

        if (usesDatabaseStorage()) {
            BanMuteManager banMuteManager = new BanMuteManager();
            if (!banMuteManager.isTempMute(player)) {
                return CompletableFuture.completedFuture(true);
            }

            return banMuteManager.getTempMute(player)
                    .thenApply(data -> parseTempMuteData(data).map(TempMuteData::isExpired).orElse(true))
                    .exceptionally(t -> {
                        plugin.getLogger4J().error("Error while checking TempMute expiration for player: " + safePlayerName(player), t);
                        return true;
                    });
        }

        return CompletableFuture.completedFuture(getFileTempMuteData(player).map(TempMuteData::isExpired).orElse(true));
    }

    public boolean isExpired(OfflinePlayer player) {
        try {
            return isExpiredAsync(player).join();
        } catch (Exception e) {
            plugin.getLogger4J().error("Error while checking TempMute expiration for player: " + safePlayerName(player), e);
            return true;
        }
    }

    @EventHandler
    public void onChatWrite(AsyncPlayerChatEvent event) {
        if (event == null || event.getPlayer() == null) return;

        Player player = event.getPlayer();
        if (isPermanentlyMuted(player)) {
            sendMessage(player, "Mute.Chat.Permanent", "§cYou are Muted!");
            event.setCancelled(true);
            return;
        }

        TempMuteData muteData = getActiveTempMuteData(player);
        if (muteData == null) return;

        if (muteData.isExpired()) {
            removeTempMute(player);
            return;
        }

        sendMessage(player, "TempMute.Chat", "§cYou are Muted! While §6%Reason% &7| §aExpired at: §6%Expire%",
                "%Reason%", muteData.reason(),
                "%Expire%", formatDate(muteData.expiresAt()));
        event.setCancelled(true);
    }

    private TempMuteData getActiveTempMuteData(OfflinePlayer player) {
        if (usesDatabaseStorage()) {
            try {
                return parseTempMuteData(new BanMuteManager().getTempMute(player).join()).orElse(null);
            } catch (Exception e) {
                plugin.getLogger4J().error("Error while loading TempMute for player: " + safePlayerName(player), e);
                return null;
            }
        }
        return getFileTempMuteData(player).orElse(null);
    }

    private Optional<TempMuteData> getFileTempMuteData(OfflinePlayer player) {
        String targetName = safePlayerName(player);
        if (!cfg.contains(targetName + ".reason")) return Optional.empty();

        String reason = cfg.getString(targetName + ".reason", "");
        Date expiresAt = getDateFromConfig(targetName + ".expire");
        return expiresAt == null ? Optional.empty() : Optional.of(new TempMuteData(reason, expiresAt));
    }

    private Optional<TempMuteData> parseTempMuteData(Map<String, String> data) {
        if (data == null) return Optional.empty();

        String expiresAtRaw = data.get("TempMute");
        String reason = data.getOrDefault("TempMuteReason", "");
        if (expiresAtRaw == null || expiresAtRaw.isBlank() || expiresAtRaw.equals(" ")) {
            return Optional.empty();
        }

        Date expiresAt = parseDate(expiresAtRaw);
        return expiresAt == null ? Optional.empty() : Optional.of(new TempMuteData(reason, expiresAt));
    }

    private void sendFileMuteInfo(CommandSender sender) {
        for (String targetName : cfg.getKeys(false)) {
            TempMuteData data = getFileTempMuteData(Bukkit.getOfflinePlayer(targetName)).orElse(null);
            if (data != null && !data.isExpired()) {
                sendMuteInfo(sender, targetName, data);
            }
        }
    }

    private void sendDatabaseMuteInfo(CommandSender sender) {
        BanMuteManager manager = new BanMuteManager();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player == null || !manager.isTempMute(player)) continue;

            manager.getTempMute(player).thenAccept(data ->
                    parseTempMuteData(data).ifPresent(tempMuteData ->
                            sendMuteInfo(sender, safePlayerName(player), tempMuteData)
                    )
            ).exceptionally(t -> {
                plugin.getLogger4J().error("Error while loading temp mute info for " + safePlayerName(player), t);
                return null;
            });
        }
    }

    private void sendMuteInfo(CommandSender sender, String playerName, TempMuteData data) {
        sendMessage(sender, "TempMute.Info.Entry", "§6%Player% §ais muted while: §6%Reason%",
                "%Player%", playerName,
                "%Reason%", data.reason(),
                "%Expire%", formatDate(data.expiresAt()));
        sendMessage(sender, "TempMute.Info.Expires", "§aExpired at §6%Expire%",
                "%Player%", playerName,
                "%Reason%", data.reason(),
                "%Expire%", formatDate(data.expiresAt()));
    }

    private void notifyMuteState(CommandSender sender, OfflinePlayer target, boolean mutedNow) {
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            sendFormatted(onlineTarget,
                    mutedNow ? "Mute.Self.Activate" : "Mute.Self.Deactivate",
                    mutedNow ? "§cYou have been muted!" : "§aYou have been unmuted!",
                    target);
        }

        sendFormatted(sender,
                mutedNow ? "Mute.Other.Activate" : "Mute.Other.Deactivate",
                mutedNow ? "§6%Player% §chas been muted!" : "§6%Player% §ahas been unmuted!",
                target);
    }

    private void sendFormatted(CommandSender sender, String key, String defaultMessage, OfflinePlayer target) {
        sendMessage(sender, key, defaultMessage, "%Player%", safePlayerName(target));
    }

    private void notifyTempMuteState(CommandSender sender, TempMuteRequest request, boolean mutedNow) {
        OfflinePlayer target = request.target();
        String expiresAt = request.expiresAt() == null ? "" : formatDate(request.expiresAt());
        String time = request.amount() <= 0 ? "" : String.valueOf(request.amount());
        String unit = request.unit() == null ? "" : request.unit().getOutput();

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            sendMessage(onlineTarget,
                    mutedNow ? "TempMute.Self.Activate" : "TempMute.Self.Deactivate",
                    mutedNow ? "§cYou have been temporarily muted while §6%Reason% §cuntil §6%Expire%!" : "§aYour temporary mute has been removed!",
                    "%Player%", safePlayerName(target),
                    "%Reason%", request.reason(),
                    "%Time%", time,
                    "%Unit%", unit,
                    "%Expire%", expiresAt);
        }

        sendMessage(sender,
                mutedNow ? "TempMute.Other.Activate" : "TempMute.Other.Deactivate",
                mutedNow ? "§6%Player% §chas been temporarily muted while §6%Reason% §cfor §6%Time% %Unit%!" : "§6%Player% §ahas been temporarily unmuted!",
                "%Player%", safePlayerName(target),
                "%Reason%", request.reason(),
                "%Time%", time,
                "%Unit%", unit,
                "%Expire%", expiresAt);
    }

    private boolean isPermanentlyMuted(OfflinePlayer player) {
        return muted.stream().anyMatch(mutedPlayer -> samePlayer(mutedPlayer, player));
    }

    private boolean samePlayer(OfflinePlayer first, OfflinePlayer second) {
        return first != null && second != null && first.getUniqueId().equals(second.getUniqueId());
    }

    private boolean usesDatabaseStorage() {
        return plugin.isMysql() || plugin.isSQL() || plugin.isMongoDB();
    }

    private OfflinePlayer resolvePlayer(CommandSender sender, String playerName) {
        try {
            return PlayerUtils.getOfflinePlayerByName(playerName);
        } catch (IllegalArgumentException ex) {
            sendMessage(sender, "TempMute.Errors.PlayerNameEmpty", "§cPlayer name cannot be empty!");
            return null;
        }
    }

    private MuteReason parseMuteReason(CommandSender sender, String value) {
        try {
            return MuteReason.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sendMessage(sender, "TempMute.Errors.UnknownReason", "§cUnknown mute reason: §6%Reason%",
                    "%Reason%", value);
            return null;
        }
    }

    private DateUnit parseDateUnit(CommandSender sender, String value) {
        try {
            return DateUnit.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sendMessage(sender, "TempMute.Errors.UnknownUnit", "§cUnknown time unit: §6%Unit%",
                    "%Unit%", value);
            return null;
        }
    }

    private Long parsePositiveLong(CommandSender sender, String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                sendMessage(sender, "TempMute.Errors.TimeGreaterThanZero", "§cTime must be greater than 0.",
                        "%Time%", value);
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            sendMessage(sender, "TempMute.Errors.InvalidTime", "§cInvalid time: §6%Time%",
                    "%Time%", value);
            return null;
        }
    }

    private Date getDateFromConfig(String path) {
        Object value = cfg.get(path);
        if (value instanceof Date date) return date;
        if (value instanceof String string) return parseDate(string);
        return null;
    }

    private Date parseDate(String value) {
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(value);
        } catch (ParseException e) {
            plugin.getLogger4J().error("Failed to parse temp mute date: " + value, e);
            return null;
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }

    private String safePlayerName(OfflinePlayer player) {
        if (player == null) return "unknown";
        String name = player.getName();
        return name == null || name.isBlank() ? player.getUniqueId().toString() : name;
    }

    private boolean hasPermission(CommandSender sender, String permissionSuffix) {
        if (sender.hasPermission(plugin.getPermissionBase() + permissionSuffix)) return true;
        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private void sendWrongArgs(CommandSender sender, String usage) {
        send(sender, plugin.getWrongArgs(sender instanceof Player player ? player : null, usage));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    private void sendMessage(CommandSender sender, String key, String defaultMessage, String... replacements) {
        send(sender, message(sender, key, defaultMessage, replacements));
    }

    private String message(CommandSender receiver, String key, String defaultMessage, String... replacements) {
        String message = plugin.getLanguageConfig(receiver).getString(key, defaultMessage);
        if (message == null) message = defaultMessage;
        message = ReplaceCharConfig.replaceParagraph(message);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = ReplaceCharConfig.replaceObjectWithData(message, replacements[i], replacements[i + 1] == null ? "" : replacements[i + 1]);
        }
        return message;
    }

    private void ensureMuteFile() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger4J().warn("Could not create data folder for tempMutes.yml");
        }
        try {
            if (!file.exists() && !file.createNewFile()) {
                plugin.getLogger4J().warn("Could not create tempMutes.yml");
            }
        } catch (IOException e) {
            plugin.getLogger4J().error("Failed to create tempMutes.yml", e);
        }
    }

    private void saveConfig() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger4J().error(e);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(TEMP_MUTE)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return TabCompleteUtils.matchingStrings(List.of("own", "type"), args[0]);
        }

        if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (offlinePlayer != null && offlinePlayer.getName() != null) {
                    playerNames.add(offlinePlayer.getName());
                }
            }
            return TabCompleteUtils.matchingStrings(playerNames, args[1]);
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("type")) {
                return TabCompleteUtils.matchingStrings(Arrays.stream(MuteReason.values()).map(Enum::name).toList(), args[2]);
            }
            if (args[0].equalsIgnoreCase("own")) {
                return TabCompleteUtils.matchingStrings(List.of("your_Message"), args[2]);
            }
        }

        if (args.length == 4) {
            return TabCompleteUtils.matchingStrings(List.of("Time"), args[3]);
        }

        if (args.length == 5) {
            return TabCompleteUtils.matchingStrings(Arrays.stream(DateUnit.values()).map(Enum::name).toList(), args[4]);
        }

        return Collections.emptyList();
    }

    private record TempMuteRequest(OfflinePlayer target, String reason, Date expiresAt, long amount, DateUnit unit) {
    }

    private record TempMuteData(String reason, Date expiresAt) {
        private boolean isExpired() {
            return expiresAt.getTime() < System.currentTimeMillis();
        }
    }

    public enum MuteReason {
        ADVERTISING("advertising"),
        CAPS("caps"),
        MILD_TOXICITY("mild toxicity"),
        TOXICITY("toxicity"),
        SPAMMING("spamming"),
        CURSING("cursing"),
        INSULTING("insulting"),
        NSFW("nfsw"),
        LEAKING_SENSITIVE_DATA("leaking sensitive Data"),
        VIOLATION_OF_THE_RULES("violation of the rules");

        private final String reason;

        MuteReason(String reason) {
            this.reason = reason;
        }

        @SuppressWarnings("unused")
        public static MuteReason getMuteReason(String reason) {
            return valueOf(reason.toUpperCase(Locale.ROOT));
        }

        public String getReason() {
            return reason;
        }
    }
}
