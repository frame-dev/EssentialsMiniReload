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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BackpackCMD extends CommandListenerBase {

    private static final String INVENTORY_SUFFIX = "'s Inventory";
    private static final String INVENTORY_PATH = ".Inventory";
    private static final int DEFAULT_BACKPACK_SIZE = 3 * 9;

    static File file;
    static FileConfiguration cfg;
    public static final Map<String, String> itemsStringHashMap = new HashMap<>();
    private final Main plugin;
    private final TextUtils textUtils = new TextUtils();
    private final Map<UUID, UUID> openBackpackOwners = new HashMap<>();

    public BackpackCMD(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("Backpack")) {
            setup("backpack", this);
            setupTabCompleter("backpack", this);
            setupListener(this);
        }
        initializeStorage();
    }

    private static void initializeStorage() {
        if (file == null) {
            file = new File(Main.getInstance().getDataFolder(), "backpack.yml");
        }
        if (cfg == null) {
            cfg = YamlConfiguration.loadConfiguration(file);
        }
    }

    @EventHandler
    public void onCloseGui(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) {
            return;
        }

        UUID ownerId = openBackpackOwners.remove(viewer.getUniqueId());
        if (ownerId != null) {
            itemsStringHashMap.put(ownerId.toString(), InventoryStringDeSerializer.itemStackArrayToBase64(event.getInventory().getContents()));
        }
    }

    // Restore BackPack into HashMap
    public static void restore(OfflinePlayer player) {
        initializeStorage();
        if (player == null) return;
        String key = player.getUniqueId().toString();
        if (cfg.contains(key + INVENTORY_PATH)) {
            String content = cfg.getString(key + INVENTORY_PATH);
            if (content != null) itemsStringHashMap.put(key, content);
        }
    }

    public static void restoreAll() {
        initializeStorage();
        itemsStringHashMap.clear();
        for (String key : cfg.getKeys(false)) {
            String content = cfg.getString(key + INVENTORY_PATH);
            if (content != null) {
                itemsStringHashMap.put(key, content);
            }
        }
    }

    // Save Backpack
    public static void save(OfflinePlayer player) {
        initializeStorage();
        if (player == null) return;
        String key = player.getUniqueId().toString();
        String content = itemsStringHashMap.get(key);
        if (content != null) {
            cfg.set(key + INVENTORY_PATH, content);
            try {
                cfg.save(file);
            } catch (IOException e) {
                Main.getInstance().getLogger4J().error(e);
            }
        }
    }

    public static void saveAll() {
        initializeStorage();
        for (Map.Entry<String, String> entry : itemsStringHashMap.entrySet()) {
            cfg.set(entry.getKey() + INVENTORY_PATH, entry.getValue());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            return true;
        }

        if (!isBackpackEnabled()) {
            return true;
        }

        if (args.length == 0) {
            openBackpack(player, player);
            return true;
        }

        if (args.length == 1) {
            String arg = args[0];
            if (arg.equalsIgnoreCase("delete")) {
                handleDelete(player);
                return true;
            }

            if (!player.hasPermission("essentialsmini.backpack.see")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }

            OfflinePlayer targetPlayer = PlayerUtils.getOfflinePlayerByName(arg);
            String targetName = targetPlayer.getName();
            if (!arg.equalsIgnoreCase(targetName)) {
                sendNoBackpackFound(player);
                return true;
            }

            openBackpack(player, targetPlayer);
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/backpack [delete|player]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> commands = getTabCandidates();
        List<String> empty = new ArrayList<>();
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
        return Collections.emptyList();
    }

    private void handleDelete(Player player) {
        if (!player.hasPermission("essentialsmini.backpack.delete")) {
            player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            return;
        }

        itemsStringHashMap.clear();
        boolean english = isEnglishLocale(player.getLocale());
        if (file != null && file.exists() && file.delete()) {
            player.sendMessage(plugin.getPrefix() + (english ? "§6Backpacks deleted!" : "§6BackPacks gelöscht!"));
        } else {
            player.sendMessage(plugin.getPrefix() + (english ? "§cError while Deleting BackPacks!" : "§cError beim Löschen der Backpacks"));
        }
        file = null;
        cfg = null;
        initializeStorage();
    }

    private void openBackpack(Player viewer, OfflinePlayer owner) {
        if (owner == null) {
            sendNoBackpackFound(viewer);
            return;
        }

        String ownerName = owner.getName() == null ? "Unknown" : owner.getName();
        int size = normalizeInventorySize(plugin.getConfig().getInt("BackPackSize", DEFAULT_BACKPACK_SIZE));
        Inventory inventory = Bukkit.createInventory(null, size, backpackTitle(ownerName));

        String stored = itemsStringHashMap.get(owner.getUniqueId().toString());
        if (stored != null) {
            try {
                inventory.setContents(InventoryStringDeSerializer.itemStackArrayFromBase64(stored));
            } catch (IOException e) {
                plugin.getLogger4J().error(e);
            }
        }
        openBackpackOwners.put(viewer.getUniqueId(), owner.getUniqueId());
        viewer.openInventory(inventory);
    }

    private String backpackTitle(String ownerName) {
        return plugin.getConfiguredGuiTitle(
                "backpack",
                ownerName + INVENTORY_SUFFIX,
                Map.of("%Owner%", ownerName)
        );
    }

    private int normalizeInventorySize(int configuredSize) {
        int minSize = 9;
        int maxSize = 54;
        if (configuredSize < minSize) return DEFAULT_BACKPACK_SIZE;
        if (configuredSize > maxSize) return maxSize;
        return configuredSize - (configuredSize % 9);
    }

    private void sendNoBackpackFound(Player player) {
        String message = plugin.getLanguageConfig(player).getString("NoBackPackFound");
        message = textUtils.replaceAndWithParagraph(message == null ? "" : message);
        player.sendMessage(plugin.getPrefix() + message);
    }

    private boolean isBackpackEnabled() {
        return plugin.getConfig().getBoolean("Backpack");
    }

    private boolean isEnglishLocale(String locale) {
        if (locale == null) return false;
        String lower = locale.toLowerCase(Locale.ROOT);
        return lower.equals("en_us") || lower.equals("en_au") || lower.equals("en_gb")
                || lower.equals("en_nz") || lower.equals("en_za") || lower.equals("en_pt");
    }

    private List<String> getTabCandidates() {
        List<String> candidates = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer != null && offlinePlayer.getName() != null) {
                candidates.add(offlinePlayer.getName());
            }
        }
        candidates.add("delete");
        return candidates;
    }
}
