package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GlobalMuteCMD extends CommandListenerBase {
    public GlobalMuteCMD(Main plugin) {
        super(plugin, "globalmute");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("essentialsmini.globalmute")) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getPlugin().getPrefix() + "Usage: /globalmute <on|off>");
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "on":
                getPlugin().getConfig().set("globalMute", true);
                getPlugin().saveConfig();
                sender.sendMessage(getPlugin().getPrefix() + "Global mute has been enabled.");
                String enableMsg = getPlugin().getConfig().getString("globalMuteMessage.enable", "The server is currently globally muted.");
                getPlugin().getServer().broadcastMessage(getPlugin().getPrefix() + ChatColor.translateAlternateColorCodes('&', enableMsg));
                return true;
            case "off":
                getPlugin().getConfig().set("globalMute", false);
                getPlugin().saveConfig();
                sender.sendMessage(getPlugin().getPrefix() + "Global mute has been disabled.");
                String disableMsg = getPlugin().getConfig().getString("globalMuteMessage.disable", "The server is no longer globally muted.");
                getPlugin().getServer().broadcastMessage(getPlugin().getPrefix() + ChatColor.translateAlternateColorCodes('&', disableMsg));
                return true;
            default:
                sender.sendMessage(getPlugin().getPrefix() + "Invalid argument. Use 'on' or 'off'.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("globalmute")) {
            return null;
        }
        if (args.length == 1) {
            return List.of("on", "off");
        }
        return null;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (getPlugin().getConfig().getBoolean("globalMute", false)) {
            if (event.getPlayer().hasPermission("essentialsmini.globalmute.bypass")) {
                String bypassMessage = getPlugin().getConfig().getString("globalMuteMessage.bypass");
                if (bypassMessage != null && !bypassMessage.isEmpty()) {
                    bypassMessage = ChatColor.translateAlternateColorCodes('&', bypassMessage);
                    event.setMessage(bypassMessage);
                } else {
                    event.getPlayer().sendMessage(getPlugin().getPrefix() + "You are not allowed to chat while the server is globally muted.");
                }
            } else {
                event.setCancelled(true);
                String muteMessage = getPlugin().getConfig().getString("globalMuteMessage.chat", "The server is currently globally muted. You cannot send messages.");
                event.getPlayer().sendMessage(getPlugin().getPrefix() + ChatColor.translateAlternateColorCodes('&', muteMessage));
            }
        }
    }
}