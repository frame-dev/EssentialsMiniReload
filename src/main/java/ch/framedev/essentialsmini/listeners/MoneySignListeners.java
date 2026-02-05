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
            if (plugin.getVaultManager() != null) {
                eco = plugin.getVaultManager().getEco();
            }
        }
    }

    @EventHandler
    public void onSignChangeBalance(SignChangeEvent e) {
        if (e == null || e.getPlayer() == null) return;

        String line0 = e.getLine(0);
        if (line0 == null || !line0.equalsIgnoreCase("[balance]")) return;

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
        if (e == null || e.getPlayer() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND || e.getHand() == null) return;
        if (e.getClickedBlock() == null) return;

        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!(e.getClickedBlock().getState() instanceof Sign s)) return;

        // Check if eco is initialized
        if (eco == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cEconomy system is not available!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        String[] lines = s.getLines();
        if (lines == null || lines.length < 4) return;

        // Handle sign update with nether star in creative mode
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE && e.getItem() != null && e.getItem().getType() == Material.NETHER_STAR) {
            if (handleSignUpdate(e, s, lines)) return;
        }

        // Handle balance sign
        if (handleBalanceSign(e, s, lines[0])) return;

        // Handle free sign
        if (handleFreeSign(e, s, lines)) return;

        // Handle buy sign
        if (handleBuySign(e, s, lines)) return;

        // Handle sell sign
        if (handleSellSign(e, s, lines)) return;

        // Handle player shop buy sign
        handlePlayerShopBuySign(e, s, lines);
    }

    private boolean handleSignUpdate(PlayerInteractEvent e, Sign s, String[] lines) {
        if (!e.getPlayer().hasPermission("essentialsmini.signs.update")) {
            return false;
        }

        String signNameBuy = Main.getInstance().getConfig().getString("MoneySign.Buy");
        if (signNameBuy != null) {
            signNameBuy = signNameBuy.replace('&', '§');
            if (lines[0] != null && lines[0].equalsIgnoreCase(signNameBuy)) {
                updateSignMoney(s, 3);
                s.update();
                e.getPlayer().sendMessage("§aUpdated");
                e.setCancelled(true);
                e.setUseInteractedBlock(Event.Result.DENY);
                return true;
            }
        }

        String signNameSell = Main.getInstance().getConfig().getString("MoneySign.Sell");
        if (signNameSell != null) {
            signNameSell = signNameSell.replace('&', '§');
            if (lines[0] != null && lines[0].equalsIgnoreCase(signNameSell)) {
                updateSignMoney(s, 3);
                s.update();
                e.getPlayer().sendMessage("§aUpdated");
                e.setCancelled(true);
                e.setUseInteractedBlock(Event.Result.DENY);
                return true;
            }
        }
        return false;
    }

    private void updateSignMoney(Sign sign, int lineIndex) {
        String line = sign.getLine(lineIndex);
        if (line == null) return;

        StringBuilder num = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (isCharNumber(c)) {
                num.append(c);
            }
        }

        if (num.length() > 0) {
            int money = Integer.parseInt(num.toString());
            sign.setLine(lineIndex, money + Main.getInstance().getCurrencySymbolMulti());
        }
    }

    private boolean handleBalanceSign(PlayerInteractEvent e, Sign s, String line0) {
        String signName = Main.getInstance().getConfig().getString("MoneySign.Balance");
        if (signName == null) return false;

        signName = signName.replace('&', '§');
        if (line0 == null || !line0.equalsIgnoreCase(signName)) return false;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.use")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setCancelled(true);
            return true;
        }

        String money = eco.format(eco.getBalance(e.getPlayer()));
        String text = Main.getInstance().getConfig().getString("Money.MSG.Balance");
        if (text == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: Money.MSG.Balance is not set!");
        } else {
            text = text.replace("[Money]", money);
            text = text.replace('&', '§');
            e.getPlayer().sendMessage(text + Main.getInstance().getCurrencySymbolMulti());
        }

        e.setUseInteractedBlock(Event.Result.DENY);
        e.setCancelled(true);
        return true;
    }

    private boolean handleFreeSign(PlayerInteractEvent e, Sign s, String[] lines) {
        String signNameFree = Main.getInstance().getConfig().getString("MoneySign.Free");
        if (signNameFree == null) return false;

        signNameFree = signNameFree.replace('&', '§');
        if (lines[0] == null || !lines[0].equalsIgnoreCase(signNameFree)) return false;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.use")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        if (lines[1] == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid sign configuration!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        Material name = Material.getMaterial(lines[1].toUpperCase());
        if (name == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid material: " + lines[1]);
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        Inventory inventory = Bukkit.createInventory(null, 3 * 9, "Free");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, new ItemStack(name, 64));
        }
        e.getPlayer().openInventory(inventory);

        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        return true;
    }

    private boolean handleBuySign(PlayerInteractEvent e, Sign s, String[] lines) {
        String signNameBuy = Main.getInstance().getConfig().getString("MoneySign.Buy");
        if (signNameBuy == null) return false;

        signNameBuy = signNameBuy.replace('&', '§');
        if (lines[0] == null || !lines[0].equalsIgnoreCase(signNameBuy)) return false;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.use")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        // Validate sign data
        if (lines[1] == null || lines[2] == null || lines[3] == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid sign configuration!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        Material name = Material.getMaterial(lines[1].toUpperCase());
        if (name == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid material: " + lines[1]);
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        int amount;
        int money;
        try {
            amount = Integer.parseInt(lines[2]);
            money = Integer.parseInt(lines[3].replace(Main.getInstance().getCurrencySymbolMulti(), ""));
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid number format on sign!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        if (lines[1].equalsIgnoreCase(name.name()) &&
                lines[2].equalsIgnoreCase(amount + "") &&
                lines[3].equalsIgnoreCase(money + Main.getInstance().getCurrencySymbolMulti())) {

            if (eco.getBalance(e.getPlayer()) < money) {
                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDu hast nicht genug §6" + Main.getInstance().getCurrencySymbolMulti());
                e.setCancelled(true);
                e.setUseInteractedBlock(Event.Result.DENY);
                return true;
            }

            ItemStack item = new ItemStack(name, amount);
            e.getPlayer().getInventory().addItem(item);
            eco.withdrawPlayer(e.getPlayer(), money);
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§aDu hast §6" + amount + "x " + name.name() + " §afür §6" + money + Main.getInstance().getCurrencySymbolMulti() + " §agekauft.");
        }

        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        return true;
    }

    private boolean handleSellSign(PlayerInteractEvent e, Sign s, String[] lines) {
        String signNameSell = Main.getInstance().getConfig().getString("MoneySign.Sell");
        if (signNameSell == null) return false;

        signNameSell = signNameSell.replace('&', '§');
        if (lines[0] == null || !lines[0].equalsIgnoreCase(signNameSell)) return false;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.use")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        // Validate sign data
        if (lines[1] == null || lines[2] == null || lines[3] == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid sign configuration!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        Material name = Material.getMaterial(lines[1].toUpperCase());
        if (name == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid material: " + lines[1]);
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        int amount;
        int money;
        try {
            amount = Integer.parseInt(lines[2]);
            money = Integer.parseInt(lines[3].replace(Main.getInstance().getCurrencySymbolMulti(), ""));
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid number format on sign!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return true;
        }

        if (lines[1].equalsIgnoreCase(name.name()) &&
                lines[2].equalsIgnoreCase(amount + "") &&
                lines[3].equalsIgnoreCase(money + Main.getInstance().getCurrencySymbolMulti())) {

            if (e.getPlayer().getInventory().contains(name, amount)) {
                ItemStack item = new ItemStack(name, amount);
                e.getPlayer().getInventory().removeItem(item);
                eco.depositPlayer(e.getPlayer(), money);
                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§aDu hast §6" + amount + "x " + name.name() + " §afür §6" + money + Main.getInstance().getCurrencySymbolMulti() + " §averkauft.");
            } else {
                e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDu hast nicht genug §6" + name.name());
            }
        }

        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        return true;
    }

    private void handlePlayerShopBuySign(PlayerInteractEvent e, Sign s, String[] lines) {
        if (lines[0] == null || !lines[0].equalsIgnoreCase("§6Buy")) return;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.use")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (!Main.getInstance().getConfig().getBoolean("PlayerShop")) {
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (lines[1] == null || lines[2] == null || lines[3] == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid sign configuration!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        String itemKey = lines[1].replace('§', '&');
        ItemStack itemStack = cfg.getItemStack("Items." + itemKey + ".item");
        if (itemStack == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDieser Shop existiert nicht!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        try {
            itemStack.setAmount(Integer.parseInt(lines[2]));
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid amount on sign!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        String shopOwner = cfg.getString("Items." + itemKey + ".player");
        if (shopOwner == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cShop owner not found!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (e.getPlayer().getName().equalsIgnoreCase(shopOwner)) {
            e.getPlayer().sendMessage("§c§lYou cannot Buy your own Item!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(lines[3]);
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid price on sign!");
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (eco.has(e.getPlayer(), price)) {
            eco.withdrawPlayer(e.getPlayer(), price);
            eco.depositPlayer(Bukkit.getOfflinePlayer(shopOwner), price);
            e.getPlayer().getInventory().addItem(itemStack);
        } else {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cDu hast nicht genug §6" + Main.getInstance().getCurrencySymbolMulti());
        }

        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void signChange(SignChangeEvent e) {
        if (e == null || e.getPlayer() == null) return;

        String line0 = e.getLine(0);
        if (line0 == null || !line0.equalsIgnoreCase("buy")) return;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.create")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            return;
        }

        String signName = Main.getInstance().getConfig().getString("MoneySign.Buy");
        if (signName == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Buy is not set!");
            return;
        }
        signName = signName.replace('&', '§');

        String[] args = e.getLines();
        if (args.length < 4 || args[1] == null || args[2] == null || args[3] == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid sign format! Use: buy / material / amount / price");
            return;
        }

        Material name = Material.getMaterial(args[1].toUpperCase());
        if (name == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid material: " + args[1]);
            return;
        }

        int amount;
        int money;
        try {
            amount = Integer.parseInt(args[2]);
            money = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid number format!");
            return;
        }

        if (e.getLine(1).equalsIgnoreCase(name.name()) &&
                e.getLine(2).equalsIgnoreCase(amount + "") &&
                e.getLine(3).equalsIgnoreCase(money + "")) {
            e.setLine(0, signName);
            e.setLine(1, name.name());
            e.setLine(2, amount + "");
            e.setLine(3, money + Main.getInstance().getCurrencySymbolMulti());
        }
    }

    @EventHandler
    public void SignChangeFree(SignChangeEvent e) {
        if (e == null || e.getPlayer() == null) return;

        String line0 = e.getLine(0);
        if (line0 == null || !line0.equalsIgnoreCase("free")) return;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.create")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            return;
        }

        String signName = Main.getInstance().getConfig().getString("MoneySign.Free");
        if (signName == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Free is not set!");
            return;
        }
        signName = signName.replace('&', '§');

        String[] args = e.getLines();
        if (args.length < 2 || args[1] == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid sign format! Use: free / material");
            return;
        }

        Material name = Material.getMaterial(args[1].toUpperCase());
        if (name == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid material: " + args[1]);
            return;
        }

        if (e.getLine(1).equalsIgnoreCase(name.name())) {
            e.setLine(0, signName);
            e.setLine(1, name.name());
        }
    }

    @EventHandler
    public void signChangeSell(SignChangeEvent e) {
        if (e == null || e.getPlayer() == null) return;

        String line0 = e.getLine(0);
        if (line0 == null || !line0.equalsIgnoreCase("sell")) return;

        if (!e.getPlayer().hasPermission("essentialsmini.signs.create")) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            return;
        }

        String signName = Main.getInstance().getConfig().getString("MoneySign.Sell");
        if (signName == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cConfiguration Error: MoneySign.Sell is not set!");
            return;
        }
        signName = signName.replace('&', '§');

        String[] args = e.getLines();
        if (args.length < 4 || args[1] == null || args[2] == null || args[3] == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid sign format! Use: sell / material / amount / price");
            return;
        }

        Material name = Material.getMaterial(args[1].toUpperCase());
        if (name == null) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid material: " + args[1]);
            return;
        }

        int amount;
        int money;
        try {
            amount = Integer.parseInt(args[2]);
            money = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(Main.getInstance().getPrefix() + "§cInvalid number format!");
            return;
        }

        if (e.getLine(1).equalsIgnoreCase(name.name()) &&
                e.getLine(2).equalsIgnoreCase(amount + "") &&
                e.getLine(3).equalsIgnoreCase(money + "")) {
            e.setLine(0, signName);
            e.setLine(1, name.name());
            e.setLine(2, amount + "");
            e.setLine(3, money + Main.getInstance().getCurrencySymbolMulti());
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
        if (!Main.getInstance().getConfig().getBoolean("PlayerShop")) return;
        if (event == null || event.getPlayer() == null) return;
        if (event.getItem() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;
        if (!event.getPlayer().hasPermission("essentialsmini.signs.create")) return;

        String[] lines = sign.getLines();
        if (lines == null || lines.length < 1 || lines[0] == null) return;
        if (!lines[0].equalsIgnoreCase("Item")) return;

        ItemStack item = event.getItem();
        sign.setLine(0, "");
        cmdMessage.put(event.getPlayer(), "itemname");
        event.getPlayer().sendMessage("§aWie soll das Item heissen?");
        playerSign.put(event.getPlayer(), sign);
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        itemHash.put(event.getPlayer(), item);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onAsync(AsyncPlayerChatEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("PlayerShop")) return;
        if (event == null || event.getPlayer() == null) return;
        if (cmdMessage.isEmpty() || !cmdMessage.containsKey(event.getPlayer())) return;

        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null) return;

        String command = cmdMessage.get(player);
        if (command == null) return;

        Sign sign = playerSign.get(player);
        if (sign == null) return;

        if (command.equalsIgnoreCase("itemname")) {
            sign.setWaxed(true);
            cmdMessage.remove(player);
            sign.setLine(1, ChatColor.translateAlternateColorCodes('&', message));
            event.setCancelled(true);
            cmdMessage.put(player, "amount");
            player.sendMessage("§aWie viel soll man kaufen können?");

            ItemStack item = itemHash.get(player);
            if (item != null) {
                cfg.set("Items." + message + ".item", item);
                cfg.set("Items." + message + ".player", player.getName());
                cfg.set("Items." + message + ".location", sign.getLocation());
                try {
                    cfg.save(file);
                } catch (IOException e) {
                    getPlugin().getLogger4J().error("Could not save items.yml", e);
                }
            }

            sign.update(true);
            playerSign.remove(player);
            playerSign.put(player, sign);

        } else if (command.equalsIgnoreCase("amount")) {
            sign.setEditable(true);
            cmdMessage.remove(player);
            sign.setLine(2, message);
            event.setCancelled(true);
            cmdMessage.put(player, "price");
            player.sendMessage("§aWie viel soll es kosten?");
            sign.update(true);
            playerSign.remove(player);
            playerSign.put(player, sign);

        } else if (command.equalsIgnoreCase("price")) {
            sign.setLine(0, "§6Buy");
            sign.setLine(3, message);
            event.setCancelled(true);
            cmdMessage.remove(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    sign.update(true, true);
                }
            }.runTaskLater(Main.getInstance(), 60);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!Main.getInstance().getConfig().getBoolean("PlayerShop")) {
            sender.sendMessage(Main.getInstance().getPrefix() + "§cPlayer shop is disabled!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Main.getInstance().getPrefix() + "§cUsage: /signremove <shop name>");
            return true;
        }

        if (!sender.hasPermission("essentialsmini.signs.delete")) {
            sender.sendMessage(Main.getInstance().getPrefix() + Main.getInstance().getNoPerms());
            return true;
        }

        StringBuilder signName = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                signName.append(args[i]);
            } else {
                signName.append(" ").append(args[i]);
            }
        }

        String shopKey = "Items." + signName;
        if (!cfg.contains(shopKey + ".item")) {
            sender.sendMessage("§cDieser Shop existiert nicht!");
            return true;
        }

        Location location = cfg.getLocation(shopKey + ".location");
        if (location != null) {
            location.getBlock().setType(Material.AIR);
        }

        cfg.set(shopKey, null);
        try {
            cfg.save(file);
            sender.sendMessage("§cDieser Shop wurde entfernt!");
        } catch (IOException e) {
            getPlugin().getLogger4J().error("Could not save items.yml", e);
            sender.sendMessage("§cError saving config file!");
        }

        return true;
    }
}
