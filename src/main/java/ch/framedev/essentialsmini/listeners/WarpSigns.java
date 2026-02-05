package ch.framedev.essentialsmini.listeners;

import ch.framedev.essentialsmini.abstracts.ListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.LocationsManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * / This Plugin was Created by FrameDev
 * / Package : de.framedev.essentialsmini.listeners
 * / ClassName WarpSigns
 * / Date: 12.07.21
 * / Project: EssentialsMini
 * / Copyrighted by FrameDev
 */

public class WarpSigns extends ListenerBase {
    public WarpSigns(Main plugin) {
        super(plugin);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String line = event.getLine(0);
        String line1 = event.getLine(1);

        // Validate input
        if (line == null || line1 == null || line1.trim().isEmpty()) {
            return;
        }

        if (!line.equalsIgnoreCase("warp")) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("essentialsmini.signs.create")) {
            player.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return;
        }

        // Create single LocationsManager instance
        LocationsManager locationsManager = new LocationsManager();
        boolean warpFound = false;

        for (String warpName : locationsManager.getWarpNames()) {
            if (warpName == null || warpName.trim().isEmpty()) {
                continue;
            }

            if (line1.equalsIgnoreCase(warpName)) {
                event.setLine(0, "§6[§bWARP§6]");
                event.setLine(1, "§a" + warpName);

                if (locationsManager.costWarp(warpName)) {
                    double cost = locationsManager.getWarpCost(warpName);
                    event.setLine(2, "§b" + cost + Main.getInstance().getCurrencySymbolMulti());
                }

                warpFound = true;
                break; // Exit loop once warp is found
            }
        }

        if (!warpFound) {
            String message = getPlugin().getLanguageConfig(player).getString("Warp.NotExist");
            if (message != null && !message.isEmpty()) {
                message = message.replace('&', '§').replace("%WarpName%", line1);
                player.sendMessage(getPlugin().getPrefix() + message);
            } else {
                player.sendMessage(getPlugin().getPrefix() + "§cThis warp does not exist!");
            }
        }
    }

    @EventHandler
    public void onClickWarp(PlayerInteractEvent event) {
        // Early returns for invalid states
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }

        Player player = event.getPlayer();

        String[] lines = sign.getTargetSide(player).getLines();
        if (lines == null || lines.length == 0 || !lines[0].equalsIgnoreCase("§6[§bWARP§6]")) {
            return;
        }

        // Cancel event early to prevent default behavior
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);

        // Check permission
        if (!player.hasPermission("essentialsmini.signs.use")) {
            player.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return;
        }

        // Extract warp name
        if (lines.length < 2 || lines[1] == null || lines[1].isEmpty()) {
            player.sendMessage(getPlugin().getPrefix() + "§cInvalid warp sign format!");
            return;
        }

        String warpName = lines[1].replace("§a", "").trim();
        if (warpName.isEmpty()) {
            player.sendMessage(getPlugin().getPrefix() + "§cInvalid warp name!");
            return;
        }

        // Handle warp cost if present
        if (lines.length >= 3 && lines[2] != null && !lines[2].isEmpty()) {
            if (!processWarpCost(player, lines[2])) {
                return; // Cost processing failed, message already sent
            }
        }

        // Check warp permission
        if (!player.hasPermission("essentialsmini.warp")) {
            player.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            return;
        }

        // Get and validate location
        LocationsManager locationsManager = new LocationsManager();
        Location location = locationsManager.getLocation("warps." + warpName);

        if (location == null) {
            player.sendMessage(getPlugin().getPrefix() + "§cWarp location not found!");
            return;
        }

        // Teleport player
        player.teleport(location);

        // Send success message
        String message = getPlugin().getLanguageConfig(player).getString("Warp.Teleport");
        if (message != null && !message.isEmpty()) {
            message = message.replace('&', '§').replace("%WarpName%", warpName);
            player.sendMessage(getPlugin().getPrefix() + message);
        }
    }

    /**
     * Process warp cost and withdraw money if applicable
     * @param player the player
     * @param costLine the cost line from the sign
     * @return true if cost was successfully processed, false otherwise
     */
    private boolean processWarpCost(Player player, String costLine) {
        if (!getPlugin().isEconomyEnabled()) {
            player.sendMessage(getPlugin().getPrefix() + "§cThe economy system is deactivated!");
            return false;
        }

        // Parse cost
        String costString = costLine.replace("§b", "")
                                    .replace(Main.getInstance().getCurrencySymbolMulti(), "")
                                    .trim();

        if (costString.isEmpty()) {
            return true; // No cost specified
        }

        double cost;
        try {
            cost = Double.parseDouble(costString);
        } catch (NumberFormatException e) {
            player.sendMessage(getPlugin().getPrefix() + "§cInvalid cost format on sign!");
            return false;
        }

        if (cost < 0) {
            player.sendMessage(getPlugin().getPrefix() + "§cInvalid cost amount!");
            return false;
        }

        // Check if player has enough money
        double balance = getPlugin().getVaultManager().getEco().getBalance(player);
        if (balance < cost) {
            player.sendMessage(getPlugin().getPrefix() + "§cYou do not have enough " + Main.getInstance().getCurrencySymbolMulti());
            return false;
        }

        // Withdraw money
        getPlugin().getVaultManager().getEco().withdrawPlayer(player, cost);
        player.sendMessage(getPlugin().getPrefix() + "§aYou have been charged §6" + cost + " §afor the warp cost!");

        return true;
    }
}
