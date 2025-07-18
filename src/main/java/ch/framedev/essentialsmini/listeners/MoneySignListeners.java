package ch.framedev.essentialsmini.listeners;

import ch.framedev.essentialsmini.abstracts.ListenerBase;
import ch.framedev.essentialsmini.main.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.listeners
 * Date: 28.10.2020
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */
public class MoneySignListeners extends ListenerBase implements CommandExecutor {

    private Economy eco;

    public MoneySignListeners(Main plugin) {
        super(plugin);
        Objects.requireNonNull(plugin.getCommand("signremove")).setExecutor(this);
        if (plugin.getConfig().getBoolean("Economy.Activate")) {
            eco = plugin.getVaultManager().getEco();
        }
    }

    @EventHandler
    public void onSignChangeBalance(SignChangeEvent e) {
        if(e.getLine(0) == null) return;
        if (Objects.requireNonNull(e.getLine(0)).equalsIgnoreCase("[balance]")) {
            if (e.getPlayer().hasPermission("essentialsmini.signs.create")) {
                String signName = Main.getInstance().getConfig().getString("MoneySign.Balance");
                if (signName == null) {
                    e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Balance is not set!");
                    return;
                }
                signName = signName.replace('&', '§');
                e.setLine(0, signName);
            } else {
                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            }
        }
    }

    public boolean isCharNumber(char c) {
        try {
            Integer.parseInt(String.valueOf(c));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    @EventHandler
    public void onClickBalance(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getHand() == EquipmentSlot.OFF_HAND) {
                return;
            }
            if(e.getHand() == null) return;
            if(e.getClickedBlock() == null) return;
            if (e.getHand().equals(EquipmentSlot.HAND) &&
                e.getClickedBlock().getState() instanceof Sign s) {
                if (e.getPlayer().getGameMode() == GameMode.CREATIVE && e.getItem() != null && e.getItem().getType() == Material.NETHER_STAR) {
                    if (e.getPlayer().hasPermission("essentialsmini.signs.update")) {
                        String signNameBuy = Main.getInstance().getConfig().getString("MoneySign.Buy");
                        if (signNameBuy == null) {
                            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Buy is not set!");
                            return;
                        }
                        signNameBuy = signNameBuy.replace('&', '§');
                        if (s.getLine(0).equalsIgnoreCase(signNameBuy)) {
                            s.setLine(1, s.getLine(1));
                            int money;
                            StringBuilder num = new StringBuilder();
                            for (char c : s.getLine(3).toCharArray()) {
                                if (isCharNumber(c)) {
                                    num.append(c);
                                }
                            }
                            money = Integer.parseInt(num.toString());
                            s.setLine(3, money + Main.getInstance().getCurrencySymbolMulti());
                        }
                        String signNameSell = Main.getInstance().getConfig().getString("MoneySign.Sell");
                        if (signNameSell == null) {
                            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Sell is not set!");
                            return;
                        }
                        signNameSell = signNameSell.replace('&', '§');
                        if (s.getLine(0).equalsIgnoreCase(signNameSell)) {
                            s.setLine(1, s.getLine(1));
                            int money;
                            StringBuilder num = new StringBuilder();
                            for (char c : s.getLine(3).toCharArray()) {
                                if (isCharNumber(c)) {
                                    num.append(c);
                                }
                            }
                            money = Integer.parseInt(num.toString());
                            s.setLine(3, money + Main.getInstance().getCurrencySymbolMulti());
                        }
                        s.update();
                        e.getPlayer().sendMessage("§aUpdated");
                        e.setCancelled(true);
                        e.setUseInteractedBlock(Event.Result.DENY);
                        return;
                    }
                }
                String signName = Main.getInstance().getConfig().getString("MoneySign.Balance");
                if (signName == null) {
                    e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Balance is not set!");
                    return;
                }
                signName = signName.replace('&', '§');
                if (s.getLine(0).equalsIgnoreCase(signName)) {
                    if (e.getPlayer().hasPermission("essentialsmini.signs.use")) {
                        String money = eco.format(eco.getBalance(e.getPlayer()));
                        String text = Main.getInstance().getConfig().getString("Money.MSG.Balance");
                        if (text == null) {
                            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: Money.MSG.Balance is not set!");
                            return;
                        }
                        text = text.replace("[Money]", money);
                        text = text.replace('&', '§');
                        e.getPlayer().sendMessage(text + Main.getInstance().getCurrencySymbolMulti());
                    } else {
                        e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
                    }
                    e.setUseInteractedBlock(Event.Result.DENY);
                    e.setCancelled(true);
                }
                String signNameFree = Main.getInstance().getConfig().getString("MoneySign.Free");
                if (signNameFree == null) {
                    e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Free is not set!");
                    return;
                }
                signNameFree = signNameFree.replace('&', '§');
                if (s.getLine(0).equalsIgnoreCase(signNameFree)) {
                    if (e.getPlayer().hasPermission("essentialsmini.signs.use")) {
                        String[] args = s.getLines();
                        Material name = Material.getMaterial(args[1].toUpperCase());
                        Inventory inventory = Bukkit.createInventory(null, 3*9, "Free");
                        for(int i = 0; i < inventory.getSize(); i++) {
                            inventory.setItem(i, new ItemStack(name, 64));
                        }
                        e.getPlayer().openInventory(inventory);
                    } else {
                        e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
                    }
                    e.setCancelled(true);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
                String signNameBuy = Main.getInstance().getConfig().getString("MoneySign.Buy");
                if (signNameBuy == null) {
                    e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Buy is not set!");
                    return;
                }
                signNameBuy = signNameBuy.replace('&', '§');
                if (s.getLine(0).equalsIgnoreCase(signNameBuy)) {
                    if (e.getPlayer().hasPermission("essentialsmini.signs.use")) {
                        String[] args = s.getLines();
                        Material name = Material.getMaterial(args[1].toUpperCase());

                        int amount = Integer.parseInt(args[2]);

                        int money = Integer.parseInt(args[3].replace(Main.getInstance().getCurrencySymbolMulti(), ""));
                        if (s.getLine(1).equalsIgnoreCase(name.name()) && s.getLine(2).equalsIgnoreCase(amount + "") && s.getLine(3).equalsIgnoreCase(money + Main.getInstance().getCurrencySymbolMulti())) {
                            if (eco.getBalance(e.getPlayer()) < money) {
                                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDu hast nicht genug §6" + Main.getInstance().getCurrencySymbolMulti());
                                e.setCancelled(true);
                                e.setUseInteractedBlock(Event.Result.DENY);
                                return;
                            }
                            ItemStack item = new ItemStack(name);
                            item.setAmount(amount);
                            e.getPlayer().getInventory().addItem(item);
                            eco.withdrawPlayer(e.getPlayer(), money);
                            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§aDu hast §6" + item.getAmount() + "x " + name.name() + " §afür §6" + money + Main.getInstance().getCurrencySymbolMulti() + " §agekauft.");
                        }
                    } else {
                        e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
                    }
                    e.setCancelled(true);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
                String signNameSell = Main.getInstance().getConfig().getString("MoneySign.Sell");
                if (signNameSell == null) {
                    e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Sell is not set!");
                    return;
                }
                signNameSell = signNameSell.replace('&', '§');
                if (s.getLine(0).equalsIgnoreCase(signNameSell)) {
                    if (e.getPlayer().hasPermission("essentialsmini.signs.use")) {
                        String[] args = s.getLines();
                        Material name = Material.getMaterial(args[1].toUpperCase());
                        int amount = Integer.parseInt(args[2]);
                        int money = Integer.parseInt(args[3].replace(Main.getInstance().getCurrencySymbolMulti(), ""));
                        if (s.getLine(1).equalsIgnoreCase(name.name())
                                && s.getLine(2).equalsIgnoreCase(amount + "")
                                && s.getLine(3).equalsIgnoreCase(money + Main.getInstance().getCurrencySymbolMulti())) {
                            if (e.getPlayer().getInventory().contains(name, amount)) {
                                ItemStack item = new ItemStack(name);
                                item.setAmount(amount);
                                e.getPlayer().getInventory().removeItem(item);
                                eco.depositPlayer(e.getPlayer(), money);
                                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§aDu hast §6" + item.getAmount() + "x " + name.name() + " §afür §6" + money + Main.getInstance().getCurrencySymbolMulti() + " §averkauft.");
                                return;
                            }
                            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDu hast nicht genug §6" + name.name());
                        }
                    } else {
                        e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
                    }
                    e.setCancelled(true);
                    e.setUseInteractedBlock(Event.Result.DENY);
                } else if (s.getLine(0).equalsIgnoreCase("§6Buy")) {
                    if (e.getPlayer().hasPermission("essentialsmini.signs.use")) {
                        if (Main.getInstance().getConfig().getBoolean("PlayerShop")) {
                            ItemStack itemStack = cfg.getItemStack("Items." + s.getLine(1).replace('§', '&') + ".item");
                            if (itemStack == null) {
                                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDieser Shop existiert nicht!");
                                e.setCancelled(true);
                                e.setUseInteractedBlock(Event.Result.DENY);
                                return;
                            }
                            itemStack.setAmount(Integer.parseInt(s.getLine(2)));
                            if (e.getPlayer().getName().equalsIgnoreCase(cfg.getString("Items." + s.getLine(1).replace('§', '&') + ".player"))) {
                                e.getPlayer().sendMessage("§c§lYou cannot Buy your own Item!");
                                e.setCancelled(true);
                                e.setUseInteractedBlock(Event.Result.DENY);
                                return;
                            }
                            if (eco.has(e.getPlayer(), Double.parseDouble(s.getLine(3)))) {
                                eco.withdrawPlayer(e.getPlayer(), Double.parseDouble(s.getLine(3)));
                                eco.depositPlayer(Bukkit.getOfflinePlayer(Objects.requireNonNull(cfg.getString("Items." + s.getLine(1).replace('§', '&') + ".player"))), Double.parseDouble(s.getLine(3)));
                                e.getPlayer().getInventory().addItem(itemStack);
                            } else {
                                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDu hast nicht genug §6" + Main.getInstance().getCurrencySymbolMulti());
                            }
                        }
                    } else {
                        e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
                    }
                    e.setCancelled(true);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @EventHandler
    public void signChange(SignChangeEvent e) {
        String signName = Main.getInstance().getConfig().getString("MoneySign.Buy");
        signName = signName.replace('&', '§');
        if (e.getLine(0).equalsIgnoreCase("buy")) {
            String[] args = e.getLines();
            Material name = Material.getMaterial(args[1].toUpperCase());
            int amount = Integer.parseInt(args[2]);
            int money = Integer.parseInt(args[3]);
            if (e.getPlayer().hasPermission("essentialsmini.signs.create")) {
                if (e.getLine(1).equalsIgnoreCase(name.name()) &&
                        e.getLine(2).equalsIgnoreCase(amount + "") &&
                        e.getLine(3).equalsIgnoreCase(money + "")) {
                    e.setLine(0, signName);
                    e.setLine(1, name.name());
                    e.setLine(2, amount + "");
                    e.setLine(3, money + Main.getInstance().getCurrencySymbolMulti());
                }
            } else {
                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @EventHandler
    public void SignChangeFree(SignChangeEvent e) {
        String signName = Main.getInstance().getConfig().getString("MoneySign.Free");
        if (signName == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Free is not set!");
            return;
        }
        signName = signName.replace('&', '§');
        if (e.getLine(0).equalsIgnoreCase("free")) {
            if (e.getPlayer().hasPermission("essentialsmini.signs.create")) {
                String[] args = e.getLines();
                Material name = Material.getMaterial(args[1].toUpperCase());
                if (e.getLine(1).equalsIgnoreCase(name.name())) {
                    e.setLine(0, signName);
                    e.setLine(1, name.name());
                }
            } else {
                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @EventHandler
    public void signChangeSell(SignChangeEvent e) {
        String signName = Main.getInstance().getConfig().getString("MoneySign.Sell");
        if (signName == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Sell is not set!");
            return;
        }
        signName = signName.replace('&', '§');
        if (e.getLine(0).equalsIgnoreCase("sell")) {
            if (e.getPlayer().hasPermission("essentialsmini.signs.create")) {
                String[] args = e.getLines();
                Material name = Material.getMaterial(args[1].toUpperCase());
                int amount = Integer.parseInt(args[2]);
                int money = Integer.parseInt(args[3]);
                if (e.getLine(1).equalsIgnoreCase(name.name()) &&
                        e.getLine(2).equalsIgnoreCase(amount + "") &&
                        e.getLine(3).equalsIgnoreCase(money + "")) {
                    e.setLine(0, signName);
                    e.setLine(1, name.name());
                    e.setLine(2, amount + "");
                    e.setLine(3, money + Main.getInstance().getCurrencySymbolMulti());
                }
            } else {
                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            }
        }
    }

    final File file = new File(Main.getInstance().getDataFolder(), "items.yml");
    final FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    final HashMap<Player, String> cmdMessage = new HashMap<>();
    final HashMap<Player, Sign> playerSign = new HashMap<>();
    final HashMap<Player, ItemStack> itemHash = new HashMap<>();

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerClickSign(PlayerInteractEvent event) {
        if (Main.getInstance().getConfig().getBoolean("PlayerShop")) {
            if (event.getItem() == null) return;
            ItemStack item = event.getItem();
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getClickedBlock() == null) return;
                if (event.getClickedBlock().getState() instanceof Sign sign) {
                    if (event.getPlayer().hasPermission("essentialsmini.signs.create")) {
                        if (sign.getLine(0).equalsIgnoreCase("Item")) {
                            sign.setLine(0, "");
                            cmdMessage.put(event.getPlayer(), "itemname");
                            event.getPlayer().sendMessage("§aWie soll das Item heissen?");
                            playerSign.put(event.getPlayer(), sign);
                            event.setCancelled(true);
                            event.setUseInteractedBlock(Event.Result.DENY);
                            itemHash.put(event.getPlayer(), item);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onAsync(AsyncPlayerChatEvent event) {
        if (Main.getInstance().getConfig().getBoolean("PlayerShop")) {
            if (!cmdMessage.isEmpty() && cmdMessage.containsKey(event.getPlayer()) && cmdMessage.get(event.getPlayer()).equalsIgnoreCase("itemname")) {
                Sign sign = playerSign.get(event.getPlayer());
                sign.setWaxed(true);
                cmdMessage.remove(event.getPlayer());
                sign.setLine(1, ChatColor.translateAlternateColorCodes('&', event.getMessage()));
                event.setCancelled(true);
                cmdMessage.put(event.getPlayer(), "amount");
                event.getPlayer().sendMessage("§aWie viel soll man kaufen können?");
                cfg.set("Items." + event.getMessage() + ".item", itemHash.get(event.getPlayer()));
                cfg.set("Items." + event.getMessage() + ".player", event.getPlayer().getName());
                cfg.set("Items." + event.getMessage() + ".location", sign.getLocation());
                try {
                    cfg.save(file);
                } catch (IOException e) {
                    getPlugin().getLogger4J().error("Could not save items.yml", e);
                }
                sign.update(true);
                playerSign.remove(event.getPlayer());
                playerSign.put(event.getPlayer(), sign);
            } else if (!cmdMessage.isEmpty() && cmdMessage.containsKey(event.getPlayer()) && cmdMessage.get(event.getPlayer()).equalsIgnoreCase("amount")) {
                Sign sign = playerSign.get(event.getPlayer());
                sign.setEditable(true);
                cmdMessage.remove(event.getPlayer());
                sign.setLine(2, event.getMessage());
                event.setCancelled(true);
                cmdMessage.put(event.getPlayer(), "price");
                event.getPlayer().sendMessage("§aWie viel soll es kosten?");
                sign.update(true);
                playerSign.remove(event.getPlayer());
                playerSign.put(event.getPlayer(), sign);
            } else if (!cmdMessage.isEmpty() && cmdMessage.containsKey(event.getPlayer()) && cmdMessage.get(event.getPlayer()).equalsIgnoreCase("price")) {
                Sign sign = playerSign.get(event.getPlayer());
                sign.setLine(0, "§6Buy");
                sign.setLine(3, event.getMessage());
                event.setCancelled(true);
                cmdMessage.remove(event.getPlayer());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sign.update(true, true);
                    }
                }.runTaskLater(Main.getInstance(), 60);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1) {
            if (sender.hasPermission("essentialsmini.signs.delete")) {
                if (Main.getInstance().getConfig().getBoolean("PlayerShop")) {
                    StringBuilder signName = new StringBuilder();
                    for (String s : args) {
                        if (s.equalsIgnoreCase("signremove")) {
                            s.replace(s, "");
                        } else if (args[0].equalsIgnoreCase(s)) {
                            signName.append(s);
                        } else {
                            signName.append(" ").append(s);
                        }
                    }
                    if (cfg.contains("Items." + signName + ".item")) {
                        Location location = cfg.getLocation("Items." + signName + ".location");
                        if (location != null) {
                            location.getBlock().setType(Material.AIR);
                        }
                        cfg.set("Items." + signName, null);
                        try {
                            cfg.save(file);
                        } catch (IOException e) {
                            getPlugin().getLogger4J().error("Could not save items.yml", e);
                        }
                        sender.sendMessage("§cDieser Shop wurde entfernt!");
                    } else {
                        sender.sendMessage("§cDieser Shop existiert nicht!");
                    }
                }
            } else {
                sender.sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            }
        }
        return false;
    }
}
