package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 18.08.2020 19:02
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import static org.bukkit.Material.AIR;

public class RepairCMD extends CommandBase {

    private final Main plugin;

    public RepairCMD(Main plugin) {
        super(plugin, "repair");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("repair")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (player.hasPermission(new Permission("essentialsmini.repair", PermissionDefault.OP))) {
                        if (player.getInventory().getItemInMainHand().getType() != AIR) {
                            ItemStack item = player.getInventory().getItemInMainHand();
                            if (item.hasItemMeta()) {
                                if (item.getItemMeta() instanceof Damageable) {
                                    Damageable damageable = (Damageable) item.getItemMeta();
                                    if (damageable.hasDamage()) {
                                        damageable.setDamage(0);
                                        item.setItemMeta((ItemMeta) damageable);
                                        String repair = plugin.getLanguageConfig(player).getString("Repair.Success");
                                        repair = ReplaceCharConfig.replaceParagraph(repair);
                                        repair = ReplaceCharConfig.replaceObjectWithData(repair, "%Item%", item.getType().name());
                                        player.sendMessage(plugin.getPrefix() + repair);
                                    } else {
                                        String repair = plugin.getLanguageConfig(player).getString("Repair.Failed");
                                        repair = ReplaceCharConfig.replaceParagraph(repair);
                                        repair = ReplaceCharConfig.replaceObjectWithData(repair, "%Item%", item.getType().name());
                                        player.sendMessage(plugin.getPrefix() + repair);
                                    }
                                }
                            }
                        } else {
                            String repair = plugin.getLanguageConfig(player).getString("Repair.AirRepair");
                            repair = ReplaceCharConfig.replaceParagraph(repair);
                            sender.sendMessage(plugin.getPrefix() + repair);
                        }
                    } else {
                        player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            } else if (args.length == 1) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[0]));
                    return true;
                }
                if (sender.hasPermission(new Permission("essentialsmini.repair.others", PermissionDefault.OP))) {
                    if (player.getInventory().getItemInMainHand().getType() != AIR) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item.hasItemMeta()) {
                            if (item.getItemMeta() instanceof Damageable) {
                                Damageable damageable = (Damageable) item.getItemMeta();
                                if (damageable.hasDamage()) {
                                    damageable.setDamage(0);
                                    item.setItemMeta((ItemMeta) damageable);
                                    if (!Main.getSilent().contains(sender.getName())) {
                                        String repair = plugin.getLanguageConfig(player).getString("Repair.Success");
                                        repair = ReplaceCharConfig.replaceParagraph(repair);
                                        repair = ReplaceCharConfig.replaceObjectWithData(repair, "%Item%", item.getType().name());
                                        player.sendMessage(plugin.getPrefix() + repair);
                                    }
                                    String repairOther = plugin.getLanguageConfig(player).getString("Repair.OtherSuccess");
                                    repairOther = ReplaceCharConfig.replaceParagraph(repairOther);
                                    repairOther = ReplaceCharConfig.replaceObjectWithData(repairOther, "%Item%", item.getType().name());
                                    repairOther = ReplaceCharConfig.replaceObjectWithData(repairOther, "%Player%", player.getName());
                                    sender.sendMessage(plugin.getPrefix() + repairOther);
                                } else {
                                    String repair = plugin.getLanguageConfig(player).getString("Repair.OtherFailed");
                                    repair = ReplaceCharConfig.replaceParagraph(repair);
                                    repair = ReplaceCharConfig.replaceObjectWithData(repair, "%Item%", item.getType().name());
                                    repair = ReplaceCharConfig.replaceObjectWithData(repair, "%Player%", player.getName());
                                    sender.sendMessage(plugin.getPrefix() + repair);
                                }
                            } else {
                                String notAble = plugin.getLanguageConfig(player).getString("Repair.Irreparable");
                                notAble = ReplaceCharConfig.replaceParagraph(notAble);
                                notAble = ReplaceCharConfig.replaceObjectWithData(notAble, "%Item%", item.getType().name());
                                notAble = ReplaceCharConfig.replaceObjectWithData(notAble, "%Player%", player.getName());
                                sender.sendMessage(plugin.getPrefix() + notAble);
                            }
                        } else {
                            String notAble = plugin.getLanguageConfig(player).getString("Repair.Irreparable");
                            notAble = ReplaceCharConfig.replaceParagraph(notAble);
                            notAble = ReplaceCharConfig.replaceObjectWithData(notAble, "%Item%", item.getType().name());
                            notAble = ReplaceCharConfig.replaceObjectWithData(notAble, "%Player%", player.getName());
                            sender.sendMessage(plugin.getPrefix() + notAble);
                        }
                    } else {
                        String repair = plugin.getLanguageConfig(player).getString("Repair.AirRepair");
                        repair = ReplaceCharConfig.replaceParagraph(repair);
                        sender.sendMessage(plugin.getPrefix() + repair);
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/repair §cor §6/repair <PlayerName>"));
            }
        }
        return false;
    }
}
