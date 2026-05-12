package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MessageCMD extends CommandBase {

    private static final String MSG = "msg";
    private static final String REPLY = "r";
    private static final String SPY = "spy";
    private static final String MSG_TOGGLE = "msgtoggle";
    private static final List<String> MSG_SUB_COMMANDS = Arrays.asList(MSG, REPLY, SPY, MSG_TOGGLE);

    private static final String NOTIFICATION_TO = "msg.notificationTo";
    private static final String NOTIFICATION_FROM = "msg.notificationFrom";
    private static final String CONSOLE_NAME = "msg.consoleName";
    private static final String DEFAULT_NOTIFICATION_TO = "§cme §r→ §a%TARGET% §f» %MESSAGE%";
    private static final String DEFAULT_NOTIFICATION_FROM = "§a%TARGET% §r→ §cme §f» %MESSAGE%";
    private static final String DEFAULT_SPY_MESSAGE = "§6%Player% §ahas sent a message to §6%Target% §awith the text §6:§c%Message%";

    private final Main plugin;
    private final Map<UUID, UUID> reply = new HashMap<>();
    private final Set<UUID> spy = new HashSet<>();
    private final Set<UUID> msgToggle = new HashSet<>();

    public MessageCMD(Main plugin) {
        super(plugin, MSG, REPLY, SPY, MSG_TOGGLE);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case MSG_TOGGLE -> handleMsgToggle(sender);
            case MSG -> handleMsg(sender, args);
            case REPLY -> handleReply(sender, args);
            case SPY -> handleSpy(sender);
            default -> false;
        };
    }

    private boolean handleMsgToggle(CommandSender sender) {
        if (!hasPermission(sender, "msgtoggle")) return true;

        Player player = requirePlayer(sender);
        if (player == null) return true;

        toggle(player, msgToggle, "MsgToggle.Activated", "MsgToggle.Deactivated",
                "§aYou can no longer receive private messages.",
                "§aYou can receive private messages again.");
        return true;
    }

    private boolean handleMsg(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "msg")) return true;

        if (args.length < 2) {
            sendWrongArgs(sender, "/msg <PlayerName> <Message>");
            return true;
        }

        Player target = findTarget(sender, args[0]);
        if (target == null) return true;

        String message = buildMessage(args, 1);
        if (message.isBlank()) {
            sendWrongArgs(sender, "/msg <PlayerName> <Message>");
            return true;
        }

        sendPrivateMessage(sender, target, message);
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "msg")) return true;

        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (args.length < 1) {
            sendWrongArgs(sender, "/r <Message>");
            return true;
        }

        UUID targetId = reply.get(player.getUniqueId());
        if (targetId == null) {
            send(sender, "§cNo recent messages to reply to!");
            return true;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            reply.remove(player.getUniqueId());
            send(sender, "§cPlayer not found.");
            return true;
        }

        String message = buildMessage(args, 0);
        if (message.isBlank()) {
            sendWrongArgs(sender, "/r <Message>");
            return true;
        }

        sendPrivateMessage(sender, target, message);
        return true;
    }

    private boolean handleSpy(CommandSender sender) {
        if (!hasPermission(sender, "spy")) return true;

        Player player = requirePlayer(sender);
        if (player == null) return true;

        toggle(player, spy, "Spy.Activate", "Spy.Deactivate",
                "§aSpy mode enabled.",
                "§aSpy mode disabled.");
        return true;
    }

    private void sendPrivateMessage(CommandSender sender, Player receiver, String message) {
        if (sender instanceof Player player && player.getUniqueId().equals(receiver.getUniqueId())) {
            send(sender, "§cYou cannot message yourself!");
            return;
        }

        if (msgToggle.contains(receiver.getUniqueId()) && !sender.hasPermission(plugin.getPermissionBase() + "msgtoggle.bypass")) {
            sendFormattedMessage(sender, "MsgToggle.Message", "§cThis player has disabled private messages.");
            return;
        }

        SenderIdentity senderIdentity = senderIdentity(sender);
        sender.sendMessage(formatTemplate(plugin.getConfig().getString(NOTIFICATION_TO, DEFAULT_NOTIFICATION_TO), Map.of(
                "%MESSAGE%", message,
                "%Message%", message,
                "%TARGET%", receiver.getName(),
                "%Target%", receiver.getName(),
                "%PLAYER%", senderIdentity.displayName(),
                "%Player%", senderIdentity.displayName(),
                "%SENDER%", senderIdentity.displayName()
        )));
        receiver.sendMessage(formatTemplate(plugin.getConfig().getString(NOTIFICATION_FROM, DEFAULT_NOTIFICATION_FROM), Map.of(
                "%MESSAGE%", message,
                "%Message%", message,
                "%TARGET%", senderIdentity.displayName(),
                "%Target%", senderIdentity.displayName(),
                "%PLAYER%", receiver.getName(),
                "%Player%", receiver.getName(),
                "%SENDER%", senderIdentity.displayName()
        )));

        notifySpies(senderIdentity, receiver, message);
        rememberReply(senderIdentity, receiver);
    }

    private void notifySpies(SenderIdentity senderIdentity, Player receiver, String message) {
        spy.removeIf(spyId -> {
            Player spyPlayer = Bukkit.getPlayer(spyId);
            if (spyPlayer == null) return true;
            if (!spyPlayer.hasPermission(plugin.getPermissionBase() + "spy")) return false;
            if (senderIdentity.playerId() != null && spyId.equals(senderIdentity.playerId())) return false;
            if (spyId.equals(receiver.getUniqueId())) return false;

            String spyMessage = plugin.getLanguageConfig(spyPlayer).getString("SpyMessage", DEFAULT_SPY_MESSAGE);
            spyPlayer.sendMessage(formatTemplate(spyMessage, Map.of(
                    "%Player%", senderIdentity.displayName(),
                    "%PLAYER%", senderIdentity.displayName(),
                    "%Target%", receiver.getName(),
                    "%TARGET%", receiver.getName(),
                    "%Message%", message,
                    "%MESSAGE%", message
            )));
            return false;
        });
    }

    private void rememberReply(SenderIdentity senderIdentity, Player receiver) {
        if (senderIdentity.playerId() == null) return;

        reply.put(receiver.getUniqueId(), senderIdentity.playerId());
        reply.put(senderIdentity.playerId(), receiver.getUniqueId());
    }

    private void toggle(Player player, Set<UUID> values, String activatedKey, String deactivatedKey,
                        String activatedDefault, String deactivatedDefault) {
        if (values.remove(player.getUniqueId())) {
            sendFormattedMessage(player, deactivatedKey, deactivatedDefault);
            return;
        }

        values.add(player.getUniqueId());
        sendFormattedMessage(player, activatedKey, activatedDefault);
    }

    private Player findTarget(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) return target;

        send(sender, "§cPlayer not found.");
        return null;
    }

    private SenderIdentity senderIdentity(CommandSender sender) {
        if (sender instanceof Player player) {
            return new SenderIdentity(player.getName(), player.getUniqueId());
        }

        String consoleName = plugin.getConfig().getString(CONSOLE_NAME, "Console");
        if (consoleName == null || consoleName.isBlank()) {
            consoleName = "Console";
        }
        return new SenderIdentity(consoleName, null);
    }

    private String buildMessage(String[] args, int start) {
        if (args.length <= start) return "";
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private String formatTemplate(String template, Map<String, String> replacements) {
        String formatted = template == null ? "" : template;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            formatted = ReplaceCharConfig.replaceObjectWithData(formatted, replacement.getKey(), replacement.getValue());
        }
        return ReplaceCharConfig.replaceParagraph(formatted);
    }

    private void sendFormattedMessage(CommandSender sender, String configKey, String defaultMessage) {
        String message = plugin.getLanguageConfig(sender).getString(configKey, defaultMessage);
        send(sender, ReplaceCharConfig.replaceParagraph(message == null ? defaultMessage : message));
    }

    private boolean hasPermission(CommandSender sender, String permissionSuffix) {
        if (sender.hasPermission(plugin.getPermissionBase() + permissionSuffix)) return true;

        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        send(sender, plugin.getOnlyPlayer(null));
        return null;
    }

    private void sendWrongArgs(CommandSender sender, String usage) {
        send(sender, plugin.getWrongArgs(sender instanceof Player player ? player : null, usage));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals(MSG) && args.length == 1) {
            return TabCompleteUtils.matchingOnlinePlayers(args[0]);
        }
        if (commandName.equals(MSG) && args.length > 1) {
            return List.of();
        }
        if (commandName.equals(REPLY) || commandName.equals(SPY) || commandName.equals(MSG_TOGGLE)) {
            return List.of();
        }
        return TabCompleteUtils.matchingStrings(MSG_SUB_COMMANDS, args.length == 0 ? "" : args[0]);
    }

    private record SenderIdentity(String displayName, UUID playerId) {
    }
}
