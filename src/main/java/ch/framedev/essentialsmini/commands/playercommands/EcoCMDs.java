package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.Language;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TextUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
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

    private void sendInvalidNumber(CommandSender sender, String input) {
        if (sender instanceof Player player && plugin.getLanguage(player) == Language.DE) {
            sender.sendMessage(plugin.getPrefix() + "§6" + input + " §cist keine gültige Zahl!");
        } else {
            sender.sendMessage(plugin.getPrefix() + "§6" + input + " §cisn't a valid number!");
        }
    }

    private void sendInvalidAmount(CommandSender sender, String input, boolean allowZero) {
        if (parseRawAmount(input) == null) {
            sendInvalidNumber(sender, input);
            return;
        }
        sender.sendMessage(plugin.getPrefix() + (allowZero ? "§cAmount must be 0 or higher." : "§cAmount must be greater than 0."));
    }

    private Double parseRawAmount(String text) {
        try {
            double amount = Double.parseDouble(text);
            if (!Double.isFinite(amount)) {
                return null;
            }
            return amount;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double parseAmount(String text, boolean allowZero) {
        Double amount = parseRawAmount(text);
        if (amount == null) {
            return null;
        }
        if (allowZero ? amount < 0.0D : amount <= 0.0D) {
            return null;
        }
        return amount;
    }

    private boolean isDouble(String text) {
        return parseRawAmount(text) != null;
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
        Economy economy = economyOrNull();
        if (economy == null) {
            sendEconomyUnavailable(sender);
            return true;
        }

        if (!(sender instanceof Player payer)) {
            sendOnlyPlayer(sender);
            return true;
        }
        if (!sender.hasPermission(plugin.getPermissionBase() + "pay")) {
            sendNoPerm(sender);
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/pay <Amount> <PlayerName> §cor §6/pay <PlayerName> <Amount>"));
            return true;
        }

        PayArguments payArguments = parsePayArguments(sender, args);
        if (payArguments == null) {
            return true;
        }

        double amount = payArguments.amount();
        if (!ensureAccount(sender, payer)) {
            return true;
        }

        if (payArguments.targetName().equalsIgnoreCase("**")) {
            List<Player> targets = new ArrayList<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.getUniqueId().equals(payer.getUniqueId())) {
                    targets.add(target);
                }
            }
            if (targets.isEmpty()) {
                sender.sendMessage(plugin.getPrefix() + "§cThere are no other online players to pay.");
                return true;
            }
            double total = amount * targets.size();
            if (!economy.has(payer, total)) {
                sendNotEnoughMoney(sender, payer);
                return true;
            }

            for (Player target : targets) {
                if (!ensureAccount(sender, target)) {
                    return true;
                }
            }

            for (Player target : targets) {
                EconomyResponse withdraw = economy.withdrawPlayer(payer, amount);
                if (!withdraw.transactionSuccess()) {
                    sendEconomyError(sender, withdraw);
                    return true;
                }
                EconomyResponse deposit = economy.depositPlayer(target, amount);
                if (!deposit.transactionSuccess()) {
                    economy.depositPlayer(payer, amount);
                    sendEconomyError(sender, deposit);
                    return true;
                }

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
                    plugin.sendConfiguredNotification(target, "moneyReceived", "economy", got,
                            Map.of("%Player%", sender.getName(), "%Money%", amount + plugin.getCurrencySymbol()));
                }
                sender.sendMessage(plugin.getPrefix() + send);
            }
            return true;
        }

        OfflinePlayer target = requireOfflinePlayer(sender, payArguments.targetName());
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(payArguments.targetName()));
            return true;
        }
        if (!ensureAccount(sender, target)) {
            return true;
        }

        if (!economy.has(payer, amount)) {
            sendNotEnoughMoney(sender, payer);
            return true;
        }

        EconomyResponse withdraw = economy.withdrawPlayer(payer, amount);
        if (!withdraw.transactionSuccess()) {
            sendEconomyError(sender, withdraw);
            return true;
        }
        EconomyResponse deposit = economy.depositPlayer(target, amount);
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(payer, amount);
            sendEconomyError(sender, deposit);
            return true;
        }

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
                plugin.sendConfiguredNotification(onlineTarget, "moneyReceived", "economy", got,
                        Map.of("%Player%", sender.getName(), "%Money%", amount + plugin.getCurrencySymbol()));
            }
        }
        return true;
    }

    private boolean handleBalance(CommandSender sender, String[] args) {
        Economy economy = economyOrNull();
        if (economy == null) {
            sendEconomyUnavailable(sender);
            return true;
        }

        if (args.length == 0) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "balance")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            if (!ensureAccount(sender, player)) {
                return true;
            }
            String balance = plugin.getLanguageConfig(player).getString("Money.MSG.Balance");
            balance = colorizeAndFill(
                    balance,
                    "[Money]",
                    economy.format(economy.getBalance(player)) + plugin.getCurrencySymbol()
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
            if (!ensureAccount(sender, player)) {
                return true;
            }
            String balance = plugin.getLanguageConfig(sender).getString("Money.MoneyBalance.Other.MSG");
            balance = colorizeAndFill(
                    balance,
                    "[Target]", String.valueOf(player.getName()),
                    "[Money]", economy.format(economy.getBalance(player)) + plugin.getCurrencySymbol()
            );
            sender.sendMessage(plugin.getPrefix() + balance);
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("§6/balance §cor §6/balance <PlayerName>"));
        return true;
    }

    private boolean handleEco(CommandSender sender, String[] args) {
        Economy economy = economyOrNull();
        if (economy == null) {
            sendEconomyUnavailable(sender);
            return true;
        }

        if (args.length < 2) {
            sendEcoUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("add", "remove", "set").contains(action)) {
            sendEcoUsage(sender);
            return true;
        }

        EcoArguments ecoArguments = parseEcoArguments(sender, action, args);
        if (ecoArguments == null) {
            return true;
        }

        return switch (action) {
            case "add" -> handleEcoAdd(sender, ecoArguments.amount(), ecoArguments.targetName());
            case "remove" -> handleEcoRemove(sender, ecoArguments.amount(), ecoArguments.targetName());
            case "set" -> handleEcoSet(sender, ecoArguments.amount(), ecoArguments.targetName());
            default -> true;
        };
    }

    private EcoArguments parseEcoArguments(CommandSender sender, String action, String[] args) {
        boolean allowZero = action.equals("set");
        if (args.length == 2) {
            Double amount = parseAmount(args[1], allowZero);
            if (amount == null) {
                sendInvalidAmount(sender, args[1], allowZero);
                return null;
            }
            return new EcoArguments(amount, null);
        }

        if (args.length == 3) {
            Double first = parseAmount(args[1], allowZero);
            Double second = parseAmount(args[2], allowZero);

            if (first != null) {
                return new EcoArguments(first, args[2]);
            }

            if (second != null) {
                return new EcoArguments(second, args[1]);
            }

            sendInvalidAmount(sender, args[1], allowZero);
            return null;
        }

        sendEcoUsage(sender);
        return null;
    }

    private void sendEcoUsage(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + "§cPlease use §6/eco <add|remove|set> <Amount> [PlayerName] §cor §6/eco <add|remove|set> <PlayerName> <Amount>");
    }

    private boolean handleEcoAdd(CommandSender sender, double amount, String targetName) {
        Economy economy = economyOrNull();
        if (targetName == null) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "eco.add")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            if (!ensureAccount(sender, player)) {
                return true;
            }
            EconomyResponse response = economy.depositPlayer(player, amount);
            if (!response.transactionSuccess()) {
                sendEconomyError(sender, response);
                return true;
            }
            String set = colorizeAndFill(
                    plugin.getLanguageConfig(player).getString("Money.MSG.Set"),
                    "[Money]", economy.getBalance(player) + plugin.getCurrencySymbol()
            );
            player.sendMessage(plugin.getPrefix() + set);
            return true;
        }

        if (!sender.hasPermission(plugin.getPermissionBase() + "eco.add.others")) {
            sendNoPerm(sender);
            return true;
        }

        OfflinePlayer target = requireOfflinePlayer(sender, targetName);
        if (target == null || !ensureAccount(sender, target)) {
            return true;
        }

        EconomyResponse response = economy.depositPlayer(target, amount);
        if (!response.transactionSuccess()) {
            sendEconomyError(sender, response);
            return true;
        }
        sendEcoOtherResult(sender, target, amount);
        return true;
    }

    private boolean handleEcoRemove(CommandSender sender, double amount, String targetName) {
        Economy economy = economyOrNull();
        if (targetName == null) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "eco.remove")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            if (!ensureAccount(sender, player)) {
                return true;
            }
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            if (!response.transactionSuccess()) {
                sendEconomyError(sender, response);
                return true;
            }
            String set = colorizeAndFill(
                    plugin.getLanguageConfig(player).getString("Money.MSG.Set"),
                    "[Money]", economy.getBalance(player) + plugin.getCurrencySymbol()
            );
            player.sendMessage(plugin.getPrefix() + set);
            return true;
        }

        if (!sender.hasPermission(plugin.getPermissionBase() + "eco.remove.others")) {
            sendNoPerm(sender);
            return true;
        }

        OfflinePlayer target = requireOfflinePlayer(sender, targetName);
        if (target == null || !ensureAccount(sender, target)) {
            return true;
        }

        EconomyResponse response = economy.withdrawPlayer(target, amount);
        if (!response.transactionSuccess()) {
            sendEconomyError(sender, response);
            return true;
        }
        sendEcoOtherResult(sender, target, amount);
        return true;
    }

    private boolean handleEcoSet(CommandSender sender, double amount, String targetName) {
        Economy economy = economyOrNull();
        if (targetName == null) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "eco.set")) {
                sendNoPerm(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sendOnlyPlayer(sender);
                return true;
            }
            if (!ensureAccount(sender, player)) {
                return true;
            }
            EconomyResponse response = setBalance(player, amount);
            if (!response.transactionSuccess()) {
                sendEconomyError(sender, response);
                return true;
            }
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
        if (target == null || !ensureAccount(sender, target)) {
            return true;
        }

        EconomyResponse response = setBalance(target, amount);
        if (!response.transactionSuccess()) {
            sendEconomyError(sender, response);
            return true;
        }
        sendEcoOtherResult(sender, target, amount);
        return true;
    }

    private void sendEcoOtherResult(CommandSender sender, OfflinePlayer target, double amount) {
        Economy economy = economyOrNull();
        if (economy == null) {
            return;
        }
        String setOther = colorizeAndFill(
                plugin.getLanguageConfig(sender).getString("Money.MoneySet.Other.MSG"),
                "[Target]", String.valueOf(target.getName()),
                "[Money]", economy.getBalance(target) + plugin.getCurrencySymbol()
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
        if (economyOrNull() == null) {
            sendEconomyUnavailable(sender);
            return true;
        }
        if (!sender.hasPermission(plugin.getPermissionBase() + "balancetop")) {
            sendNoPerm(sender);
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
        Economy economy = economyOrNull();
        if (economy == null) {
            return 0.0D;
        }

        double total = economy.getBalance(player);
        for (String bank : economy.getBanks()) {
            boolean member = economy.isBankMember(bank, player).transactionSuccess();
            boolean owner = economy.isBankOwner(bank, player).transactionSuccess();
            if (member || owner) {
                total += economy.bankBalance(bank).balance;
            }
        }
        return total;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (CMD_PAY.equals(cmd)) {
            if (args.length == 1) {
                Economy economy = economyOrNull();
                if (economy != null && sender instanceof Player player && sender.hasPermission(plugin.getPermissionBase() + "pay")) {
                    List<String> list = new ArrayList<>();
                    list.add(economy.format(economy.getBalance(player)));
                    list.addAll(onlinePlayerNames());
                    return list;
                }
                return List.of();
            }

            if (args.length == 2) {
                if (isDouble(args[0])) {
                    List<String> players = new ArrayList<>();
                    players.add("**");
                    players.addAll(onlinePlayerNames());
                    return filterAndSort(players, args[1]);
                }
                return filterAndSort(amountSuggestions(), args[1]);
            }
        }

        if (CMD_ECO.equals(cmd)) {
            if (args.length == 1) {
                return filterAndSort(List.of("add", "remove", "set"), args[0]);
            }

            if (args.length == 2 && List.of("add", "remove", "set").contains(args[0].toLowerCase(Locale.ROOT))) {
                List<String> suggestions = new ArrayList<>(amountSuggestions());
                suggestions.addAll(onlinePlayerNames());
                return filterAndSort(suggestions, args[1]);
            }

            if (args.length == 3 && List.of("add", "remove", "set").contains(args[0].toLowerCase(Locale.ROOT))) {
                if (isDouble(args[1])) {
                    return filterAndSort(onlinePlayerNames(), args[2]);
                }
                return filterAndSort(amountSuggestions(), args[2]);
            }
        }

        return List.of();
    }

    private List<String> amountSuggestions() {
        return List.of("1", "10", "100", "1000");
    }

    private List<String> onlinePlayerNames() {
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return players;
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

    private PayArguments parsePayArguments(CommandSender sender, String[] args) {
        Double first = parseAmount(args[0], false);
        Double second = parseAmount(args[1], false);

        if (first != null) {
            return new PayArguments(first, args[1]);
        }

        if (second != null) {
            return new PayArguments(second, args[0]);
        }

        sendInvalidAmount(sender, args[0], false);
        return null;
    }

    private boolean ensureAccount(CommandSender sender, OfflinePlayer player) {
        Economy economy = economyOrNull();
        if (economy == null) {
            sendEconomyUnavailable(sender);
            return false;
        }
        if (economy.hasAccount(player)) {
            return true;
        }
        if (economy.createPlayerAccount(player) || economy.hasAccount(player)) {
            return true;
        }
        sender.sendMessage(plugin.getPrefix() + "§cCould not create an economy account for " + (player.getName() == null ? "that player" : player.getName()) + ".");
        return false;
    }

    private EconomyResponse setBalance(OfflinePlayer player, double amount) {
        Economy economy = economyOrNull();
        if (economy == null) {
            return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.FAILURE, "Economy provider is not available.");
        }

        double current = economy.getBalance(player);
        if (Double.compare(current, amount) == 0) {
            return new EconomyResponse(0.0D, current, EconomyResponse.ResponseType.SUCCESS, "");
        }

        if (current > amount) {
            return economy.withdrawPlayer(player, current - amount);
        }
        return economy.depositPlayer(player, amount - current);
    }

    private void sendNotEnoughMoney(CommandSender sender, OfflinePlayer player) {
        String moneySet = plugin.getLanguageConfig(sender).getString("Money.MSG.NotEnough");
        if (moneySet == null) {
            sender.sendMessage(plugin.getPrefix() + "§cMessage config 'Money.MSG.NotEnough' is missing!");
            return;
        }
        Economy economy = economyOrNull();
        double balance = economy == null ? 0.0D : economy.getBalance(player);
        moneySet = ReplaceCharConfig.replaceParagraph(moneySet);
        moneySet = ReplaceCharConfig.replaceObjectWithData(
                moneySet,
                "%Money%",
                balance + plugin.getCurrencySymbol()
        );
        sender.sendMessage(plugin.getPrefix() + moneySet);
    }

    private Economy economyOrNull() {
        if (plugin.getVaultManager() == null) {
            return null;
        }
        return plugin.getVaultManager().getEco();
    }

    private void sendEconomyUnavailable(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + "§cEconomy provider is not available. Please enable Vault and the economy feature.");
    }

    private void sendEconomyError(CommandSender sender, EconomyResponse response) {
        String error = response == null || response.errorMessage == null || response.errorMessage.isBlank()
                ? "Economy transaction failed."
                : response.errorMessage;
        sender.sendMessage(plugin.getPrefix() + "§c" + error);
    }

    private record PayArguments(double amount, String targetName) {
    }

    private record EcoArguments(double amount, String targetName) {
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
