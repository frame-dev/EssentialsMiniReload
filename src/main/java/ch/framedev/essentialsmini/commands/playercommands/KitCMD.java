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

import java.text.SimpleDateFormat;
import java.util.*;

public class KitCMD extends CommandBase {

    private final Main plugin;
    public final HashMap<String, HashMap<String, Cooldown>> cooldowns = new HashMap<>();
    public final HashMap<String, Cooldown> coo = new HashMap<>();
    private final boolean eco;

    public KitCMD(Main plugin) {
        super(plugin, "kits", "createkit");
        this.plugin = plugin;
        this.eco = plugin.getVaultManager() != null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player p) {
            if (command.getName().equalsIgnoreCase("kits")) {
                if (args.length != 0) {
                    String name = args[0];
                    if (p.hasPermission(plugin.getPermissionBase() + "kits." + name)) {
                        if (args.length == 1) {
                            if (KitManager.getCustomConfig().contains("Items." + name)) {
                                KitManager kit = new KitManager();
                                if (kit.getCost(name) == 0) {
                                    if (kit.getCooldown(name) == 0) {
                                        kit.loadKits(name, p);
                                    } else {
                                        if (cooldowns.containsKey(sender.getName())) {
                                            if (cooldowns.get(sender.getName()).containsKey(name))
                                                if (!cooldowns.get(sender.getName()).get(name).check()) {
                                                    long secondsLeft = cooldowns.get(sender.getName()).get(name).getSecondsLeft();
                                                    long millis = cooldowns.get(sender.getName()).get(name).getMilliSeconds();
                                                    String format = new SimpleDateFormat("mm:ss").format(new Date(millis));
                                                    if (secondsLeft > 0) {
                                                        // Still cooling down
                                                        sender.sendMessage("§cYou cant use that commands for another " + format + "!");
                                                        return true;
                                                    }
                                                }
                                        }
                                        // No cooldown found or cooldown has expired, save new cooldown
                                        if (cooldowns.containsKey(sender.getName())) {
                                            cooldowns.get(sender.getName()).remove(name);

                                            if (cooldowns.get(sender.getName()).isEmpty()) {
                                                cooldowns.remove(sender.getName());
                                            }
                                        }
                                        coo.put(name,
                                                new Cooldown(kit.getCooldown(name), System.currentTimeMillis()));
                                        cooldowns.put(sender.getName(), coo);
                                        kit.loadKits(name, p);
                                        return true;
                                    }
                                } else {
                                    if (eco) {
                                        if (kit.getCooldown(name) == 0) {
                                            if (!plugin.getVaultManager().getEconomy().has(p, kit.getCost(name))) {
                                                String notEnough = plugin.getLanguageConfig(sender).getString(Variables.MONEY_MESSAGE + ".MSG.NotEnough");
                                                notEnough = new TextUtils().replaceAndWithParagraph(notEnough);
                                                notEnough = ReplaceCharConfig.replaceObjectWithData(notEnough, "%Money%", plugin.getVaultManager().getEco().getBalance(p) + plugin.getCurrencySymbol());
                                                p.sendMessage(plugin.getPrefix() + notEnough);
                                                return true;
                                            }
                                            plugin.getVaultManager().getEconomy().withdrawPlayer(p, kit.getCost(name));
                                            kit.loadKits(name, p);
                                        } else {
                                            if (cooldowns.containsKey(sender.getName())) {
                                                if (cooldowns.get(sender.getName()).containsKey(name))
                                                    if (!cooldowns.get(sender.getName()).get(name).check()) {
                                                        long secondsLeft = cooldowns.get(sender.getName()).get(name).getSecondsLeft();
                                                        long millis = cooldowns.get(sender.getName()).get(name).getMilliSeconds();
                                                        String format = new SimpleDateFormat("mm:ss").format(new Date(millis));
                                                        if (secondsLeft > 0) {
                                                            // Still cooling down
                                                            sender.sendMessage("§cYou cant use that commands for another " + format + "!");
                                                            return true;
                                                        }
                                                    }
                                            }
                                            if (!plugin.getVaultManager().getEconomy().has(p, kit.getCost(name))) {
                                                String notEnough = plugin.getLanguageConfig(sender).getString(Variables.MONEY_MESSAGE + ".MSG.NotEnough");
                                                notEnough = new TextUtils().replaceAndWithParagraph(notEnough);
                                                notEnough = ReplaceCharConfig.replaceObjectWithData(notEnough, "%Money%", plugin.getVaultManager().getEco().getBalance(p) + plugin.getCurrencySymbol());
                                                p.sendMessage(plugin.getPrefix() + notEnough);
                                                return true;
                                            }
                                            plugin.getVaultManager().getEconomy().withdrawPlayer(p, kit.getCost(name));
                                            // No cooldown found or cooldown has expired, save new cooldown
                                            if (cooldowns.containsKey(sender.getName())) {
                                                cooldowns.get(sender.getName()).remove(name);

                                                if (cooldowns.get(sender.getName()).isEmpty()) {
                                                    cooldowns.remove(sender.getName());
                                                }
                                            }
                                            coo.put(name,
                                                    new Cooldown(kit.getCooldown(name), System.currentTimeMillis()));
                                            cooldowns.put(sender.getName(), coo);
                                            kit.loadKits(name, p);
                                            return true;
                                        }
                                    } else {
                                        p.sendMessage(getPrefix() + "§cEconomy not enabled!");
                                    }
                                }
                            } else {
                                p.sendMessage(plugin.getPrefix() + "§cDieses Kit existiert nicht!");
                            }
                        } else {
                            p.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/kits <kitname>"));
                        }
                    } else {
                        p.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    p.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/kits <kitname>"));
                }
            }
            if (command.getName().equalsIgnoreCase("createkit")) {
                if (p.hasPermission(plugin.getPermissionBase() + "createkit")) {
                    if (args.length == 1) {
                        ItemStack[] items = p.getInventory().getContents();
                        new KitManager().createKit(args[0], items);
                        p.sendMessage(plugin.getPrefix() + "§aKit Created §6" + args[0]);
                        p.getInventory().clear();
                    } else if (args.length == 2) {
                        ItemStack[] items = p.getInventory().getContents();
                        new KitManager().createKit(args[0], items, Integer.parseInt(args[1]));
                        p.sendMessage(plugin.getPrefix() + "§aKit Created §6" + args[0]);
                        p.getInventory().clear();
                    } else if (args.length == 3) {
                        ItemStack[] items = p.getInventory().getContents();
                        new KitManager().createKit(args[0], items, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                        p.sendMessage(plugin.getPrefix() + "§aKit Created §6" + args[0]);
                        p.getInventory().clear();
                    } else {
                        p.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/createkit <KitName>"));
                    }
                } else {
                    p.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
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
