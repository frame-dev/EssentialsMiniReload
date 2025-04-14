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
import ch.framedev.essentialsmini.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceCMD extends CommandListenerBase {

    public MaintenanceCMD(Main plugin) {
        super(plugin, "maintenance");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("essentialsmini.maintenance")) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }
        if (args.length == 0) {
            if (getPlugin().getConfig().getBoolean("maintenance.enabled")) {
                getPlugin().getConfig().set("maintenance.enabled", false);
                getPlugin().saveConfig();
                sender.sendMessage(getPlugin().getPrefix() + "§aMaintenance mode has been disabled!");
                for (Player player : Bukkit.getOnlinePlayers())
                    player.setPlayerListHeaderFooter(null, null);
            } else {
                getPlugin().getConfig().set("maintenance.enabled", true);
                getPlugin().saveConfig();
                sender.sendMessage(getPlugin().getPrefix() + "§cMaintenance mode has been enabled!");
                String tabListHeader = getPlugin().getConfig().getString("maintenance.tabList.header", "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«");
                String tabListFooter = getPlugin().getConfig().getString("maintenance.tabList.footer", "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«");
                tabListHeader = tabListHeader.replace("§", "&");
                tabListFooter = tabListFooter.replace("§", "&");
                for (Player player : Bukkit.getOnlinePlayers())
                    player.setPlayerListHeaderFooter(tabListHeader, tabListFooter);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                sender.sendMessage(getPlugin().getPrefix() + "§cPlease specify a player name.");
                return true;
            }
            OfflinePlayer offlinePlayer = PlayerUtils.getOfflinePlayerByName(args[1]);
            List<String> uuids = getPlugin().getConfig().contains("maintenance.players") ? getPlugin().getConfig().getStringList("maintenance.players") : new ArrayList<>();
            uuids.add(offlinePlayer.getUniqueId().toString());
            getPlugin().getConfig().set("maintenance.players", uuids);
            getPlugin().saveConfig();
            String addMessage = getPlugin().getConfig()
                            .getString("maintenance.list.added", "%Prefix%&aPlayer %Player% has been added to the maintenance list!");
            addMessage = addMessage.replace("§", "&")
                            .replace("%Prefix%", getPlugin().getPrefix())
                            .replace("%Player%", args[1]);
            sender.sendMessage(addMessage);
            return true;
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                sender.sendMessage(getPlugin().getPrefix() + "§cPlease specify a player name.");
                return true;
            }
            OfflinePlayer offlinePlayer = PlayerUtils.getOfflinePlayerByName(args[1]);
            List<String> uuids = getPlugin().getConfig().contains("maintenance.players") ? getPlugin().getConfig().getStringList("maintenance.players") : new ArrayList<>();
            if (uuids.contains(offlinePlayer.getUniqueId().toString())) {
                uuids.remove(offlinePlayer.getUniqueId().toString());
                getPlugin().getConfig().set("maintenance.players", uuids);
                getPlugin().saveConfig();
                String removeMessage = getPlugin().getConfig()
                                .getString("maintenance.list.removed", "%Prefix%&aPlayer %Player% has been removed from the maintenance list!");
                removeMessage = removeMessage.replace("§", "&")
                                .replace("%Prefix%", getPlugin().getPrefix())
                                .replace("%Player%", args[1]);
                sender.sendMessage(removeMessage);
            } else {
                String notInListMessage = getPlugin().getConfig()
                                .getString("maintenance.list.notInList", "%Prefix%&cPlayer %Player% is not on the maintenance list!");
                notInListMessage = notInListMessage.replace("§", "&")
                                .replace("%Prefix%", getPlugin().getPrefix())
                                .replace("%Player%", args[1]);
                sender.sendMessage(notInListMessage);
            }
            return true;
        }
        return true;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (getPlugin().getConfig().getBoolean("maintenance.enabled")) {
            if (!getPlugin().getConfig().getStringList("maintenance.players").contains(event.getUniqueId().toString()) &&
                    !PlayerUtils.getOfflinePlayerByName(event.getName()).isOp() &&
                    (!Main.isLuckPermsInstalled() || !LuckPermsManager.hasOfflinePermission(PlayerUtils.getOfflinePlayerByName(event.getName()), "essentialsmini.maintenance.bypass"))) {event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, getPlugin().getPrefix() + "§cThis server is currently in maintenance mode!");
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                String kickMessage = getPlugin().getConfig().getString("maintenance.kickMessage", "%Prefix%&cThis server is currently in maintenance mode!");
                kickMessage = kickMessage.replace("§", "&");
                kickMessage = kickMessage.replace("%Prefix%", getPlugin().getPrefix());
                event.setKickMessage(kickMessage);
            }
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        if (getPlugin().getConfig().getBoolean("maintenance.enabled")) {
            if (getPlugin().getConfig().getStringList("maintenance.players").contains(event.getPlayer().getUniqueId().toString())) {
                String joinMessage = getPlugin().getConfig().getString("maintenance.joinMessage", "%Prefix%&6%Player% &cjoined the server in maintenance mode!");
                joinMessage = joinMessage.replace("§", "&");
                joinMessage = joinMessage.replace("%Prefix%", getPlugin().getPrefix());
                joinMessage = joinMessage.replace("%Player%", event.getPlayer().getName());
                event.setJoinMessage(joinMessage);
                String tabListHeader = getPlugin().getConfig().getString("maintenance.tabList.header", "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«");
                String tabListFooter = getPlugin().getConfig().getString("maintenance.tabList.footer", "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«");
                tabListHeader = tabListHeader.replace("§", "&");
                tabListFooter = tabListFooter.replace("§", "&");
                event.getPlayer().setPlayerListHeaderFooter(tabListHeader, tabListFooter);
            }
        }
    }

    @EventHandler
    public void onServerPingEvent(ServerListPingEvent event) {
        if (getPlugin().getConfig().getBoolean("playerPingServerMessage", true)) {
            String ipAddress = event.getAddress().getHostAddress();
            getPlugin().getLogger4J().info("Ping from: " + ipAddress);
        }

        if (getPlugin().getConfig().getBoolean("maintenance.enabled")) {
            String motd = getPlugin().getConfig().getString("maintenance.motd");
            if (motd == null)
                motd = "%PREFIX%&c&l»This server is currently in maintenance mode!" + System.lineSeparator() + "&r&cPlease try again later!«";
            motd = motd.replace("&", "§") // Convert color codes
                    .replace("%PREFIX%", getPrefix()) // Replace %PREFIX% with actual prefix
                    .replace("\\n", System.lineSeparator()); // Replace escaped \n with a real newline
            event.setMotd(motd);
            event.setMaxPlayers(-1);
        } else {
            event.setMotd(getPlugin().getServer().getMotd());
        }
    }
}
