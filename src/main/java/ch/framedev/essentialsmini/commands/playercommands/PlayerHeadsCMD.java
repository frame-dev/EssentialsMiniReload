package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 12.08.2020 23:35
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.SkullBuilder;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public class PlayerHeadsCMD extends CommandBase {

    private final Main plugin;

    public PlayerHeadsCMD(Main plugin) {
        super(plugin, "playerheads");
        this.plugin = plugin;
    }

    @Deprecated
    public ItemStack ItemStackSkull(String name) {
        ItemStack skull = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
            meta.setDisplayName("§a" + name);
        }
        skull.setItemMeta(meta);
        return skull;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission(plugin.getPermissionBase() + "playerhead")) {
                if (args.length == 1) {
                    if (((Player) sender).getInventory().getItemInMainHand().getType() == Material.PLAYER_HEAD) {
                        ItemStack skull = ((Player) sender).getInventory().getItemInMainHand();
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        if(meta == null) {
                            sender.sendMessage(plugin.getPrefix() + "§cKein Player Head in der Hand gefunden!");
                            return true;
                        }
                        meta.setOwningPlayer(PlayerUtils.getOfflinePlayerByName(args[0]));
                        meta.setDisplayName("§a" + args[0]);
                        skull.setItemMeta(meta);
                        sender.sendMessage(plugin.getPrefix() + "§aDu hast den Player Head von §6" + args[0] + " §abekommen!");
                    } else {
                        sender.sendMessage(plugin.getPrefix() + "§cKein Player Head in der Hand gefunden!");
                    }
                } else if (args.length == 2) {
                    Player player = Bukkit.getPlayer(args[1]);
                    if (player != null) {
                        player.getInventory().addItem(new SkullBuilder(args[0]).setDisplayName(args[0]).create());
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                            return true;
                        }
                        sender.sendMessage(plugin.getPrefix() + "§6" + target.getName() + " §ahat den Player Head von §6" + args[0] + " §abekommen!");
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/playerheads <SpielerName>"));
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
        }
        return false;
    }
}
