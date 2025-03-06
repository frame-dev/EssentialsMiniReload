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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SleepCMD extends CommandBase {

    private final Main plugin;

    private final ArrayList<Material> block = new ArrayList<>();

    public SleepCMD(Main plugin) {
        super(plugin, "sleep");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("essentialsmini.sleep")) {
                Player player = (Player) sender;
                Location location = player.getLocation().subtract(0, 0, 0);
                block.add(location.getBlock().getType());
                try {
                    if (plugin.getConfig().getString("BedColor") == null) {
                        player.sendMessage(plugin.getPrefix() + "§cThis Color doesn't exists!");
                        return true;
                    }
                    DyeColor dyeColor = DyeColor.valueOf(Objects.requireNonNull(plugin.getConfig().getString("BedColor")).toUpperCase());
                    setBed(location.getBlock(), BlockFace.NORTH, Material.getMaterial(dyeColor.name().toUpperCase() + "_BED"));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Material material : block) {
                                if (material != Material.AIR) {
                                    location.getBlock().setType(material);
                                }
                            }
                            if (location.getWorld() != null && location.getWorld().getTime() >= 0) {
                                location.getBlock().setType(Material.AIR);
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0, 5);
                    player.sleep(player.getLocation(), false);
                } catch (Exception ignored) {
                    player.sendMessage(plugin.getPrefix() + "§cThis Color doesn't exists!");
                    return true;
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
        }
        return false;
    }

    public void setBed(Block start, BlockFace facing, Material material) {
        for (Bed.Part part : Bed.Part.values()) {
            start.setBlockData(Bukkit.createBlockData(material, (data) -> {
                ((Bed) data).setPart(part);
                ((Bed) data).setFacing(facing);
            }));
            start = start.getRelative(facing.getOppositeFace());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return super.onTabComplete(sender, command, label, args);
    }
}
