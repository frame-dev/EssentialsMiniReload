package ch.framedev.essentialsmini.listeners;

import ch.framedev.essentialsmini.abstracts.ListenerBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.help.HelpTopic;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 13.07.2020 13:00
 */
public class DisallowCommands extends ListenerBase {

    private final Main plugin;

    public DisallowCommands(Main plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    private String getNotAllowMessage(Player player) {
        String message = plugin.getConfig().getString("NotAllowCommand");
        if (message == null) {
            if (player != null && player.isOp()) {
                player.sendMessage("§cCould not find Config Key 'NotAllowCommand' in the config.yml");
            }
            return null;
        }
        return message.contains("&") ? message.replace('&', '§') : message;
    }

    @EventHandler
    public void onDisallowCommand(PlayerCommandSendEvent event) {
        if (event == null) return;
        Player player = event.getPlayer();
        List<String> blockedCommands = new ArrayList<>();
        if (!player.hasPermission("essentialsmini.setspawn")) {
            blockedCommands.add("setspawn");
        }
        if (!player.hasPermission("essentialsmini.fly")) {
            blockedCommands.add("fly");
        }
        if (!player.hasPermission("essentialsmini.invsee")) {
            blockedCommands.add("invsee");
        }
        if (!Main.getInstance().getConfig().getBoolean("Back")) {
            blockedCommands.add("back");
        }
        if (!player.hasPermission("minecraft.command.me")) {
            blockedCommands.add("me");
        }
        if (!player.hasPermission("essentialsmini.me")) {
            blockedCommands.add("me");
        }
        if (!player.hasPermission("bukkit.command.plugins")) {
            blockedCommands.add("pl");
            blockedCommands.add("plugins");
            blockedCommands.add("bukkit:pl");
        }
        if (!player.hasPermission("bukkit.command.help")) {
            blockedCommands.add("/?");
            blockedCommands.add("help");
            blockedCommands.add("bukkit:help");
            blockedCommands.add("bukkit:?");
        }
        if (!player.hasPermission("bukkit.command.version")) {
            blockedCommands.add("version");
        }

        if (!player.hasPermission("essentialsmini.vanish")) {
            blockedCommands.add("v");
            blockedCommands.add("vanish");
        }
        if (!player.hasPermission("essentialsmini.setwarp")) {
            blockedCommands.add("setwarp");
        }
        if (!player.hasPermission("essentialsmini.warp")) {
            blockedCommands.add("warp");
        }
        if (!player.hasPermission("essentialsmini.warps")) {
            blockedCommands.add("warps");
        }
        if (!player.hasPermission("essentialsmini.sleep")) {
            blockedCommands.add("sleep");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "god") && !player.hasPermission(plugin.getPermissionBase() + "godmode")) {
            blockedCommands.add("godmode");
            blockedCommands.add("god");
        }
        if (!player.hasPermission("essentialsmini.deletehome.others")) {
            blockedCommands.add("delotherhome");
        }
        if (!player.hasPermission("essentialsmini.killall")) {
            blockedCommands.add("killall");
        }
        if (!player.hasPermission(Main.getInstance().getPermissionBase() + "suicid")) {
            blockedCommands.add("suicid");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "day")) {
            blockedCommands.add("day");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "rain")) {
            blockedCommands.add("rain");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "thunder")) {
            blockedCommands.add("thunder");
        }
        if (!player.hasPermission("essentialsmini.repair")) {
            blockedCommands.add("repair");
        }
        if (!player.hasPermission("essentialsmini.heal")) {
            blockedCommands.add("heal");
            blockedCommands.add("healme");
        }
        if (!player.hasPermission("essentialsmini.feed")) {
            blockedCommands.add("feed");
        }
        if (!player.hasPermission("essentialsmini.trash")) {
            blockedCommands.add("trash");
        }
        if (!player.hasPermission("essentialsmini.enderchest")) {
            blockedCommands.add("enderchest");
            blockedCommands.add("ec");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "key")) {
            blockedCommands.add("key");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "enchant")) {
            blockedCommands.add("enchant");
        }

        if (!player.hasPermission("essentialsmini.gamemode")) {
            blockedCommands.add("gamemode");
            blockedCommands.add("gm");
        }
        if (plugin.getVariables().isOnlineMode()) {
            blockedCommands.add("register");
            blockedCommands.add("login");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "chatclear")) {
            blockedCommands.add("cc");
            blockedCommands.add("clearchat");
            blockedCommands.add("chatclear");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "pay")) {
            blockedCommands.add("pay");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "balance")) {
            blockedCommands.add("balance");
            blockedCommands.add("bal");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "eco.set")) {
            blockedCommands.add("eco");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "lightningstrike")) {
            blockedCommands.add("lightningstrike");
            blockedCommands.add("lightning");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "speed"))
            blockedCommands.add("speed");
        if (!player.hasPermission(plugin.getPermissionBase() + "afk"))
            blockedCommands.add("afk");
        if (!player.hasPermission(plugin.getPermissionBase() + "infoeconomy"))
            blockedCommands.add("infoeconomy");
        if (!player.hasPermission(plugin.getPermissionBase() + "item")) {
            blockedCommands.add("item");
            blockedCommands.add("i");
        }

        if (!player.hasPermission(plugin.getPermissionBase() + "tempban")) {
            blockedCommands.add("tempban");
            blockedCommands.add("removetempban");
        }

        if (!player.hasPermission(plugin.getPermissionBase() + "mute")) {
            blockedCommands.add("mute");
        }

        if (!player.hasPermission(plugin.getPermissionBase() + "tempmute")) {
            blockedCommands.add("tempmute");
            blockedCommands.add("removetempmute");
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "muteinfo")) blockedCommands.add("muteinfo");

        if (!player.hasPermission(plugin.getPermissionBase() + "ban")) blockedCommands.add("eban");
        if (!player.hasPermission(plugin.getPermissionBase() + "unban")) blockedCommands.add("eunban");

        if (!plugin.getConfig().getBoolean("HomeTP")) {
            blockedCommands.add("sethome");
            blockedCommands.add("home");
            blockedCommands.add("delhome");
            blockedCommands.add("delotherhomes");
            blockedCommands.add("homegui");
        }

        if (!player.hasPermission(plugin.getPermissionBase() + "book")) {
            blockedCommands.add("bock");
            blockedCommands.add("copybook");
        }

        // Disable TabCompleter
        if (plugin.getConfig().getBoolean("DisableTabComplete", false))
            if (!player.hasPermission("essentialsmini.tabcomplete")) {
                blockedCommands.addAll(event.getCommands());
            }

        if (!player.hasPermission(plugin.getPermissionBase() + "xp")) {
            blockedCommands.add("xp");
            blockedCommands.add("exp");
            blockedCommands.add("experience");
        }

        if (!player.hasPermission(plugin.getPermissionBase() + "globalmute")) {
            blockedCommands.add("globalmute");
            blockedCommands.add("glmute");
            blockedCommands.add("gmute");
        }
        if (!event.getCommands().isEmpty()) {
            event.getCommands().removeAll(blockedCommands);
            event.getCommands().removeIf(string -> string.contains(":"));
        }
    }

    @EventHandler
    public void onSendCommand(PlayerCommandPreprocessEvent event) {
        if (event == null) return;
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message.isBlank()) return;

        String baseCmd = message.split(" ", 2)[0];
        String baseCmdLower = baseCmd.toLowerCase();

        if (!player.hasPermission("essentialsmini.plugins")) {
            if (baseCmdLower.equals("/pl") || baseCmdLower.equals("/bukkit:pl") || baseCmdLower.equals("/plugins")
                    || baseCmdLower.equals("/bukkit:plugins")) {
                player.sendMessage(ChatColor.WHITE + "Plugins(3): " + ChatColor.GREEN + "Nothing" + ChatColor.WHITE + ", " + ChatColor.GREEN + "too" + ChatColor.WHITE + ", " + ChatColor.GREEN + "see!");
                event.setCancelled(true);
            }
        }
        if (!player.hasPermission("essentialsmini.me")) {
            if (baseCmdLower.equals("/me") || baseCmdLower.equals("/bukkit:me") || baseCmdLower.equals("/minecraft:me")) {
                String notAllow = getNotAllowMessage(player);
                if (notAllow == null) return;
                player.sendMessage(notAllow);
                event.setCancelled(true);
            }
        }
        if (!player.hasPermission(plugin.getPermissionBase() + "fuck")) {
            if (message.contains("/fuck") || message.contains("/essentialsmini:fuck")) {
                String notAllow = getNotAllowMessage(player);
                if (notAllow == null) return;
                player.sendMessage(notAllow);
                event.setCancelled(true);
            }
        }
        if (baseCmdLower.equals("/?") || baseCmdLower.equals("/help") ||
                baseCmdLower.equals("/bukkit:help") || baseCmdLower.equals("/bukkit:?")) {
            if (!player.hasPermission("essentialsmini.help")) {
                String notAllow = getNotAllowMessage(player);
                if (notAllow == null) return;
                player.sendMessage(notAllow);
                event.setCancelled(true);
            }
        }
        if (!event.isCancelled()) {
            HelpTopic topic = getServer().getHelpMap().getHelpTopic(baseCmd);
            if (topic == null && plugin.getLanguageConfig(player) != null) {
                if (plugin.getLanguageConfig(player).contains("UnknownCommand")) {
                    String notFound = plugin.getLanguageConfig(player).getString("UnknownCommand");
                    if (notFound == null) {
                        if (player.isOp()) {
                            player.sendMessage("§cCould not find Config Key 'UnknownCommand' in the language Config");
                        }
                        return;
                    }
                    notFound = notFound.replace('&', '§');
                    notFound = notFound.replace("%CMD%", baseCmd);
                    player.sendMessage(plugin.getPrefix() + notFound);
                    event.setCancelled(true);
                } else {
                    System.err.println(plugin.getPrefix() + "Cannot found 'UnkownCommand' in messages.yml");
                }
            }
        }
    }
}