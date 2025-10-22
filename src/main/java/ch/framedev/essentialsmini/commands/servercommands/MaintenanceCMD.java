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
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaintenanceCMD extends CommandListenerBase {

    public MaintenanceCMD(Main plugin) {
        super(plugin, "maintenance");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("essentialsmini.maintenance")) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }
        if (args.length == 0) {
            boolean enabled = getPlugin().getConfig().getBoolean("maintenance.enabled");
            getPlugin().getConfig().set("maintenance.enabled", !enabled);
            getPlugin().saveConfig();
            sender.sendMessage(getPlugin().getPrefix() + (!enabled ? "§cMaintenance mode has been enabled!" : "§aMaintenance mode has been disabled!"));
            String tabListHeader = getPlugin().getConfig().getString("maintenance.tabList.header", "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«").replace("&", "§");
            String tabListFooter = getPlugin().getConfig().getString("maintenance.tabList.footer", "&c&l»This server is currently in maintenance mode!\n&r&cPlease try again later!«").replace("&", "§");
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setPlayerListHeaderFooter(!enabled ? tabListHeader : null, !enabled ? tabListFooter : null);
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
            String addMessage = getPlugin().getConfig().getString("maintenance.list.added", "%Prefix%&aPlayer %Player% has been added to the maintenance list!");
            addMessage = addMessage.replace("§", "&").replace("%Prefix%", getPlugin().getPrefix()).replace("%Player%", args[1]);
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
                String removeMessage = getPlugin().getConfig().getString("maintenance.list.removed", "%Prefix%&aPlayer %Player% has been removed from the maintenance list!");
                removeMessage = removeMessage.replace("§", "&").replace("%Prefix%", getPlugin().getPrefix()).replace("%Player%", args[1]);
                sender.sendMessage(removeMessage);
            } else {
                String notInListMessage = getPlugin().getConfig().getString("maintenance.list.notInList", "%Prefix%&cPlayer %Player% is not on the maintenance list!");
                notInListMessage = notInListMessage.replace("§", "&").replace("%Prefix%", getPlugin().getPrefix()).replace("%Player%", args[1]);
                sender.sendMessage(notInListMessage);
            }
            return true;
        }
        return true;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!getPlugin().getConfig().getBoolean("maintenance.enabled")) return;

        UUID uuid = event.getUniqueId();
        OfflinePlayer offline = PlayerUtils.getOfflinePlayerByName(event.getName());
        boolean allowedInList = getPlugin().getConfig().getStringList("maintenance.players").contains(uuid.toString());
        boolean isOp = offline != null && offline.isOp();
        boolean hasPermissionBypass = Main.isLuckPermsInstalled() && offline != null && LuckPermsManager.hasOfflinePermission(offline, "essentialsmini.maintenance.bypass");

        boolean floodgateBypassEnabled = getPlugin().getConfig().getBoolean("maintenance.floodgateBypass", false);
        boolean isFloodgate = GeyserManager.isFloodgateInstalled() && GeyserManager.isFloodgatePlayer(uuid);

        if (!allowedInList && !isOp && !hasPermissionBypass && !(isFloodgate && floodgateBypassEnabled)) {
            String kickMessage = getPlugin().getConfig().getString("maintenance.kickMessage", "%Prefix%&cThis server is currently in maintenance mode!");
            kickMessage = kickMessage.replace("&", "§").replace("%Prefix%", getPlugin().getPrefix());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(kickMessage);
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        if (getPlugin().getConfig().getBoolean("maintenance.enabled")) {
            if (getPlugin().getConfig().getStringList("maintenance.players").contains(event.getPlayer().getUniqueId().toString())) {
                String joinMessage = getPlugin().getConfig().getString("maintenance.joinMessage", "%Prefix%&6%Player% &cjoined the server in maintenance mode!");
                joinMessage = joinMessage.replace("&", "§").replace("%Prefix%", getPlugin().getPrefix()).replace("%Player%", event.getPlayer().getName());
                event.setJoinMessage(joinMessage);
                String tabListHeader = getPlugin().getConfig().getString("maintenance.tabList.header", "&c&l»This server is currently in maintenance mode!«").replace("&", "§");
                String tabListFooter = getPlugin().getConfig().getString("maintenance.tabList.footer", "&c&l»This server is currently in maintenance mode!«").replace("&", "§");
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
            motd = motd.replace("&", "§").replace("%PREFIX%", getPlugin().getPrefix()).replace("\\n", System.lineSeparator());
            event.setMotd(motd);
            event.setMaxPlayers(-1);
        } else {
            event.setMotd(getPlugin().getServer().getMotd());
        }
    }
}
