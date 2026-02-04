package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalMuteCMD extends CommandListenerBase {

    private static final String PERMISSION_BASE = "essentialsmini.globalmute";
    private static final String PERMISSION_BYPASS = PERMISSION_BASE + ".bypass";

    public GlobalMuteCMD(Main plugin) {
        super(plugin, "globalmute");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Null check for args (defensive programming)
        if (args == null) {
            args = new String[0];
        }

        // Permission check
        if (!sender.hasPermission(PERMISSION_BASE)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }

        // Args validation
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "on":
            case "enable":
            case "true":
                return handleEnable(sender);
            case "off":
            case "disable":
            case "false":
                return handleDisable(sender);
            case "status":
            case "check":
                return handleStatus(sender);
            default:
                sender.sendMessage(getPlugin().getPrefix() + "§cInvalid argument. Use: on, off, or status");
                sendUsage(sender);
                return true;
        }
    }

    /**
     * Send usage message
     */
    private void sendUsage(@NotNull CommandSender sender) {
        sender.sendMessage(getPlugin().getPrefix() + "§7Usage: §e/globalmute <on|off|status>");
        sender.sendMessage(getPlugin().getPrefix() + "§7  on/enable  - Enable global mute");
        sender.sendMessage(getPlugin().getPrefix() + "§7  off/disable - Disable global mute");
        sender.sendMessage(getPlugin().getPrefix() + "§7  status - Check current status");
    }

    /**
     * Enable global mute
     */
    private boolean handleEnable(@NotNull CommandSender sender) {
        // Check if already enabled
        if (getPlugin().getConfig().getBoolean("globalMute", false)) {
            sender.sendMessage(getPlugin().getPrefix() + "§eGlobal mute is already enabled.");
            return true;
        }

        try {
            getPlugin().getConfig().set("globalMute", true);
            getPlugin().saveConfig();

            sender.sendMessage(getPlugin().getPrefix() + "§aGlobal mute has been enabled.");

            // Broadcast message
            String enableMsg = getPlugin().getConfig().getString("globalMuteMessage.enable",
                "The server is currently globally muted.");
            if (enableMsg != null && !enableMsg.isEmpty()) {
                String formattedMsg = ChatColor.translateAlternateColorCodes('&', enableMsg);
                getPlugin().getServer().broadcastMessage(getPlugin().getPrefix() + formattedMsg);
            }

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError enabling global mute: " + e.getMessage());
            getPlugin().getLogger().severe("Error enabling global mute: " + e.getMessage());
            return true;
        }
    }

    /**
     * Disable global mute
     */
    private boolean handleDisable(@NotNull CommandSender sender) {
        // Check if already disabled
        if (!getPlugin().getConfig().getBoolean("globalMute", false)) {
            sender.sendMessage(getPlugin().getPrefix() + "§eGlobal mute is already disabled.");
            return true;
        }

        try {
            getPlugin().getConfig().set("globalMute", false);
            getPlugin().saveConfig();

            sender.sendMessage(getPlugin().getPrefix() + "§aGlobal mute has been disabled.");

            // Broadcast message
            String disableMsg = getPlugin().getConfig().getString("globalMuteMessage.disable",
                "The server is no longer globally muted.");
            if (disableMsg != null && !disableMsg.isEmpty()) {
                String formattedMsg = ChatColor.translateAlternateColorCodes('&', disableMsg);
                getPlugin().getServer().broadcastMessage(getPlugin().getPrefix() + formattedMsg);
            }

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError disabling global mute: " + e.getMessage());
            getPlugin().getLogger().severe("Error disabling global mute: " + e.getMessage());
            return true;
        }
    }

    /**
     * Check global mute status
     */
    private boolean handleStatus(@NotNull CommandSender sender) {
        boolean isEnabled = getPlugin().getConfig().getBoolean("globalMute", false);

        sender.sendMessage(getPlugin().getPrefix() + "§7Global Mute Status: " +
            (isEnabled ? "§aEnabled" : "§cDisabled"));

        if (isEnabled) {
            sender.sendMessage(getPlugin().getPrefix() + "§7Players without bypass permission cannot chat.");
            sender.sendMessage(getPlugin().getPrefix() + "§7Use §e/globalmute off §7to disable.");
        } else {
            sender.sendMessage(getPlugin().getPrefix() + "§7All players can chat normally.");
            sender.sendMessage(getPlugin().getPrefix() + "§7Use §e/globalmute on §7to enable.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Null checks
        if (args == null || command == null) {
            return Collections.emptyList();
        }

        // Check command name
        if (!command.getName().equalsIgnoreCase("globalmute")) {
            return Collections.emptyList();
        }

        // Check permission
        if (!sender.hasPermission(PERMISSION_BASE)) {
            return Collections.emptyList();
        }

        // First argument - actions
        if (args.length == 1) {
            List<String> options = Arrays.asList("on", "off", "enable", "disable", "status");
            return options.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Handle chat event for global mute
     * Using HIGHEST priority to run after other plugins
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        // Null checks
        if (event == null || event.getPlayer() == null) {
            return;
        }

        // Check if global mute is enabled
        if (!getPlugin().getConfig().getBoolean("globalMute", false)) {
            return; // Global mute is off, allow chat
        }

        // Check bypass permission
        if (event.getPlayer().hasPermission(PERMISSION_BYPASS)) {
            // Player has bypass permission, allow chat with optional prefix
            handleBypassChat(event);
            return;
        }

        // Cancel chat for non-bypass players
        event.setCancelled(true);

        // Send mute message
        String muteMessage = getPlugin().getConfig().getString("globalMuteMessage.chat",
            "The server is currently globally muted. You cannot send messages.");

        if (muteMessage != null && !muteMessage.isEmpty()) {
            String formattedMsg = ChatColor.translateAlternateColorCodes('&', muteMessage);
            event.getPlayer().sendMessage(getPlugin().getPrefix() + formattedMsg);
        }
    }

    /**
     * Handle chat for players with bypass permission
     */
    private void handleBypassChat(@NotNull AsyncPlayerChatEvent event) {
        String bypassFormat = getPlugin().getConfig().getString("globalMuteMessage.bypass");

        // If no bypass format configured, just allow normal chat
        if (bypassFormat == null || bypassFormat.isEmpty()) {
            return;
        }

        try {
            // Replace placeholders
            String formattedMsg = ChatColor.translateAlternateColorCodes('&', bypassFormat);
            formattedMsg = formattedMsg.replace("%player%", event.getPlayer().getName());
            formattedMsg = formattedMsg.replace("%Player%", event.getPlayer().getName());
            formattedMsg = formattedMsg.replace("%displayname%", event.getPlayer().getDisplayName());
            formattedMsg = formattedMsg.replace("%DisplayName%", event.getPlayer().getDisplayName());
            formattedMsg = formattedMsg.replace("%message%", event.getMessage());
            formattedMsg = formattedMsg.replace("%Message%", event.getMessage());

            // Set the new chat format
            event.setFormat(formattedMsg);

        } catch (Exception e) {
            getPlugin().getLogger().warning("Error formatting bypass chat message: " + e.getMessage());
            // Allow normal chat on error
        }
    }
}