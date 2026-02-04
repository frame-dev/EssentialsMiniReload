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
import ch.framedev.essentialsmini.utils.TextUtils;
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
import org.jetbrains.annotations.NotNull;

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
        // Try to parse owner name from the inventory title in the form "<name>'s Inventory"
        String title = event.getView().getTitle();
        final String suffix = "'s Inventory";
        if (title.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
            String ownerName = title.substring(0, title.length() - suffix.length());
            if (!ownerName.isEmpty()) {
                OfflinePlayer offlinePlayer = PlayerUtils.getOfflinePlayerByName(ownerName);
                // store the serialized inventory contents keyed by UUID string
                itemsStringHashMap.put(offlinePlayer.getUniqueId().toString(), InventoryStringDeSerializer.itemStackArrayToBase64(event.getInventory().getContents()));
            }
        }
    }

    // Restore BackPack into HashMap
    public static void restore(OfflinePlayer player) {
        if (player == null) return;
        String key = player.getUniqueId().toString();
        if (cfg.contains(key + ".Inventory")) {
            String content = cfg.getString(key + ".Inventory");
            if (content != null) itemsStringHashMap.put(key, content);
        }
    }

    // Save Backpack
    public static void save(OfflinePlayer player) {
        if (player == null) return;
        String key = player.getUniqueId().toString();
        if (!itemsStringHashMap.isEmpty()) {
            if (itemsStringHashMap.containsKey(key)) {
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                if (plugin.getConfig().getBoolean("Backpack")) {
                    Inventory inventory = Bukkit.createInventory(null, plugin.getConfig().getInt("BackPackSize", 3 * 9), player.getName() + "'s Inventory");
                    String key = player.getUniqueId().toString();
                    String stored = itemsStringHashMap.get(key);
                    if (stored != null) {
                        try {
                            inventory.setContents(InventoryStringDeSerializer.itemStackArrayFromBase64(stored));
                        } catch (IOException e) {
                            Main.getInstance().getLogger4J().error(e);
                        }
                    }
                    player.openInventory(inventory);
                }
            } else if (args.length == 1) {
                String arg = args[0];
                // If arg equals "delete" handle deletion
                if (arg.equalsIgnoreCase("delete")) {
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
                    return true;
                }

                // Otherwise try to open another player's backpack
                OfflinePlayer targetPlayer = PlayerUtils.getOfflinePlayerByName(arg);

                if (!arg.equalsIgnoreCase(targetPlayer.getName())) {
                    // name mismatch; treat as not found
                    String message = plugin.getLanguageConfig(player).getString("NoBackPackFound");
                    if (message != null) message = new TextUtils().replaceAndWithParagraph(message);
                    else message = "";
                    player.sendMessage(plugin.getPrefix() + message);
                    return true;
                }

                if (!player.hasPermission("essentialsmini.backpack.see")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }

                if (plugin.getConfig().getBoolean("Backpack")) {
                    Inventory inventory = Bukkit.createInventory(null, 3 * 9, targetPlayer.getName() + "'s Inventory");
                    String key = targetPlayer.getUniqueId().toString();
                    String stored = itemsStringHashMap.get(key);
                    if (stored != null) {
                        try {
                            inventory.setContents(InventoryStringDeSerializer.itemStackArrayFromBase64(stored));
                        } catch (IOException e) {
                            plugin.getLogger4J().error(e);
                        }
                        player.openInventory(inventory);
                    } else {
                        String message = plugin.getLanguageConfig(player).getString("NoBackPackFound");
                        if (message != null) {
                            message = new TextUtils().replaceAndWithParagraph(message);
                        } else message = "";
                        player.sendMessage(plugin.getPrefix() + message);
                    }
                }
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        ArrayList<OfflinePlayer> players = new ArrayList<>(Arrays.asList(Bukkit.getOfflinePlayers()));
        ArrayList<String> playerNames = new ArrayList<>();
        for (OfflinePlayer player : players) {
            if (player != null && player.getName() != null)
                playerNames.add(player.getName());
        }
        ArrayList<String> commands = new ArrayList<>(playerNames);
        commands.add("delete");
        ArrayList<String> empty = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String s : commands) {
                if (s.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    empty.add(s);
                }
            }
            Collections.sort(empty);
            return empty;
        }
        return null;
    }
}
