package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.KitManager;
import ch.framedev.essentialsmini.utils.Cooldown;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.Variables;
import org.bukkit.command.Command;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class KitCMD extends CommandBase {

    private final Main plugin;
    // Map<playerName, Map<kitName, Cooldown>>
    public final HashMap<String, HashMap<String, Cooldown>> cooldowns = new HashMap<>();
    private final boolean eco;

    public KitCMD(Main plugin) {
        super(plugin, "kits", "createkit");
        this.plugin = plugin;
        this.eco = plugin.getVaultManager() != null;
    }

    // Get (or create) per-player cooldown map
    private HashMap<String, Cooldown> getUserCooldownMap(String playerName) {
        return cooldowns.computeIfAbsent(playerName, k -> new HashMap<>());
    }

    // Check if player is on cooldown for kit. If yes, send formatted message and return true.
    private boolean checkAndNotifyCooldown(Player p, String kitName) {
        HashMap<String, Cooldown> userMap = cooldowns.get(p.getName());
        if (userMap == null) return false;
        Cooldown cd = userMap.get(kitName);
        if (cd == null) return false;
        if (!cd.check()) {
            long secondsLeft = cd.getSecondsLeft();
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;
            String format = String.format("%02d:%02d", minutes, seconds);
            p.sendMessage(plugin.getPrefix() + "§cYou can't use that command for another " + format + "!");
            return true;
        }
        return false;
    }

    // Set cooldown for player/kit
    private void setCooldown(Player p, String kitName, int seconds) {
        HashMap<String, Cooldown> userMap = getUserCooldownMap(p.getName());
        userMap.put(kitName, new Cooldown(seconds, System.currentTimeMillis()));
        cooldowns.put(p.getName(), userMap);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player p) {
            if (command.getName().equalsIgnoreCase("kits")) {
                if (args.length != 0) {
                    String name = args[0];
                    if (!p.hasPermission(plugin.getPermissionBase() + "kits." + name)) {
                        p.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                        return true;
                    }
                    if (args.length != 1) {
                        p.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/kits <kitname>"));
                        return true;
                    }
                    if (!KitManager.getCustomConfig().contains("Items." + name)) {
                        p.sendMessage(plugin.getPrefix() + "§cDieses Kit existiert nicht!");
                        return true;
                    }

                    KitManager kit = new KitManager();
                    int cost = kit.getCost(name);
                    int cdSeconds = kit.getCooldown(name);

                    // First handle cooldown checks
                    if (cdSeconds > 0) {
                        if (checkAndNotifyCooldown(p, name)) return true;
                    }

                    // If kit is free
                    if (cost <= 0) {
                        // set cooldown if needed
                        if (cdSeconds > 0) setCooldown(p, name, cdSeconds);
                        kit.loadKits(name, p);
                        return true;
                    }

                    // Paid kit
                    if (!eco) {
                        p.sendMessage(plugin.getPrefix() + "§cEconomy not enabled!");
                        return true;
                    }

                    if (!plugin.getVaultManager().getEconomy().has(p, cost)) {
                        String notEnough = plugin.getLanguageConfig(sender).getString(Variables.MONEY_MESSAGE + ".MSG.NotEnough");
                        notEnough = new TextUtils().replaceAndWithParagraph(notEnough);
                        notEnough = ReplaceCharConfig.replaceObjectWithData(notEnough, "%Money%", plugin.getVaultManager().getEco().getBalance(p) + plugin.getCurrencySymbol());
                        p.sendMessage(plugin.getPrefix() + notEnough);
                        return true;
                    }

                    plugin.getVaultManager().getEconomy().withdrawPlayer(p, cost);
                    if (cdSeconds > 0) setCooldown(p, name, cdSeconds);
                    kit.loadKits(name, p);
                    return true;
                }
            }
            if (command.getName().equalsIgnoreCase("createkit")) {
                if (!p.hasPermission(plugin.getPermissionBase() + "createkit")) {
                    p.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                if (args.length == 1 || args.length == 2 || args.length == 3) {
                    ItemStack[] items = p.getInventory().getContents();
                    try {
                        if (args.length == 1) {
                            new KitManager().createKit(args[0], items);
                        } else if (args.length == 2) {
                            new KitManager().createKit(args[0], items, Integer.parseInt(args[1]));
                        } else {
                            new KitManager().createKit(args[0], items, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                        }
                        p.sendMessage(plugin.getPrefix() + "§aKit Created §6" + args[0]);
                        p.getInventory().clear();
                    } catch (NumberFormatException nfe) {
                        p.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/createkit <KitName> [cost] [cooldown]"));
                    }
                } else {
                    p.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/createkit <KitName> [cost] [cooldown]"));
                }
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kits") && args.length == 1) {
            ArrayList<String> list = new ArrayList<>();
            ConfigurationSection cs = KitManager.getCustomConfig().getConfigurationSection("Items");
            if (cs == null) {
                return list; // No kits available
            }
            for (String s : cs.getKeys(false)) {
                if (sender.hasPermission(plugin.getPermissionBase() + "kits." + s)) {
                    list.add(s);
                }
            }
            return list;
        }
        if (command.getName().equalsIgnoreCase("createkit") && args.length == 1) {
            return Collections.singletonList("Kit_Name");
        }
        return null;
    }
}
