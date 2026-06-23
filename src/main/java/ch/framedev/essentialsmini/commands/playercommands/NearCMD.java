package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NearCMD extends CommandBase {

    private static final String USAGE = "/near [radius]";
    private static final double DEFAULT_RADIUS = 100.0;
    private static final double MAX_RADIUS = 1000.0;

    private final Main plugin;

    public NearCMD(Main plugin) {
        super(plugin, "near");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer(null));
            return true;
        }

        if (!player.hasPermission(plugin.getPermissionBase() + "near")) {
            player.sendMessage(plugin.getPrefix() + plugin.getNoPerms(player));
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(plugin.getPrefix() + plugin.getWrongArgs(player, USAGE));
            return true;
        }

        Double radius = parseRadius(args.length == 0 ? null : args[0]);
        if (radius == null) {
            sendMessage(player, "Near.InvalidRadius", "&cPlease enter a valid radius between &61 &cand &6%MaxRadius%&c.", 0, "", 0);
            return true;
        }

        List<NearbyPlayer> nearbyPlayers = getNearbyPlayers(player, radius);
        if (nearbyPlayers.isEmpty()) {
            sendMessage(player, "Near.None", "&cNo players found within &6%Radius% &cblocks.", radius, "", 0);
            return true;
        }

        String players = formatPlayers(nearbyPlayers);
        sendMessage(player, "Near.Found", "&aPlayers within &6%Radius% &ablocks: &6%Players%", radius, players, nearbyPlayers.size());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of("25", "50", "100", "250");
        }
        return Collections.emptyList();
    }

    private Double parseRadius(String input) {
        if (input == null || input.isBlank()) {
            return DEFAULT_RADIUS;
        }

        try {
            double radius = Double.parseDouble(input);
            if (radius <= 0 || radius > MAX_RADIUS) {
                return null;
            }
            return radius;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<NearbyPlayer> getNearbyPlayers(Player player, double radius) {
        double radiusSquared = radius * radius;
        List<NearbyPlayer> nearbyPlayers = new ArrayList<>();

        for (Player target : plugin.getServer().getOnlinePlayers()) {
            if (target.equals(player) || !target.getWorld().equals(player.getWorld())) {
                continue;
            }

            double distanceSquared = target.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared <= radiusSquared) {
                nearbyPlayers.add(new NearbyPlayer(target.getName(), Math.sqrt(distanceSquared)));
            }
        }

        nearbyPlayers.sort(Comparator.comparingDouble(NearbyPlayer::distance));
        return nearbyPlayers;
    }

    private String formatPlayers(List<NearbyPlayer> nearbyPlayers) {
        List<String> names = new ArrayList<>();
        for (NearbyPlayer nearbyPlayer : nearbyPlayers) {
            names.add(nearbyPlayer.name() + " (" + Math.round(nearbyPlayer.distance()) + "m)");
        }
        return String.join("§7, §6", names);
    }

    private void sendMessage(Player player, String key, String fallback, double radius, String players, int count) {
        String message = plugin.getLanguageConfig(player).getString(key, fallback);
        if (message == null) message = fallback;

        message = ReplaceCharConfig.replaceParagraph(message);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Radius%", formatNumber(radius));
        message = ReplaceCharConfig.replaceObjectWithData(message, "%MaxRadius%", formatNumber(MAX_RADIUS));
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Players%", players);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Count%", String.valueOf(count));
        player.sendMessage(plugin.getPrefix() + message);
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private record NearbyPlayer(String name, double distance) {
    }
}
