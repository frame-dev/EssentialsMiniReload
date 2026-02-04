package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.Variables;
import ch.framedev.essentialsmini.utils.TextUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands
 * Date: 23.11.2020
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
public class BankCMD extends CommandBase {

    private final Main plugin;

    public BankCMD(Main plugin) {
        super(plugin, "bank");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String prefix = plugin.getPrefix();
        if(args.length == 0) {
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
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                if (sender.hasPermission("essentialsmini.bank.list")) {
                    List<String> banks = plugin.getVaultManager().getBanks();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < banks.size(); ++i) {
                        stringBuilder.append(banks.get(i));
                        if (i < banks.size() - 1) {
                            stringBuilder.append(", ");
                        }
                    }
                    sender.sendMessage(prefix + "§6<<<===>>>");
                    sender.sendMessage(prefix + "§a" + stringBuilder);
                    sender.sendMessage(prefix + "§6<<<===>>>");
                    return true;
                } else {
                    sender.sendMessage(prefix + plugin.getNoPerms());
                    return true;
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info")) {
                if (!sender.hasPermission(plugin.getPermissionBase() + "bank.info")) {
                    sender.sendMessage(prefix + plugin.getNoPerms());
                    return true;
                }
                String name = args[1];
                List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                if (banks != null && banks.contains(name)) {
                    OfflinePlayer owner = null;
                    for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        if (plugin.getVaultManager().getEconomy().isBankOwner(name, player).transactionSuccess()) {
                            owner = player;
                            break;
                        }
                    }
                    sender.sendMessage(prefix + "BankName : " + name);
                    sender.sendMessage(prefix + "Balance : " + plugin.getVaultManager().getEconomy().bankBalance(name).balance);
                    if (owner != null)
                        sender.sendMessage(prefix + "Owner : " + owner.getName());
                    List<String> members = plugin.getVaultManager().getBankMembers(name);
                    sender.sendMessage(prefix + "Members : " + (members == null ? "[]" : members.toString()));
                } else {
                    String bankNotFound = plugin.getLanguageConfig(sender).getString(Variables.BANK + ".NotFound");
                    bankNotFound = new TextUtils().replaceAndWithParagraph(bankNotFound);
                    sender.sendMessage(prefix + bankNotFound);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("create")) {
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.create")) {
                        EconomyResponse economyResponse = plugin.getVaultManager().getEconomy().createBank(args[1], player);
                        if (economyResponse.transactionSuccess()) {
                            String created = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Created");
                            created = new TextUtils().replaceAndWithParagraph(created);
                            player.sendMessage(prefix + created);
                        } else {
                            String error = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Error");
                            error = new TextUtils().replaceAndWithParagraph(error);
                            error = new TextUtils().replaceObject(error, "%Reason%", "Creating Bank!");
                            error = new TextUtils().replaceObject(error, "%Error%", economyResponse.errorMessage == null ? "Unknown" : economyResponse.errorMessage);
                            player.sendMessage(prefix + error);
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("balance")) {
                String bankName = args[1];
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.balance")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().isBankOwner(bankName, player).transactionSuccess() || plugin.getVaultManager().getEconomy().isBankMember(bankName, player).transactionSuccess()) {
                                String balance = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Balance");
                                balance = new TextUtils().replaceAndWithParagraph(balance);
                                balance = new TextUtils().replaceObject(balance, "%Balance%", String.valueOf(plugin.getVaultManager().getEconomy().bankBalance(bankName).balance));
                                player.sendMessage(prefix + balance);
                            } else {
                                String message = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotOwnerOrMember");
                                message = new TextUtils().replaceAndWithParagraph(message);
                                player.sendMessage(prefix + message);
                            }
                        } else {
                            String bankNotFound = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotFound");
                            bankNotFound = new TextUtils().replaceAndWithParagraph(bankNotFound);
                            player.sendMessage(prefix + bankNotFound);
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                String bankName = args[1];
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.remove")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().isBankOwner(bankName, player).transactionSuccess()) {
                                if (plugin.getVaultManager().getEconomy().deleteBank(bankName).transactionSuccess()) {
                                    String deleted = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Deleted");
                                    deleted = new TextUtils().replaceAndWithParagraph(deleted);
                                    player.sendMessage(prefix + deleted);
                                } else {
                                    String error = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Error");
                                    error = new TextUtils().replaceAndWithParagraph(error);
                                    error = new TextUtils().replaceObject(error, "%Reason%", "deleting Bank!");
                                    error = new TextUtils().replaceObject(error, "%Error%", "Error : None");
                                    player.sendMessage(prefix + error);
                                }
                            } else {
                                String message = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotOwner");
                                message = new TextUtils().replaceAndWithParagraph(message);
                                player.sendMessage(prefix + message);
                            }
                        } else {
                            String bankNotFound = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotFound");
                            bankNotFound = new TextUtils().replaceAndWithParagraph(bankNotFound);
                            player.sendMessage(prefix + bankNotFound);
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("listmembers")) {
                String bankName = args[1];
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.listmembers")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().isBankOwner(bankName, player).transactionSuccess() || plugin.getVaultManager().getEco().isBankMember(bankName, player).transactionSuccess()) {
                                List<String> bankMembers = new ArrayList<>(plugin.getVaultManager().getBankMembers(bankName));
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < bankMembers.size(); ++i) {
                                    stringBuilder.append(bankMembers.get(i));
                                    if (i < bankMembers.size() - 1) {
                                        stringBuilder.append(", ");
                                    }
                                }
                                player.sendMessage(prefix + "§6<<<===>>>");
                                player.sendMessage(prefix + "§a" + stringBuilder);
                                player.sendMessage(prefix + "§6<<<===>>>");
                                return true;
                            }
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("deposit")) {
                String bankName = args[1];
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(prefix + "§cInvalid amount: " + args[2]);
                    return true;
                }
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.deposit")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().has(player, amount)) {
                                plugin.getVaultManager().getEconomy().withdrawPlayer(player, amount);
                                if (plugin.getVaultManager().getEconomy().bankDeposit(bankName, amount).transactionSuccess()) {
                                    String deposit = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Deposit");
                                    deposit = new TextUtils().replaceAndWithParagraph(deposit);
                                    deposit = new TextUtils().replaceObject(deposit, "%Amount%", amount + "");
                                    player.sendMessage(prefix + deposit);
                                } else {
                                    String error = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Error");
                                    error = new TextUtils().replaceAndWithParagraph(error);
                                    error = new TextUtils().replaceObject(error, "%Reason%", "Deposit to the Bank!");
                                    error = new TextUtils().replaceObject(error, "%Error%", "Error : None");
                                    player.sendMessage(prefix + error);
                                }
                            } else {
                                player.sendMessage(prefix + "§cNot enougt Money!");
                            }
                        } else {
                            String bankNotFound = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotFound");
                            bankNotFound = new TextUtils().replaceAndWithParagraph(bankNotFound);
                            player.sendMessage(prefix + bankNotFound);
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            } else if (args[0].equalsIgnoreCase("withdraw")) {
                String bankName = args[1];
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(prefix + "§cInvalid amount: " + args[2]);
                    return true;
                }
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.withdraw")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().isBankOwner(bankName, player).transactionSuccess() || plugin.getVaultManager().getEconomy().isBankMember(bankName, player).transactionSuccess()) {
                                if (plugin.getVaultManager().getEconomy().bankHas(bankName, amount).transactionSuccess()) {
                                    plugin.getVaultManager().getEconomy().depositPlayer(player, amount);
                                    plugin.getVaultManager().getEconomy().bankWithdraw(bankName, amount);
                                    String withdraw = plugin.getLanguageConfig(player).getString(Variables.BANK + ".Withdraw");
                                    withdraw = new TextUtils().replaceAndWithParagraph(withdraw);
                                    withdraw = new TextUtils().replaceObject(withdraw, "%Amount%", amount + "");
                                    player.sendMessage(prefix + withdraw);
                                } else {
                                    player.sendMessage(prefix + "§cThe Bank has not enought Money!");
                                }
                            } else {
                                String message = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotOwnerOrMember");
                                message = new TextUtils().replaceAndWithParagraph(message);
                                player.sendMessage(prefix + message);
                            }
                        } else {
                            String bankNotFound = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotFound");
                            bankNotFound = new TextUtils().replaceAndWithParagraph(bankNotFound);
                            player.sendMessage(prefix + bankNotFound);
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            } else if (args[0].equalsIgnoreCase("addmember")) {
                String bankName = args[1];
                OfflinePlayer offline = PlayerUtils.getOfflinePlayerByName(args[2]);
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.addmember")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().isBankOwner(bankName, player).transactionSuccess()) {
                                plugin.getVaultManager().addBankMember(bankName, offline);
                                player.sendMessage(prefix + "§6" + offline.getName() + " §ais now Successfully a Member of your Bank!");
                                if (offline.isOnline())
                                    ((Player) offline).sendMessage(prefix + "§aYou are now a Member of §6" + player.getName() + "'s §aBank!");
                            } else {
                                String message = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotOwner");
                                message = new TextUtils().replaceAndWithParagraph(message);
                                player.sendMessage(prefix + message);
                            }
                        } else {
                            String bankNotFound = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotFound");
                            bankNotFound = new TextUtils().replaceAndWithParagraph(bankNotFound);
                            player.sendMessage(prefix + bankNotFound);
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            } else if (args[0].equalsIgnoreCase("removemember")) {
                String bankName = args[1];
                OfflinePlayer offline = PlayerUtils.getOfflinePlayerByName(args[2]);
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.removemember")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().isBankOwner(bankName, player).transactionSuccess()) {
                                plugin.getVaultManager().removeBankMember(bankName, offline);
                                player.sendMessage(prefix + "§6" + offline.getName() + " §ais no longer a member of your Bank!");
                                if (offline.isOnline())
                                    ((Player) offline).sendMessage(prefix + "§cYou are no longer a Member of §6" + player.getName() + "'s §cBank!");
                            } else {
                                String message = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotOwner");
                                message = new TextUtils().replaceAndWithParagraph(message);
                                player.sendMessage(prefix + message);
                            }
                        } else {
                            String bankNotFound = plugin.getLanguageConfig(player).getString(Variables.BANK + ".NotFound");
                            bankNotFound = new TextUtils().replaceAndWithParagraph(bankNotFound);
                            player.sendMessage(prefix + bankNotFound);
                        }
                    } else {
                        player.sendMessage(prefix + plugin.getNoPerms());
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + plugin.getOnlyPlayer());
                }
                return true;
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("transfer")) {
                String bankName = args[1];
                String otherBankName = args[2];
                double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(prefix + "§cInvalid amount: " + args[3]);
                    return true;
                }
                if (sender instanceof Player player) {
                    if (player.hasPermission("essentialsmini.bank.transfer")) {
                        List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                        if (banks != null && banks.contains(bankName)) {
                            if (plugin.getVaultManager().getEconomy().isBankOwner(bankName, player).transactionSuccess()) {
                                if (banks.contains(otherBankName)) {
                                    plugin.getVaultManager().getEconomy().bankWithdraw(bankName, amount);
                                    plugin.getVaultManager().getEconomy().bankDeposit(otherBankName, amount);
                                    player.sendMessage(getPrefix() + "§aYou transferred §6" + amount + " §ato the Bank §6" + otherBankName + "!");
                                } else {
                                    player.sendMessage(getPrefix() + "§6" + otherBankName + " §cdoesn't exist!");
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(Arrays.asList("remove", "create", "balance", "withdraw", "deposit", "addmember", "removemember", "listmembers", "list", "info", "transfer"));
            List<String> empty = new ArrayList<>();
            for (String s : commands) {
                if (s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    empty.add(s);
                }
            }
            Collections.sort(empty);
            return empty;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list")) return new ArrayList<>();
            if (args[0].equalsIgnoreCase("info")) return new ArrayList<>();
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("remove"))
                return new ArrayList<>(Collections.singletonList("<BANKNAME>"));
            List<String> banksList = new ArrayList<>();
            List<String> empty = new ArrayList<>();
            OfflinePlayer senderOffline = (sender instanceof Player) ? Bukkit.getOfflinePlayer(((Player) sender).getUniqueId()) : null;
            for (String banks : plugin.getVaultManager().getEconomy().getBanks()) {
                if (senderOffline == null || plugin.getVaultManager().getEconomy().isBankMember(banks, senderOffline).transactionSuccess() || plugin.getVaultManager().getEconomy().isBankOwner(banks, senderOffline).transactionSuccess()) {
                    banksList.add(banks);
                }
            }
            for (String s : banksList) {
                if (s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    empty.add(s);
            }
            Collections.sort(empty);
            return empty;
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("listmembers") || args[0].equalsIgnoreCase("info"))
                return new ArrayList<>();
            if (args[0].equalsIgnoreCase("balance")) return new ArrayList<>();
            if (args[0].equalsIgnoreCase("addmember") || args[0].equalsIgnoreCase("removemember")) {
                List<String> players = new ArrayList<>();
                List<String> empty = new ArrayList<>();
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (offlinePlayer.getName() != null) players.add(offlinePlayer.getName());
                }
                for (String s : players) {
                    if (s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                        empty.add(s);
                }
                Collections.sort(empty);
                return empty;
            }
            if (args[0].equalsIgnoreCase("transfer")) {
                List<String> banks = plugin.getVaultManager().getEconomy().getBanks();
                List<String> empty = new ArrayList<>();
                for (String s : banks) {
                    if (s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                        empty.add(s);
                }
                Collections.sort(empty);
                return empty;
            }
            List<String> empty = new ArrayList<>();
            if (args[0].equalsIgnoreCase("deposit")) {
                double bal = 0.0;
                if (sender instanceof Player) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(((Player) sender).getUniqueId());
                    bal = plugin.getVaultManager().getEconomy().getBalance(off);
                }
                empty.add(String.valueOf(bal));
            } else if (args[0].equalsIgnoreCase("withdraw")) {
                double bal = 0.0;
                try {
                    bal = plugin.getVaultManager().getEconomy().bankBalance(args[1]).balance;
                } catch (Exception ignored) {
                }
                empty.add(String.valueOf(bal));
            }
            return empty;
        } else if (args.length == 4) {
            List<String> empty = new ArrayList<>();
            try {
                empty.add(String.valueOf(plugin.getVaultManager().getEconomy().bankBalance(args[1]).balance));
            } catch (Exception ignored) {
            }
            return empty;
        }
        return super.onTabComplete(sender, command, label, args);
    }
}
