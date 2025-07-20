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

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings("DanglingJavadoc")
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

    // Vault variables
    private VaultManager vaultManager;

    // Variables for the plugin
    private Variables variables;

    @Override
    public void onEnable() {
        instance = this;

        // Loads the default configuration file
        saveDefaultConfig();
        saveConfig();

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
            if (Bukkit.getPluginManager().getPlugin("SpigotMongoDBUtils") == null) {
                Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cSpigotMongoDBUtils plugin not found!");
                Bukkit.getConsoleSender().sendMessage("Download the Plugin here: https://repository.framedev.ch:444/releases/ch/framedev/SpigotMongoDBUtils/1.0.1-SNAPSHOT/SpigotMongoDBUtils-1.0.1-20250125.124232-1.jar");
                return;
            } else {
                this.mongoDBUtils = new MongoDBUtils(this);
                if (isMongoDB()) {
                    for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        databaseManager.getBackendManager().createUser(player, "essentialsmini_data", new BackendManager.Callback<>() {
                            @Override
                            public void onResult(Void result) {
                            }

                            @Override
                            public void onError(Exception exception) {
                                logger.error("Could not create User: " + exception.getMessage(), exception);
                            }
                        });
                    }
                    Bukkit.getConsoleSender().sendMessage(getPrefix() + "§aMongoDB Enabled!");
                }
            }
        }

        // Enable economy if enabled in config
        if (getConfig().getBoolean("Economy.Activate")) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                this.vaultManager = new VaultManager(this);
            }
        }
        // Register Commands, TabCompleters and Listeners
        new RegisterManager(this);

        // Register KitManager and create custom config
        new KitManager().createCustomConfig();

        // Checking for Update and when enabled, Download the Latest Version automatically

        // Server not online, so the update check is disabled
        // if (!checkUpdate(getConfig().getBoolean("AutoDownload"))) {
        //    Bukkit.getConsoleSender().sendMessage(getPrefix() + "§aNo new updates found!");
        //} else {
        //    Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cPlease restart the server to apply the update!");
        //}*/

        // Restore Backpacks
        Arrays.stream(Bukkit.getOfflinePlayers()).forEach(BackpackCMD::restore);

        // Log that the plugin has been enabled
        getLogger4J().info("EssentialsMini has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save Backpacks
        Arrays.stream(Bukkit.getOfflinePlayers()).forEach(BackpackCMD::save);

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

    /**
     * Moves example messages from the resources to the messages-examples directory.
     * The method checks if the destination file already exists before copying.
     */
    public void moveExampleMessages() {
        String[] locales = {"de-DE", "en-EN", "fr-FR", "it-IT", "pt-PT", "pl-PL", "es-ES", "ru-RU"};

        File destinationDir = new File(getDataFolder(), "messages-examples");
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            getLogger4J().error("Failed to create destination directory: " + destinationDir.getPath());
            return;
        }

        for (String locale : locales) {
            File sourceFile = getFromResourceFile("locale-examples/" + "messages_" + locale + "-examples.yml", Main.class);

            File destinationFile = new File(destinationDir, "messages_" + locale + "-examples.yml");
            if (destinationFile.exists()) continue;

            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(destinationFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                getLogger4J().info("Successfully copied: " + sourceFile.getName());
            } catch (IOException e) {
                getLogger4J().error("Failed to copy example messages file: " + sourceFile.getName(), e);
            }
        }
    }

    /**
     * @param in InputStream
     * @return the File from the InputStream
     */
    protected File streamToFile(InputStream in) {
        if (in == null) {
            return null;
        }
        FileOutputStream out = null;
        try {
            // Create a Temp File
            File f = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
            f.deleteOnExit();

            out = new FileOutputStream(f);
            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            // Return the Temp File
            return f;
        } catch (IOException e) {
            logger.log(Level.ERROR, "Error while creating Temp File", e);
            return null;
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                logger.log(Level.ERROR, "Error while closing FileOutputStream", e);
            }
        }
    }

    public File getFromResourceFile(String file, Class<?> class_) {
        InputStream resource = class_.getClassLoader().getResourceAsStream(file);
        if (resource == null) {
            throw new IllegalArgumentException("File not found!");
        } else {
            return streamToFile(resource);
        }
    }

    /**
     * Checks if the messages configuration files exist, and if not, copies them from the example directory.
     * The method checks for multiple language configurations and copies them if they do not exist.
     */
    public void checkAndMoveMessagesConfigs() {
        List<String> exampleConfigFiles = Arrays.asList(
                "plugins/EssentialsMini/messages-examples/messages_de-DE-examples.yml",
                "plugins/EssentialsMini/messages-examples/messages_en-EN-examples.yml",
                "plugins/EssentialsMini/messages-examples/messages_fr-FR-examples.yml",
                "plugins/EssentialsMini/messages-examples/messages_it-IT-examples.yml",
                "plugins/EssentialsMini/messages-examples/messages_pt-PT-examples.yml",
                "plugins/EssentialsMini/messages-examples/messages_es-ES-examples.yml",
                "plugins/EssentialsMini/messages-examples/messages_ru-RU-examples.yml"
        );
        List<String> configFiles = Arrays.asList(
                "plugins/EssentialsMini/messages_de-DE.yml",
                "plugins/EssentialsMini/messages_en-EN.yml",
                "plugins/EssentialsMini/messages_fr-FR.yml",
                "plugins/EssentialsMini/messages_it-IT.yml",
                "plugins/EssentialsMini/messages_pt-PT.yml",
                "plugins/EssentialsMini/messages_es-ES.yml",
                "plugins/EssentialsMini/messages_ru-RU.yml"
        );

        boolean configsExist = configFiles.stream().allMatch(path -> new File(path).exists());

        if (!configsExist) {
            getLogger4J().info("No configuration files found. Downloading default configuration...");
            for(int i = 0; i < configFiles.size(); i++) {
                try {
                    Files.copy(new File(exampleConfigFiles.get(i)).toPath(),
                            new File(configFiles.get(i)).toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
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
        String locale = "en"; // Default locale
        File configFile;

        if (player == null) {
            configFile = new File(getDataFolder(), "messages_en-EN.yml");
            return YamlConfiguration.loadConfiguration(configFile);
        }

        if (player instanceof Player) {
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
        } else {
            getLogger4J().info("CommandSender is not a Player. Using default locale (en-EN).");
        }

        // Load the appropriate file
        configFile = new File(getDataFolder(), "messages_" + locale + ".yml");
        if (!configFile.exists()) {
            getLogger4J().warn("Language file for locale '" + locale + "' not found. Falling back to default (en-EN).");
            configFile = new File(getDataFolder(), "messages_en-EN.yml");
        }

        return YamlConfiguration.loadConfiguration(configFile);
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
        String locale; // Default locale
        File configFile;

        if (player == null) {
            configFile = new File(getDataFolder(), "messages_en-EN.yml");
            return YamlConfiguration.loadConfiguration(configFile);
        }

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

        // Load the appropriate file
        configFile = new File(getDataFolder(), "messages_" + locale + ".yml");
        if (!configFile.exists()) {
            getLogger4J().warn("Language file for locale '" + locale + "' not found. Falling back to default (en-EN).");
            configFile = new File(getDataFolder(), "messages_en-EN.yml");
        }

        return YamlConfiguration.loadConfiguration(configFile);
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

            Map<String, Language> languageMap = new HashMap<>() {{
                put("en", Language.EN);
                put("de", Language.DE);
                put("fr", Language.FR);
                put("it", Language.IT);
                put("es", Language.ES);
                put("pt", Language.PT);
                put("pl", Language.PL);
                put("ru", Language.RU);
            }};

            for (Map.Entry<String, Language> entry : languageMap.entrySet()) {
                if (playerLocale.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        // Default fallback
        return Language.EN;
    }

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

    public String getOnlyPlayer(Player player) {
        String onlyPlayer = getLanguageConfig(player).getString("OnlyPlayer");
        if (onlyPlayer == null) return "";
        onlyPlayer = onlyPlayer.replace('&', '§');
        return onlyPlayer;
    }

    public Logger getLogger4J() {
        return logger;
    }

    public String getCurrencySymbol() {
        return getConfig().getString("Currency.Single");
    }

    public String getCurrencySymbolMulti() {
        return getConfig().getString("Currency.Multi");
    }

    public boolean isMongoDB() {
        if (mongoDBUtils == null) return false;
        return mongoDBUtils.isMongoDb();
    }

    public static boolean isLuckPermsInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MongoDBUtils getMongoDBUtils() {
        return mongoDBUtils;
    }

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
                if (!oldVersion.contains("PRE-RELEASE") || !oldVersion.contains("1.20.6-HIGHER-RELEASE")) {
                    if (download) {
                        downloadLatest();
                        Bukkit.getConsoleSender().sendMessage(getPrefix() + "Latest Version will be Downloaded : New Version : " + latestVersion);
                    } else {
                        Bukkit.getConsoleSender().sendMessage(getPrefix() + "A new update is available: version " + latestVersion);
                    }
                    return true;
                } else {
                    if (new UpdateChecker().hasPreReleaseUpdate()) {
                        Bukkit.getConsoleSender().sendMessage(getPrefix() + "A new pre-release update is available: version " + new UpdateChecker().getLatestPreRelease());
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
