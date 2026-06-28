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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final Set<String> loadedLanguageLocales = new LinkedHashSet<>();

    private static final List<String> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            "de-DE", "en-EN", "fr-FR", "it-IT", "pt-PT", "pl-PL", "es-ES", "ru-RU"
    ));
    private static final Pattern MESSAGE_FILE_PATTERN = Pattern.compile("^messages_([A-Za-z][A-Za-z0-9_-]*)\\.yml$");

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

        // Initialize the logger
        logger = Logger.getLogger("EssentialsMini");
        BasicConfigurator.configure();

        // Loads the default configuration file and merges new defaults/comments into existing configs
        saveDefaultConfig();
        reloadConfig();
        debug = getConfig().getBoolean("debug", false);

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

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        mergeBundledConfigDefaults();
        debug = getConfig().getBoolean("debug", false);
    }

    private void mergeBundledConfigDefaults() {
        YamlConfiguration bundledConfig = loadBundledConfig();
        if (bundledConfig == null) {
            return;
        }

        FileConfiguration config = getConfig();
        config.options().parseComments(true);
        config.options().copyDefaults(true);
        config.options().copyHeader(true);
        config.setDefaults(bundledConfig);

        boolean changed = false;
        for (String path : bundledConfig.getKeys(true)) {
            if (!config.contains(path, true)) {
                Object value = bundledConfig.get(path);
                if (!(value instanceof ConfigurationSection)) {
                    config.set(path, value);
                    changed = true;
                }
            }

            if (copyConfigComments(bundledConfig, config, path)) {
                changed = true;
            }
        }

        if (!bundledConfig.options().getHeader().equals(config.options().getHeader())) {
            config.options().setHeader(bundledConfig.options().getHeader());
            changed = true;
        }

        if (!bundledConfig.options().getFooter().equals(config.options().getFooter())) {
            config.options().setFooter(bundledConfig.options().getFooter());
            changed = true;
        }

        if (changed) {
            saveConfig();
        }
    }

    private YamlConfiguration loadBundledConfig() {
        try (InputStream inputStream = getResource("config.yml")) {
            if (inputStream == null) {
                getLogger().warning("Bundled config.yml was not found; default config values could not be merged.");
                return null;
            }

            YamlConfiguration bundledConfig = new YamlConfiguration();
            bundledConfig.options().parseComments(true);
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                bundledConfig.load(reader);
            }
            return bundledConfig;
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("Bundled config.yml could not be loaded: " + e.getMessage());
            return null;
        }
    }

    private boolean copyConfigComments(YamlConfiguration source, FileConfiguration target, String path) {
        boolean changed = false;

        List<String> comments = source.getComments(path);
        if (!comments.equals(target.getComments(path))) {
            target.setComments(path, comments);
            changed = true;
        }

        List<String> inlineComments = source.getInlineComments(path);
        if (!inlineComments.equals(target.getInlineComments(path))) {
            target.setInlineComments(path, inlineComments);
            changed = true;
        }

        return changed;
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
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> checkUpdate(autoDownload));
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
        if (listeners != null) listeners.clear();
        if (commands != null) commands.clear();
        if (tabCompleters != null) tabCompleters.clear();
        vaultManager = null;
        mongoDBUtils = null;
        variables = null;
        if (silent != null) silent.clear();
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
        return getFeaturePrefix(detectFeatureKeyFromCaller());
    }

    public String getFeaturePrefix(String feature) {
        String globalPrefix = getFormattedGlobalPrefix();
        if (!getConfig().getBoolean("customization.prefixes.enabled", false)) {
            return globalPrefix;
        }

        String featureKey = feature == null || feature.isBlank() ? "default" : feature;
        String path = "customization.prefixes.features." + featureKey;
        if (!getConfig().getBoolean(path + ".enabled", true)) {
            return globalPrefix;
        }

        return formatText(getConfig().getString(path + ".value", globalPrefix));
    }

    private String getFormattedGlobalPrefix() {
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

    public String formatText(String text) {
        if (text == null) {
            return "";
        }

        String formatted = text.replace('&', '§');
        formatted = formatted.replace(">>", "»");
        formatted = formatted.replace("<<", "«");
        formatted = formatted.replace("->", "→");
        formatted = formatted.replace("<-", "←");
        return formatted;
    }

    public String applyPlaceholders(String text, Map<String, String> placeholders) {
        String formatted = formatText(text);
        if (placeholders == null) {
            return formatted;
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return formatted;
    }

    public void sendFeatureMessage(CommandSender sender, String feature, String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }

        sender.sendMessage(getFeaturePrefix(feature) + formatText(message));
    }

    public void sendConfiguredNotification(Player player, String notificationKey, String feature, String defaultMessage, Map<String, String> placeholders) {
        if (player == null) {
            return;
        }

        String path = "customization.notifications." + notificationKey;
        if (!getConfig().getBoolean(path + ".enabled", true)) {
            return;
        }

        if (getConfig().getBoolean(path + ".chat.enabled", true)) {
            String chatMessage = getConfig().getString(path + ".chat.message", defaultMessage);
            if (chatMessage != null && !chatMessage.isBlank()) {
                boolean usePrefix = getConfig().getBoolean(path + ".chat.useFeaturePrefix", true);
                String message = applyPlaceholders(chatMessage, placeholders);
                player.sendMessage((usePrefix ? getFeaturePrefix(feature) : "") + message);
            }
        }

        if (getConfig().getBoolean(path + ".actionbar.enabled", false)) {
            String actionbarMessage = applyPlaceholders(getConfig().getString(path + ".actionbar.message", defaultMessage), placeholders);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionbarMessage));
        }

        if (getConfig().getBoolean(path + ".title.enabled", false)) {
            String title = applyPlaceholders(getConfig().getString(path + ".title.title", ""), placeholders);
            String subtitle = applyPlaceholders(getConfig().getString(path + ".title.subtitle", defaultMessage), placeholders);
            player.sendTitle(
                    title,
                    subtitle,
                    getConfig().getInt(path + ".title.fadeIn", 10),
                    getConfig().getInt(path + ".title.stay", 40),
                    getConfig().getInt(path + ".title.fadeOut", 10)
            );
        }

        if (getConfig().getBoolean(path + ".sound.enabled", false)) {
            String soundName = getConfig().getString(path + ".sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
            try {
                Sound sound = Sound.valueOf(soundName == null ? "ENTITY_EXPERIENCE_ORB_PICKUP" : soundName.toUpperCase(Locale.ROOT));
                player.playSound(
                        player.getLocation(),
                        sound,
                        (float) getConfig().getDouble(path + ".sound.volume", 1.0D),
                        (float) getConfig().getDouble(path + ".sound.pitch", 1.0D)
                );
            } catch (IllegalArgumentException ex) {
                getLogger4J().warn("Invalid notification sound '" + soundName + "' for " + notificationKey);
            }
        }
    }

    public String getConfiguredGuiTitle(String guiKey, String defaultTitle, Map<String, String> placeholders) {
        String title = getConfig().getString("customization.guis." + guiKey + ".title", defaultTitle);
        return applyPlaceholders(title, placeholders);
    }

    public ItemStack getConfiguredGuiItem(String path, Material defaultMaterial, String defaultName, List<String> defaultLore, Map<String, String> placeholders) {
        String basePath = "customization.guis." + path;
        Material material = matchMaterial(getConfig().getString(basePath + ".material"), defaultMaterial);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(applyPlaceholders(getConfig().getString(basePath + ".name", defaultName), placeholders));

        List<String> configuredLore = getConfig().getStringList(basePath + ".lore");
        List<String> lore = configuredLore.isEmpty() ? defaultLore : configuredLore;
        List<String> formattedLore = new ArrayList<>();
        for (String line : lore) {
            formattedLore.add(applyPlaceholders(line, placeholders));
        }
        meta.setLore(formattedLore);
        item.setItemMeta(meta);
        return item;
    }

    private Material matchMaterial(String materialName, Material defaultMaterial) {
        if (materialName == null || materialName.isBlank()) {
            return defaultMaterial;
        }

        Material material = Material.matchMaterial(materialName);
        return material == null ? defaultMaterial : material;
    }

    private String detectFeatureKeyFromCaller() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (className.equals(Main.class.getName())
                    || className.startsWith("java.")
                    || className.startsWith("org.bukkit.")) {
                continue;
            }

            if (className.contains("Eco") || className.contains("Bank") || className.contains("Money") || className.contains("Vault")) return "economy";
            if (className.contains("Ban") || className.contains("Mute") || className.contains("Maintenance") || className.contains("ClearChat")) return "moderation";
            if (className.contains("Mail")) return "mail";
            if (className.contains("Teleport") || className.contains("Warp") || className.contains("Home") || className.contains("Spawn") || className.contains("BackCMD") || className.contains("TopCMD")) return "teleport";
            if (className.contains("Help")) return "help";
            if (className.contains("Kit")) return "kits";
            if (className.contains("Trash")) return "trash";
            if (className.contains("Backpack")) return "backpack";
            if (className.contains("UtilityStation")) return "utilityStations";
        }
        return "default";
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
        if (player instanceof Player) {
            return resolveLanguageConfigLocale(((Player) player).getLocale());
        }

        return "en-EN";
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
        loadedLanguageLocales.clear();

        for (String locale : getActiveMessageLocales()) {
            File configFile = new File(getDataFolder(), "messages_" + locale + ".yml");
            if (configFile.exists()) {
                languageConfigs.put(locale, YamlConfiguration.loadConfiguration(configFile));
                loadedLanguageLocales.add(locale);
            } else if (!SUPPORTED_LOCALES.contains(locale)) {
                getLogger4J().warn("Configured custom message locale '" + locale + "' was not found at " + configFile.getPath());
            }
        }

        languageConfigs.computeIfAbsent("en-EN", locale ->
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages_en-EN.yml")));
        loadedLanguageLocales.add("en-EN");

        promptForUnconfiguredCustomMessageFiles();
    }

    private FileConfiguration getCachedLanguageConfig(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        FileConfiguration config = normalizedLocale == null ? null : languageConfigs.get(normalizedLocale);
        if (config != null) {
            return config;
        }

        String prefixMatch = findLoadedLocaleByPrefix(normalizedLocale == null ? locale : normalizedLocale);
        if (prefixMatch != null) {
            return languageConfigs.get(prefixMatch);
        }

        getLogger4J().warn("Language file for locale '" + locale + "' not found. Falling back to default (en-EN).");
        return languageConfigs.get("en-EN");
    }

    private Set<String> getActiveMessageLocales() {
        Set<String> locales = new LinkedHashSet<>(SUPPORTED_LOCALES);
        for (String locale : getConfiguredCustomMessageLocales()) {
            locales.add(locale);
        }
        return locales;
    }

    public List<String> getConfiguredCustomMessageLocales() {
        List<String> locales = new ArrayList<>();
        for (String locale : getConfig().getStringList("messages.customLocales")) {
            String normalizedLocale = normalizeLocale(locale);
            if (normalizedLocale != null && !SUPPORTED_LOCALES.contains(normalizedLocale) && !locales.contains(normalizedLocale)) {
                locales.add(normalizedLocale);
            }
        }
        return locales;
    }

    public List<String> getLoadedMessageLocales() {
        return new ArrayList<>(loadedLanguageLocales);
    }

    public List<String> getDetectedCustomMessageLocales() {
        List<String> locales = new ArrayList<>();
        File[] files = getDataFolder().listFiles();
        if (files == null) {
            return locales;
        }

        for (File file : files) {
            String locale = getMessageLocaleFromFileName(file.getName());
            if (locale != null && !SUPPORTED_LOCALES.contains(locale) && !locales.contains(locale)) {
                locales.add(locale);
            }
        }
        Collections.sort(locales);
        return locales;
    }

    public List<String> getUnconfiguredCustomMessageLocales() {
        List<String> unconfiguredLocales = new ArrayList<>(getDetectedCustomMessageLocales());
        unconfiguredLocales.removeAll(getConfiguredCustomMessageLocales());
        return unconfiguredLocales;
    }

    public boolean addCustomMessageLocale(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        if (normalizedLocale == null || SUPPORTED_LOCALES.contains(normalizedLocale)) {
            return false;
        }

        List<String> locales = getConfiguredCustomMessageLocales();
        if (!locales.contains(normalizedLocale)) {
            locales.add(normalizedLocale);
            Collections.sort(locales);
            getConfig().set("messages.customLocales", locales);
            saveConfig();
        }
        reloadLanguageConfigs();
        return true;
    }

    public boolean removeCustomMessageLocale(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        if (normalizedLocale == null) {
            return false;
        }

        List<String> locales = getConfiguredCustomMessageLocales();
        boolean removed = locales.remove(normalizedLocale);
        if (removed) {
            getConfig().set("messages.customLocales", locales);
            saveConfig();
            reloadLanguageConfigs();
        }
        return removed;
    }

    public boolean createCustomMessageFile(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        if (normalizedLocale == null || SUPPORTED_LOCALES.contains(normalizedLocale)) {
            return false;
        }

        File targetFile = getMessageFile(normalizedLocale);
        if (targetFile.exists()) {
            return true;
        }

        File sourceFile = getMessageFile("en-EN");
        if (!sourceFile.exists()) {
            getLogger4J().warn("Default English message file was not found: " + sourceFile.getPath());
            return false;
        }

        try {
            Files.copy(sourceFile.toPath(), targetFile.toPath());
            return true;
        } catch (IOException e) {
            getLogger4J().error("Failed to create custom message file: " + targetFile.getPath(), e);
            return false;
        }
    }

    public File getMessageFile(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        return new File(getDataFolder(), "messages_" + (normalizedLocale == null ? locale : normalizedLocale) + ".yml");
    }

    public boolean isBuiltInMessageLocale(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        return normalizedLocale != null && SUPPORTED_LOCALES.contains(normalizedLocale);
    }

    private void promptForUnconfiguredCustomMessageFiles() {
        if (!getConfig().getBoolean("messages.promptForCustomLocales", true)) {
            return;
        }

        List<String> locales = getUnconfiguredCustomMessageLocales();
        if (locales.isEmpty()) {
            return;
        }

        getLogger4J().info("Custom message file(s) detected but not enabled: " + String.join(", ", locales));
        getLogger4J().info("Run /essentialsmini messages add <locale> or add them to messages.customLocales in config.yml to use them.");
    }

    @NotNull
    private String resolveLanguageConfigLocale(String playerLocale) {
        String normalizedLocale = normalizeLocale(playerLocale);
        if (normalizedLocale != null && languageConfigs.containsKey(normalizedLocale)) {
            return normalizedLocale;
        }

        String prefixMatch = findLoadedLocaleByPrefix(normalizedLocale == null ? playerLocale : normalizedLocale);
        if (prefixMatch != null) {
            return prefixMatch;
        }

        return "en-EN";
    }

    private String findLoadedLocaleByPrefix(String locale) {
        String prefix = getLocalePrefix(locale);
        if (prefix == null) {
            return null;
        }

        for (String loadedLocale : loadedLanguageLocales) {
            String loadedPrefix = getLocalePrefix(loadedLocale);
            if (prefix.equals(loadedPrefix)) {
                return loadedLocale;
            }
        }
        return null;
    }

    private String getMessageLocaleFromFileName(String fileName) {
        Matcher matcher = MESSAGE_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }
        return normalizeLocale(matcher.group(1));
    }

    private String getLocalePrefix(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        if (normalizedLocale == null) {
            return null;
        }

        int separatorIndex = normalizedLocale.indexOf('-');
        if (separatorIndex <= 0) {
            return normalizedLocale.toLowerCase(Locale.ROOT);
        }

        return normalizedLocale.substring(0, separatorIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeLocale(String locale) {
        if (locale == null) {
            return null;
        }

        String cleanedLocale = locale.trim();
        if (cleanedLocale.isEmpty()) {
            return null;
        }

        if (cleanedLocale.startsWith("messages_")) {
            cleanedLocale = cleanedLocale.substring("messages_".length());
        }
        if (cleanedLocale.endsWith(".yml")) {
            cleanedLocale = cleanedLocale.substring(0, cleanedLocale.length() - ".yml".length());
        }

        cleanedLocale = cleanedLocale.replace('_', '-');
        String[] parts = cleanedLocale.split("-", 2);
        String language = parts[0].toLowerCase(Locale.ROOT);
        if (!language.matches("[a-z][a-z0-9]*")) {
            return null;
        }

        if (parts.length == 1 || parts[1].isBlank()) {
            return language;
        }

        String country = parts[1].toUpperCase(Locale.ROOT);
        if (!country.matches("[A-Z0-9-]+")) {
            return null;
        }
        return language + "-" + country;
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

        UpdateChecker updateChecker = new UpdateChecker();
        UpdateChecker.UpdateStatus updateStatus = updateChecker.checkForUpdate();
        if (!updateStatus.metadataAvailable()) {
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cFailed to check for updates on framedev.ch");
            return false;
        }

        if (!updateStatus.updateAvailable()) {
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "§aYou're running the newest plugin version!");
            return false;
        }

        Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cA new update is available: version " + updateStatus.latestVersion());
        if (!download) {
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "§7Current version: " + updateStatus.currentVersion());
            return true;
        }

        if (downloadLatest(updateStatus.latestVersion())) {
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "§aLatest version downloaded: " + updateStatus.latestVersion());
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cPlease restart the server to apply the update!");
        } else {
            Bukkit.getConsoleSender().sendMessage(getPrefix() + "§cThe update was found, but the download failed.");
        }
        return true;
    }

    /**
     * Download the Latest Plugin from the Website <a href="https://framedev.ch">https://framedev.ch</a>
     */
    public void downloadLatest() {
        downloadLatest(new UpdateChecker().getLatestVersion());
    }

    public boolean downloadLatest(String latest) {
        final File updaterFile = getServer().getUpdateFolderFile();
        if (!updaterFile.exists())
            if (!updaterFile.mkdir())
                getLogger4J().error("Could not create Update Directory : " + updaterFile.getAbsolutePath());
        if (!updaterFile.exists()) {
            return false;
        }
        return new UpdateChecker().downloadVersion(latest, updaterFile.getAbsolutePath(), "EssentialsMini.jar");
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
