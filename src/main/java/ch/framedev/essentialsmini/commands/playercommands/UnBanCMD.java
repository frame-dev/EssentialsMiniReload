package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanFileManager;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UnBanCMD extends CommandBase {

	private static final String COMMAND_NAME = "eunban";
	private static final String USAGE = "/eunban <Player>";

	public UnBanCMD(Main plugin) {
		super(plugin, COMMAND_NAME);
	}

    @Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase(COMMAND_NAME)) {
			return super.onCommand(sender, cmd, label, args);
		}

		if (!sender.hasPermission(getPlugin().getPermissionBase() + "unban")) {
			send(sender, getPlugin().getNoPerms());
			return true;
		}

		if (args.length != 1 || args[0].isBlank()) {
			send(sender, getPlugin().getWrongArgs(USAGE));
			return true;
		}

		OfflinePlayer target = findKnownPlayer(sender, args[0]);
		if (target == null) {
			return true;
		}

		unban(target);
		sendMessage(sender, "EUnBan.Success", "§6%Player% §ahas been unbanned!",
				"%Player%", target.getName());
		return true;
	}

	private OfflinePlayer findKnownPlayer(CommandSender sender, String playerName) {
		try {
			OfflinePlayer target = PlayerUtils.getOfflinePlayerByName(playerName);
			if (target != null && target.getName() != null) {
				return target;
			}
		} catch (IllegalArgumentException ignored) {
		}

		sendMessage(sender, "EUnBan.Errors.PlayerNotFound", getPlugin().getVariables().getPlayerNameNotOnline(playerName),
				"%Player%", playerName);
		return null;
	}

	private void unban(OfflinePlayer target) {
		String playerName = target.getName();
		if (playerName == null) {
			return;
		}

		if (isDatabaseMode()) {
			new BanMuteManager().setPermBan(target, BanCMD.BanType.HACKING, false);
			return;
		}

		BanFileManager.unBanPlayer(playerName);
	}

	private boolean isDatabaseMode() {
		return getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB();
	}

	private void send(CommandSender sender, String message) {
		sender.sendMessage(getPlugin().getPrefix() + message);
	}

	private void sendMessage(CommandSender sender, String key, String defaultMessage, String... replacements) {
		send(sender, message(sender, key, defaultMessage, replacements));
	}

	private String message(CommandSender receiver, String key, String defaultMessage, String... replacements) {
		String message = getPlugin().getLanguageConfig(receiver).getString(key, defaultMessage);
		if (message == null) message = defaultMessage;
		message = ReplaceCharConfig.replaceParagraph(message);
		for (int i = 0; i + 1 < replacements.length; i += 2) {
			message = ReplaceCharConfig.replaceObjectWithData(message, replacements[i], replacements[i + 1] == null ? "" : replacements[i + 1]);
		}
		return message;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (!command.getName().equalsIgnoreCase(COMMAND_NAME) || args.length != 1) {
			return super.onTabComplete(sender, command, label, args);
		}

		return TabCompleteUtils.matchingStrings(knownPlayerNames(), args[0]);
	}

	private List<String> knownPlayerNames() {
		List<String> names = new ArrayList<>();
		for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
			String name = offlinePlayer.getName();
			if (name != null && !name.isBlank()) {
				names.add(name);
			}
		}
		return names;
	}
	
}
