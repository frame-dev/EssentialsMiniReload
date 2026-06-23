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
import ch.framedev.essentialsmini.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SkullType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class PlayerHeadsCMD extends CommandBase {

    private static final String USAGE = "/playerheads <SpielerName> [TargetPlayer]";
    private static final String NO_HEAD_IN_HAND = "§cKein Player Head in der Hand gefunden!";

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
            meta.setOwningPlayer(resolveOfflinePlayer(name));
            meta.setDisplayName("§a" + name);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (!hasPermission(player)) return true;

        if (args.length == 1) {
            updateHeldHead(player, args[0]);
            return true;
        }

        if (args.length == 2) {
            giveHeadToTarget(player, args[0], args[1]);
            return true;
        }

        sendWrongArgs(player);
        return true;
    }

    private void updateHeldHead(Player player, String ownerName) {
        ItemStack skull = player.getInventory().getItemInMainHand();
        if (skull.getType() != Material.PLAYER_HEAD || !(skull.getItemMeta() instanceof SkullMeta meta)) {
            send(player, NO_HEAD_IN_HAND);
            return;
        }

        meta.setOwningPlayer(resolveOfflinePlayer(ownerName));
        meta.setDisplayName("§a" + ownerName);
        skull.setItemMeta(meta);
        sendReceivedMessage(player, player.getName(), ownerName);
    }

    private void giveHeadToTarget(CommandSender sender, String ownerName, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            sendPlayerNotOnline(sender, targetName);
            return;
        }

        target.getInventory().addItem(createPlayerHead(ownerName));
        sendReceivedMessage(sender, target.getName(), ownerName);
    }

    private ItemStack createPlayerHead(String ownerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(resolveOfflinePlayer(ownerName));
            meta.setDisplayName(ownerName);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private OfflinePlayer resolveOfflinePlayer(String playerName) {
        try {
            return PlayerUtils.getOfflinePlayerByName(playerName);
        } catch (IllegalArgumentException ex) {
            return plugin.getServer().getOfflinePlayer(playerName == null ? "unknown" : playerName);
        }
    }

    private boolean hasPermission(Player player) {
        if (player.hasPermission(plugin.getPermissionBase() + "playerhead")) return true;

        send(player, plugin.getNoPerms(player));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        send(sender, plugin.getOnlyPlayer(null));
        return null;
    }

    private void sendReceivedMessage(CommandSender sender, String targetName, String ownerName) {
        send(sender, "§6" + targetName + " §ahat den Player Head von §6" + ownerName + " §abekommen!");
    }

    private void sendPlayerNotOnline(CommandSender sender, String playerName) {
        String message = plugin.getVariables() == null
                ? "§cPlayer §6" + playerName + " §cis not online!"
                : plugin.getVariables().getPlayerNameNotOnline(playerName);
        send(sender, message);
    }

    private void sendWrongArgs(CommandSender sender) {
        send(sender, plugin.getWrongArgs(sender instanceof Player player ? player : null, USAGE));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }
}
