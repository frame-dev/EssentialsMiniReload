/*
 * Dies ist ein Plugin von FrameDev
 * Bitte nichts ändern, @Copyright by FrameDev
 */
package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.Variables;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author DHZoc
 */
public class TeleportCMD implements CommandExecutor, Listener {

    private static final String TPA = "tpa";
    private static final String TPA_ACCEPT = "tpaaccept";
    private static final String TPA_DENY = "tpadeny";
    private static final String TPA_HERE = "tpahere";
    private static final String TPA_HERE_ACCEPT = "tpahereaccept";
    private static final String TPA_HERE_DENY = "tpaheredeny";
    private static final String TP_HERE_ALL = "tphereall";
    private static final String TP_TOGGLE = "tptoggle";

    private final Main plugin;
    private final Map<UUID, UUID> tpRequests = new HashMap<>();
    private final Map<UUID, UUID> tpHereRequests = new HashMap<>();
    private final Set<UUID> tpToggle = new HashSet<>();
    private final Set<UUID> teleportQueue = new HashSet<>();

    public TeleportCMD(Main plugin) {
        this.plugin = plugin;
        registerCommands(TPA, TPA_ACCEPT, TPA_DENY, TP_HERE_ALL, TPA_HERE, TPA_HERE_ACCEPT, TPA_HERE_DENY, TP_TOGGLE);
        plugin.getListeners().add(this);
    }

    private void registerCommands(String... commands) {
        for (String command : commands) {
            plugin.getCommands().put(command, this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case TP_TOGGLE -> handleTeleportToggle(sender);
            case TPA -> handleRequest(sender, args, RequestType.TPA);
            case TPA_ACCEPT -> handleAccept(sender, RequestType.TPA);
            case TPA_DENY -> handleDeny(sender, RequestType.TPA);
            case TPA_HERE -> handleRequest(sender, args, RequestType.TPA_HERE);
            case TPA_HERE_ACCEPT -> handleAccept(sender, RequestType.TPA_HERE);
            case TPA_HERE_DENY -> handleDeny(sender, RequestType.TPA_HERE);
            case TP_HERE_ALL -> handleTeleportHereAll(sender);
            default -> false;
        };
    }

    private boolean handleTeleportToggle(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!hasPermission(player, "tptoggle")) {
            return true;
        }

        if (tpToggle.remove(player.getUniqueId())) {
            send(player, "§aPlayers can now Teleport to you or send you a Tpa Request!");
        } else {
            tpToggle.add(player.getUniqueId());
            send(player, "§6Players §ccan no more Teleporting to you or Send a Tpa Request");
        }
        return true;
    }

    private boolean handleRequest(CommandSender sender, String[] args, RequestType type) {
        Player requester = requirePlayer(sender);
        if (requester == null) {
            return true;
        }

        if (args.length != 1) {
            send(sender, plugin.getWrongArgs(type.usage));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(sender, plugin.getVariables().getPlayerNameNotOnline(args[0]));
            return true;
        }

        if (requester.getUniqueId().equals(target.getUniqueId())) {
            send(sender, "§cYou cannot send a teleport request to yourself!");
            return true;
        }

        if (tpToggle.contains(target.getUniqueId()) && !requester.hasPermission(plugin.getPermissionBase() + "tptoggle.bypass")) {
            send(sender, "§cThis Player doesn't accept Teleport!");
            return true;
        }

        requestsFor(type).put(target.getUniqueId(), requester.getUniqueId());
        notifyRequestSent(requester, target, type);
        return true;
    }

    private void notifyRequestSent(Player requester, Player target, RequestType type) {
        if (type == RequestType.TPA) {
            plugin.sendConfiguredNotification(requester, "teleportRequestSent", "teleport",
                    lang(requester, "TpaMessages.TeleportSend", "&aYou sent &6%Target% &aa teleport request!",
                            Map.of("%Target%", target.getName())),
                    Map.of("%Target%", target.getName(), "%Player%", requester.getName()));
            plugin.sendConfiguredNotification(target, "teleportRequestReceived", "teleport",
                    lang(target, "TpaMessages.TeleportGot", "&aYou got a teleport request from &6%Player%!",
                            Map.of("%Player%", requester.getName())),
                    Map.of("%Target%", target.getName(), "%Player%", requester.getName()));
        } else {
            plugin.sendConfiguredNotification(target, "teleportRequestReceived", "teleport",
                    lang(target, "TpaMessages.TpaHereTarget", "&6%Player% &awants you to teleport to him!",
                            Map.of("%Player%", requester.getName())),
                    Map.of("%Target%", target.getName(), "%Player%", requester.getName()));
            plugin.sendConfiguredNotification(requester, "teleportRequestSent", "teleport",
                    lang(requester, "TpaMessages.TpaHere", "&aDo you want to teleport &6%Player% to you?",
                            Map.of("%Player%", target.getName())),
                    Map.of("%Target%", target.getName(), "%Player%", requester.getName()));
        }

        sendRequestButtons(target, type.acceptCommand + " " + requester.getName(), type.denyCommand + " " + requester.getName(), type.acceptHover, type.denyHover);
    }

    private boolean handleAccept(CommandSender sender, RequestType type) {
        Player receiver = requirePlayer(sender);
        if (receiver == null) {
            return true;
        }

        UUID requesterId = requestsFor(type).remove(receiver.getUniqueId());
        if (requesterId == null) {
            sendNoRequest(receiver);
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null) {
            sendNoRequest(receiver);
            return true;
        }

        Player movingPlayer = type == RequestType.TPA ? requester : receiver;
        Player destinationPlayer = type == RequestType.TPA ? receiver : requester;
        teleportWithConfiguredDelay(movingPlayer, destinationPlayer);
        return true;
    }

    private boolean handleDeny(CommandSender sender, RequestType type) {
        Player receiver = requirePlayer(sender);
        if (receiver == null) {
            return true;
        }

        UUID requesterId = requestsFor(type).remove(receiver.getUniqueId());
        if (requesterId == null) {
            sendNoRequest(receiver);
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        String deniedKey = type == RequestType.TPA ? "TpaMessages.TpaDeny" : "TpaMessages.TpaHereDeny";
        String deniedTargetKey = type == RequestType.TPA ? "TpaMessages.TpaDenyTarget" : "TpaMessages.TpaHereDenyTarget";
        String deniedDefault = type == RequestType.TPA ? "&aYou declined the teleportation request!" : "&cYou declined the tpahere request.";
        String deniedTargetDefault = type == RequestType.TPA
                ? "&6%Player% &chas declined your teleportation request!"
                : "&6%Player% &chas declined the tpahere request!";

        send(receiver, lang(receiver, deniedKey, deniedDefault));
        if (requester != null) {
            send(requester, lang(requester, deniedTargetKey, deniedTargetDefault, Map.of("%Player%", receiver.getName())));
        }
        return true;
    }

    private boolean handleTeleportHereAll(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!hasPermission(player, "tphereall")) {
            return true;
        }

        Location location = player.getLocation();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.teleport(location);
        }
        return true;
    }

    private Map<UUID, UUID> requestsFor(RequestType type) {
        return type == RequestType.TPA ? tpRequests : tpHereRequests;
    }

    private void teleportWithConfiguredDelay(Player movingPlayer, Player destinationPlayer) {
        if (!plugin.getConfig().getBoolean("TeleportInOtherWorld")) {
            teleportIfSameWorld(movingPlayer, destinationPlayer);
            return;
        }

        int delay = Math.max(0, plugin.getConfig().getInt("TeleportDelay"));
        if (delay == 0) {
            completeTeleport(movingPlayer, destinationPlayer);
            return;
        }

        teleportQueue.add(movingPlayer.getUniqueId());
        plugin.sendConfiguredNotification(movingPlayer, "teleportDelay", "teleport",
                lang(movingPlayer, "TpaMessages.Delay",
                        "&aYou'll be teleported in &6%Time%! &aIf you move the teleportation will be cancelled.",
                        Map.of("%Time%", String.valueOf(delay))),
                Map.of("%Time%", String.valueOf(delay), "%Player%", destinationPlayer.getName()));
        scheduleTeleport(movingPlayer.getUniqueId(), destinationPlayer.getUniqueId(), delay);
    }

    private void teleportIfSameWorld(Player movingPlayer, Player destinationPlayer) {
        if (movingPlayer.getWorld().equals(destinationPlayer.getWorld())) {
            completeTeleport(movingPlayer, destinationPlayer);
            return;
        }

        send(movingPlayer, "§aThe Player §6" + destinationPlayer.getName() + " §cis not in the same World!");
    }

    private void scheduleTeleport(UUID movingPlayerId, UUID destinationPlayerId, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!teleportQueue.remove(movingPlayerId)) {
                    return;
                }

                Player movingPlayer = Bukkit.getPlayer(movingPlayerId);
                Player destinationPlayer = Bukkit.getPlayer(destinationPlayerId);
                if (movingPlayer == null || destinationPlayer == null) {
                    return;
                }

                completeTeleport(movingPlayer, destinationPlayer);
            }
        }.runTaskLater(plugin, 20L * delay);
    }

    private void completeTeleport(Player movingPlayer, Player destinationPlayer) {
        movingPlayer.teleport(destinationPlayer.getLocation());
        plugin.sendConfiguredNotification(movingPlayer, "teleportCompleted", "teleport",
                lang(movingPlayer, "TpaMessages.TeleportToPlayer", "&aYou were teleported to &6%Player%!",
                        Map.of("%Player%", destinationPlayer.getName())),
                Map.of("%Player%", destinationPlayer.getName(), "%Target%", movingPlayer.getName()));
        send(destinationPlayer, lang(destinationPlayer, "TpaMessages.TargetMessage", "&6%Target% &ateleported to you!",
                Map.of("%Target%", movingPlayer.getName())));
    }

    private void sendRequestButtons(Player target, String acceptCommand, String denyCommand, String acceptHover, String denyHover) {
        TextComponent accept = new TextComponent("§6[Accept]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + acceptCommand));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(acceptHover)));

        TextComponent deny = new TextComponent("§c[Deny]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + denyCommand));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(denyHover)));

        target.spigot().sendMessage(accept);
        target.spigot().sendMessage(deny);
    }

    private void sendNoRequest(Player player) {
        send(player, lang(player, Variables.TP_MESSAGES + ".NoRequest", "&cYou don't have any requests!"));
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }

        send(sender, plugin.getOnlyPlayer());
        return null;
    }

    private boolean hasPermission(CommandSender sender, String permissionSuffix) {
        if (sender.hasPermission(plugin.getPermissionBase() + permissionSuffix)) {
            return true;
        }

        send(sender, plugin.getNoPerms());
        return false;
    }

    private String lang(CommandSender sender, String key, String defaultMessage) {
        return lang(sender, key, defaultMessage, Map.of());
    }

    private String lang(CommandSender sender, String key, String defaultMessage, Map<String, String> replacements) {
        String message = null;
        try {
            message = plugin.getLanguageConfig(sender).getString(key);
        } catch (RuntimeException ignored) {
        }

        if (message == null) {
            message = defaultMessage;
        }

        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            message = ReplaceCharConfig.replaceObjectWithData(message, replacement.getKey(), replacement.getValue());
        }
        return ReplaceCharConfig.replaceParagraph(message);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (plugin.getConfig().getBoolean("TeleportInOtherWorld") || event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }

        Player player = event.getPlayer();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            if (sameBlock(event.getTo(), target.getLocation()) && !target.getWorld().equals(player.getWorld())) {
                send(player, "§6" + target.getName() + " §cis not in the same World!");
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!teleportQueue.contains(event.getPlayer().getUniqueId()) || event.getTo() == null) {
            return;
        }

        int moveX = event.getFrom().getBlockX() - event.getTo().getBlockX();
        int moveZ = event.getFrom().getBlockZ() - event.getTo().getBlockZ();
        if (Math.abs(moveX) > 0 || Math.abs(moveZ) > 0) {
            teleportQueue.remove(event.getPlayer().getUniqueId());
            send(event.getPlayer(), lang(event.getPlayer(), Variables.TP_MESSAGES + ".Denied",
                    "&cYou moved. &6Teleportation has been cancelled!"));
        }
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    public void runnable(Player player, Player target, int delay) {
        teleportQueue.add(player.getUniqueId());
        scheduleTeleport(player.getUniqueId(), target.getUniqueId(), Math.max(0, delay));
    }

    private enum RequestType {
        TPA("/tpa <PlayerName>", TPA_ACCEPT, TPA_DENY, "§aAccept Tpa Request!", "§cDeny Tpa Request!"),
        TPA_HERE("/tpahere <PlayerName>", TPA_HERE_ACCEPT, TPA_HERE_DENY, "§aAccept TpaHere Request!", "§cDeny TpaHere Request!");

        private final String usage;
        private final String acceptCommand;
        private final String denyCommand;
        private final String acceptHover;
        private final String denyHover;
        RequestType(String usage, String acceptCommand, String denyCommand, String acceptHover, String denyHover) {
            this.usage = usage;
            this.acceptCommand = acceptCommand;
            this.denyCommand = denyCommand;
            this.acceptHover = acceptHover;
            this.denyHover = denyHover;
        }
    }
}
