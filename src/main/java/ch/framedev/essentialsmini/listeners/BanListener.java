package ch.framedev.essentialsmini.listeners;

import ch.framedev.essentialsmini.abstracts.ListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanFileManager;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class BanListener extends ListenerBase {

    public BanListener(Main plugin) {
        super(plugin);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(AsyncPlayerPreLoginEvent e) {
        if (e == null) return;
        boolean useDb = getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB();
        var player = Bukkit.getOfflinePlayer(e.getUniqueId());
        String playerName = e.getName();
        BanMuteManager manager = new BanMuteManager();

        if (useDb) {
            var tempBan = manager.getTempBan(player);
            if (tempBan != null) {
                boolean expired = manager.isExpiredTempBan(player);
                if (!expired) {
                    final String[] reason = new String[1];
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss");
                    tempBan.forEach((expiresAt, banReason) -> {
                        try {
                            Date expires = formatter.parse(expiresAt);
                            if (expires != null) {
                                String safeReason = banReason != null ? banReason : "Unknown";
                                Bukkit.getServer().getBanList(BanList.Type.NAME)
                                        .addBan(playerName, "§aYou are Banned. Reason:§c " + safeReason, expires, "true");
                                long rest = expires.getTime() - System.currentTimeMillis();
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTimeInMillis(rest);
                                String t = String.format("%tT", calendar.getTimeInMillis() - TimeZone.getDefault().getRawOffset());
                                reason[0] = "§aYou are Banned. Reason:§c " + safeReason + " §aExpired at §6: " + expiresAt + " §aWait another : §6" + t;
                            }
                        } catch (ParseException parseException) {
                            getPlugin().getLogger4J().error(parseException.getMessage(), parseException);
                        }
                    });
                    e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
                    e.setKickMessage(reason[0] != null ? reason[0] : "§cYou are temporarily banned.");
                    return;
                }
                manager.removeTempBan(player);
                e.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            }
        }

        if (useDb) {
            if (manager.isPermBan(player)) {
                e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                e.setKickMessage(ChatColor.RED + "You are Banned while " + ChatColor.GOLD + manager.getPermBanReason(player));
            }
        } else {
            if (BanFileManager.cfg.getBoolean(e.getName() + ".isBanned")) {
                e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                e.setKickMessage(ChatColor.RED + "You are Banned while " + ChatColor.GOLD + BanFileManager.cfg.getString(e.getName() + ".reason"));
            }
        }
    }
}
