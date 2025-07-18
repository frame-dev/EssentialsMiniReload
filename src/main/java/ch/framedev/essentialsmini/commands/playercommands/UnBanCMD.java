package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanFileManager;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class UnBanCMD extends CommandBase {

	public UnBanCMD(Main plugin) {
		super(plugin, "eunban");
	}

	@SuppressWarnings("deprecation")
    @Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if(sender.hasPermission(getPlugin().getPermissionBase() + "unban")) {
			if(args.length == 1) {
                if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
					new BanMuteManager().setPermBan(Bukkit.getOfflinePlayer(args[0]), BanCMD.BanType.HACKING, false);
				} else {
					BanFileManager.unBanPlayer(args[0]);
				}
			} else {
				sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/eunban <Player>"));
			}
		}
		return super.onCommand(sender,cmd,label,args);
	}
	
}
