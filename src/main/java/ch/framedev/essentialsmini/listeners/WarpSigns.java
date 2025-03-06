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
        if (line == null) return;
        if (line1 == null) return;
        if (line.equalsIgnoreCase("warp")) {
            if (event.getPlayer().hasPermission("essentialsmini.signs.create")) {
                boolean success = false;
                for (String location : new LocationsManager().getWarpNames()) {
                    if (location == null) continue;
                    if (line1.equalsIgnoreCase(location)) {
                        event.setLine(0, "§6[§bWARP§6]");
                        event.setLine(1, "§a" + location);
                        if (new LocationsManager().costWarp(location)) {
                            event.setLine(2, "§b" + new LocationsManager().getWarpCost(location) + Main.getInstance().getCurrencySymbolMulti());
                        }
                        success = true;
                    }
                }
                if (!success) {
                    if (getPlugin().getLanguageConfig(null).getString("Warp.NotExist") != null) {
                        String message = getPlugin().getLanguageConfig(null).getString("Warp.NotExist");
                        if (message == null) return;
                        if (message.contains("&"))
                            message = message.replace('&', '§');
                        if (message.contains("%WarpName%"))
                            message = message.replace("%WarpName%", line1);
                        event.getPlayer().sendMessage(getPlugin().getPrefix() + message);
                    } else {
                        event.getPlayer().sendMessage(getPlugin().getPrefix() + "§cDieser Warp existiert nicht!");
                    }
                }
            } else {
                event.getPlayer().sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
            }
        }
    }

    @EventHandler
    public void onClickWarp(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return;
            }
            Block block = event.getClickedBlock();
            if (block == null) return;
            EquipmentSlot equipmentSlot = event.getHand();
            if (equipmentSlot == null) return;
            if (event.getHand().equals(EquipmentSlot.HAND) &&
                    event.getClickedBlock().getState() instanceof Sign) {
                Sign s = (Sign) event.getClickedBlock().getState();
                String[] lines = s.getTargetSide(event.getPlayer()).getLines();
                if (lines[0].equalsIgnoreCase("§6[§bWARP§6]")) {
                    if (event.getPlayer().hasPermission("essentialsmini.signs.use")) {
                        String warpName = lines[1].replace("§a", "");
                        if (lines.length >= 3 && lines[2] != null && !lines[2].isEmpty()) {
                            String cost = lines[2];
                            cost = cost.replace("§b", "").replace(Main.getInstance().getCurrencySymbolMulti(), "");
                            if (!getPlugin().isEconomyEnabled()) {
                                event.getPlayer().sendMessage(getPlugin().getPrefix() + "§cThe economy system is deactivated!");
                                event.setCancelled(true);
                                event.setUseInteractedBlock(Event.Result.DENY);
                                return;
                            }
                            if (Double.parseDouble(cost) > getPlugin().getVaultManager().getEco().getBalance(event.getPlayer())) {
                                event.getPlayer().sendMessage(getPlugin().getPrefix() + "§cYou do not have enough " + Main.getInstance().getCurrencySymbolMulti());
                                event.setCancelled(true);
                                event.setUseInteractedBlock(Event.Result.DENY);
                                return;
                            } else {
                                getPlugin().getVaultManager().getEco().withdrawPlayer(event.getPlayer(), Double.parseDouble(cost));
                                event.getPlayer().sendMessage(getPrefix() + "§aYou have been charged §6" + cost + " §afor the warp cost!");
                            }
                        }
                        Location location = new LocationsManager().getLocation("warps." + warpName);
                        if (event.getPlayer().hasPermission("essentialsmini.warp")) {
                            event.getPlayer().teleport(location);
                            Player player = event.getPlayer();
                            String message = getPlugin().getLanguageConfig(player).getString("Warp.Teleport");
                            if (message == null) return;
                            if (message.contains("&"))
                                message = message.replace('&', '§');
                            if (message.contains("%WarpName%"))
                                message = message.replace("%WarpName%", warpName);
                            player.sendMessage(getPlugin().getPrefix() + message);
                        }
                    } else {
                        event.getPlayer().sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                    }
                    event.setCancelled(true);
                    event.setUseInteractedBlock(Event.Result.DENY);
                }
            }
        }
    }
}
