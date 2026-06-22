package ch.framedev.essentialsmini.commands.playercommands;



/*
 * ch.framedev.essentialsmini.commands.playercommands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 15.01.2025 19:40
 */

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MuteForPlayerCMD extends CommandListenerBase {

    private static final String COMMAND_NAME = "muteforplayer";
    private static final String USAGE = "/muteforplayer <mode> <player>";

    private final Map<UUID, Set<UUID>> mutedPlayers = new HashMap<>();

    public MuteForPlayerCMD(Main plugin) {
        super(plugin, COMMAND_NAME);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (args.length != 2) {
            sendWrongArgs(player);
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sendPlayerNotOnline(player, args[1]);
            return true;
        }

        if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
            send(player, "§cYou cannot mute yourself for yourself.");
            return true;
        }

        boolean muted = toggleMutedPlayer(player, targetPlayer);
        send(player, "§aPlayer §6" + targetPlayer.getName() + (muted
                ? " §ahas been muted for you!"
                : " §ahas been unmuted for you!"));
        return true;
    }

    private boolean toggleMutedPlayer(Player receiver, Player mutedPlayer) {
        Set<UUID> mutedSet = mutedPlayers.computeIfAbsent(receiver.getUniqueId(), ignored -> new HashSet<>());
        if (mutedSet.remove(mutedPlayer.getUniqueId())) {
            if (mutedSet.isEmpty()) {
                mutedPlayers.remove(receiver.getUniqueId());
            }
            return false;
        }
        mutedSet.add(mutedPlayer.getUniqueId());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND_NAME) || !(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return TabCompleteUtils.matchingOnlinePlayers(args[1]);
        }

        return Collections.emptyList();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event == null) return;

        UUID senderId = event.getPlayer().getUniqueId();
        event.getRecipients().removeIf(recipient -> isMutedFor(recipient, senderId));
    }

    private boolean isMutedFor(Player recipient, UUID senderId) {
        if (recipient == null || senderId == null) return false;

        Set<UUID> mutedForRecipient = mutedPlayers.get(recipient.getUniqueId());
        return mutedForRecipient != null && mutedForRecipient.contains(senderId);
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        send(sender, getPlugin().getOnlyPlayer(null));
        return null;
    }

    private void sendWrongArgs(CommandSender sender) {
        send(sender, getPlugin().getWrongArgs(sender instanceof Player player ? player : null, USAGE));
    }

    private void sendPlayerNotOnline(CommandSender sender, String playerName) {
        String message = getPlugin().getVariables() == null
                ? "§cPlayer §6" + playerName + " §cis not online!"
                : getPlugin().getVariables().getPlayerNameNotOnline(playerName);
        send(sender, message);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(getPrefix() + message);
    }
}
