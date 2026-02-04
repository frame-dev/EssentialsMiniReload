package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 14.07.2020 22:52
 */
public class GameModeCMD extends CommandBase {

    private final Main plugin;

    public GameModeCMD(Main plugin) {
        super(plugin, "gamemode");
        setupTabCompleter(this);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("gamemode")) return false;

        // /gamemode <mode>           -> self
        if (args.length == 1) {
            if (!sender.hasPermission("essentialsmini.gamemode")) {
                sender.sendMessage(Main.getInstance().getPrefix() + plugin.getNoPerms());
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }

            GameMode mode = parseGameMode(args[0]);
            if (mode == null) {
                sender.sendMessage(plugin.getPrefix() + "§cNo valid GameMode found §6" + args[0] + "§c!");
                return true;
            }

            applyGameModeToTarget(player, sender, mode, false);
            return true;
        }

        // /gamemode <mode> <player>  -> other
        if (args.length == 2) {
            if (!sender.hasPermission("essentialsmini.gamemode.others")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
                return true;
            }

            GameMode mode = parseGameMode(args[0]);
            if (mode == null) {
                sender.sendMessage(plugin.getPrefix() + "§cNo valid GameMode found with the number or name §6" + args[0] + "§c!");
                return true;
            }

            applyGameModeToTarget(target, sender, mode, true);
            return true;
        }

        // Wrong usage: show help
        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/gamemode <Gamemode (Name oder Zahl)> §cor §6/gamemode <Gamemode (Name oder Zahl)> <Spieler Name>"));
        return true;
    }

    // Parse the qualifier (either numeric id or name/alias)
    private GameMode parseGameMode(String qualifier) {
        if (qualifier == null || qualifier.isEmpty()) return null;
        try {
            int id = Integer.parseInt(qualifier);
            return getGameModeById(id);
        } catch (NumberFormatException ignored) {
            // try by qualifier name
            return getGameModeByQualifier(qualifier.toLowerCase());
        }
    }

    // Centralized application and messaging
    private void applyGameModeToTarget(Player target, CommandSender sender, GameMode mode, boolean notifySender) {
        // prepare localized messages (use defaults if missing)
        String targetMsg = plugin.getLanguageConfig(target).getString("GameModeChanged");
        if (targetMsg == null) targetMsg = "§aYour gamemode has been set to %GameMode%";
        String otherMsg = plugin.getLanguageConfig(sender).getString("GameModeOtherChanged");
        if (otherMsg == null) otherMsg = "§aSet gamemode for %Player% to %GameMode%";

        // set gamemode
        target.setGameMode(mode);

        // message to target (respect silent)
        if (!Main.getSilent().contains(sender.getName())) {
            String gm = targetMsg.replace("%GameMode%", mode.name());
            gm = ReplaceCharConfig.replaceParagraph(gm);
            target.sendMessage(plugin.getPrefix() + gm);
        }

        // message to sender (if other and notify)
        if (notifySender) {
            String om = otherMsg.replace("%Player%", target.getName()).replace("%GameMode%", mode.name());
            om = ReplaceCharConfig.replaceParagraph(om);
            sender.sendMessage(plugin.getPrefix() + om);
        }
    }

    @SuppressWarnings("unused")
    public static GameMode getGameModeByQualifier(String qualifier) {
        return switch (qualifier) {
            case "0", "s", "su", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "cr", "creative" -> GameMode.CREATIVE;
            case "2", "a", "ad", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spe", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    @SuppressWarnings("unused")
    public static GameMode getGameModeById(int id) {
        return switch (id) {
            case 0 -> GameMode.SURVIVAL;
            case 1 -> GameMode.CREATIVE;
            case 2 -> GameMode.ADVENTURE;
            case 3 -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            ArrayList<String> empty = getList(args);
            Collections.sort(empty);
            return empty;
        } else if (args.length == 2) {
            // suggest online player names for the second argument
            ArrayList<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) names.add(p.getName());
            }
            Collections.sort(names);
            return names;
        }
        return null;
    }

    private static @NotNull ArrayList<String> getList(String[] args) {
        ArrayList<String> gameModes = new ArrayList<>();
        ArrayList<String> empty = new ArrayList<>();
        gameModes.add("creative");
        gameModes.add("survival");
        gameModes.add("adventure");
        gameModes.add("spectator");
        gameModes.add("c");
        gameModes.add("s");
        gameModes.add("a");
        gameModes.add("sp");
        gameModes.add("0");
        gameModes.add("1");
        gameModes.add("2");
        gameModes.add("3");
        for (String s : gameModes) {
            if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                empty.add(s);
            }
        }
        return empty;
    }
}
