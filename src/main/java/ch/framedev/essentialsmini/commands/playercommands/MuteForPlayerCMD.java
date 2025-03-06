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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class MuteForPlayerCMD extends CommandListenerBase {

    private final Map<String, Set<Player>> mutedPlayers = new HashMap<>();

    public MuteForPlayerCMD(Main plugin) {
        super(plugin, "muteforplayer");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
            return true;
        }
        if(args.length != 2) {
            player.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getWrongArgs(getCmdNames()[0]));
            return true;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if(targetPlayer == null) {
            player.sendMessage(getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[1]));
            return true;
        }
        mutedPlayers.putIfAbsent(player.getName(), new HashSet<>());
        Set<Player> mutedSet = mutedPlayers.get(player.getName());
        if(!mutedSet.contains(targetPlayer)) {
            mutedSet.add(targetPlayer);
            player.sendMessage(getPrefix() + "§aPlayer §6" + targetPlayer.getName() + " §ahas been muted for you!");
        } else {
            mutedSet.remove(targetPlayer);
            player.sendMessage(getPrefix() + "§aPlayer §6" + targetPlayer.getName() + " §ahas been unmuted for you!");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the command matches and the sender is a player
        if (!command.getName().equalsIgnoreCase("muteforplayer") || !(sender instanceof Player)) {
            return Collections.emptyList();
        }

        // Tab complete for the second argument (player names)
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName) // Get all player names
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase())) // Filter by prefix
                    .toList(); // Collect into a list
        }

        // Default behavior
        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer(); // The player sending the message

        // Iterate through all recipients of the message
        event.getRecipients().removeIf(recipient -> {
            Set<Player> mutedForRecipient = mutedPlayers.get(recipient.getName());
            return mutedForRecipient != null && mutedForRecipient.contains(sender);
        });
    }
}
