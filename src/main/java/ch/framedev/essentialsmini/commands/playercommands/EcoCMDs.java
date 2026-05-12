package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.Language;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.UUIDFetcher;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmin.commands
 * Date: 26.10.2020
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
public class EcoCMDs extends CommandBase {

    private static final String CMD_PAY = "pay";
    private static final String CMD_BALANCE = "balance";
    private static final String CMD_ECO = "eco";
    private static final String CMD_BALANCE_TOP = "balancetop";

    private final Main plugin;

    public EcoCMDs(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        setup(CMD_PAY, this);
        setup(CMD_BALANCE, this);
        setup(CMD_ECO, this);
        setup(CMD_BALANCE_TOP, this);
        setupTabCompleter(CMD_PAY, this);
        setupTabCompleter(CMD_ECO, this);
    }

    // Helper: require an OfflinePlayer by name, send a 'not online/name' message when missing
    private OfflinePlayer requireOfflinePlayer(CommandSender sender, String name) {
        OfflinePlayer player = PlayerUtils.getOfflinePlayerByName(name);
        if (player == null || player.getName() == null) {
            sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(name));
            return null;
        }
        return player;
    }

    private String colorizeAndFill(String raw, String... replacements) {
        if (raw == null) {
            return "";
        }
        String message = new TextUtils().replaceAndWithParagraph(raw);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = new TextUtils().replaceObject(message, replacements[i], replacements[i + 1]);
        }
        return message;
    }

    private void sendNoPerm(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
    }

    private void sendOnlyPlayer(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
    }

    private void sendInvalidNumber(Player player, String input) {
        if (plugin.getLanguage(player) == Language.DE) {
            player.sendMessage(plugin.getPrefix() + "§6" + input + " §cist keine Nummer!");
        } else {
            player.sendMessage(plugin.getPrefix() + "§6" + input + " §cisn't a Number!");
        }
    }

    private boolean isDouble(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        return switch (commandName) {
            case CMD_PAY -> handlePay(sender, args);
            case CMD_BALANCE -> handleBalance(sender, args);
            case CMD_ECO -> handleEco(sender, args);
            case CMD_BALANCE_TOP -> handleBalanceTop(sender);
            default -> false;
        };
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player payer)) {
            sendOnlyPlayer(sender);
            return true;
        }
        if (!sender.hasPermission(plugin.getPermissionBase() + "pay")) {
            sendNoPerm(sender);
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/pay <Amount> <PlayerName>"));
            return true;
        }
        if (!isDouble(args[0])) {
            sendInvalidNumber(payer, args[0]);
            return true;
        }

        double amount = Double.parseDouble(args[0]);
        if (args[1].equalsIgnoreCase("**")) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!plugin.getVaultManager().getEco().has(payer, amount)) {
                    String notEnough = plugin.getLanguageConfig(payer).getString("Money.MSG.NotEnough");
                    if (notEnough == null) {
                        payer.sendMessage(plugin.getPrefix() + "§cMessage config 'Money.MSG.NotEnough' is missing!");
                        return true;
                    }
                    notEnough = ReplaceCharConfig.replaceParagraph(notEnough);
                    notEnough = ReplaceCharConfig.replaceObjectWithData(
                            notEnough,
                            "%Money%",
                            plugin.getVaultManager().getEco().getBalance(payer) + plugin.getCurrencySymbol()
                    );
                    sender.sendMessage(plugin.getPrefix() + notEnough);
                    return true;
                }

                plugin.getVaultManager().getEco().withdrawPlayer(payer, amount);
                plugin.getVaultManager().getEco().depositPlayer(target, amount);

                String send = plugin.getLanguageConfig(payer).getString("Money.MSG.Pay");
                if (send == null) {
                    return true;
                }
                send = send.replace('&', '§')
                        .replace("[Target]", target.getName())
                        .replace("[Money]", amount + plugin.getCurrencySymbol());

                String got = plugin.getLanguageConfig(target).getString("Money.MSG.GotPay");
                if (got != null) {
                    got = colorizeAndFill(got,
                            "[Player]", sender.getName(),
                            "[Money]", amount + plugin.getCurrencySymbol());
                    target.sendMessage(plugin.getPrefix() + got);
                }
                sender.sendMessage(plugin.getPrefix() + send);
            }
            return true;
        }

        OfflinePlayer target = requireOfflinePlayer(sender, args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[1]));
            return true;
        }

        if (!plugin.getVaultManager().getEco().has(payer, amount)) {
            String moneySet = plugin.getLanguageConfig(sender).getString("Money.MSG.NotEnough");
            if (moneySet == null) {
                sender.sendMessage(plugin.getPrefix() + "§cMessage config 'Money.MSG.NotEnough' is missing!");
                return true;
            }
            moneySet = ReplaceCharConfig.replaceParagraph(moneySet);
            moneySet = ReplaceCharConfig.replaceObjectWithData(
                    moneySet,
                    "%Money%",
                    plugin.getVaultManager().getEco().getBalance(payer) + plugin.getCurrencySymbol()
            );
            sender.sendMessage(plugin.getPrefix() + moneySet);
            return true;
        }

        plugin.getVaultManager().getEco().withdrawPlayer(payer, amount);
        plugin.getVaultManager().getEco().depositPlayer(target, amount);

        String send = plugin.getLanguageConfig(sender).getString("Money.MSG.Pay");
        if (send != null) {
            send = send.replace('&', '§')
                    .replace("[Target]", target.getName())
                    .replace("[Money]", amount + plugin.getCurrencySymbol());
            sender.sendMessage(plugin.getPrefix() + send);
        }

        if (target.isOnline() && target instanceof Player onlineTarget) {
            String got = plugin.getLanguageConfig(onlineTarget).getString("Money.MSG.GotPay");
            if (got != null) {
                got = colorizeAndFill(got,
                        "[Player]", sender.getName(),
                        "[Money]", amount + plugin.getCurrencySymbol());
                onlineTarget.sendMessage(plugin.getPrefix() + got);
            }
        }
        return true;
    }

    private boolean handleBalance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "balance")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            String balance = plugin.getLanguageConfig(player).getString("Money.MSG.Balance");
            balance = colorizeAndFill(
                    balance,
                    "[Money]",
                    plugin.getVaultManager().getEco().format(plugin.getVaultManager().getEco().getBalance(player)) + plugin.getCurrencySymbol()
            );
            player.sendMessage(plugin.getPrefix() + balance);
            return true;
        }

        if (args.length == 1) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "balance.others")) {
                sendNoPerm(sender);
                return true;
            }
            OfflinePlayer player = requireOfflinePlayer(sender, args[0]);
            if (player == null) {
                return true;
            }
            String balance = plugin.getLanguageConfig(sender).getString("Money.MoneyBalance.Other.MSG");
            balance = colorizeAndFill(
                    balance,
                    "[Target]", String.valueOf(player.getName()),
                    "[Money]", plugin.getVaultManager().getEco().format(plugin.getVaultManager().getEco().getBalance(player)) + plugin.getCurrencySymbol()
            );
            sender.sendMessage(plugin.getPrefix() + balance);
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/balance §cor §6/balance <PlayerName>"));
        return true;
    }

    private boolean handleEco(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "§cPlease use §6/eco <add|remove|set> <Amount> [PlayerName]");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (!isDouble(args[1])) {
            if (sender instanceof Player player) {
                sendInvalidNumber(player, args[1]);
            } else {
                sender.sendMessage(plugin.getPrefix() + "§6" + args[1] + " §cisn't a Number!");
            }
            return true;
        }

        double amount = Double.parseDouble(args[1]);
        boolean self = args.length == 2;

        return switch (action) {
            case "add" -> handleEcoAdd(sender, amount, self ? null : args[2]);
            case "remove" -> handleEcoRemove(sender, amount, self ? null : args[2]);
            case "set" -> handleEcoSet(sender, amount, self ? null : args[2]);
            default -> {
                sender.sendMessage(plugin.getPrefix() + "§cPlease use §6/eco <add|remove|set> <Amount> [PlayerName]");
                yield true;
            }
        };
    }

    private boolean handleEcoAdd(CommandSender sender, double amount, String targetName) {
        if (targetName == null) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "eco.add")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            plugin.getVaultManager().getEco().depositPlayer(player, amount);
            String set = colorizeAndFill(
                    plugin.getLanguageConfig(player).getString("Money.MSG.Set"),
                    "[Money]", plugin.getVaultManager().getEco().getBalance(player) + plugin.getCurrencySymbol()
            );
            player.sendMessage(plugin.getPrefix() + set);
            return true;
        }

        if (!sender.hasPermission(plugin.getPermissionBase() + "eco.add.others")) {
            sendNoPerm(sender);
            return true;
        }

        OfflinePlayer target;
        if (Bukkit.getOnlineMode()) {
            UUID id = UUIDFetcher.getUUID(targetName);
            if (id == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(targetName));
                return true;
            }
            target = Bukkit.getOfflinePlayer(id);
        } else {
            target = requireOfflinePlayer(sender, targetName);
            if (target == null) {
                return true;
            }
        }

        plugin.getVaultManager().getEco().depositPlayer(target, amount);
        sendEcoOtherResult(sender, target, amount);
        return true;
    }

    private boolean handleEcoRemove(CommandSender sender, double amount, String targetName) {
        if (targetName == null) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "eco.add")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            plugin.getVaultManager().getEco().withdrawPlayer(player, amount);
            String set = colorizeAndFill(
                    plugin.getLanguageConfig(player).getString("Money.MSG.Set"),
                    "[Money]", plugin.getVaultManager().getEco().getBalance(player) + plugin.getCurrencySymbol()
            );
            player.sendMessage(plugin.getPrefix() + set);
            return true;
        }

        if (!sender.hasPermission(plugin.getPermissionBase() + "eco.add.others")) {
            sendNoPerm(sender);
            return true;
        }

        OfflinePlayer target = requireOfflinePlayer(sender, targetName);
        if (target == null) {
            return true;
        }

        plugin.getVaultManager().getEco().withdrawPlayer(target, amount);
        sendEcoOtherResult(sender, target, amount);
        return true;
    }

    private boolean handleEcoSet(CommandSender sender, double amount, String targetName) {
        if (targetName == null) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "eco.set")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            plugin.getVaultManager().getEco().withdrawPlayer(player, plugin.getVaultManager().getEco().getBalance(player));
            plugin.getVaultManager().getEco().depositPlayer(player, amount);
            String set = colorizeAndFill(
                    plugin.getLanguageConfig(player).getString("Money.MSG.Set"),
                    "[Money]", amount + plugin.getCurrencySymbol()
            );
            player.sendMessage(plugin.getPrefix() + set);
            return true;
        }

        if (!sender.hasPermission(plugin.getPermissionBase() + "eco.set.others")) {
            sendNoPerm(sender);
            return true;
        }

        OfflinePlayer target = requireOfflinePlayer(sender, targetName);
        if (target == null) {
            return true;
        }

        plugin.getVaultManager().getEco().withdrawPlayer(target, plugin.getVaultManager().getEco().getBalance(target));
        plugin.getVaultManager().getEco().depositPlayer(target, amount);
        sendEcoOtherResult(sender, target, amount);
        return true;
    }

    private void sendEcoOtherResult(CommandSender sender, OfflinePlayer target, double amount) {
        String setOther = colorizeAndFill(
                plugin.getLanguageConfig(sender).getString("Money.MoneySet.Other.MSG"),
                "[Target]", String.valueOf(target.getName()),
                "[Money]", plugin.getVaultManager().getEco().getBalance(target) + plugin.getCurrencySymbol()
        );
        sender.sendMessage(plugin.getPrefix() + setOther);

        if (target.isOnline() && target instanceof Player onlineTarget && !Main.getSilent().contains(sender.getName())) {
            String set = colorizeAndFill(
                    plugin.getLanguageConfig(onlineTarget).getString("Money.MSG.Set"),
                    "[Money]", amount + plugin.getCurrencySymbol()
            );
            onlineTarget.sendMessage(plugin.getPrefix() + set);
        }
    }

    private boolean handleBalanceTop(CommandSender sender) {
        if (!sender.hasPermission(plugin.getPermissionBase() + "balancetop")) {
            return true;
        }

        Map<String, Double> mostPlayers = new HashMap<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName() != null) {
                mostPlayers.put(online.getName(), calculateTotalBalance(online));
            }
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && !mostPlayers.containsKey(offline.getName())) {
                mostPlayers.put(offline.getName(), calculateTotalBalance(offline));
            }
        }

        TreeMap<String, Double> sorted = new TreeMap<>(new ValueComparator(mostPlayers));
        sorted.putAll(mostPlayers);

        int i = 0;
        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
            i++;
            sender.sendMessage("§a" + i + "st [§6" + entry.getKey() + " §b: " + entry.getValue() + plugin.getCurrencySymbolMulti() + "§a]");
            if (i == 3) {
                break;
            }
        }

        return true;
    }

    private double calculateTotalBalance(OfflinePlayer player) {
        double total = plugin.getVaultManager().getEco().getBalance(player);
        for (String bank : plugin.getVaultManager().getEco().getBanks()) {
            boolean member = plugin.getVaultManager().getEco().isBankMember(bank, player).transactionSuccess();
            boolean owner = plugin.getVaultManager().getEco().isBankOwner(bank, player).transactionSuccess();
            if (member || owner) {
                total += plugin.getVaultManager().getEco().bankBalance(bank).balance;
            }
        }
        return total;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (CMD_PAY.equals(cmd)) {
            if (args.length == 1) {
                if (sender instanceof Player player && sender.hasPermission(plugin.getPermissionBase() + "pay")) {
                    List<String> list = new ArrayList<>();
                    list.add(plugin.getVaultManager().getEco().format(plugin.getVaultManager().getEco().getBalance(player)));
                    return list;
                }
                return List.of();
            }

            if (args.length == 2) {
                List<String> players = new ArrayList<>();
                players.add("**");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                return filterAndSort(players, args[1]);
            }
        }

        if (CMD_ECO.equals(cmd) && args.length == 1) {
            return filterAndSort(List.of("add", "remove", "set"), args[0]);
        }

        return List.of();
    }

    private List<String> filterAndSort(List<String> source, String prefix) {
        List<String> filtered = new ArrayList<>();
        String check = prefix.toLowerCase(Locale.ROOT);
        for (String value : source) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(check)) {
                filtered.add(value);
            }
        }
        filtered.sort(String::compareToIgnoreCase);
        return filtered;
    }

    record ValueComparator(Map<String, Double> base) implements Comparator<String> {

        @Override
        @SuppressWarnings("ComparatorMethodParameterNotUsed")
        public int compare(String a, String b) {
            double left = base.getOrDefault(a, 0.0D);
            double right = base.getOrDefault(b, 0.0D);
            return left >= right ? -1 : 1; // returning 0 would merge keys
        }
    }
}
