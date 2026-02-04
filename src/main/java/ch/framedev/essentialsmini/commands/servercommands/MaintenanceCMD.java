package ch.framedev.essentialsmini.commands.servercommands;

/*
 * ch.framedev.essentialsmini.commands.servercommands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 28.01.2025 19:31
 */

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.LuckPermsManager;
import ch.framedev.essentialsmini.utils.GeyserManager;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MaintenanceCMD extends CommandListenerBase {

    private static final String PERMISSION_BASE = "essentialsmini.maintenance";
    private static final String PERMISSION_BYPASS = PERMISSION_BASE + ".bypass";

    public MaintenanceCMD(Main plugin) {
        super(plugin, "maintenance");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Null check for args
        if (args == null) {
            args = new String[0];
        }

        // Permission check
        if (!sender.hasPermission(PERMISSION_BASE)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }

        // No args - toggle maintenance
        if (args.length == 0) {
            return handleToggle(sender);
        }

        // Handle subcommands
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "on":
            case "enable":
                return handleEnable(sender);
            case "off":
            case "disable":
                return handleDisable(sender);
            case "add":
                return handleAdd(sender, args);
            case "remove":
            case "rem":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender);
            case "status":
            case "info":
                return handleStatus(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    /**
     * Send usage message
     */
    private void sendUsage(@NotNull CommandSender sender) {
        sender.sendMessage(getPlugin().getPrefix() + "§7Usage:");
        sender.sendMessage("  §e/maintenance §8- §7Toggle maintenance mode");
        sender.sendMessage("  §e/maintenance on/off §8- §7Enable/disable maintenance");
        sender.sendMessage("  §e/maintenance add <player> §8- §7Add player to whitelist");
        sender.sendMessage("  §e/maintenance remove <player> §8- §7Remove player from whitelist");
        sender.sendMessage("  §e/maintenance list §8- §7Show whitelisted players");
        sender.sendMessage("  §e/maintenance status §8- §7Check maintenance status");
    }

    /**
     * Toggle maintenance mode
     */
    private boolean handleToggle(@NotNull CommandSender sender) {
        try {
            boolean enabled = getPlugin().getConfig().getBoolean("maintenance.enabled", false);
            boolean newState = !enabled;

            getPlugin().getConfig().set("maintenance.enabled", newState);
            getPlugin().saveConfig();

            sender.sendMessage(getPlugin().getPrefix() +
                (newState ? "§cMaintenance mode has been enabled!" : "§aMaintenance mode has been disabled!"));

            // Update tab list for online players
            updateTabListForAll(newState);

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError toggling maintenance: " + e.getMessage());
            getPlugin().getLogger().severe("Error toggling maintenance: " + e.getMessage());
            return true;
        }
    }

    /**
     * Enable maintenance mode
     */
    private boolean handleEnable(@NotNull CommandSender sender) {
        try {
            if (getPlugin().getConfig().getBoolean("maintenance.enabled", false)) {
                sender.sendMessage(getPlugin().getPrefix() + "§eMaintenance mode is already enabled.");
                return true;
            }

            getPlugin().getConfig().set("maintenance.enabled", true);
            getPlugin().saveConfig();

            sender.sendMessage(getPlugin().getPrefix() + "§cMaintenance mode has been enabled!");
            updateTabListForAll(true);

            // Kick non-whitelisted players
            kickNonWhitelistedPlayers();

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError enabling maintenance: " + e.getMessage());
            getPlugin().getLogger().severe("Error enabling maintenance: " + e.getMessage());
            return true;
        }
    }

    /**
     * Disable maintenance mode
     */
    private boolean handleDisable(@NotNull CommandSender sender) {
        try {
            if (!getPlugin().getConfig().getBoolean("maintenance.enabled", false)) {
                sender.sendMessage(getPlugin().getPrefix() + "§eMaintenance mode is already disabled.");
                return true;
            }

            getPlugin().getConfig().set("maintenance.enabled", false);
            getPlugin().saveConfig();

            sender.sendMessage(getPlugin().getPrefix() + "§aMaintenance mode has been disabled!");
            updateTabListForAll(false);

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError disabling maintenance: " + e.getMessage());
            getPlugin().getLogger().severe("Error disabling maintenance: " + e.getMessage());
            return true;
        }
    }

    /**
     * Add player to whitelist
     */
    private boolean handleAdd(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getPlugin().getPrefix() + "§cPlease specify a player name.");
            sender.sendMessage(getPlugin().getPrefix() + "§7Usage: §e/maintenance add <player>");
            return true;
        }

        try {
            OfflinePlayer offlinePlayer = PlayerUtils.getOfflinePlayerByName(args[1]);
            if (offlinePlayer == null) {
                sender.sendMessage(getPlugin().getPrefix() + "§cPlayer not found: " + args[1]);
                return true;
            }

            List<String> uuids = new ArrayList<>(getPlugin().getConfig().getStringList("maintenance.players"));
            String uuid = offlinePlayer.getUniqueId().toString();

            // Check if already in list
            if (uuids.contains(uuid)) {
                sender.sendMessage(getPlugin().getPrefix() + "§e" + args[1] + " is already on the maintenance whitelist.");
                return true;
            }

            uuids.add(uuid);
            getPlugin().getConfig().set("maintenance.players", uuids);
            getPlugin().saveConfig();

            String addMessage = getPlugin().getConfig().getString("maintenance.list.added",
                "%Prefix%&aPlayer %Player% has been added to the maintenance list!");
            addMessage = ChatColor.translateAlternateColorCodes('&', addMessage)
                .replace("%Prefix%", getPlugin().getPrefix())
                .replace("%Player%", args[1]);
            sender.sendMessage(addMessage);

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError adding player: " + e.getMessage());
            getPlugin().getLogger().severe("Error adding player to maintenance list: " + e.getMessage());
            return true;
        }
    }

    /**
     * Remove player from whitelist
     */
    private boolean handleRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getPlugin().getPrefix() + "§cPlease specify a player name.");
            sender.sendMessage(getPlugin().getPrefix() + "§7Usage: §e/maintenance remove <player>");
            return true;
        }

        try {
            OfflinePlayer offlinePlayer = PlayerUtils.getOfflinePlayerByName(args[1]);
            if (offlinePlayer == null) {
                sender.sendMessage(getPlugin().getPrefix() + "§cPlayer not found: " + args[1]);
                return true;
            }

            List<String> uuids = new ArrayList<>(getPlugin().getConfig().getStringList("maintenance.players"));
            String uuid = offlinePlayer.getUniqueId().toString();

            if (uuids.contains(uuid)) {
                uuids.remove(uuid);
                getPlugin().getConfig().set("maintenance.players", uuids);
                getPlugin().saveConfig();

                String removeMessage = getPlugin().getConfig().getString("maintenance.list.removed",
                    "%Prefix%&aPlayer %Player% has been removed from the maintenance list!");
                removeMessage = ChatColor.translateAlternateColorCodes('&', removeMessage)
                    .replace("%Prefix%", getPlugin().getPrefix())
                    .replace("%Player%", args[1]);
                sender.sendMessage(removeMessage);

                // Kick player if online and maintenance is enabled
                if (getPlugin().getConfig().getBoolean("maintenance.enabled", false) && offlinePlayer.isOnline()) {
                    Player onlinePlayer = (Player) offlinePlayer;
                    if (!onlinePlayer.hasPermission(PERMISSION_BYPASS) && !onlinePlayer.isOp()) {
                        String kickMsg = ChatColor.translateAlternateColorCodes('&',
                            getPlugin().getConfig().getString("maintenance.kickMessage",
                                "&cThis server is currently in maintenance mode!"));
                        onlinePlayer.kickPlayer(kickMsg);
                    }
                }
            } else {
                String notInListMessage = getPlugin().getConfig().getString("maintenance.list.notInList",
                    "%Prefix%&cPlayer %Player% is not on the maintenance list!");
                notInListMessage = ChatColor.translateAlternateColorCodes('&', notInListMessage)
                    .replace("%Prefix%", getPlugin().getPrefix())
                    .replace("%Player%", args[1]);
                sender.sendMessage(notInListMessage);
            }

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError removing player: " + e.getMessage());
            getPlugin().getLogger().severe("Error removing player from maintenance list: " + e.getMessage());
            return true;
        }
    }

    /**
     * List whitelisted players
     */
    private boolean handleList(@NotNull CommandSender sender) {
        List<String> uuids = getPlugin().getConfig().getStringList("maintenance.players");

        if (uuids.isEmpty()) {
            sender.sendMessage(getPlugin().getPrefix() + "§7No players on the maintenance whitelist.");
            return true;
        }

        sender.sendMessage(getPlugin().getPrefix() + "§7Maintenance Whitelist §8(§e" + uuids.size() + "§8):");

        for (String uuidStr : uuids) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String name = offlinePlayer.getName();
                if (name == null) name = "Unknown";

                String status = offlinePlayer.isOnline() ? "§aOnline" : "§7Offline";
                sender.sendMessage("  §e" + name + " §8- " + status);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("  §c" + uuidStr + " §8- §cInvalid UUID");
            }
        }

        return true;
    }

    /**
     * Show maintenance status
     */
    private boolean handleStatus(@NotNull CommandSender sender) {
        boolean enabled = getPlugin().getConfig().getBoolean("maintenance.enabled", false);
        List<String> whitelist = getPlugin().getConfig().getStringList("maintenance.players");
        boolean floodgateBypass = getPlugin().getConfig().getBoolean("maintenance.floodgateBypass", false);

        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(getPlugin().getPrefix() + "§7Maintenance Status");
        sender.sendMessage("");
        sender.sendMessage("§7Status: " + (enabled ? "§cEnabled" : "§aDisabled"));
        sender.sendMessage("§7Whitelisted Players: §e" + whitelist.size());
        sender.sendMessage("§7Floodgate Bypass: " + (floodgateBypass ? "§aEnabled" : "§cDisabled"));

        if (GeyserManager.isFloodgateInstalled()) {
            sender.sendMessage("§7Floodgate: §aInstalled");
        }

        if (Main.isLuckPermsInstalled()) {
            sender.sendMessage("§7LuckPerms: §aInstalled");
            sender.sendMessage("§7Bypass Permission: §e" + PERMISSION_BYPASS);
        }

        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    /**
     * Update tab list for all online players
     */
    private void updateTabListForAll(boolean maintenanceEnabled) {
        if (!maintenanceEnabled) {
            // Clear tab list headers/footers when maintenance is disabled
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setPlayerListHeaderFooter(null, null);
            }
            return;
        }

        String tabListHeader = getPlugin().getConfig().getString("maintenance.tabList.header",
            "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«");
        String tabListFooter = getPlugin().getConfig().getString("maintenance.tabList.footer",
            "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«");

        if (tabListHeader != null) {
            tabListHeader = ChatColor.translateAlternateColorCodes('&', tabListHeader);
        }
        if (tabListFooter != null) {
            tabListFooter = ChatColor.translateAlternateColorCodes('&', tabListFooter);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(tabListHeader, tabListFooter);
        }
    }

    /**
     * Kick non-whitelisted players when maintenance is enabled
     */
    private void kickNonWhitelistedPlayers() {
        List<String> whitelist = getPlugin().getConfig().getStringList("maintenance.players");
        String kickMessage = ChatColor.translateAlternateColorCodes('&',
            getPlugin().getConfig().getString("maintenance.kickMessage",
                "&cThis server is currently in maintenance mode!"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean isWhitelisted = whitelist.contains(uuid.toString());
            boolean hasPermission = player.hasPermission(PERMISSION_BYPASS);
            boolean isOp = player.isOp();

            boolean floodgateBypassEnabled = getPlugin().getConfig().getBoolean("maintenance.floodgateBypass", false);
            boolean isFloodgate = GeyserManager.isFloodgateInstalled() && GeyserManager.isFloodgatePlayer(uuid);

            if (!isWhitelisted && !hasPermission && !isOp && !(isFloodgate && floodgateBypassEnabled)) {
                player.kickPlayer(kickMessage);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        // Null checks
        if (event == null || event.getUniqueId() == null || event.getName() == null) {
            return;
        }

        // Check if maintenance is enabled
        if (!getPlugin().getConfig().getBoolean("maintenance.enabled", false)) {
            return;
        }

        UUID uuid = event.getUniqueId();

        try {
            // Check whitelist
            List<String> whitelist = getPlugin().getConfig().getStringList("maintenance.players");
            boolean allowedInList = whitelist.contains(uuid.toString());

            // Check OP status
            OfflinePlayer offline = PlayerUtils.getOfflinePlayerByName(event.getName());
            boolean isOp = offline != null && offline.isOp();

            // Check LuckPerms permission (if available)
            boolean hasPermissionBypass = false;
            if (Main.isLuckPermsInstalled() && offline != null) {
                hasPermissionBypass = LuckPermsManager.hasOfflinePermission(offline, PERMISSION_BYPASS);
            }

            // Check Floodgate bypass
            boolean floodgateBypassEnabled = getPlugin().getConfig().getBoolean("maintenance.floodgateBypass", false);
            boolean isFloodgate = GeyserManager.isFloodgateInstalled() && GeyserManager.isFloodgatePlayer(uuid);

            // Deny login if not allowed
            if (!allowedInList && !isOp && !hasPermissionBypass && !(isFloodgate && floodgateBypassEnabled)) {
                String kickMessage = getPlugin().getConfig().getString("maintenance.kickMessage",
                    "%Prefix%&cThis server is currently in maintenance mode!");
                kickMessage = ChatColor.translateAlternateColorCodes('&', kickMessage)
                    .replace("%Prefix%", getPlugin().getPrefix());

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            }
        } catch (Exception e) {
            getPlugin().getLogger().severe("Error in maintenance pre-login check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        // Null checks
        if (event == null || event.getPlayer() == null) {
            return;
        }

        // Check if maintenance is enabled
        if (!getPlugin().getConfig().getBoolean("maintenance.enabled", false)) {
            return;
        }

        try {
            Player player = event.getPlayer();
            List<String> whitelist = getPlugin().getConfig().getStringList("maintenance.players");

            if (whitelist.contains(player.getUniqueId().toString())) {
                // Custom join message for whitelisted players
                String joinMessage = getPlugin().getConfig().getString("maintenance.joinMessage",
                    "%Prefix%&6%Player% &cjoined the server in maintenance mode!");
                joinMessage = ChatColor.translateAlternateColorCodes('&', joinMessage)
                    .replace("%Prefix%", getPlugin().getPrefix())
                    .replace("%Player%", player.getName());
                event.setJoinMessage(joinMessage);

                // Set tab list
                String tabListHeader = getPlugin().getConfig().getString("maintenance.tabList.header",
                    "&c&l»This server is currently in maintenance mode!«");
                String tabListFooter = getPlugin().getConfig().getString("maintenance.tabList.footer",
                    "&c&l»This server is currently in maintenance mode!«");

                if (tabListHeader != null) {
                    tabListHeader = ChatColor.translateAlternateColorCodes('&', tabListHeader);
                }
                if (tabListFooter != null) {
                    tabListFooter = ChatColor.translateAlternateColorCodes('&', tabListFooter);
                }

                player.setPlayerListHeaderFooter(tabListHeader, tabListFooter);
            }
        } catch (Exception e) {
            getPlugin().getLogger().severe("Error in maintenance join event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onServerPingEvent(ServerListPingEvent event) {
        // Null check
        if (event == null) {
            return;
        }

        try {
            // Log ping if enabled
            if (getPlugin().getConfig().getBoolean("playerPingServerMessage", true)) {
                if (event.getAddress() != null) {
                    String ipAddress = event.getAddress().getHostAddress();
                    getPlugin().getLogger4J().info("Ping from: " + ipAddress);
                }
            }

            // Set maintenance MOTD if enabled
            if (getPlugin().getConfig().getBoolean("maintenance.enabled", false)) {
                String motd = getPlugin().getConfig().getString("maintenance.motd");
                if (motd == null || motd.isEmpty()) {
                    motd = "%PREFIX%&c&l»This server is currently in maintenance mode!" +
                           System.lineSeparator() + "&r&cPlease try again later!«";
                }

                motd = ChatColor.translateAlternateColorCodes('&', motd)
                    .replace("%PREFIX%", getPlugin().getPrefix())
                    .replace("\\n", System.lineSeparator());

                event.setMotd(motd);
                event.setMaxPlayers(-1);
            } else {
                // Normal MOTD
                String serverMotd = getPlugin().getServer().getMotd();
                if (serverMotd != null) {
                    event.setMotd(serverMotd);
                }
            }
        } catch (Exception e) {
            getPlugin().getLogger().severe("Error in maintenance ping event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Null check and permission check
        if (args == null || !sender.hasPermission(PERMISSION_BASE)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> commands = Arrays.asList("on", "off", "enable", "disable", "add", "remove", "list", "status");
            return commands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            // Suggest online player names
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
