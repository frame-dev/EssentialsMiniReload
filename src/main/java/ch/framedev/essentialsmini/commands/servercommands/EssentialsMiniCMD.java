package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EssentialsMiniCMD extends CommandBase {

    private static final String PERMISSION_BASE = "essentialsmini.";
    private static final String PREFIX = "§aEssentialsMini §8» §7";

    public EssentialsMiniCMD(Main plugin) {
        super(plugin, "essentialsmini");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Null check for args (should never be null, but defensive programming)
        if (args == null) {
            args = new String[0];
        }

        if (args.length == 0) {
            sendMainHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "economy" -> handleEconomy(sender, args);
            case "messages" -> handleMessages(sender, args);
            case "help" -> handleHelp(sender);
            case "version", "ver" -> handleVersion(sender);
            case "about", "info" -> handleAbout(sender);
            default -> {
                sender.sendMessage(PREFIX + "§cUnknown command. Use §e/essentialsmini help §cfor a list of commands.");
                yield true;
            }
        };
    }

    /**
     * Send main help message
     */
    private void sendMainHelp(@NotNull CommandSender sender) {
        try {
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§aEssentialsMini §7by §eFrameDev");
            sender.sendMessage("§7Version: §a" + getPlugin().getDescription().getVersion());
            sender.sendMessage("§7Website: §b" + getPlugin().getDescription().getWebsite());
            sender.sendMessage("");
            sender.sendMessage("§7Commands:");

            if (sender.hasPermission(PERMISSION_BASE + "reload")) {
                sender.sendMessage("  §a/essentialsmini reload §8- §7Reload configuration");
            }
            if (sender.hasPermission(PERMISSION_BASE + "help")) {
                sender.sendMessage("  §a/essentialsmini help §8- §7Show detailed help");
            }
            if (sender.hasPermission(PERMISSION_BASE + "economy")) {
                sender.sendMessage("  §a/essentialsmini economy <on|off> §8- §7Toggle economy");
            }
            if (sender.hasPermission(PERMISSION_BASE + "messages")) {
                sender.sendMessage("  §a/essentialsmini messages <list|add|remove|create> §8- §7Manage custom message files");
            }
            if (sender.hasPermission(PERMISSION_BASE + "version")) {
                sender.sendMessage("  §a/essentialsmini version §8- §7Show version info");
            }

            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        } catch (Exception e) {
            // Fallback if getDescription() fails
            sender.sendMessage(PREFIX + "§aEssentialsMini by FrameDev");
            sender.sendMessage(PREFIX + "§7Use /essentialsmini help for commands");
        }
    }

    /**
     * Handle reload command
     */
    private boolean handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_BASE + "reload")) {
            sender.sendMessage(PREFIX + "§cYou do not have permission to use this command.");
            return true;
        }

        try {
            long startTime = System.currentTimeMillis();
            getPlugin().reloadConfig();
            getPlugin().reloadLanguageConfigs();
            long duration = System.currentTimeMillis() - startTime;

            sender.sendMessage(PREFIX + "§aConfiguration reloaded successfully! §7(" + duration + "ms)");
            sender.sendMessage(PREFIX + "§7Some changes may require a server restart to take effect.");
        } catch (Exception e) {
            sender.sendMessage(PREFIX + "§cError reloading configuration: " + e.getMessage());
            getPlugin().getLogger().severe("Error reloading config: " + e.getMessage());
        }

        return true;
    }

    /**
     * Handle economy command
     */
    private boolean handleEconomy(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_BASE + "economy")) {
            sender.sendMessage(PREFIX + "§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(PREFIX + "§cUsage: /essentialsmini economy <on|off|true|false>");
            return true;
        }

        String value = args[1].toLowerCase();
        boolean activate;

        switch (value) {
            case "true":
            case "on":
            case "enable":
            case "enabled":
                activate = true;
                break;
            case "false":
            case "off":
            case "disable":
            case "disabled":
                activate = false;
                break;
            default:
                sender.sendMessage(PREFIX + "§cInvalid value. Use: on, off, true, or false");
                return true;
        }

        try {
            getPlugin().getConfig().set("Economy.Activate", activate);
            getPlugin().saveConfig();

            sender.sendMessage(PREFIX + "§aEconomy feature has been " +
                (activate ? "§aenabled§7" : "§cdisabled§7") + ".");

            if (activate) {
                sender.sendMessage(PREFIX + "§7Please ensure you have Vault and an economy plugin installed.");
                sender.sendMessage(PREFIX + "§7A restart may be required for changes to take effect.");
            } else {
                sender.sendMessage(PREFIX + "§7Economy features are now disabled.");
            }
        } catch (Exception e) {
            sender.sendMessage(PREFIX + "§cError saving configuration: " + e.getMessage());
            getPlugin().getLogger().severe("Error saving config: " + e.getMessage());
        }

        return true;
    }

    private boolean handleMessages(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_BASE + "messages")) {
            sender.sendMessage(PREFIX + "§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sendMessagesHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        return switch (action) {
            case "list" -> handleMessagesList(sender);
            case "add" -> handleMessagesAdd(sender, args);
            case "remove" -> handleMessagesRemove(sender, args);
            case "create" -> handleMessagesCreate(sender, args);
            default -> {
                sendMessagesHelp(sender);
                yield true;
            }
        };
    }

    private void sendMessagesHelp(@NotNull CommandSender sender) {
        sender.sendMessage(PREFIX + "§7Usage:");
        sender.sendMessage(PREFIX + "§a/essentialsmini messages list §8- §7Show loaded and detected message files");
        sender.sendMessage(PREFIX + "§a/essentialsmini messages add <locale> §8- §7Enable an existing custom message file");
        sender.sendMessage(PREFIX + "§a/essentialsmini messages remove <locale> §8- §7Disable a custom message locale");
        sender.sendMessage(PREFIX + "§a/essentialsmini messages create <locale> §8- §7Create a custom file from English and enable it");
    }

    private boolean handleMessagesList(@NotNull CommandSender sender) {
        sender.sendMessage(PREFIX + "§7Loaded message locales: §a" + formatList(getPlugin().getLoadedMessageLocales()));
        sender.sendMessage(PREFIX + "§7Enabled custom locales: §a" + formatList(getPlugin().getConfiguredCustomMessageLocales()));

        List<String> detectedLocales = getPlugin().getUnconfiguredCustomMessageLocales();
        if (detectedLocales.isEmpty()) {
            sender.sendMessage(PREFIX + "§7Unconfigured custom files: §aNone");
        } else {
            sender.sendMessage(PREFIX + "§7Unconfigured custom files: §e" + formatList(detectedLocales));
            sender.sendMessage(PREFIX + "§7Use §e/essentialsmini messages add <locale> §7to add one to the project.");
        }
        return true;
    }

    private boolean handleMessagesAdd(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cUsage: /essentialsmini messages add <locale>");
            return true;
        }

        String locale = args[2];
        if (getPlugin().isBuiltInMessageLocale(locale)) {
            sender.sendMessage(PREFIX + "§eThat locale is already built in.");
            return true;
        }

        if (!getPlugin().getMessageFile(locale).exists()) {
            sender.sendMessage(PREFIX + "§cNo custom message file exists for that locale.");
            sender.sendMessage(PREFIX + "§7Create §emessages_" + locale + ".yml §7or run §e/essentialsmini messages create " + locale + "§7.");
            return true;
        }

        if (getPlugin().addCustomMessageLocale(locale)) {
            sender.sendMessage(PREFIX + "§aCustom message locale added and reloaded.");
        } else {
            sender.sendMessage(PREFIX + "§cCould not add that custom message locale.");
        }
        return true;
    }

    private boolean handleMessagesRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cUsage: /essentialsmini messages remove <locale>");
            return true;
        }

        if (getPlugin().removeCustomMessageLocale(args[2])) {
            sender.sendMessage(PREFIX + "§aCustom message locale removed and languages reloaded.");
        } else {
            sender.sendMessage(PREFIX + "§eThat locale is not enabled as a custom message locale.");
        }
        return true;
    }

    private boolean handleMessagesCreate(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cUsage: /essentialsmini messages create <locale>");
            return true;
        }

        String locale = args[2];
        if (getPlugin().isBuiltInMessageLocale(locale)) {
            sender.sendMessage(PREFIX + "§eThat locale is already built in.");
            return true;
        }

        if (!getPlugin().createCustomMessageFile(locale)) {
            sender.sendMessage(PREFIX + "§cCould not create the custom message file.");
            return true;
        }

        if (getPlugin().addCustomMessageLocale(locale)) {
            sender.sendMessage(PREFIX + "§aCustom message file created, added, and reloaded.");
        } else {
            sender.sendMessage(PREFIX + "§cThe file was created, but the locale could not be added.");
        }
        return true;
    }

    private String formatList(@NotNull List<String> values) {
        if (values.isEmpty()) {
            return "None";
        }
        return String.join(", ", values);
    }

    /**
     * Handle help command
     */
    private boolean handleHelp(@NotNull CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_BASE + "help")) {
            sender.sendMessage(PREFIX + "§cYou do not have permission to use this command.");
            return true;
        }

        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§aEssentialsMini §7Help");
        sender.sendMessage("");

        if (sender.hasPermission(PERMISSION_BASE + "reload")) {
            sender.sendMessage("§a/essentialsmini reload");
            sender.sendMessage("  §7Reloads the configuration from disk");
            sender.sendMessage("");
        }

        if (sender.hasPermission(PERMISSION_BASE + "economy")) {
            sender.sendMessage("§a/essentialsmini economy <on|off>");
            sender.sendMessage("  §7Enables or disables the economy feature");
            sender.sendMessage("  §7Requires Vault and an economy plugin");
            sender.sendMessage("");
        }

        if (sender.hasPermission(PERMISSION_BASE + "messages")) {
            sender.sendMessage("§a/essentialsmini messages <list|add|remove|create>");
            sender.sendMessage("  §7Manages custom messages_<locale>.yml files");
            sender.sendMessage("");
        }

        if (sender.hasPermission(PERMISSION_BASE + "version")) {
            sender.sendMessage("§a/essentialsmini version");
            sender.sendMessage("  §7Shows plugin version and information");
            sender.sendMessage("");
        }

        sender.sendMessage("§7For more help, visit:");
        sender.sendMessage("§bhttps://github.com/FrameDev/EssentialsMini");
        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return true;
    }

    /**
     * Handle version command
     */
    private boolean handleVersion(@NotNull CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_BASE + "version")) {
            sender.sendMessage(PREFIX + "§cYou do not have permission to use this command.");
            return true;
        }

        try {
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§aEssentialsMini §7Version Information");
            sender.sendMessage("");
            sender.sendMessage("§7Version: §a" + getPlugin().getDescription().getVersion());
            sender.sendMessage("§7Author: §e" + String.join(", ", getPlugin().getDescription().getAuthors()));
            sender.sendMessage("§7Description: §f" + getPlugin().getDescription().getDescription());

            // Database info
            if (getPlugin().isMysql()) {
                sender.sendMessage("§7Database: §aMySQL");
            } else if (getPlugin().isSQL()) {
                sender.sendMessage("§7Database: §aSQLite");
            } else if (getPlugin().isMongoDB()) {
                sender.sendMessage("§7Database: §aMongoDB");
            } else {
                sender.sendMessage("§7Database: §eFile-based");
            }

            // Economy status
            boolean economyActive = getPlugin().getConfig().getBoolean("Economy.Activate", false);
            sender.sendMessage("§7Economy: " + (economyActive ? "§aEnabled" : "§cDisabled"));

            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        } catch (Exception e) {
            sender.sendMessage(PREFIX + "§cError retrieving version information.");
        }

        return true;
    }

    /**
     * Handle about command
     */
    private boolean handleAbout(@NotNull CommandSender sender) {
        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§aEssentialsMini");
        sender.sendMessage("");
        sender.sendMessage("§7A comprehensive essentials plugin for Bukkit/Spigot");
        sender.sendMessage("§7providing essential commands and features for");
        sender.sendMessage("§7your Minecraft server.");
        sender.sendMessage("");
        sender.sendMessage("§7Created by: §eFrameDev");
        sender.sendMessage("§7GitHub: §bhttps://github.com/FrameDev/EssentialsMini");
        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        // Null check
        if (args == null || args.length == 0) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("reload", "help", "economy", "messages", "version", "about", "info");

            // Filter by permission and starts with
            completions = subCommands.stream()
                    .filter(sub -> sender.hasPermission(PERMISSION_BASE + sub) || sub.equals("help") || sub.equals("about") || sub.equals("info"))
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());

        } else if (args.length == 2 && args[0].equalsIgnoreCase("economy")) {
            // Second argument for economy command
            if (sender.hasPermission(PERMISSION_BASE + "economy")) {
                List<String> values = Arrays.asList("on", "off", "true", "false", "enable", "disable");
                completions = values.stream()
                        .filter(val -> val.startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("messages")) {
            if (sender.hasPermission(PERMISSION_BASE + "messages")) {
                List<String> values = Arrays.asList("list", "add", "remove", "create");
                completions = values.stream()
                        .filter(val -> val.startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("messages")) {
            if (sender.hasPermission(PERMISSION_BASE + "messages")) {
                List<String> locales = args[1].equalsIgnoreCase("add")
                        ? getPlugin().getUnconfiguredCustomMessageLocales()
                        : getPlugin().getConfiguredCustomMessageLocales();
                completions = locales.stream()
                        .filter(locale -> locale.toLowerCase().startsWith(args[2].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
