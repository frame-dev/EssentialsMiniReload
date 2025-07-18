package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands
 * Date: 24.10.2020
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
public class ClearChatCMD extends CommandBase {

    private final Main plugin;

    public ClearChatCMD(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        setup("chatclear", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission(plugin.getPermissionBase() + "chatclear")) {
            clearChat(sender);
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
        }
        return super.onCommand(sender, command, label, args);
    }

    public void clearChat(CommandSender sender) {
        for (int i = 0; i <= 500; i++) {
            Bukkit.broadcastMessage(" ");
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            String message = plugin.getLanguageConfig(player).getString("ChatClear");
            if (message != null) {
                message = new TextUtils().replaceAndWithParagraph(message);
                message = new TextUtils().replaceObject(message, "%Player%", sender.getName());
                player.sendMessage(message);
            }
        });
        if (!(sender instanceof Player)) {
            String message = plugin.getLanguageConfig(sender).getString("ChatClear");
            if (message != null) {
                message = new TextUtils().replaceAndWithParagraph(message);
                message = new TextUtils().replaceObject(message, "%Player%", sender.getName());
                sender.sendMessage(message);
            }
        }
    }
}
