package ch.framedev.essentialsmini.commands.playercommands;



/*
 * ch.framedev.essentialsmini.commands.playercommands
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 25.01.2025 13:31
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerWeatherCMD extends CommandBase {

    public PlayerWeatherCMD(Main plugin) {
        super(plugin,"playerweather", "resetplayerweather");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
            return true;
        }
        if(command.getName().equalsIgnoreCase("playerweather")) {
            if(!player.hasPermission(getPlugin().getPermissionBase() + "playerweather")) {
                player.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }
            if(args.length != 1) {
                player.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getWrongArgs("/playerweather (CLEAR/DOWNFALL)"));
                return true;
            }
           try {
               WeatherType weatherType = WeatherType.valueOf(args[0].toUpperCase());
               player.setPlayerWeather(weatherType);
               player.sendMessage(getPlugin().getPrefix() + "You set your weather to " + args[0].toLowerCase() + ".");
               return true;
           } catch (Exception e) {
               player.sendMessage(getPlugin().getPrefix() + "Please use valid Weather Types (clear/downfall)!");
               return true;
           }
        } else if(command.getName().equalsIgnoreCase("resetplayerweather")) {
            if(!player.hasPermission(getPlugin().getPermissionBase() + "playerweather")) {
                player.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }
            player.resetPlayerWeather();
            player.sendMessage(getPlugin().getPrefix() + "Your player weather has been reset.");
            return true;
        }
        return super.onCommand(sender, command, label, args);
    }
}
