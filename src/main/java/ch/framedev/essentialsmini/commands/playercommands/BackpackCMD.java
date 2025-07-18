package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 08.08.2020 20:56
 */

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.InventoryStringDeSerializer;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.simplejavautils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BackpackCMD extends CommandListenerBase {

    static File file;
    static FileConfiguration cfg;
    public final static HashMap<String, String> itemsStringHashMap = new HashMap<>();
    private final Main plugin;

    public BackpackCMD(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("Backpack")) {
            setup("backpack", this);
            setupTabCompleter("backpack", this);
            setupListener(this);
        }
        file = new File(Main.getInstance().getDataFolder(), "backpack.yml");
        cfg = YamlConfiguration.loadConfiguration(file);
    }


    @EventHandler
    public void onCloseGui(InventoryCloseEvent event) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (event.getView().getTitle().equalsIgnoreCase(offlinePlayer.getName() + "'s Inventory")) {
                itemsStringHashMap.put(offlinePlayer.getUniqueId().toString(), InventoryStringDeSerializer.itemStackArrayToBase64(event.getInventory().getContents()));
            }
        }
    }

    // Restore BackPack into HashMap
    public static void restore(OfflinePlayer player) {
        if (cfg.contains(player.getUniqueId().toString() + ".Inventory")) {
            String content = cfg.getString(player.getUniqueId().toString() + ".Inventory");
            if (content != null)
                itemsStringHashMap.put(player.getUniqueId().toString(), content);
        }
    }

    // Save Backpack
    public static void save(OfflinePlayer player) {
        if (!itemsStringHashMap.isEmpty()) {
            if (itemsStringHashMap.containsKey(player.getUniqueId().toString())) {
                for (Map.Entry<String, String> entry : itemsStringHashMap.entrySet()) {
                    cfg.set(entry.getKey() + ".Inventory", entry.getValue());
                }
                try {
                    cfg.save(file);
                } catch (IOException e) {
                    Main.getInstance().getLogger4J().error(e);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (args.length == 0) {
                Player player = (Player) sender;
                if (plugin.getConfig().getBoolean("Backpack")) {
                    Inventory inventory = Bukkit.createInventory(null, plugin.getConfig().getInt("BackPackSize", 3*9), player.getName() + "'s Inventory");
                    if (itemsStringHashMap.containsKey(player.getUniqueId().toString()) && !(itemsStringHashMap.get(player.getUniqueId().toString()) == null)) {
                        try {
                            inventory.setContents(InventoryStringDeSerializer.itemStackArrayFromBase64(itemsStringHashMap.get(player.getUniqueId().toString())));
                        } catch (IOException e) {
                            Main.getInstance().getLogger4J().error(e);
                        }
                        player.openInventory(inventory);
                    } else {
                        player.openInventory(inventory);
                    }
                }
            } else if (args.length == 1) {
                Player player = (Player) sender;
                OfflinePlayer targetPlayer = PlayerUtils.getOfflinePlayerByName(args[0]);
                if (args[0].equalsIgnoreCase(targetPlayer.getName()) && !args[0].equalsIgnoreCase("delete")) {
                    if (player.hasPermission("essentialsmini.backpack.see")) {
                        if (plugin.getConfig().getBoolean("Backpack")) {
                            Inventory inventory = Bukkit.createInventory(null, 3 * 9, targetPlayer.getName() + "'s Inventory");
                            if (itemsStringHashMap.containsKey(targetPlayer.getUniqueId().toString()) && !(itemsStringHashMap.get(targetPlayer.getUniqueId().toString()) == null)) {
                                try {
                                    inventory.setContents(InventoryStringDeSerializer.itemStackArrayFromBase64(itemsStringHashMap.get(targetPlayer.getUniqueId().toString())));
                                } catch (IOException e) {
                                    plugin.getLogger4J().error(e);
                                }
                                player.openInventory(inventory);
                            } else {
                                String message = plugin.getLanguageConfig(player).getString("NoBackPackFound");
                                if (message != null) {
                                    message = new TextUtils().replaceAndWithParagraph(message);
                                }
                                player.sendMessage(plugin.getPrefix() + message);
                            }
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                }
                if (args[0].equalsIgnoreCase("delete")) {
                    if (player.hasPermission("essentialsmini.backpack.delete")) {
                        itemsStringHashMap.clear();
                        String locale = player.getLocale();
                        final boolean b = locale.equalsIgnoreCase("en_us") || locale.equalsIgnoreCase("en_au") ||
                                locale.equalsIgnoreCase("en_gb") || locale.equalsIgnoreCase("en_nz") ||
                                locale.equalsIgnoreCase("en_za") || locale.equalsIgnoreCase("en_pt");
                        if (file.exists()) {
                            if (file.delete()) {
                                if (b) {
                                    player.sendMessage(plugin.getPrefix() + "§6Backpacks deleted!");
                                } else {
                                    player.sendMessage(plugin.getPrefix() + "§6BackPacks gelöscht!");
                                }
                            } else {
                                if (b) {
                                    player.sendMessage(plugin.getPrefix() + "§cError while Deleting BackPacks!");
                                } else {
                                    player.sendMessage(plugin.getPrefix() + "§cError beim Löschen der Backpacks");
                                }
                            }
                        } else {
                            if (b) {
                                player.sendMessage(plugin.getPrefix() + "§cError while Deleting BackPacks!");
                            } else {
                                player.sendMessage(plugin.getPrefix() + "§cError beim Löschen der Backpacks");
                            }
                        }
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                }
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<OfflinePlayer> players = new ArrayList<>(Arrays.asList(Bukkit.getOfflinePlayers()));
        ArrayList<String> playerNames = new ArrayList<>();
        for (OfflinePlayer player : players) {
            if (player != null)
                playerNames.add(player.getName());
        }
        ArrayList<String> commands = new ArrayList<>(playerNames);
        commands.add("delete");
        ArrayList<String> empty = new ArrayList<>();
        if (args.length == 1) {
            for (String s : commands) {
                if (s.toLowerCase().startsWith(args[0])) {
                    empty.add(s);
                }
            }
            Collections.sort(empty);
            return empty;
        }
        return null;
    }
}
