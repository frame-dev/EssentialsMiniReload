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
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RepairCMD extends CommandBase {

    private static final String COMMAND_NAME = "repair";
    private static final String USAGE = "/repair §cor §6/repair <PlayerName>";
    private static final String PERMISSION_SELF = "repair";
    private static final String PERMISSION_OTHERS = "repair.others";

    private final Main plugin;

    public RepairCMD(Main plugin) {
        super(plugin, COMMAND_NAME);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(COMMAND_NAME)) return false;

        if (args.length == 0) {
            return repairSelf(sender);
        }

        if (args.length == 1) {
            return repairOther(sender, args[0]);
        }

        sendWrongArgs(sender);
        return true;
    }

    private boolean repairSelf(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (!hasPermission(player, PERMISSION_SELF)) return true;

        RepairResult result = repairHeldItem(player);
        sendRepairMessage(player, player, result, false);
        return true;
    }

    private boolean repairOther(CommandSender sender, String targetName) {
        if (!hasPermission(sender, PERMISSION_OTHERS)) return true;

        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            sendPlayerNotOnline(sender, targetName);
            return true;
        }

        RepairResult result = repairHeldItem(target);
        if (result == RepairResult.REPAIRED && shouldNotifyTarget(sender)) {
            sendRepairMessage(target, target, result, false);
        }
        sendRepairMessage(sender, target, result, true);
        return true;
    }

    private RepairResult repairHeldItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return RepairResult.AIR;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return RepairResult.IRREPARABLE;
        }

        if (!damageable.hasDamage()) {
            return RepairResult.NOT_DAMAGED;
        }

        damageable.setDamage(0);
        item.setItemMeta(damageable);
        return RepairResult.REPAIRED;
    }

    private void sendRepairMessage(CommandSender receiver, Player target, RepairResult result, boolean otherMessage) {
        ItemStack item = target.getInventory().getItemInMainHand();
        MessageSpec spec = messageSpec(result, otherMessage);
        String message = plugin.getLanguageConfig(receiver).getString(spec.key(), spec.defaultMessage());
        if (message == null) message = spec.defaultMessage();

        message = ReplaceCharConfig.replaceParagraph(message);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", item.getType().name());
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Player%", target.getName());
        send(receiver, message);
    }

    private MessageSpec messageSpec(RepairResult result, boolean otherMessage) {
        return switch (result) {
            case REPAIRED -> otherMessage
                    ? new MessageSpec("Repair.OtherSuccess", "§aThe item §6: %Item% §afrom §6: %Player% §ahas been repaired!")
                    : new MessageSpec("Repair.Success", "§aThe item §6: %Item% §ahas been repaired!");
            case NOT_DAMAGED -> otherMessage
                    ? new MessageSpec("Repair.OtherFailed", "§cThe item §6: %Item% §cfrom §6: %Player% §cdoesn't have to be repaired!")
                    : new MessageSpec("Repair.Failed", "§cThe item §6: %Item% §cdoesn't need to be repaired!");
            case IRREPARABLE -> new MessageSpec("Repair.Irreparable", "§cThe Item §6: %Item% §cfrom §6: %Player% §ccan't be repaired");
            case AIR -> new MessageSpec("Repair.AirRepair", "§cAir can't be repaired!");
        };
    }

    private boolean shouldNotifyTarget(CommandSender sender) {
        List<String> silent = Main.getSilent();
        return silent == null || !silent.contains(sender.getName());
    }

    private boolean hasPermission(CommandSender sender, String permissionSuffix) {
        if (sender.hasPermission(plugin.getPermissionBase() + permissionSuffix)) return true;

        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        send(sender, plugin.getOnlyPlayer(null));
        return null;
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

    private enum RepairResult {
        REPAIRED,
        NOT_DAMAGED,
        IRREPARABLE,
        AIR
    }

    private record MessageSpec(String key, String defaultMessage) {
    }
}
