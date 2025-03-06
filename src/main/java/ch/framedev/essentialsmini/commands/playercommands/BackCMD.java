package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.simplejavautils.TextUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 14.07.2020 16:47
 */
public class BackCMD extends CommandListenerBase {

    // This Plugin
    private final Main plugin;

    // death HashMap
    private final HashMap<Player, Location> deaths = new HashMap<>();

    public BackCMD(Main plugin) {
        super(plugin, "back");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("back")) {
            if (plugin.getConfig().getBoolean("Back")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                if (!deaths.containsKey(player)) {
                    String message = plugin.getLanguageConfig(player).getString("NoDeathLocationFound");
                    if (message != null) {
                        message = new TextUtils().replaceAndWithParagraph(message);
                    }
                    player.sendMessage(plugin.getPrefix() + message);
                    return true;
                }
                /*  Player Teleports to the Death Location */
                if (player.teleport(deaths.get(player))) {
                    String message = plugin.getLanguageConfig(player).getString("DeathTeleport");
                    if (message != null) {
                        message = new TextUtils().replaceAndWithParagraph(message);
                    }
                    player.sendMessage(plugin.getPrefix() + message);
                    /* Death Point remove */
                    deaths.remove(player);
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("Back")) return;
        Player player = event.getEntity();
        deaths.put(player, player.getLocation());
        String message = plugin.getLanguageConfig(player).getString("DeathCommandUsage");
        if (message != null) {
            message = new TextUtils().replaceAndWithParagraph(message);
        }
        player.sendMessage(plugin.getPrefix() + message);
    }
}
