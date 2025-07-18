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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(AsyncPlayerPreLoginEvent e) {
        if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
            if (!new BanMuteManager().isExpiredTempBan(Bukkit.getOfflinePlayer(e.getUniqueId()))) {
                final String[] reason = new String[1];
                if (new BanMuteManager().getTempBan(Bukkit.getOfflinePlayer(e.getUniqueId())) != null) {
                    new BanMuteManager().getTempBan(Bukkit.getOfflinePlayer(e.getUniqueId())).forEach((s, s2) -> {
                        try {
                            Bukkit.getServer().getBanList(BanList.Type.NAME).addBan(e.getName(), "§aYou are Banned. Reason:§c " + s2, new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").parse(s), "true");
                            long rest = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").parse(s).getTime() - new Date().getTime();
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(rest);
                            String t = String.format("%tT", calendar.getTimeInMillis()-TimeZone.getDefault().getRawOffset());
                            reason[0] = "§aYou are Banned. Reason:§c " + s2 + " §aExpired at §6: " + s + " §aWait another : §6" + t;
                        } catch (ParseException parseException) {
                            getPlugin().getLogger4J().error(parseException.getMessage(), parseException);
                        }
                    });
                    // Set the login result to kick banned with the reason
                    e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
                    e.setKickMessage(reason[0]);
                }
            } else {
                // Remove expired temp ban
                new BanMuteManager().removeTempBan(Bukkit.getOfflinePlayer(e.getUniqueId()));
                e.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            }
        }
        // Check if player is banned in the Database
        if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
            if (new BanMuteManager().isPermBan(Bukkit.getOfflinePlayer(e.getUniqueId()))) {
                e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                e.setKickMessage(ChatColor.RED + "You are Banned while " + ChatColor.GOLD + new BanMuteManager().getPermBanReason(Bukkit.getOfflinePlayer(e.getUniqueId())));
            }
        } else {
            // Check if player is banned in the BanFileManager
            if (BanFileManager.cfg.getBoolean(e.getName() + ".isBanned")) {
                e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                e.setKickMessage(ChatColor.RED + "You are Banned while " + ChatColor.GOLD + BanFileManager.cfg.getString(e.getName() + ".reason"));
            }
        }
    }
}
