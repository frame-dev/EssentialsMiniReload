package ch.framedev.essentialsmini.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for optional Geyser and Floodgate integration.
 *
 * <p>All Geyser and Floodgate calls are made through reflection, so the plugin can
 * still load on servers that do not have either plugin installed.</p>
 */
@SuppressWarnings({"unused", "SameParameterValue"})
public final class GeyserManager {

    private static final String GEYSER_API_CLASS = "org.geysermc.geyser.api.GeyserApi";
    private static final String FLOODGATE_API_CLASS = "org.geysermc.floodgate.api.FloodgateApi";
    private static final String[] GEYSER_PLUGIN_NAMES = {"Geyser-Spigot", "Geyser-Bukkit", "Geyser"};
    private static final String[] FLOODGATE_PLUGIN_NAMES = {"floodgate", "Floodgate"};

    private GeyserManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns true if Geyser is available either through the API class or as an enabled Bukkit plugin.
     */
    public static boolean isGeyserInstalled() {
        return isGeyserApiAvailable() || isAnyPluginEnabled(GEYSER_PLUGIN_NAMES);
    }

    /**
     * Returns true if the Geyser API class is visible to this plugin.
     */
    public static boolean isGeyserApiAvailable() {
        return isClassAvailable(GEYSER_API_CLASS);
    }

    /**
     * Returns true if Floodgate is available either through the API class or as an enabled Bukkit plugin.
     */
    public static boolean isFloodgateInstalled() {
        return isFloodgateApiAvailable() || isAnyPluginEnabled(FLOODGATE_PLUGIN_NAMES);
    }

    /**
     * Returns true if the Floodgate API class is visible to this plugin.
     */
    public static boolean isFloodgateApiAvailable() {
        return isClassAvailable(FLOODGATE_API_CLASS);
    }

    /**
     * Returns true when the UUID belongs to a Bedrock player known by either Geyser or Floodgate.
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        return isGeyserPlayer(uuid) || isFloodgatePlayer(uuid);
    }

    /**
     * Returns true when Geyser currently has a connection for the UUID.
     */
    public static boolean isGeyserPlayer(UUID uuid) {
        return getGeyserConnection(uuid).isPresent();
    }

    /**
     * Uses FloodgateApi.getInstance().isFloodgatePlayer(UUID) via reflection.
     * Returns true only if Floodgate is installed and the UUID belongs to a Floodgate player.
     */
    public static boolean isFloodgatePlayer(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        return getFloodgateApi()
                .flatMap(api -> invoke(api, "isFloodgatePlayer", new Class<?>[]{UUID.class}, uuid))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    /**
     * Returns the Geyser connection object for an online Bedrock player.
     */
    public static Optional<Object> getGeyserConnection(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        return getGeyserApi()
                .flatMap(api -> invoke(api, "connectionByUuid", new Class<?>[]{UUID.class}, uuid));
    }

    /**
     * Returns the Geyser connection object for an online Bedrock player by XUID.
     */
    public static Optional<Object> getGeyserConnectionByXuid(String xuid) {
        if (isBlank(xuid)) {
            return Optional.empty();
        }

        return getGeyserApi()
                .flatMap(api -> invoke(api, "connectionByXuid", new Class<?>[]{String.class}, xuid.trim()));
    }

    /**
     * Finds an online Geyser connection by Bedrock name, Java name, or Floodgate-prefixed name.
     */
    public static Optional<Object> findGeyserConnectionByName(String playerName) {
        if (isBlank(playerName)) {
            return Optional.empty();
        }

        return getOnlineGeyserConnections().stream()
                .filter(connection -> matchesName(playerName, getString(connection, "bedrockUsername").orElse(null))
                        || matchesName(playerName, getString(connection, "javaUsername").orElse(null)))
                .findFirst();
    }

    /**
     * Returns all online Geyser connection objects.
     */
    public static List<Object> getOnlineGeyserConnections() {
        Optional<Object> onlineConnections = getGeyserApi()
                .flatMap(api -> invoke(api, "onlineConnections"));

        if (onlineConnections.isEmpty() || !(onlineConnections.get() instanceof Collection<?> connections)) {
            return Collections.emptyList();
        }

        List<Object> result = new ArrayList<>();
        for (Object connection : connections) {
            if (connection != null) {
                result.add(connection);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns Bukkit players that are currently connected through Geyser or Floodgate.
     */
    public static List<Player> getOnlineBedrockPlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isBedrockPlayer(player.getUniqueId())) {
                players.add(player);
            }
        }
        return Collections.unmodifiableList(players);
    }

    public static int getOnlineBedrockPlayerCount() {
        return getOnlineBedrockPlayers().size();
    }

    public static Optional<String> getBedrockUsername(UUID uuid) {
        return getGeyserConnection(uuid)
                .flatMap(connection -> getString(connection, "bedrockUsername"))
                .or(() -> getFloodgatePlayerString(uuid, "getUsername", "username", "bedrockUsername"));
    }

    public static Optional<String> getJavaUsername(UUID uuid) {
        return getGeyserConnection(uuid)
                .flatMap(connection -> getString(connection, "javaUsername"))
                .or(() -> getFloodgatePlayerString(uuid, "getJavaUsername", "javaUsername"));
    }

    public static Optional<String> getXuid(UUID uuid) {
        return getGeyserConnection(uuid)
                .flatMap(connection -> getString(connection, "xuid"))
                .or(() -> getFloodgatePlayerString(uuid, "getXuid", "xuid"));
    }

    public static Optional<String> getBedrockVersion(UUID uuid) {
        return getGeyserConnection(uuid).flatMap(connection -> getString(connection, "version"));
    }

    public static Optional<String> getLanguageCode(UUID uuid) {
        return getGeyserConnection(uuid).flatMap(connection -> getString(connection, "languageCode"));
    }

    public static Optional<String> getPlatform(UUID uuid) {
        return getGeyserConnection(uuid).flatMap(connection -> getString(connection, "platform"));
    }

    public static Optional<String> getInputMode(UUID uuid) {
        return getGeyserConnection(uuid).flatMap(connection -> getString(connection, "inputMode"));
    }

    public static Optional<Integer> getPing(UUID uuid) {
        return getGeyserConnection(uuid).flatMap(connection -> getInteger(connection, "ping"));
    }

    public static boolean isLinked(UUID uuid) {
        return getGeyserConnection(uuid)
                .flatMap(connection -> getBoolean(connection, "isLinked"))
                .or(() -> getFloodgatePlayerBoolean(uuid, "isLinked"))
                .orElse(false);
    }

    /**
     * Transfers a connected Bedrock player to another Bedrock server.
     */
    public static boolean transfer(UUID uuid, String host, int port) {
        if (isBlank(host) || port < 1 || port > 65535) {
            return false;
        }

        return getGeyserConnection(uuid)
                .flatMap(connection -> invoke(connection, "transfer", new Class<?>[]{String.class, int.class}, host.trim(), port))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    /**
     * Runs a command as the Bedrock client through Geyser.
     */
    public static boolean sendCommand(UUID uuid, String command) {
        if (isBlank(command)) {
            return false;
        }

        return getGeyserConnection(uuid)
                .flatMap(connection -> invoke(connection, "sendCommand", new Class<?>[]{String.class}, command.trim()))
                .isPresent();
    }

    public static boolean hasFormOpen(UUID uuid) {
        return getGeyserConnection(uuid)
                .flatMap(connection -> getBoolean(connection, "hasFormOpen"))
                .orElse(false);
    }

    public static boolean closeForm(UUID uuid) {
        return getGeyserConnection(uuid)
                .flatMap(connection -> invoke(connection, "closeForm"))
                .isPresent();
    }

    /**
     * Returns the player's own Geyser entity object if Geyser exposes it.
     */
    public static Optional<Object> getGeyserPlayerEntity(UUID uuid) {
        return getGeyserConnection(uuid)
                .flatMap(connection -> invoke(connection, "entities"))
                .flatMap(entities -> invoke(entities, "playerEntity"));
    }

    /**
     * Legacy-friendly alias for older callers. Prefer {@link #getGeyserPlayerEntity(UUID)}
     * or {@link #getGeyserConnection(UUID)} for new code.
     */
    @Deprecated
    public static Object getGeyserPlayer(UUID uuid) {
        return getGeyserPlayerEntity(uuid).orElse(null);
    }

    public static Optional<String> getFloodgatePlayerPrefix() {
        return getFloodgateApi().flatMap(api -> getString(api, "getPlayerPrefix"));
    }

    public static String stripFloodgatePrefix(String playerName) {
        if (playerName == null) {
            return null;
        }

        Optional<String> prefix = getFloodgatePlayerPrefix();
        if (prefix.isPresent() && !prefix.get().isEmpty() && playerName.startsWith(prefix.get())) {
            return playerName.substring(prefix.get().length());
        }
        return playerName;
    }

    public static boolean matchesBedrockName(UUID uuid, String playerName) {
        if (uuid == null || isBlank(playerName)) {
            return false;
        }

        return getBedrockUsername(uuid)
                .map(name -> matchesName(playerName, name))
                .orElse(false);
    }

    private static Optional<Object> getGeyserApi() {
        return getStaticApi(GEYSER_API_CLASS, "api");
    }

    private static Optional<Object> getFloodgateApi() {
        return getStaticApi(FLOODGATE_API_CLASS, "getInstance");
    }

    private static Optional<Object> getFloodgatePlayer(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        return getFloodgateApi()
                .flatMap(api -> invoke(api, "getPlayer", new Class<?>[]{UUID.class}, uuid));
    }

    private static Optional<String> getFloodgatePlayerString(UUID uuid, String... methodNames) {
        Optional<Object> player = getFloodgatePlayer(uuid);
        if (player.isEmpty()) {
            return Optional.empty();
        }

        for (String methodName : methodNames) {
            Optional<String> value = getString(player.get(), methodName);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static Optional<Boolean> getFloodgatePlayerBoolean(UUID uuid, String... methodNames) {
        Optional<Object> player = getFloodgatePlayer(uuid);
        if (player.isEmpty()) {
            return Optional.empty();
        }

        for (String methodName : methodNames) {
            Optional<Boolean> value = getBoolean(player.get(), methodName);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static Optional<Object> getStaticApi(String className, String methodName) {
        try {
            Class<?> apiClass = Class.forName(className);
            Method method = apiClass.getMethod(methodName);
            return Optional.ofNullable(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> invoke(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private static Optional<Object> invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        if (target == null) {
            return Optional.empty();
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return Optional.ofNullable(method.invoke(target, arguments));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<String> getString(Object target, String methodName) {
        return invoke(target, methodName)
                .map(String::valueOf)
                .filter(value -> !value.isEmpty());
    }

    private static Optional<Boolean> getBoolean(Object target, String methodName) {
        return invoke(target, methodName)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast);
    }

    private static Optional<Integer> getInteger(Object target, String methodName) {
        return invoke(target, methodName)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue);
    }

    private static boolean matchesName(String expected, String actual) {
        if (isBlank(expected) || isBlank(actual)) {
            return false;
        }

        String expectedName = expected.trim();
        String actualName = actual.trim();
        return expectedName.equalsIgnoreCase(actualName)
                || stripFloodgatePrefix(expectedName).equalsIgnoreCase(actualName)
                || expectedName.equalsIgnoreCase(stripFloodgatePrefix(actualName));
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className, false, GeyserManager.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isAnyPluginEnabled(String... pluginNames) {
        for (String pluginName : pluginNames) {
            try {
                if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                    return true;
                }
            } catch (IllegalStateException | LinkageError ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
