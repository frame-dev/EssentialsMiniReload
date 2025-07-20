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
        this.plugin = getPlugin();
    }

    @EventHandler
    public void onDisallowCommand(PlayerCommandSendEvent event) {
        List<String> blockedCommands = new ArrayList<>();
        if (!event.getPlayer().hasPermission("essentialsmini.setspawn")) {
            blockedCommands.add("setspawn");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.fly")) {
            blockedCommands.add("fly");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.invsee")) {
            blockedCommands.add("invsee");
        }
        if (!Main.getInstance().getConfig().getBoolean("Back")) {
            blockedCommands.add("back");
        }
        if (!event.getPlayer().hasPermission("minecraft.command.me")) {
            blockedCommands.add("me");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.me")) {
            blockedCommands.add("me");
        }
        if (!event.getPlayer().hasPermission("bukkit.command.plugins")) {
            blockedCommands.add("pl");
            blockedCommands.add("plugins");
            blockedCommands.add("bukkit:pl");
        }
        if (!event.getPlayer().hasPermission("bukkit.command.help")) {
            blockedCommands.add("/?");
            blockedCommands.add("help");
            blockedCommands.add("bukkit:help");
            blockedCommands.add("bukkit:?");
        }
        if (!event.getPlayer().hasPermission("bukkit.command.version")) {
            blockedCommands.add("version");
        }

        if (!event.getPlayer().hasPermission("essentialsmini.vanish")) {
            blockedCommands.add("v");
            blockedCommands.add("vanish");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.setwarp")) {
            blockedCommands.add("setwarp");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.warp")) {
            blockedCommands.add("warp");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.warps")) {
            blockedCommands.add("warps");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.saveinventory")) {
            blockedCommands.add("saveinventory");
        }
        if (!Main.getInstance().getConfig().getBoolean("SaveInventory")) {
            blockedCommands.add("saveinventory");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.sleep")) {
            blockedCommands.add("sleep");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.getlocation")) {
            blockedCommands.add("getlocation");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.playerdata")) {
            blockedCommands.add("playerdata");
            blockedCommands.add("pldata");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "god") || !event.getPlayer().hasPermission(plugin.getPermissionBase() + "godmode")) {
            blockedCommands.add("godmode");
            blockedCommands.add("god");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.deletehome.others")) {
            blockedCommands.add("delotherhome");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.killall")) {
            blockedCommands.add("killall");
        }
        if (!event.getPlayer().hasPermission(Main.getInstance().getPermissionBase() + "suicid")) {
            blockedCommands.add("suicid");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "day")) {
            blockedCommands.add("day");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "rain")) {
            blockedCommands.add("rain");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "thunder")) {
            blockedCommands.add("thunder");
        }
        if (!plugin.getConfig().getBoolean("ShowItem")) {
            blockedCommands.add("showitem");
        }
        if (!plugin.getConfig().getBoolean("ShowCrafting")) {
            blockedCommands.add("showcrafting");
        }
        if (!plugin.getConfig().getBoolean("ShowLocation")) {
            blockedCommands.add("showlocation");
        }
        if (!plugin.getConfig().getBoolean("Position")) {
            blockedCommands.add("position");
            blockedCommands.add("pos");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "showitem")) {
            blockedCommands.add("showitem");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "showcrafting")) {
            blockedCommands.add("showcrafting");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "signitem")) {
            blockedCommands.add("signitem");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "renameitem")) {
            blockedCommands.add("renameitem");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "showlocation")) {
            blockedCommands.add("showlocation");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "position")) {
            blockedCommands.add("position");
            blockedCommands.add("pos");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.repair")) {
            blockedCommands.add("repair");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.heal")) {
            blockedCommands.add("heal");
            blockedCommands.add("healme");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.feed")) {
            blockedCommands.add("feed");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.trash")) {
            blockedCommands.add("trash");
        }
        if (!event.getPlayer().hasPermission("essentialsmini.enderchest")) {
            blockedCommands.add("enderchest");
            blockedCommands.add("ec");
        }
        if (!event.getPlayer().hasPermission(plugin.getVariables().getPermissionBase() + "restart")) {
            blockedCommands.add("srestart");
        }
        if (!event.getPlayer().hasPermission(plugin.getVariables().getPermissionBase() + "worldutils")) {
            blockedCommands.add("worldtp");
            blockedCommands.add("addworld");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "key")) {
            blockedCommands.add("key");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "enchant")) {
            blockedCommands.add("enchant");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "thunder")) {
            blockedCommands.add("thunder");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "summon")) {
            blockedCommands.add("summon");
        }

        if (!event.getPlayer().hasPermission("essentialsmini.gamemode")) {
            blockedCommands.add("gamemode");
            blockedCommands.add("gm");
        }
        if (plugin.getVariables().isOnlineMode()) {
            blockedCommands.add("register");
            blockedCommands.add("login");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "chatclear")) {
            blockedCommands.add("cc");
            blockedCommands.add("clearchat");
            blockedCommands.add("chatclear");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "fuck")) {
            blockedCommands.add("fuck");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "pay")) {
            blockedCommands.add("pay");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "balance")) {
            blockedCommands.add("balance");
            blockedCommands.add("bal");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "eco.set")) {
            blockedCommands.add("eco");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "lightningstrike")) {
            blockedCommands.add("lightningstrike");
            blockedCommands.add("lightning");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "speed"))
            blockedCommands.add("speed");
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "afk"))
            blockedCommands.add("afk");
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "infoeconomy"))
            blockedCommands.add("infoeconomy");
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "item")) {
            blockedCommands.add("item");
            blockedCommands.add("i");
        }

        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "tempban")) {
            blockedCommands.add("tempban");
            blockedCommands.add("removetempban");
        }

        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "mute")) {
            blockedCommands.add("mute");
        }

        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "tempmute")) {
            blockedCommands.add("tempmute");
            blockedCommands.add("removetempmute");
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "muteinfo")) blockedCommands.add("muteinfo");

        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "ban")) blockedCommands.add("eban");
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "unban")) blockedCommands.add("eunban");

        if (!plugin.getConfig().getBoolean("HomeTP")) {
            blockedCommands.add("sethome");
            blockedCommands.add("home");
            blockedCommands.add("delhome");
            blockedCommands.add("delotherhomes");
            blockedCommands.add("homegui");
        }

        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "book")) {
            blockedCommands.add("bock");
            blockedCommands.add("copybook");
        }

        // Disable TabCompleter
        if (plugin.getConfig().getBoolean("DisableTabComplete", false))
            if (!event.getPlayer().hasPermission("essentialsmini.tabcomplete")) {
                blockedCommands.addAll(event.getCommands());
            }

        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "xp")) {
            blockedCommands.add("xp");
            blockedCommands.add("exp");
            blockedCommands.add("experience");
        }

        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + ".globalmute")) {
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
        if (!event.getPlayer().hasPermission("essentialsmini.plugins")) {
            if (event.getMessage().split(" ")[0].equalsIgnoreCase("/pl") || event.getMessage().split(" ")[0].equalsIgnoreCase("/bukkit:pl") || event.getMessage().split(" ")[0].equalsIgnoreCase("/plugins")
                    || event.getMessage().split(" ")[0].equalsIgnoreCase("/bukkit:plugins")) {
                event.getPlayer().sendMessage(ChatColor.WHITE + "Plugins(3): " + ChatColor.GREEN + "Nothing" + ChatColor.WHITE + ", " + ChatColor.GREEN + "too" + ChatColor.WHITE + ", " + ChatColor.GREEN + "see!");
                event.setCancelled(true);
            }
        }
        if (!event.getPlayer().hasPermission("essentialsmini.me")) {
            if (event.getMessage().startsWith("/me") || event.getMessage().startsWith("/bukkit:me") || event.getMessage().startsWith("/minecraft:me")) {
                if (!event.getMessage().equalsIgnoreCase("/pltime") || !event.getMessage().equalsIgnoreCase("/resetpltime")) {
                    String message = plugin.getConfig().getString("NotAllowCommand");
                    if (message == null) {
                        if (event.getPlayer().isOp()) {
                            event.getPlayer().sendMessage("§cCould not find Config Key 'NotAllowCommand' in the config.yml");
                        }
                        return;
                    }
                    if (message.contains("&"))
                        message = message.replace('&', '§');
                    event.getPlayer().sendMessage(message);
                    event.setCancelled(true);
                }
            }
        }
        if (!event.getPlayer().hasPermission(plugin.getPermissionBase() + "fuck")) {
            if (event.getMessage().contains("/fuck") || event.getMessage().contains("/essentialsmini:fuck")) {
                String message = plugin.getConfig().getString("NotAllowCommand");
                if (message == null) {
                    if (event.getPlayer().isOp()) {
                        event.getPlayer().sendMessage("§cCould not find Config Key 'NotAllowCommand' in the config.yml");
                    }
                    return;
                }
                if (message.contains("&"))
                    message = message.replace('&', '§');
                event.getPlayer().sendMessage(message);
                event.setCancelled(true);
            }
        }
        if (event.getMessage().split(" ")[0].equalsIgnoreCase("/?") || event.getMessage().split(" ")[0].equalsIgnoreCase("/help") ||
                event.getMessage().split(" ")[0].equalsIgnoreCase("/bukkit:help") || event.getMessage().split(" ")[0].equalsIgnoreCase("/bukkit:?")) {
            if (!event.getPlayer().hasPermission("essentialsmini.help")) {
                String message = plugin.getConfig().getString("NotAllowCommand");
                if (message == null) {
                    if (event.getPlayer().isOp()) {
                        event.getPlayer().sendMessage("§cCould not find Config Key 'NotAllowCommand' in the config.yml");
                    }
                    return;
                }
                if (message.contains("&"))
                    message = message.replace('&', '§');
                event.getPlayer().sendMessage(message);
                event.setCancelled(true);
            }
        }
        if (!(event.isCancelled())) {
            Player player = event.getPlayer();
            String msg = event.getMessage().split(" ")[0];
            HelpTopic topic = getServer().getHelpMap().getHelpTopic(msg);
            if (topic == null) {
                if (plugin.getLanguageConfig(player).contains("UnknownCommand")) {
                    String notFound = plugin.getLanguageConfig(player).getString("UnknownCommand");
                    if (notFound == null) {
                        if (player.isOp()) {
                            player.sendMessage("§cCould not find Config Key 'UnknownCommand' in the language Config");
                        }
                        return;
                    }
                    notFound = notFound.replace('&', '§');
                    notFound = notFound.replace("%CMD%", msg);
                    player.sendMessage(plugin.getPrefix() + notFound);
                    event.setCancelled(true);
                } else {
                    System.err.println(plugin.getPrefix() + "Cannot found 'UnkownCommand' in messages.yml");
                }
            }
        }
    }
}