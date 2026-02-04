package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 17.07.2020 22:52
 */
public class PlayerListCMD extends CommandBase {

    private static final String PERMISSION_ONLINE = "essentialsmini.online";
    private static final String PERMISSION_OFFLINE = "essentialsmini.offline";
    private static final int MAX_OFFLINE_PLAYERS_TO_SHOW = 100; // Prevent massive list

    public PlayerListCMD(Main plugin) {
        super(plugin, "online", "offline");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Null checks
        if (command == null || command.getName() == null) {
            return false;
        }

        String cmd = command.getName().toLowerCase();

        if ("online".equals(cmd)) {
            return handleOnlineCommand(sender);
        }

        if ("offline".equals(cmd)) {
            return handleOfflineCommand(sender);
        }

        return false;
    }

    /**
     * Handle /online command
     */
    private boolean handleOnlineCommand(@NotNull CommandSender sender) {
        // Permission check
        if (!sender.hasPermission(PERMISSION_ONLINE)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }

        try {
            // Get online players with null safety
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player != null && player.getName() != null)
                    .map(Player::getName)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());

            // Display header
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§6§l» §cOnline Players §6§l«");
            sender.sendMessage("");

            // Display players or "None"
            if (players.isEmpty()) {
                sender.sendMessage("§7No players online");
            } else {
                // Display players in a formatted list
                sender.sendMessage("§e" + String.join("§7, §e", players));
            }

            sender.sendMessage("");
            sender.sendMessage("§6Total: §e" + players.size() + " §6player" + (players.size() == 1 ? "" : "s"));
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError retrieving online players: " + e.getMessage());
            getPlugin().getLogger().severe("Error in /online command: " + e.getMessage());
            return true;
        }
    }

    /**
     * Handle /offline command
     */
    private boolean handleOfflineCommand(@NotNull CommandSender sender) {
        // Permission check
        if (!sender.hasPermission(PERMISSION_OFFLINE)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return true;
        }

        try {
            // Warning: Getting offline players can be expensive!
            sender.sendMessage(getPlugin().getPrefix() + "§7Retrieving offline players... This may take a moment.");

            // Get offline players with null safety
            OfflinePlayer[] allOfflinePlayers = Bukkit.getOfflinePlayers();

            if (allOfflinePlayers == null || allOfflinePlayers.length == 0) {
                sender.sendMessage(getPlugin().getPrefix() + "§7No offline players found.");
                return true;
            }

            List<String> players = new ArrayList<>();

            for (OfflinePlayer offlinePlayer : allOfflinePlayers) {
                // Null checks
                if (offlinePlayer == null) {
                    continue;
                }

                // Skip online players
                if (offlinePlayer.isOnline()) {
                    continue;
                }

                // Get name with null check
                String name = offlinePlayer.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }

                players.add(name);

                // Limit to prevent massive lists
                if (players.size() >= MAX_OFFLINE_PLAYERS_TO_SHOW) {
                    break;
                }
            }

            // Sort players alphabetically
            Collections.sort(players, String.CASE_INSENSITIVE_ORDER);

            // Display header
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§6§l» §cOffline Players §6§l«");
            sender.sendMessage("");

            // Display players or "None"
            if (players.isEmpty()) {
                sender.sendMessage("§7No offline players");
            } else {
                // Display players in a formatted list
                sender.sendMessage("§e" + String.join("§7, §e", players));

                // Show truncation warning if list was limited
                if (players.size() >= MAX_OFFLINE_PLAYERS_TO_SHOW) {
                    sender.sendMessage("");
                    sender.sendMessage("§7§o(Showing first " + MAX_OFFLINE_PLAYERS_TO_SHOW + " players only)");
                }
            }

            sender.sendMessage("");
            sender.sendMessage("§6Total Shown: §e" + players.size() + " §6player" + (players.size() == 1 ? "" : "s"));
            sender.sendMessage("§6Total Offline: §e" + (allOfflinePlayers.length - Bukkit.getOnlinePlayers().size()));
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return true;
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getPrefix() + "§cError retrieving offline players: " + e.getMessage());
            getPlugin().getLogger().severe("Error in /offline command: " + e.getMessage());
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // No tab completion needed for these commands
        return Collections.emptyList();
    }
}