package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.ListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands
 * Date: 31.01.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

public class AFKCMD implements CommandExecutor {
    private static HashMap<String, String> afkPlayerMap;
    private static HashMap<String, Long> afkTimeMap;
    private final Main plugin;
    private final HashMap<String, Location> locationMap;

    public AFKCMD(Main plugin) {
        this.plugin = plugin;
        this.locationMap = new HashMap<>();
        afkPlayerMap = new HashMap<>();
        afkTimeMap = new HashMap<>();
        int afkTime = plugin.getConfig().getInt("AFK.Time", 120);
        Objects.requireNonNull(plugin.getCommand("afk")).setExecutor(this);
        new Events(plugin);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new IdleTimer(plugin), (afkTime * 20L), (afkTime * 20L));
    }

    public static void putPlayerToAfkMap(String PlayerName) {
        afkPlayerMap.put(PlayerName, Objects.requireNonNull(Bukkit.getPlayer(PlayerName)).getPlayerListName());
    }

    public static void removePlayerFromAfkMap(String playerName) {
        afkPlayerMap.remove(playerName);
    }

    public static String getAfkPlayerName(String playerName) {
        return afkPlayerMap.get(playerName);
    }

    public static void putPlayerToTimeMap(String playerName) {
        afkTimeMap.put(playerName, Objects.requireNonNull(Bukkit.getPlayer(playerName)).getPlayerTime());
        Objects.requireNonNull(Bukkit.getPlayer(playerName)).resetPlayerTime();
    }

    public static void removePlayerFromTimeMap(String playerName) {
        afkTimeMap.remove(playerName);
    }

    public static Long getPlayerAfkTime(String playerName) {
        return afkTimeMap.get(playerName);
    }

    public static boolean isPlayerAfk(String playerName) {
        return afkPlayerMap.containsKey(playerName);
    }


    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (player.hasPermission(plugin.getPermissionBase() + "afk")) {
                if (command.getName().equalsIgnoreCase("afk")) {
                    if (isPlayerAfk(player.getName())) {
                        afk(player, true, "");
                        return true;
                    }
                    afk(player, false, "");
                    return true;
                }
            } else {
                player.sendMessage(plugin.getPrefix() + " " + plugin.getNoPerms());
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"unused", "deprecation"})
    public void afk(Player player, boolean afk, String reason) {
        String isAfkMessage = this.plugin.getConfig().getString("AFK.IsAFK");
        String notAfkMessage = this.plugin.getConfig().getString("AFK.IsNotAFK");


        if (afk) {
            if (notAfkMessage != null && notAfkMessage.matches(".*\\[Time].*")) {
                String afkTime = returnAfkTime(player.getName());
                notAfkMessage = notAfkMessage.replace("[Time]", "");
                Objects.requireNonNull(Bukkit.getPlayer(player.getName())).setPlayerListName(getAfkPlayerName(player.getName()));
                removePlayerFromAfkMap(player.getName());
                notAfkMessage = ReplaceCharConfig.replaceParagraph(notAfkMessage);
                TextComponent tc = new TextComponent();
                tc.setText(player.getName());
                tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(player.getName())).create()));
                notAfkMessage = ReplaceCharConfig.replaceObjectWithData(notAfkMessage, "[Player]", player.getName());
                Bukkit.broadcastMessage(notAfkMessage);
                Bukkit.getConsoleSender().sendMessage("§7" + player.getName() + " is no longer AFK " + afkTime);
            }
        } else {

            TextComponent tc = new TextComponent();
            tc.setText(player.getName());
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(player.getName())).create()));
            putPlayerToAfkMap(player.getName());
            putPlayerToTimeMap(player.getName());
            isAfkMessage = ReplaceCharConfig.replaceParagraph(isAfkMessage);
            isAfkMessage = ReplaceCharConfig.replaceObjectWithData(isAfkMessage, "[Player]", player.getName());
            Bukkit.broadcastMessage(isAfkMessage);
        }
    }


    String returnAfkTime(String playerName) {
        long oldTime = getPlayerAfkTime(playerName);
        long currentTime = Objects.requireNonNull(Bukkit.getPlayer(playerName)).getPlayerTime();
        long minutes = (currentTime - oldTime) / 20L / 60L;
        long seconds = (currentTime - oldTime) / 20L % 60L;
        return (minutes == 0L) ? (seconds + "s") : (minutes + "m" + seconds + "s");
    }

    HashMap<String, Location> getLocationMap() {
        return this.locationMap;
    }

    public class Events extends ListenerBase {

        public Events(Main plugin) {
            super(plugin);
            setupListener(this);
        }

        @EventHandler
        private void onPlayerMove(PlayerMoveEvent event) {
            if (AFKCMD.isPlayerAfk(event.getPlayer().getName())) {
                Location toLocation = event.getTo();
                if(toLocation != null) {
                    int movX = event.getFrom().getBlockX() - toLocation.getBlockX();
                    int movZ = event.getFrom().getBlockZ() - toLocation.getBlockZ();
                    if (Math.abs(movX) > 0 || Math.abs(movZ) > 0) {
                        AFKCMD.this.afk(event.getPlayer(), true, "");
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerDamage(EntityDamageEvent event) {
            if (event.getEntity() instanceof Player player) {
                if (AFKCMD.isPlayerAfk(player.getName())) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event) {
            if (AFKCMD.isPlayerAfk(event.getPlayer().getName())) {
                String playerName = event.getPlayer().getName();
                AFKCMD.removePlayerFromAfkMap(event.getPlayer().getName());
                AFKCMD.removePlayerFromTimeMap(event.getPlayer().getName());
                AFKCMD.this.returnAfkTime(event.getPlayer().getName());
                AFKCMD.this.getLocationMap().remove(playerName);
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if (AFKCMD.isPlayerAfk(event.getPlayer().getName())) {
                String playerName = event.getPlayer().getName();
                AFKCMD.removePlayerFromAfkMap(event.getPlayer().getName());
                AFKCMD.removePlayerFromTimeMap(event.getPlayer().getName());
                AFKCMD.this.returnAfkTime(event.getPlayer().getName());
                AFKCMD.this.getLocationMap().remove(playerName);
            }
        }

        @EventHandler
        public void onChat(AsyncPlayerChatEvent event) {
            if (AFKCMD.isPlayerAfk(event.getPlayer().getName())) {
                String playerName = event.getPlayer().getName();
                AFKCMD.removePlayerFromAfkMap(event.getPlayer().getName());
                AFKCMD.removePlayerFromTimeMap(event.getPlayer().getName());
                AFKCMD.this.returnAfkTime(event.getPlayer().getName());
                AFKCMD.this.getLocationMap().remove(playerName);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onPlayerQuit(PlayerQuitEvent event) {
            String playerName = event.getPlayer().getName();
            AFKCMD.removePlayerFromAfkMap(playerName);
            AFKCMD.removePlayerFromTimeMap(playerName);
            AFKCMD.this.getLocationMap().remove(playerName);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onPlayerKicked(PlayerKickEvent event) {
            String playerName = event.getPlayer().getName();
            AFKCMD.removePlayerFromAfkMap(playerName);
            AFKCMD.removePlayerFromTimeMap(playerName);
            AFKCMD.this.getLocationMap().remove(playerName);
        }
    }

    class IdleTimer implements Runnable {

        private final Main plugin;
        final int afkTime = Main.getInstance().getConfig().getInt("AFK.Time");

        IdleTimer(Main plugin) {
            this.plugin = plugin;
        }

        public void run() {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                if (AFKCMD.this.getLocationMap().containsKey(player.getName()) && AFKCMD.this.getLocationMap().get(player.getName()).equals(player.getLocation()) && !AFKCMD.isPlayerAfk(player.getName())) {
                    long idleTime = (this.afkTime * 20L);
                    player.setPlayerTime(-idleTime, true);
                    AFKCMD.this.afk(player, false, "");
                }
                AFKCMD.this.getLocationMap().put(player.getName(), player.getLocation());
            }
        }
    }
}
