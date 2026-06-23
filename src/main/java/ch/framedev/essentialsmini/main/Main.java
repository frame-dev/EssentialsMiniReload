package ch.framedev.essentialsmini.main;



/*
 * ch.framedev.essentialsmini
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 06.03.2025 12:39
 */

import ch.framedev.essentialsmini.commands.playercommands.BackpackCMD;
import ch.framedev.essentialsmini.database.mongodb.BackendManager;
import ch.framedev.essentialsmini.database.mongodb.DatabaseManager;
import ch.framedev.essentialsmini.managers.KitManager;
import ch.framedev.essentialsmini.managers.RegisterManager;
import ch.framedev.essentialsmini.managers.VaultManager;
import ch.framedev.essentialsmini.utils.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;

public class Main extends JavaPlugin {

    // Singleton instance of the plugin
    private static Main instance;

    // Logger for the plugin (log4j)
    private Logger logger;

    private Map<String, CommandExecutor> commands;
    // Register TabCompleter HashMap
    private Map<String, TabCompleter> tabCompleters;
    // Register Listener List
    private List<Listener> listeners;

    private static List<String> silent;

    // Database variables
    private DatabaseManager databaseManager;
    private MongoDBUtils mongoDBUtils;

    private SkinService skinService;

    // Vault variables
    private VaultManager vaultManager;

    // Variables for the plugin
    private Variables variables;

    // RegisterManager for cleanup
    private RegisterManager registerManager;

    private boolean debug;

    private final Map<String, FileConfiguration> languageConfigs = new HashMap<>();

    private static final Map<String, Language> LANGUAGE_BY_PREFIX = Map.of(
            "en", Language.EN,
            "de", Language.DE,
            "fr", Language.FR,
            "it", Language.IT,
            "es", Language.ES,
            "pt", Language.PT,
            "pl", Language.PL,
            "ru", Language.RU
    );


    @Override
    public void onEnable() {
        // Set the instance to this plugin
        instance = this;

        // Loads the default configuration file
        saveDefaultConfig();
        saveConfig();
        debug = getConfig().getBoolean("debug", false);

        // Initialize the logger
        logger = Logger.getLogger("EssentialsMini");
        BasicConfigurator.configure();

        // create the messages-examples directory if it does not exist
        if (!new File(getDataFolder() + "/messages-examples").exists()) {
            if (!new File(getDataFolder() + "/messages-examples").mkdir()) {
                getLogger4J().error("Could not create directory " + getDataFolder() + "/messages-examples");
            }
        }
        // Move example messages to the messages-examples directory
        moveExampleMessages();
        // Check and move messages configs
        checkAndMoveMessagesConfigs();
        reloadLanguageConfigs();

        /* HashMaps / Lists Initialing */
        this.commands = new HashMap<>();
        this.listeners = new ArrayList<>();
        this.tabCompleters = new HashMap<>();

        // Initialize the silent list
        silent = new ArrayList<>();

        // Initialize the Variables, DatabaseManager and MongoDBUtils
        this.variables = new Variables();
        this.databaseManager = new DatabaseManager(this);

        if (getConfig().getBoolean("MongoDB.Boolean") || getConfig().getBoolean("MongoDB.LocalHost")) {
            this.mongoDBUtils = new MongoDBUtils(this);
            if (isMongoDB()) {
                initializeMongoUsersAsync(Bukkit.getOfflinePlayers());
                Bukkit.getConsoleSender().sendMessage(getPrefix() + "§aMongoDB Enabled!");
            }
        }

        // Ensure ProtocolLib is present
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            skinService = new SkinService(this);
            skinService.start();
        }

        // Enable economy if enabled in config
        if (getConfig().getBoolean("Economy.Activate")) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                this.vaultManager = new VaultManager(this);
            }
        }
        // Register Commands, TabCompleters and Listeners
        this.registerManager = new RegisterManager(this);

        // Register KitManager and create custom config
        new KitManager().createCustomConfig();

        // Checking for Update and when enabled, Download the Latest Version automatically

        if (getConfig().getBoolean("checkForUpdates")) {
            checkUpdatesAsync(getConfig().getBoolean("AutoDownload"));
        } else {
            // Update check is disabled in config.yml
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cUpdate check is disabled in config.yml!");
        }

        // Restore Backpacks
        BackpackCMD.restoreAll();

        // Log that the plugin has been enabled
        getLogger4J().info("EssentialsMini has been enabled!");
    }

    private void initializeMongoUsersAsync(OfflinePlayer[] offlinePlayers) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            for (OfflinePlayer player : offlinePlayers) {
                databaseManager.getBackendManager().createUser(player, "essentialsmini_data", new BackendManager.Callback<>() {
                    @Override
                    public void onResult(Void result) {
                        logger.info("User " + player.getUniqueId() + " created successfully in MongoDB.");
                    }

                    @Override
                    public void onError(Exception exception) {
                        logger.error("Could not create User: " + exception.getMessage(), exception);
                    }
                });
            }
        });
    }

    private void checkUpdatesAsync(boolean autoDownload) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean updateAvailable = checkUpdate(autoDownload);
            Bukkit.getScheduler().runTask(this, () -> {
                if (updateAvailable) {
                    Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cPlease restart the server to apply the update!");
                } else {
                    Bukkit.getConsoleSender().sendMessage(getPrefix() + "§aNo new updates found!");
                }
            });
        });
    }

    @Override
    public void onDisable() {
        // Cleanup RegisterManager (including TrashInventory)
        if (registerManager != null) {
            registerManager.cleanup();
        }

        // Save Backpacks
        BackpackCMD.saveAll();

        // Clear lists, maps and variables
        instance = null;
        listeners.clear();
        commands.clear();
        tabCompleters.clear();
        vaultManager = null;
        mongoDBUtils = null;
        variables = null;
        silent.clear();
        databaseManager = null;
        // Log that the plugin has been disabled
        getLogger4J().info("EssentialsMini has been disabled!");
    }

    public boolean isDebug() {
        return debug;
    }

    public SkinService getSkinService() {
        return skinService;
    }

    private static final String[] SUPPORTED_LOCALES = {"de-DE", "en-EN", "fr-FR", "it-IT", "pt-PT", "pl-PL", "es-ES", "ru-RU"};

    /**
     * Moves example messages from the resources to the messages-examples directory.
     * The method checks if the destination file already exists before copying.
     */
    public void moveExampleMessages() {
        File destinationDir = new File(getDataFolder(), "messages-examples");
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            getLogger4J().error("Failed to create destination directory: " + destinationDir.getPath());
            return;
        }

        for (String locale : SUPPORTED_LOCALES) {
            String resourcePath = "locale-examples/messages_" + locale + "-examples.yml";
            File destinationFile = new File(destinationDir, "messages_" + locale + "-examples.yml");
            if (destinationFile.exists()) continue;

            try (InputStream in = getResource(resourcePath)) {
                if (in == null) {
                    getLogger4J().error("Missing bundled resource: " + resourcePath);
                    continue;
                }

                try (OutputStream out = new FileOutputStream(destinationFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }

                getLogger4J().info("Successfully copied: " + destinationFile.getName());
            } catch (IOException e) {
                getLogger4J().error("Failed to copy example messages file: " + resourcePath, e);
            }
        }
    }

    /**
     * Checks if the messages configuration files exist, and if not, copies them from the example directory.
     * The method checks for multiple language configurations and copies them if they do not exist.
     */
    public void checkAndMoveMessagesConfigs() {
        File dataFolder = getDataFolder();
        File exampleFolder = new File(dataFolder, "messages-examples");
        boolean copiedAny = false;

        for (String locale : SUPPORTED_LOCALES) {
            File configFile = new File(dataFolder, "messages_" + locale + ".yml");
            if (configFile.exists()) {
                continue;
            }

            File exampleFile = new File(exampleFolder, "messages_" + locale + "-examples.yml");
            if (!exampleFile.exists()) {
                getLogger4J().warn("Example language file not found: " + exampleFile.getPath());
                continue;
            }

            try {
                Files.copy(exampleFile.toPath(), configFile.toPath());
                copiedAny = true;
            } catch (IOException e) {
                getLogger4J().error("Failed to copy language file: " + configFile.getPath(), e);
            }
        }

        if (copiedAny) {
            getLogger4J().info("Missing language configuration files were created.");
        } else {
            getLogger4J().info("Configuration files already exist. Skipping download.");
        }
    }

    public boolean isMysql() {
        return getConfig().getBoolean("MySQL.Use", false);
    }

    public boolean isSQL() {
        return getConfig().getBoolean("SQLite.Use", false);
    }

    public boolean isPostgres() {
        return getConfig().getBoolean("PostgreSQL.Use", false);
    }

    public static Main getInstance() {
        return instance;
    }

    public Map<String, CommandExecutor> getCommands() {
        return commands;
    }

    public Map<String, TabCompleter> getTabCompleters() {
        return tabCompleters;
    }

    public List<Listener> getListeners() {
        return listeners;
    }

    public static List<String> getSilent() {
        return silent;
    }

    @SuppressWarnings("SameReturnValue")
    public String getPermissionBase() {
        return "essentialsmini.";
    }

    public Variables getVariables() {
        return variables;
    }

    public boolean isHomeTP() {
        return getConfig().getBoolean("HomeTP", false);
    }

    /**
     * Retrieves the prefix from the configuration file.
     * If the prefix is not found, it checks for an old format and updates it.
     * It also replaces color codes and special characters in the prefix.
     *
     * @return The formatted prefix string.
     * @throws NullPointerException if the prefix is not found in the configuration.
     */
    public String getPrefix() {
        // If prefix is not found in config.yml, check for prefix in config.yml (old format) and update config.yml
        if (!getConfig().contains("prefix") && getConfig().contains("Prefix")) {
            getConfig().set("prefix", getConfig().getString("Prefix"));
            saveConfig();
        }
        // retrieve the prefix from the config (key: "prefix")
        String prefix = getConfig().getString("prefix");
        if (prefix == null) {
            throw new NullPointerException("Prefix cannot be Found in Config.yml add (prefix:'YourPrefix') to the config.yml");
        }
        // Replace color codes and special characters in prefix
        if (prefix.contains("&"))
            prefix = prefix.replace('&', '§');
        if (prefix.contains(">>"))
            prefix = prefix.replace(">>", "»");
        if (prefix.contains("<<"))
            prefix = prefix.replace("<<", "«");
        if (prefix.contains("->"))
            prefix = prefix.replace("->", "→");
        if (prefix.contains("<-"))
            prefix = prefix.replace("<-", "←");
        return prefix;
    }

    /**
     * Returns the language configuration for the given player or command sender.
     * If the player is null, it defaults to English (en-EN).
     * If the player's locale is not supported, it falls back to English.
     *
     * @param player The CommandSender or Player for whom to get the language configuration.
     * @return The FileConfiguration for the player's language.
     */
    public FileConfiguration getLanguageConfig(CommandSender player) {
        if (player == null) {
            return getCachedLanguageConfig("en-EN");
        }

        String locale = "en-EN";
        if (player instanceof Player) {
            locale = getLanguageCode(player);
        }

        return getCachedLanguageConfig(locale);
    }

    @NotNull
    private String getLanguageCode(CommandSender player) {
        String locale;
        Language language = getLanguage(player);
        switch (language) {
            case DE -> locale = "de-DE";
            case FR -> locale = "fr-FR";
            case IT -> locale = "it-IT";
            case ES -> locale = "es-ES";
            case PT -> locale = "pt-PT";
            case PL -> locale = "pl-PL";
            case RU -> locale = "ru-RU";
            default -> locale = "en-EN";
        }
        return locale;
    }

    /**
     * Returns the language configuration for the given player.
     * If the player is null, it defaults to English (en-EN).
     * If the player's locale is not supported, it falls back to English.
     *
     * @param player The Player for whom to get the language configuration.
     * @return The FileConfiguration for the player's language.
     */
    public FileConfiguration getLanguageConfig(Player player) {
        if (player == null) {
            return getCachedLanguageConfig("en-EN");
        }

        return getCachedLanguageConfig(getLanguageCode(player));
    }

    public void reloadLanguageConfigs() {
        languageConfigs.clear();
        for (String locale : SUPPORTED_LOCALES) {
            File configFile = new File(getDataFolder(), "messages_" + locale + ".yml");
            if (configFile.exists()) {
                languageConfigs.put(locale, YamlConfiguration.loadConfiguration(configFile));
            }
        }

        languageConfigs.computeIfAbsent("en-EN", locale ->
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages_en-EN.yml")));
    }

    private FileConfiguration getCachedLanguageConfig(String locale) {
        FileConfiguration config = languageConfigs.get(locale);
        if (config != null) {
            return config;
        }

        getLogger4J().warn("Language file for locale '" + locale + "' not found. Falling back to default (en-EN).");
        return languageConfigs.get("en-EN");
    }

    /**
     * Determines the language of the player based on their locale.
     * If the player's locale is not recognized, it defaults to English.
     *
     * @param player The CommandSender or Player whose language to determine.
     * @return The Language enum corresponding to the player's locale.
     */
    public Language getLanguage(CommandSender player) {
        if (player instanceof Player) {
            String playerLocale = ((Player) player).getLocale().toLowerCase();

            for (Map.Entry<String, Language> entry : LANGUAGE_BY_PREFIX.entrySet()) {
                if (playerLocale.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        // Default fallback
        return Language.EN;
    }

    /**
     * Determines the language of the player based on their locale.
     * If the player's locale is not recognized, it defaults to English.
     *
     * @return The Language enum corresponding to the player's locale.
     */
    public String getNoPerms() {
        String permission = getLanguageConfig(null).getString("NoPermissions");
        if (permission == null) return "";
        permission = permission.replace('&', '§');
        return permission;
    }

    public String getNoPerms(Player player) {
        String permission = getLanguageConfig(player).getString("NoPermissions");
        if (permission == null) return "";
        permission = permission.replace('&', '§');
        return permission;
    }

    public String getWrongArgs(String cmdName) {
        String wrongArgs = getLanguageConfig(null).getString("WrongArgs");
        if (wrongArgs == null) return "";
        wrongArgs = wrongArgs.replace("%cmdUsage%", cmdName);
        wrongArgs = wrongArgs.replace('&', '§');
        return wrongArgs;
    }

    public String getWrongArgs(Player player, String cmdName) {
        String wrongArgs = getLanguageConfig(player).getString("WrongArgs");
        if (wrongArgs == null) return "";
        wrongArgs = wrongArgs.replace("%cmdUsage%", cmdName);
        wrongArgs = wrongArgs.replace('&', '§');
        return wrongArgs;
    }

    public String getOnlyPlayer() {
        String onlyPlayer = getLanguageConfig(null).getString("OnlyPlayer");
        if (onlyPlayer == null) return "";
        onlyPlayer = onlyPlayer.replace('&', '§');
        return onlyPlayer;
    }

    /**
     * Get the OnlyPlayer message from messages.yml
     */
    public String getOnlyPlayer(Player player) {
        String onlyPlayer = getLanguageConfig(player).getString("OnlyPlayer");
        if (onlyPlayer == null) return "";
        onlyPlayer = onlyPlayer.replace('&', '§');
        return onlyPlayer;
    }

    /**
     * Get the log4j Logger
     */
    public Logger getLogger4J() {
        return logger;
    }

    /**
     * Get the Single Currency Symbol from config.yml
     */
    public String getCurrencySymbol() {
        return getConfig().getString("Currency.Single");
    }

    /**
     * Get the Multi Currency Symbol from config.yml
     */
    public String getCurrencySymbolMulti() {
        return getConfig().getString("Currency.Multi");
    }

    /**
     * MongoDB Methods
     */
    public boolean isMongoDB() {
        if (mongoDBUtils == null) return false;
        return mongoDBUtils.isMongoDb();
    }

    /**
     * LuckPerms Methods
     */
    public static boolean isLuckPermsInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
    }

    /**
     * Database Methods
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * MongoDB Methods
     */
    public MongoDBUtils getMongoDBUtils() {
        return mongoDBUtils;
    }

    /**
     * Vault Methods
     */
    public VaultManager getVaultManager() {
        return vaultManager;
    }

    /**
     * Checks for updates of the plugin on framedev.ch.
     * If download is true, it will download the latest version.
     *
     * @param download Whether to download the latest version or not.
     * @return true if an update is available, false otherwise.
     */
    public boolean checkUpdate(boolean download) {
        Bukkit.getConsoleSender().sendMessage(getPrefix() + "Checking for updates...");
        URLConnection conn;
        BufferedReader br = null;
        try {
            conn = new URL("https://framedev.ch/others/versions/essentialsmini-versions.json").openConnection();
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JsonElement jsonElement = JsonParser.parseReader(br);
            String latestVersion = jsonElement.getAsJsonObject().get("latest").getAsString();
            String oldVersion = Main.getInstance().getDescription().getVersion();
            if (!latestVersion.equalsIgnoreCase(oldVersion)) {
                UpdateChecker updateChecker = new UpdateChecker();
                if (!updateChecker.isOldVersionPreRelease()) {
                    if (download) {
                        downloadLatest();
                        Bukkit.getConsoleSender().sendMessage(getPrefix() + "Latest Version will be Downloaded : New Version : " + latestVersion);
                    } else {
                        Bukkit.getConsoleSender().sendMessage(getPrefix() + "A new update is available: version " + latestVersion);
                    }
                    return true;
                } else {
                    if (updateChecker.hasPreReleaseUpdate()) {
                        Bukkit.getConsoleSender().sendMessage(getPrefix() + "A new pre-release update is available: version " + updateChecker.getLatestPreRelease());
                        return true;
                    }
                }
            } else {
                Bukkit.getConsoleSender().sendMessage(getPrefix() + "You're running the newest plugin version!");
                return false;
            }
        } catch (IOException ex) {
            getLogger4J().log(Level.ERROR, "Error", ex);
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "Failed to check for updates on framedev.ch");
            // Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cPlease write an Email to framedev@framedev.stream with the Error");
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                getLogger4J().error(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Download the Latest Plugin from the Website <a href="https://framedev.ch">https://framedev.ch</a>
     */
    public void downloadLatest() {
        final File pluginFile = getDataFolder().getParentFile();
        final File updaterFile = new File(pluginFile, "update");
        if (!updaterFile.exists())
            if (!updaterFile.mkdir())
                getLogger4J().error("Could not create Update Directory : " + updaterFile.getAbsolutePath());
        try {
            URL url = new URL("https://framedev.ch/others/versions/essentialsmini-versions.json");
            JsonElement jsonElement = JsonParser.parseReader(new InputStreamReader(url.openConnection().getInputStream()));
            String latest = jsonElement.getAsJsonObject().get("latest").getAsString();
            new UpdateChecker().download("https://framedev.ch/downloads/EssentialsMini-" + latest + ".jar", getServer().getUpdateFolder(), "EssentialsMini.jar");
        } catch (IOException ex) {
            getLogger4J().error(ex.getMessage(), ex);
        }
    }

    /**
     * Checks if Economy is enabled in the config.
     *
     * @return true if Economy is enabled, false otherwise.
     */
    public boolean isEconomyEnabled() {
        return getConfig().getBoolean("Economy.Activate", false);
    }
}
