package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimePlayedCMD extends CommandBase {

    private static final String TIME_PLAYED_KEY = "timePlayed";
    private static final String DEFAULT_SELF_MESSAGE = "§aPlayed: §6%TimePlayed%";
    private static final String TARGET_MESSAGE = "§aPlayer §6%Player% §ahas Played: §6%TimePlayed%";

    public TimePlayedCMD(Main plugin) {
        super(plugin, "timeplayed", "playedtime");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        return switch (args.length) {
            case 0 -> handleOwnPlaytime(sender);
            case 1 -> handleOtherPlaytime(sender, args[0]);
            default -> {
                sender.sendMessage(getPrefix() + getPlugin().getWrongArgs("/timeplayed [Player]"));
                yield true;
            }
        };
    }

    private boolean handleOwnPlaytime(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPrefix() + getPlugin().getOnlyPlayer());
            return true;
        }

        sendPlaytimeMessage(player, player, false);
        return true;
    }

    private boolean handleOtherPlaytime(CommandSender sender, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            sender.sendMessage(getPrefix() + getPlugin().getWrongArgs("/timeplayed [Player]"));
            return true;
        }

        Player online = Bukkit.getPlayer(playerName);
        if (online != null) {
            sendPlaytimeMessage(sender, online, true);
            return true;
        }

        OfflinePlayer offline = findOfflinePlayer(playerName);
        if (offline == null || !offline.hasPlayedBefore()) {
            sender.sendMessage(getPrefix() + "§cPlayer §6" + playerName + " §cis not known.");
            return true;
        }

        sender.sendMessage(getPrefix() + "§cPlayer §6" + safeName(offline, playerName)
                + " §ais offline; playtime data is unavailable.");
        return true;
    }

    private OfflinePlayer findOfflinePlayer(String playerName) {
        try {
            return PlayerUtils.getOfflinePlayerByName(playerName);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void sendPlaytimeMessage(CommandSender receiver, Player target, boolean includePlayerName) {
        String formattedTime = toFormattedTime(calculateSeconds(target));
        String message = includePlayerName
                ? TARGET_MESSAGE
                : getLanguageMessage(receiver);

        message = replacePlaceholders(message, target.getName(), formattedTime);
        receiver.sendMessage(getPrefix() + message);
    }

    private String getLanguageMessage(CommandSender sender) {
        String message = getPlugin().getLanguageConfig(sender).getString(TIME_PLAYED_KEY);
        return message == null ? DEFAULT_SELF_MESSAGE : message;
    }

    private String replacePlaceholders(String message, String playerName, String formattedTime) {
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", playerName);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%TimePlayed%", formattedTime);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Time%", formattedTime);
        return ReplaceCharConfig.replaceParagraph(message);
    }

    private long calculateSeconds(Player player) {
        long playedTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        return Math.max(0L, playedTicks / 20L);
    }

    private String safeName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name == null ? fallback : name;
    }

    private String toFormattedTime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 1) {
            return super.onTabComplete(sender, command, label, args);
        }

        List<String> playerNames = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null) {
                playerNames.add(offlinePlayer.getName());
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        return TabCompleteUtils.matchingStrings(playerNames, args[0]);
    }
}
