package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 10.08.2020 16:38
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class SleepCMD extends CommandBase {

    private final Main plugin;

    public SleepCMD(Main plugin) {
        super(plugin, "sleep");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            return true;
        }

        if (!sender.hasPermission("essentialsmini.sleep")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return true;
        }

        Material bedMaterial = getConfiguredBedMaterial();
        if (bedMaterial == null) {
            player.sendMessage(plugin.getPrefix() + "§cThis Color doesn't exists!");
            return true;
        }

        Block foot = player.getLocation().getBlock();
        BlockFace facing = player.getFacing();
        Block head = foot.getRelative(facing);
        BlockData originalFoot = foot.getBlockData();
        BlockData originalHead = head.getBlockData();

        setBed(foot, facing, bedMaterial);
        player.sleep(foot.getLocation(), false);

        new BukkitRunnable() {
            @Override
            public void run() {
                foot.setBlockData(originalFoot, false);
                head.setBlockData(originalHead, false);
            }
        }.runTaskLater(plugin, 140L);

        return true;
    }

    public void setBed(Block start, BlockFace facing, Material material) {
        start.setBlockData(Bukkit.createBlockData(material, (data) -> {
            ((Bed) data).setPart(Bed.Part.FOOT);
            ((Bed) data).setFacing(facing);
        }));
        start.getRelative(facing).setBlockData(Bukkit.createBlockData(material, (data) -> {
            ((Bed) data).setPart(Bed.Part.HEAD);
            ((Bed) data).setFacing(facing);
        }));
    }

    private Material getConfiguredBedMaterial() {
        String configuredColor = plugin.getConfig().getString("BedColor", "RED");
        try {
            DyeColor dyeColor = DyeColor.valueOf(configuredColor.toUpperCase(Locale.ROOT));
            return Material.matchMaterial(dyeColor.name() + "_BED");
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return super.onTabComplete(sender, command, label, args);
    }
}
