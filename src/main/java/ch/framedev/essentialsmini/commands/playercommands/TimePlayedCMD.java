package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TimePlayedCMD extends CommandBase {
    public TimePlayedCMD(Main plugin) {
        super(plugin, "playedtime", "timeplayed");
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                long seconds = calculateSeconds(player);
                player.sendMessage(getPrefix() + "§aPlayed: §6" + toFormattedTime(seconds));
            } else {
                sender.sendMessage(getPrefix() + getPlugin().getOnlyPlayer());
            }
            return true;
        }

        if (args.length == 1) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (offline == null || !offline.hasPlayedBefore()) {
                sender.sendMessage(getPrefix() + "§cPlayer §6" + args[0] + " §cis not known.");
                return true;
            }

            Player online = offline.getPlayer();
            if (online != null) {
                long seconds = calculateSeconds(online);
                sender.sendMessage(getPrefix() + "§aPlayer §6" + online.getName() + " §ahas Played: §6" + toFormattedTime(seconds));
            } else {
                // Cannot access statistics for truly offline players via Bukkit API without loading player data.
                sender.sendMessage(getPrefix() + "§cPlayer §6" + (offline.getName() != null ? offline.getName() : args[0])
                        + " §ais offline; playtime data is unavailable.");
            }
            return true;
        }

        return super.onCommand(sender, command, label, args);
    }

    private long calculateSeconds(OfflinePlayer player) {
        if (player instanceof Player p) {
            long playedTicks = p.getStatistic(Statistic.PLAY_ONE_MINUTE); // ticks
            return playedTicks / 20L; // convert ticks to seconds
        }
        return 0L;
    }

    private String toFormattedTime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }
}