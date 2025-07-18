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

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TimePlayedCMD extends CommandBase {
    public TimePlayedCMD(Main plugin) {
        super(plugin, "playedtime", "timeplayed");
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("timeplayed")) {
            if (args.length == 0) {
                if (sender instanceof Player player) {
                    player.sendMessage(getPrefix() + "§aPlayed Hours : §6" + toFormatted(calculateHours(player)));
                } else {
                    sender.sendMessage(getPrefix() + getPlugin().getOnlyPlayer());
                }
                return true;
            } else if (args.length == 1) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                if (offlinePlayer.hasPlayedBefore())
                    sender.sendMessage(getPrefix() + "§aPlayer §6" + offlinePlayer.getName() + " §ahas Played Hours of : §6"
                            + toFormatted(calculateHours(offlinePlayer)));
            }
        }
        return super.onCommand(sender, command, label, args);
    }

    private String toFormatted(double hours) {
        return BigDecimal.valueOf(hours).setScale(3, RoundingMode.HALF_UP).toString();
    }

    private double calculateHours(OfflinePlayer player) {
        if (player.hasPlayedBefore()) {
            long played = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            double seconds = (double) played / 20;
            double minutes = seconds / 60;
            return minutes / 60;
        }
        return 0.0d;
    }
}
