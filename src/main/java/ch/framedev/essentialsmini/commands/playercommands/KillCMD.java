package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 18.07.2020 13:32
 */
public class KillCMD extends CommandBase {

    private static final String KILL_ALL = "killall";
    private static final String SUICID = "suicid";
    private static final String KILL_ALL_USAGE = "/killall <animals|mobs|players|items>";
    private static final String SUICID_USAGE = "/suicid [player]";

    public static final Set<UUID> suicidPlayers = ConcurrentHashMap.newKeySet();

    private final Main plugin;

    public KillCMD(Main plugin) {
        super(plugin, KILL_ALL, SUICID);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        return switch (commandName) {
            case KILL_ALL -> handleKillAll(sender, args);
            case SUICID -> handleSuicid(sender, args);
            default -> false;
        };
    }

    private boolean handleKillAll(CommandSender sender, String[] args) {
        if (!hasPermission(sender, plugin.getPermissionBase() + "killall")) return true;

        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (args.length != 1) {
            sendWrongArgs(sender, KILL_ALL_USAGE);
            return true;
        }

        KillTarget target = KillTarget.fromArgument(args[0]);
        if (target == null) {
            sendWrongArgs(sender, KILL_ALL_USAGE);
            return true;
        }

        int removed = target.kill(player);
        send(sender, target.message(removed));
        return true;
    }

    private boolean handleSuicid(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!hasPermission(sender, plugin.getPermissionBase() + "suicid")) return true;

            Player player = requirePlayer(sender);
            if (player == null) return true;

            suicide(player);
            return true;
        }

        if (args.length == 1) {
            if (!hasPermission(sender, plugin.getPermissionBase() + "suicid.others")) return true;

            Player target = findTarget(sender, args[0]);
            if (target == null) return true;

            suicide(target);
            if (!sender.equals(target)) {
                send(sender, "§6" + target.getName() + " §ahat Suizid begangen!");
            }
            return true;
        }

        sendWrongArgs(sender, SUICID_USAGE);
        return true;
    }

    private void suicide(Player player) {
        suicidPlayers.add(player.getUniqueId());
        killPlayer(player);
        broadcastToWorld(player.getWorld(), "§6" + player.getName() + " §ahat Suizid begangen!");
    }

    private void killPlayer(Player player) {
        try {
            player.setHealth(0);
            player.setFoodLevel(0);
        } catch (RuntimeException ignored) {
            // Bukkit may reject health changes for already-dead/invalid players.
        }
    }

    private void broadcastToWorld(World world, String message) {
        if (world == null) return;
        for (Player player : world.getPlayers()) {
            send(player, message);
        }
    }

    private Player findTarget(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) return target;

        send(sender, playerNotOnline(playerName));
        return null;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;

        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        send(sender, plugin.getOnlyPlayer(null));
        return null;
    }

    private String playerNotOnline(String playerName) {
        if (plugin.getVariables() == null) {
            return "§cPlayer §6" + playerName + " §cis not online!";
        }
        return plugin.getVariables().getPlayerNameNotOnline(playerName);
    }

    private void sendWrongArgs(CommandSender sender, String usage) {
        send(sender, plugin.getWrongArgs(usage));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals(KILL_ALL) && args.length == 1 && sender.hasPermission(plugin.getPermissionBase() + "killall")) {
            return matchingKillTargets(args[0]);
        }

        if (commandName.equals(SUICID) && args.length == 1 && sender.hasPermission(plugin.getPermissionBase() + "suicid.others")) {
            return matchingPlayers(args[0]);
        }

        return Collections.emptyList();
    }

    private List<String> matchingKillTargets(String prefix) {
        return TabCompleteUtils.matchingStrings(Arrays.stream(KillTarget.values())
                .map(target -> target.argument)
                .toList(), prefix);
    }

    private List<String> matchingPlayers(String prefix) {
        return TabCompleteUtils.matchingOnlinePlayers(prefix);
    }

    private enum KillTarget {
        ANIMALS("animals", entity -> entity instanceof Animals, "§aEs wurden §6%d §aTiere in deiner Umgebung entfernt!"),
        MOBS("mobs", entity -> entity instanceof Monster, "§aEs wurden §6%d §aMonster in deiner Umgebung entfernt!"),
        ITEMS("items", entity -> entity instanceof Item, "§aEs wurden §6%d §aItems in deiner Umgebung entfernt!"),
        PLAYERS("players", entity -> entity instanceof Player, "§aEs wurden §6%d §aSpieler in deiner Umgebung entfernt!");

        private static final Set<KillTarget> ENTITY_TARGETS = EnumSet.of(ANIMALS, MOBS, ITEMS);

        private final String argument;
        private final Predicate<Entity> predicate;
        private final String message;

        KillTarget(String argument, Predicate<Entity> predicate, String message) {
            this.argument = argument;
            this.predicate = predicate;
            this.message = message;
        }

        private static KillTarget fromArgument(String argument) {
            for (KillTarget target : values()) {
                if (target.argument.equalsIgnoreCase(argument)) {
                    return target;
                }
            }
            return null;
        }

        private int kill(Player sender) {
            if (this == PLAYERS) {
                return killPlayers(sender);
            }
            if (!ENTITY_TARGETS.contains(this)) {
                return 0;
            }

            int removed = 0;
            for (Entity entity : sender.getWorld().getEntities()) {
                if (predicate.test(entity)) {
                    entity.remove();
                    removed++;
                }
            }
            return removed;
        }

        private int killPlayers(Player sender) {
            int killed = 0;
            for (Player target : sender.getWorld().getPlayers()) {
                if (target.getUniqueId().equals(sender.getUniqueId())) continue;

                try {
                    target.setHealth(0);
                    target.setFoodLevel(0);
                    killed++;
                } catch (RuntimeException ignored) {
                    // Keep killing the remaining players if one target cannot be changed.
                }
            }
            return killed;
        }

        private String message(int count) {
            return String.format(Locale.ROOT, message, count);
        }
    }
}
