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
import java.util.Map;
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
    private final Map<UUID, Location> deaths = new HashMap<>();
    private final TextUtils textUtils = new TextUtils();

    public BackCMD(Main plugin) {
        super(plugin, "back");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("back")) {
            return false;
        }

        if (isBackDisabled()) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            return true;
        }

        UUID uuid = player.getUniqueId();
        Location deathLocation = deaths.get(uuid);
        if (deathLocation == null) {
            sendLang(player, "NoDeathLocationFound");
            return true;
        }

        if (!player.teleport(deathLocation)) {
            return true;
        }

        sendLang(player, "DeathTeleport");
        deaths.remove(uuid);
        return true;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (isBackDisabled()) return;
        Player player = event.getEntity();
        // store by UUID to avoid retaining Player instance
        deaths.put(player.getUniqueId(), player.getLocation());
        sendLang(player, "DeathCommandUsage");
    }

    private boolean isBackDisabled() {
        return !plugin.getConfig().getBoolean("Back");
    }

    private void sendLang(Player player, String key) {
        String message = plugin.getLanguageConfig(player).getString(key);
        message = textUtils.replaceAndWithParagraph(message == null ? "" : message);
        player.sendMessage(plugin.getPrefix() + message);
    }
}
