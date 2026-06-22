package ch.framedev.essentialsmini.commands.playercommands;

/*
 * ch.framedev.essentialsmini.commands.playercommands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 25.01.2025 13:31
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlayerWeatherCMD extends CommandBase {

    private static final String PLAYER_WEATHER = "playerweather";
    private static final String RESET_PLAYER_WEATHER = "resetplayerweather";
    private static final String PERMISSION = "playerweather";
    private static final String USAGE = "/playerweather (CLEAR/DOWNFALL)";
    private static final String MESSAGE_KEY = "PlayerWeather";
    private static final String DEFAULT_SET_MESSAGE = "§aYour Player Weather has been set to §6%WEATHER%§a!";
    private static final String DEFAULT_RESET_MESSAGE = "§aYour Player weather has been reset!";

    public PlayerWeatherCMD(Main plugin) {
        super(plugin, PLAYER_WEATHER, RESET_PLAYER_WEATHER);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (!hasPermission(player)) return true;

        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case PLAYER_WEATHER -> handleSetWeather(player, args);
            case RESET_PLAYER_WEATHER -> handleResetWeather(player);
            default -> false;
        };
    }

    private boolean handleSetWeather(Player player, String[] args) {
        if (args.length != 1) {
            sendWrongArgs(player);
            return true;
        }

        WeatherType weatherType = parseWeatherType(args[0]);
        if (weatherType == null) {
            send(player, "§cPlease use valid Weather Types (clear/downfall)!");
            return true;
        }

        player.setPlayerWeather(weatherType);
        send(player, message(player, MESSAGE_KEY + ".Set", DEFAULT_SET_MESSAGE)
                .replace("%WEATHER%", weatherType.name().toLowerCase(Locale.ROOT)));
        return true;
    }

    private boolean handleResetWeather(Player player) {
        player.resetPlayerWeather();
        send(player, message(player, MESSAGE_KEY + ".Reset", DEFAULT_RESET_MESSAGE));
        return true;
    }

    private WeatherType parseWeatherType(String value) {
        try {
            return WeatherType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean hasPermission(Player player) {
        if (player.hasPermission(getPlugin().getPermissionBase() + PERMISSION)) return true;

        send(player, getPlugin().getNoPerms(player));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        send(sender, getPlugin().getOnlyPlayer(null));
        return null;
    }

    private String message(Player player, String key, String defaultMessage) {
        String value = getPlugin().getLanguageConfig(player).getString(key, defaultMessage);
        return ReplaceCharConfig.replaceParagraph(value == null ? defaultMessage : value);
    }

    private void sendWrongArgs(Player player) {
        send(player, getPlugin().getWrongArgs(player, USAGE));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(getPlugin().getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase(PLAYER_WEATHER) && args.length == 1) {
            return TabCompleteUtils.matchingStrings(Arrays.stream(WeatherType.values()).map(Enum::name).toList(), args[0]);
        }
        return List.of();
    }
}
