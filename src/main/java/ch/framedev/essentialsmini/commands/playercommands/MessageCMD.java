package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MessageCMD extends CommandBase {

    private final Main plugin;
    private final Map<String, String> reply = new HashMap<>();
    private final Set<String> spy = new HashSet<>();
    private final Set<String> msgToggle = new HashSet<>();

    public MessageCMD(Main plugin) {
        super(plugin, "msg", "r", "spy", "msgtoggle");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "msgtoggle" -> {
                handleMsgToggle(sender);
                yield true;
            }
            case "msg" -> {
                handleMsg(sender, args);
                yield true;
            }
            case "r" -> {
                handleReply(sender, args);
                yield true;
            }
            case "spy" -> {
                handleSpy(sender);
                yield true;
            }
            default -> false;
        };
    }

    // ------------------------------------------
    // ✅ MSGTOGGLE COMMAND
    // ------------------------------------------
    private void handleMsgToggle(CommandSender sender) {
        if (!sender.hasPermission(plugin.getPermissionBase() + "msgtoggle")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return;
        }

        if (msgToggle.contains(sender.getName())) {
            msgToggle.remove(sender.getName());
            sendFormattedMessage(sender, "MsgToggle.Deactivated");
        } else {
            msgToggle.add(sender.getName());
            sendFormattedMessage(sender, "MsgToggle.Activated");
        }
    }

    // ------------------------------------------
    // ✅ MSG COMMAND
    // ------------------------------------------
    private void handleMsg(CommandSender sender, String[] args) {

        if (!sender.hasPermission("essentialsmini.msg")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/msg <PlayerName> <Message>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "§cPlayer not found.");
            return;
        }

        // Prevent messaging oneself
        if (sender instanceof Player && sender.getName().equalsIgnoreCase(target.getName())) {
            sender.sendMessage(plugin.getPrefix() + "§cYou cannot message yourself!");
            return;
        }

        if (msgToggle.contains(target.getName()) && !sender.hasPermission(plugin.getPermissionBase() + "msgtoggle.bypass")) {
            sendFormattedMessage(sender, "MsgToggle.Message");
            return;
        }

        String message = buildMessageFromArgs(args, 1);
        sendMessageBetweenPlayers(sender, target, message);
    }

    // ------------------------------------------
    // ✅ REPLY COMMAND
    // ------------------------------------------
    private void handleReply(CommandSender sender, String[] args) {

        if (!sender.hasPermission("essentialsmini.msg")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return;
        }

        // Ensure sender is a Player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use /r!");
            return;
        }

        if (!reply.containsKey(player.getName())) {
            sender.sendMessage(plugin.getPrefix() + "§cNo recent messages to reply to!");
            return;
        }

        Player target = Bukkit.getPlayer(reply.get(player.getName()));
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "§cPlayer not found.");
            return;
        }

        String message = buildMessageFromArgs(args, 0);
        sendMessageBetweenPlayers(sender, target, message);
    }

    // ------------------------------------------
    // ✅ SPY COMMAND
    // ------------------------------------------
    private void handleSpy(CommandSender sender) {

        if (!sender.hasPermission("essentialsmini.spy")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return;
        }

        if (spy.contains(sender.getName())) {
            spy.remove(sender.getName());
            sendFormattedMessage(sender, "Spy.Deactivate");
        } else {
            spy.add(sender.getName());
            sendFormattedMessage(sender, "Spy.Activate");
        }
    }

    // ------------------------------------------
    // 🔧 HELPER METHODS
    // ------------------------------------------
    private String buildMessageFromArgs(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private void sendMessageBetweenPlayers(CommandSender sender, Player receiver, String message) {
        String senderName;

        // Determine sender name
        if (sender instanceof Player) {
            senderName = sender.getName();
        } else if (sender instanceof ConsoleCommandSender) {
            senderName = plugin.getConfig().getString("consoleName", "Console");
        } else {
            senderName = "Unknown";
        }

        // Prevent sending messages to oneself
        if (sender instanceof Player && sender.getName().equalsIgnoreCase(receiver.getName())) {
            sender.sendMessage(plugin.getPrefix() + "§cYou cannot message yourself!");
            return;
        }

        // Fetch and replace notification messages
        String messageTo = Optional.ofNullable(plugin.getConfig().getString("msg.notificationTo"))
                .orElse("§cError: msg.notificationTo not found!");

        messageTo = ReplaceCharConfig.replaceObjectWithData(messageTo, "%MESSAGE%", message);
        messageTo = ReplaceCharConfig.replaceObjectWithData(messageTo, "%TARGET%", receiver.getName());
        messageTo = ReplaceCharConfig.replaceParagraph(messageTo);
        sender.sendMessage(messageTo);

        String messageFrom = Optional.ofNullable(plugin.getConfig().getString("msg.notificationFrom"))
                .orElse("§cError: msg.notificationFrom not found!");

        messageFrom = ReplaceCharConfig.replaceObjectWithData(messageFrom, "%PLAYER%", receiver.getName());
        messageFrom = ReplaceCharConfig.replaceObjectWithData(messageFrom, "%TARGET%", senderName);
        messageFrom = ReplaceCharConfig.replaceObjectWithData(messageFrom, "%MESSAGE%", message);
        messageFrom = ReplaceCharConfig.replaceParagraph(messageFrom);
        receiver.sendMessage(messageFrom);

        // Notify players with spy mode enabled
        for (String spyName : spy) {
            Player spyPlayer = Bukkit.getPlayer(spyName);
            if (spyPlayer == null || !spyPlayer.hasPermission("essentialsmini.spy")) continue;

            String spyMessage = Optional.ofNullable(plugin.getLanguageConfig(spyPlayer).getString("SpyMessage"))
                    .orElse("§cError: SpyMessage not found!");

            spyMessage = ReplaceCharConfig.replaceParagraph(spyMessage);
            spyMessage = ReplaceCharConfig.replaceObjectWithData(spyMessage, "%Player%", senderName);
            spyMessage = ReplaceCharConfig.replaceObjectWithData(spyMessage, "%Target%", receiver.getName());
            spyMessage = ReplaceCharConfig.replaceObjectWithData(spyMessage, "%Message%", message);
            spyPlayer.sendMessage(spyMessage);
        }

        // Store last messaged player for reply functionality
        reply.put(receiver.getName(), senderName);
    }

    private void sendFormattedMessage(CommandSender player, String configKey) {
        String msg = ReplaceCharConfig.replaceParagraph(plugin.getLanguageConfig(player).getString(configKey));
        player.sendMessage(plugin.getPrefix() + msg);
    }
}
