package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.TextUtils;
import ch.framedev.essentialsmini.utils.Variables;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.util.List;
import java.util.Locale;

/**
 * This Plugin was Created by FrameDev
 * Package : ch.framedev.essentialsmini.commands
 * Date: 23.11.2020
 * Last changes made: 10.04.2026
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
public class BankCMD extends CommandBase {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "remove", "create", "balance", "withdraw", "deposit", "addmember",
            "removemember", "listmembers", "list", "info", "transfer"
    );

    private final Main plugin;
    private final TextUtils textUtils = new TextUtils();

    public BankCMD(Main plugin) {
        super(plugin, "bank");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (!ensureEconomyAvailable(sender)) {
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "list" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            case "create" -> handleCreate(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "balance" -> handleBalance(sender, args);
            case "deposit" -> handleDeposit(sender, args);
            case "withdraw" -> handleWithdraw(sender, args);
            case "addmember" -> handleAddMember(sender, args);
            case "removemember" -> handleRemoveMember(sender, args);
            case "listmembers" -> handleListMembers(sender, args);
            case "transfer" -> handleTransfer(sender, args);
            default -> true;
        };
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length != 1) return true;
        if (!hasPermission(sender, "essentialsmini.bank.list")) return true;

        List<String> banks = plugin.getVaultManager().getBanks();
        String joined = banks == null || banks.isEmpty() ? "-" : String.join(", ", banks);
        sendFramedList(sender, joined);
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length != 2) return true;
        if (!hasPermission(sender, plugin.getPermissionBase() + "bank.info")) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(sender);
            return true;
        }

        OfflinePlayer owner = null;
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (isOwner(bankName, player)) {
                owner = player;
                break;
            }
        }

        net.milkbowl.vault.economy.Economy economy = economyOrNull();
        double balanceValue = economy == null ? 0.0D : economy.bankBalance(bankName).balance;
        sender.sendMessage(plugin.getPrefix() + "BankName : " + bankName);
        sender.sendMessage(plugin.getPrefix() + "Balance : " + balanceValue);
        if (owner != null) {
            sender.sendMessage(plugin.getPrefix() + "Owner : " + (owner.getName() == null ? "Unknown" : owner.getName()));
        }
        List<String> members = plugin.getVaultManager().getBankMembers(bankName);
        sender.sendMessage(plugin.getPrefix() + "Members : " + (members == null ? "[]" : members));
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length != 2) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.create")) return true;

        if (bankExists(args[1])) {
            player.sendMessage(plugin.getPrefix() + "§6" + args[1] + " §calready exists!");
            return true;
        }

        Economy economy = economyOrNull();
        if (economy == null || !ensureAccount(player, player)) {
            return true;
        }

        EconomyResponse response = economy.createBank(args[1], player);
        if (response.transactionSuccess()) {
            player.sendMessage(plugin.getPrefix() + lang(player, "Created"));
        } else {
            sendError(player, "Creating Bank!", response.errorMessage == null ? "Unknown" : response.errorMessage);
        }
        return true;
    }

    private boolean handleBalance(CommandSender sender, String[] args) {
        if (args.length != 2) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.balance")) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(player);
            return true;
        }

        if (!isOwnerOrMember(bankName, player)) {
            player.sendMessage(plugin.getPrefix() + lang(player, "NotOwnerOrMember"));
            return true;
        }

        String balance = langWithObject(player, "Balance", "%Balance%",
                String.valueOf(plugin.getVaultManager().getEconomy().bankBalance(bankName).balance));
        player.sendMessage(plugin.getPrefix() + balance);
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length != 2) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.remove")) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(player);
            return true;
        }

        if (!isOwner(bankName, player)) {
            player.sendMessage(plugin.getPrefix() + lang(player, "NotOwner"));
            return true;
        }

        if (plugin.getVaultManager().getEconomy().deleteBank(bankName).transactionSuccess()) {
            player.sendMessage(plugin.getPrefix() + lang(player, "Deleted"));
        } else {
            sendError(player, "deleting Bank!", "Error : None");
        }
        return true;
    }

    private boolean handleListMembers(CommandSender sender, String[] args) {
        if (args.length != 2) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.listmembers")) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(player);
            return true;
        }
        if (!isOwnerOrMember(bankName, player)) {
            player.sendMessage(plugin.getPrefix() + lang(player, "NotOwnerOrMember"));
            return true;
        }

        List<String> members = plugin.getVaultManager().getBankMembers(bankName);
        String joinedMembers = members == null || members.isEmpty() ? "-" : String.join(", ", members);
        sendFramedList(player, joinedMembers);
        return true;
    }

    private boolean handleDeposit(CommandSender sender, String[] args) {
        if (args.length != 3) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.deposit")) return true;
        Economy economy = economyOrNull();
        if (economy == null || !ensureAccount(player, player)) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(player);
            return true;
        }

        Double amount = parsePositiveAmount(sender, args[2]);
        if (amount == null) return true;

        if (!economy.has(player, amount)) {
            player.sendMessage(plugin.getPrefix() + "§cNot enougt Money!");
            return true;
        }

        EconomyResponse response = economy.bankDeposit(bankName, amount);
        if (response.transactionSuccess()) {
            EconomyResponse withdraw = economy.withdrawPlayer(player, amount);
            if (withdraw.transactionSuccess()) {
                player.sendMessage(plugin.getPrefix() + langWithObject(player, "Deposit", "%Amount%", String.valueOf(amount)));
                return true;
            }
            economy.bankWithdraw(bankName, amount);
            sendError(player, "Deposit to the Bank!", withdraw.errorMessage == null ? "Error : None" : withdraw.errorMessage);
            return true;
        }
        sendError(player, "Deposit to the Bank!", response.errorMessage == null ? "Error : None" : response.errorMessage);
        return true;
    }

    private boolean handleWithdraw(CommandSender sender, String[] args) {
        if (args.length != 3) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.withdraw")) return true;
        Economy economy = economyOrNull();
        if (economy == null || !ensureAccount(player, player)) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(player);
            return true;
        }
        if (!isOwnerOrMember(bankName, player)) {
            player.sendMessage(plugin.getPrefix() + lang(player, "NotOwnerOrMember"));
            return true;
        }

        Double amount = parsePositiveAmount(sender, args[2]);
        if (amount == null) return true;

        if (!economy.bankHas(bankName, amount).transactionSuccess()) {
            player.sendMessage(plugin.getPrefix() + "§cThe Bank has not enought Money!");
            return true;
        }

        EconomyResponse withdraw = economy.bankWithdraw(bankName, amount);
        if (!withdraw.transactionSuccess()) {
            sendError(player, "Withdraw from the Bank!", withdraw.errorMessage == null ? "Error : None" : withdraw.errorMessage);
            return true;
        }
        EconomyResponse deposit = economy.depositPlayer(player, amount);
        if (!deposit.transactionSuccess()) {
            economy.bankDeposit(bankName, amount);
            sendError(player, "Withdraw from the Bank!", deposit.errorMessage == null ? "Error : None" : deposit.errorMessage);
            return true;
        }
        player.sendMessage(plugin.getPrefix() + langWithObject(player, "Withdraw", "%Amount%", String.valueOf(amount)));
        return true;
    }

    private boolean handleAddMember(CommandSender sender, String[] args) {
        if (args.length != 3) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.addmember")) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(player);
            return true;
        }
        if (!isOwner(bankName, player)) {
            player.sendMessage(plugin.getPrefix() + lang(player, "NotOwner"));
            return true;
        }

        OfflinePlayer offline = PlayerUtils.getOfflinePlayerByName(args[2]);
        String targetName = offline.getName();
        if (targetName == null || targetName.isBlank()) {
            player.sendMessage(plugin.getPrefix() + "§cPlayer not found: " + args[2]);
            return true;
        }

        plugin.getVaultManager().addBankMember(bankName, offline);
        player.sendMessage(plugin.getPrefix() + "§6" + targetName + " §ais now Successfully a Member of your Bank!");

        if (offline.isOnline() && offline.getPlayer() != null) {
            offline.getPlayer().sendMessage(plugin.getPrefix() + "§aYou are now a Member of §6" + player.getName() + "'s §aBank!");
        }
        return true;
    }

    private boolean handleRemoveMember(CommandSender sender, String[] args) {
        if (args.length != 3) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.removemember")) return true;

        String bankName = args[1];
        if (!bankExists(bankName)) {
            sendBankNotFound(player);
            return true;
        }
        if (!isOwner(bankName, player)) {
            player.sendMessage(plugin.getPrefix() + lang(player, "NotOwner"));
            return true;
        }

        OfflinePlayer offline = PlayerUtils.getOfflinePlayerByName(args[2]);
        String targetName = offline.getName();
        if (targetName == null || targetName.isBlank()) {
            player.sendMessage(plugin.getPrefix() + "§cPlayer not found: " + args[2]);
            return true;
        }

        plugin.getVaultManager().removeBankMember(bankName, offline);
        player.sendMessage(plugin.getPrefix() + "§6" + targetName + " §ais no longer a member of your Bank!");

        if (offline.isOnline() && offline.getPlayer() != null) {
            offline.getPlayer().sendMessage(plugin.getPrefix() + "§cYou are no longer a Member of §6" + player.getName() + "'s §cBank!");
        }
        return true;
    }

    private boolean handleTransfer(CommandSender sender, String[] args) {
        if (args.length != 4) return true;
        Player player = requirePlayer(sender);
        if (player == null || !hasPermission(player, "essentialsmini.bank.transfer")) return true;

        String fromBank = args[1];
        String toBank = args[2];
        if (!bankExists(fromBank)) {
            sendBankNotFound(player);
            return true;
        }
        if (!isOwner(fromBank, player)) {
            player.sendMessage(plugin.getPrefix() + lang(player, "NotOwner"));
            return true;
        }
        if (!bankExists(toBank)) {
            player.sendMessage(plugin.getPrefix() + "§6" + toBank + " §cdoesn't exist!");
            return true;
        }

        Double amount = parsePositiveAmount(sender, args[3]);
        if (amount == null) return true;
        Economy economy = economyOrNull();
        if (economy == null) return true;

        if (!economy.bankHas(fromBank, amount).transactionSuccess()) {
            player.sendMessage(plugin.getPrefix() + "§cThe Bank has not enought Money!");
            return true;
        }

        EconomyResponse withdraw = economy.bankWithdraw(fromBank, amount);
        if (!withdraw.transactionSuccess()) {
            sendError(player, "Transfer from the Bank!", withdraw.errorMessage == null ? "Error : None" : withdraw.errorMessage);
            return true;
        }
        EconomyResponse deposit = economy.bankDeposit(toBank, amount);
        if (!deposit.transactionSuccess()) {
            economy.bankDeposit(fromBank, amount);
            sendError(player, "Transfer to the Bank!", deposit.errorMessage == null ? "Error : None" : deposit.errorMessage);
            return true;
        }
        player.sendMessage(plugin.getPrefix() + "§aYou transferred §6" + amount + " §ato the Bank §6" + toBank + "!");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (economyOrNull() == null) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return filterByPrefix(SUB_COMMANDS, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list")) return new ArrayList<>();
            if (args[0].equalsIgnoreCase("info")) return new ArrayList<>();
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("remove"))
                return new ArrayList<>(Collections.singletonList("<BANKNAME>"));

            List<String> banksList = new ArrayList<>();
            OfflinePlayer senderOffline = (sender instanceof Player) ? Bukkit.getOfflinePlayer(((Player) sender).getUniqueId()) : null;

            for (String banks : safeBanks()) {
                if (senderOffline == null || plugin.getVaultManager().getEconomy().isBankMember(banks, senderOffline).transactionSuccess() || plugin.getVaultManager().getEconomy().isBankOwner(banks, senderOffline).transactionSuccess()) {
                    banksList.add(banks);
                }
            }
            return filterByPrefix(banksList, args[1]);
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("listmembers") || args[0].equalsIgnoreCase("info"))
                return new ArrayList<>();
            if (args[0].equalsIgnoreCase("balance")) return new ArrayList<>();

            if (args[0].equalsIgnoreCase("addmember") || args[0].equalsIgnoreCase("removemember")) {
                List<String> players = new ArrayList<>();
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (offlinePlayer.getName() != null) players.add(offlinePlayer.getName());
                }
                return filterByPrefix(players, args[2]);
            }

            if (args[0].equalsIgnoreCase("transfer")) {
                return filterByPrefix(safeBanks(), args[2]);
            }

            List<String> empty = new ArrayList<>();
            if (args[0].equalsIgnoreCase("deposit")) {
                double bal = 0.0;
                if (sender instanceof Player) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(((Player) sender).getUniqueId());
                    net.milkbowl.vault.economy.Economy economy = economyOrNull();
                    bal = economy == null ? 0.0D : economy.getBalance(off);
                }
                empty.add(String.valueOf(bal));
            } else if (args[0].equalsIgnoreCase("withdraw")) {
                double bal = 0.0;
                try {
                    net.milkbowl.vault.economy.Economy economy = economyOrNull();
                    if (economy != null) {
                        bal = economy.bankBalance(args[1]).balance;
                    }
                } catch (Exception ignored) {
                }
                empty.add(String.valueOf(bal));
            }
            return empty;
        } else if (args.length == 4) {
            List<String> empty = new ArrayList<>();
            try {
                net.milkbowl.vault.economy.Economy economy = economyOrNull();
                if (economy != null) {
                    empty.add(String.valueOf(economy.bankBalance(args[1]).balance));
                }
            } catch (Exception ignored) {
            }
            return empty;
        }
        return super.onTabComplete(sender, command, label, args);
    }

    private void sendHelp(CommandSender sender) {
        String prefix = plugin.getPrefix();
        sender.sendMessage(prefix + "§a/bank list");
        sender.sendMessage(prefix + "§a/bank info <BankName>");
        sender.sendMessage(prefix + "§a/bank create <BankName>");
        sender.sendMessage(prefix + "§a/bank remove <BankName>");
        sender.sendMessage(prefix + "§a/bank balance <BankName>");
        sender.sendMessage(prefix + "§a/bank deposit <BankName> <Amount>");
        sender.sendMessage(prefix + "§a/bank withdraw <BankName> <Amount>");
        sender.sendMessage(prefix + "§a/bank addmember <BankName> <Player>");
        sender.sendMessage(prefix + "§a/bank removemember <BankName> <Player>");
        sender.sendMessage(prefix + "§a/bank listmembers <BankName>");
        sender.sendMessage(prefix + "§a/bank transfer <FromBank> <ToBank> <Amount>");
    }

    private boolean bankExists(String bankName) {
        return safeBanks().contains(bankName);
    }

    private List<String> safeBanks() {
        net.milkbowl.vault.economy.Economy economy = economyOrNull();
        if (economy == null) {
            return Collections.emptyList();
        }
        List<String> banks = economy.getBanks();
        return banks == null ? Collections.emptyList() : banks;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
        return null;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
        return false;
    }

    private boolean isOwner(String bankName, OfflinePlayer player) {
        net.milkbowl.vault.economy.Economy economy = economyOrNull();
        if (economy == null) return false;
        return economy.isBankOwner(bankName, player).transactionSuccess();
    }

    private boolean isOwnerOrMember(String bankName, OfflinePlayer player) {
        net.milkbowl.vault.economy.Economy economy = economyOrNull();
        if (economy == null) return false;
        return economy.isBankOwner(bankName, player).transactionSuccess() || economy.isBankMember(bankName, player).transactionSuccess();
    }

    private String lang(CommandSender sender, String key) {
        FileConfiguration languageConfig = plugin.getLanguageConfig(sender);
        if (languageConfig == null) {
            return "";
        }
        String value = languageConfig.getString(Variables.BANK + "." + key);
        return textUtils.replaceAndWithParagraph(value == null ? "" : value);
    }

    private String langWithObject(CommandSender sender, String key, String placeholder, String replacement) {
        String translated = lang(sender, key);
        return textUtils.replaceObject(translated, placeholder, replacement);
    }

    private void sendError(CommandSender sender, String reason, String errorText) {
        String error = langWithObject(sender, "Error", "%Reason%", reason);
        error = textUtils.replaceObject(error, "%Error%", errorText);
        sender.sendMessage(plugin.getPrefix() + error);
    }

    private void sendBankNotFound(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + lang(sender, "NotFound"));
    }

    private void sendFramedList(CommandSender sender, String line) {
        sender.sendMessage(plugin.getPrefix() + "§6<<<===>>>");
        sender.sendMessage(plugin.getPrefix() + "§a" + line);
        sender.sendMessage(plugin.getPrefix() + "§6<<<===>>>");
    }

    private List<String> filterByPrefix(List<String> values, String argPrefix) {
        List<String> filtered = new ArrayList<>();
        if (values == null) {
            return filtered;
        }

        String lowerPrefix = argPrefix == null ? "" : argPrefix.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                filtered.add(value);
            }
        }
        Collections.sort(filtered);
        return filtered;
    }

    private Double parsePositiveAmount(CommandSender sender, String raw) {
        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.getPrefix() + "§cInvalid amount: " + raw);
            return null;
        }

        if (!Double.isFinite(amount) || amount <= 0.0D) {
            sender.sendMessage(plugin.getPrefix() + "§cAmount must be greater than 0.");
            return null;
        }
        return amount;
    }

    private boolean ensureAccount(CommandSender sender, OfflinePlayer player) {
        Economy economy = economyOrNull();
        if (economy == null) {
            sender.sendMessage(plugin.getPrefix() + "§cEconomy provider is not available.");
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

    private net.milkbowl.vault.economy.Economy economyOrNull() {
        if (plugin.getVaultManager() == null) {
            return null;
        }
        return plugin.getVaultManager().getEconomy();
    }

    private boolean ensureEconomyAvailable(CommandSender sender) {
        if (economyOrNull() != null) {
            return true;
        }
        sender.sendMessage(plugin.getPrefix() + "§cEconomy provider is not available.");
        return false;
    }
}
