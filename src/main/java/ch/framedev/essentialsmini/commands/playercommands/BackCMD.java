package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

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
    // store UUIDs rather than Player objects to avoid memory leaks and stale references
    private final HashMap<UUID, Location> deaths = new HashMap<>();

    public BackCMD(Main plugin) {
        super(plugin, "back");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("back")) {
            if (plugin.getConfig().getBoolean("Back")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                UUID uuid = player.getUniqueId();
                if (!deaths.containsKey(uuid)) {
                    String message = plugin.getLanguageConfig(player).getString("NoDeathLocationFound");
                    if (message != null) {
                        message = new TextUtils().replaceAndWithParagraph(message);
                    } else {
                        message = "";
                    }
                    player.sendMessage(plugin.getPrefix() + message);
                    return true;
                }
                /*  Player Teleports to the Death Location */
                Location loc = deaths.get(uuid);
                boolean teleported = false;
                if (loc != null) {
                    teleported = player.teleport(loc);
                }
                if (teleported) {
                    String message = plugin.getLanguageConfig(player).getString("DeathTeleport");
                    if (message != null) {
                        message = new TextUtils().replaceAndWithParagraph(message);
                    } else {
                        message = "";
                    }
                    player.sendMessage(plugin.getPrefix() + message);
                    /* Death Point remove */
                    deaths.remove(uuid);
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
        // store by UUID to avoid retaining Player instance
        deaths.put(player.getUniqueId(), player.getLocation());
        String message = plugin.getLanguageConfig(player).getString("DeathCommandUsage");
        if (message != null) {
            message = new TextUtils().replaceAndWithParagraph(message);
        }
        if (message == null) message = "";
        player.sendMessage(plugin.getPrefix() + message);
    }
}
