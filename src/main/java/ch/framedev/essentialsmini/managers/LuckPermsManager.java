package ch.framedev.essentialsmini.managers;



/*
 * ch.framedev.essentialsmini.utils
 * =============================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * =============================================
 * This Class was created at 01.02.2025 18:20
 */

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsManager {
    public static boolean hasOfflinePermission(OfflinePlayer offlinePlayer, String permission) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return false;
        }
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if(provider == null) return false;
        LuckPerms luckPerms = provider.getProvider();
        User user = luckPerms.getUserManager().loadUser(offlinePlayer.getUniqueId()).join();

        return user != null && user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
}
