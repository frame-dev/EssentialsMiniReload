/**
 * Dies ist ein Plugin von FrameDev
 * Bitte nichts §ndern, @Copyright by FrameDev
 */
package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.InventoryManager;
import ch.framedev.essentialsmini.managers.ItemBuilder;
import ch.framedev.essentialsmini.managers.LocationsManager;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author DHZoc
 */
public class HomeCMD extends CommandListenerBase {

    private static final String HOME = "home";
    private static final String SET_HOME = "sethome";
    private static final String DEL_HOME = "delhome";
    private static final String DEL_OTHER_HOME = "delotherhome";
    private static final String HOME_GUI = "homegui";
    private static final String DEFAULT_HOME = "home";
    private static final String INVENTORY_TITLE = "§aHomes";
    private static final String EMPTY_LOCATION = " ";

    private final Main plugin;
    private LocationsManager locationsManager;

    final ArrayList<String> homes = new ArrayList<>();

    public HomeCMD(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        if (plugin.isHomeTP()) {
            setup(HOME, this);
            setup(SET_HOME, this);
            setup(DEL_HOME, this);
            setup(DEL_OTHER_HOME, this);
            setup(HOME_GUI, this);
            plugin.getTabCompleters().put(HOME, this);
            plugin.getTabCompleters().put(DEL_HOME, this);
            this.locationsManager = new LocationsManager();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);

        if (args.length == 0) {
            return switch (commandName) {
                case HOME_GUI -> openHomeGui(sender);
                case SET_HOME -> setHome(sender, DEFAULT_HOME, true);
                case HOME -> teleportHome(sender, DEFAULT_HOME, true);
                case DEL_HOME -> deleteOwnHome(sender, DEFAULT_HOME, true);
                default -> sendUsage(sender);
            };
        }

        if (args.length == 1) {
            String homeName = normalizeHomeName(args[0]);
            return switch (commandName) {
                case SET_HOME -> setHome(sender, homeName, false);
                case HOME -> teleportHome(sender, homeName, false);
                case DEL_HOME -> deleteOwnHome(sender, homeName, false);
                default -> sendUsage(sender);
            };
        }

        if (args.length == 2 && commandName.equals(DEL_OTHER_HOME)) {
            return deleteOtherHome(sender, normalizeHomeName(args[0]), args[1]);
        }

        return sendUsage(sender);
    }

    private boolean openHomeGui(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        InventoryManager inventoryManager = new InventoryManager(INVENTORY_TITLE);
        inventoryManager.setSize(3);
        inventoryManager.create();

        List<String> homesList = collectHomes(player.getName());
        int maxHomes = Math.min(homesList.size(), inventoryManager.getSize());
        for (int i = 0; i < maxHomes; i++) {
            String homeName = homesList.get(i);
            inventoryManager.setItem(i, new ItemBuilder(Material.BLACK_BED)
                    .setDisplayName("§6" + homeName)
                    .setLore("§aTeleport to Home §6" + homeName)
                    .build());
        }

        inventoryManager.fillNull();
        player.openInventory(inventoryManager.getInventory());
        return true;
    }

    private boolean setHome(CommandSender sender, String homeName, boolean defaultHome) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (!hasPermission(sender, "essentialsmini.sethome")) return true;

        String path = homePath(player.getName(), homeName);
        if (!defaultHome && homeExists(path)) {
            send(sender, langOrDefault(sender, "HomeExist", "§cHome already exists."));
            return true;
        }

        new LocationsManager(path).setLocation(player.getLocation());
        reloadLocations();

        String message = defaultHome
                ? langOrDefault(sender, "HomeSet", "§aHome set!")
                : formatHomeName(langOrDefault(sender, "HomeSetOther", "§aHome %Name% set."), homeName);
        send(sender, message);
        return true;
    }

    private boolean teleportHome(CommandSender sender, String homeName, boolean defaultHome) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (!hasPermission(sender, plugin.getPermissionBase() + "home")) return true;

        Location location = getHomeLocation(player.getName(), homeName);
        if (location == null) {
            sendHomeMissingMessage(player, homeName, defaultHome);
            return true;
        }

        player.teleport(location);
        String message = defaultHome
                ? langOrDefault(sender, "HomeTeleport", "§aTeleported to home.")
                : formatHomeName(langOrDefault(sender, "HomeTeleportOther", "§aTeleported to %Name%."), homeName);
        send(sender, message);
        return true;
    }

    private boolean deleteOwnHome(CommandSender sender, String homeName, boolean defaultHome) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (!deleteHome(player.getName(), homeName)) {
            sendHomeNotExist(sender);
            return true;
        }

        String message = defaultHome
                ? langOrDefault(sender, "HomeDelete", "§aHome deleted.")
                : formatHomeName(langOrDefault(sender, "HomeDeleteOther", "§aHome %Name% deleted."), homeName);
        send(sender, message);
        return true;
    }

    private boolean deleteOtherHome(CommandSender sender, String homeName, String targetNameArgument) {
        if (!hasPermission(sender, "essentialsmini.deletehome.others")) return true;

        OfflinePlayer target;
        try {
            target = PlayerUtils.getOfflinePlayerByName(targetNameArgument);
        } catch (IllegalArgumentException ex) {
            send(sender, "§cPlayer name cannot be empty!");
            return true;
        }

        String targetName = target.getName() == null ? targetNameArgument : target.getName();
        if (!deleteHome(targetName, homeName)) {
            send(sender, "§cDieses Home wurde noch nicht gesetzt!");
            send(sender, "§cOder wurde schon entfernt!");
            return true;
        }

        send(sender, "§aDen Home von §6" + targetName + " §amit dem Namen §6" + homeName + " §awurde §centfernt!");
        return true;
    }

    private boolean deleteHome(String playerName, String homeName) {
        String path = homePath(playerName, homeName);
        if (!homeExists(path)) return false;

        LocationsManager manager = new LocationsManager(path);
        manager.getCfg().set(path, EMPTY_LOCATION);
        manager.saveCfg();
        reloadLocations();
        return true;
    }

    private Location getHomeLocation(String playerName, String homeName) {
        String path = homePath(playerName, homeName);
        if (!homeExists(path)) return null;
        return new LocationsManager(path).getLocation();
    }

    private boolean homeExists(String path) {
        Object value = locations().getCfg().get(path);
        return value != null && !EMPTY_LOCATION.equals(String.valueOf(value));
    }

    private List<String> collectHomes(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        ConfigurationSection section = locations().getCfg().getConfigurationSection(playerName + ".home");
        if (section != null) {
            for (String homeName : section.getKeys(false)) {
                if (!DEFAULT_HOME.equalsIgnoreCase(homeName) && homeExists(homePath(playerName, homeName))) {
                    list.add(homeName);
                }
            }
        }

        Collections.sort(list);
        homes.clear();
        homes.addAll(list);
        return list;
    }

    private void sendHomeMissingMessage(Player player, String homeName, boolean defaultHome) {
        sendHomeNotExist(player);
        send(player, langOrDefault(player, "HomeButton", "§aSet a home?"));

        String command = defaultHome ? "/sethome" : "/sethome " + homeName;
        String hoverText = defaultHome
                ? langOrDefault(player, "ShowTextHover", "§aSet Home!")
                : formatHoverHomeName(langOrDefault(player, "ShowTextHoverOther", "§aSet Home!"), homeName);
        sendSetHomeButton(player, command, hoverText);
    }

    private void sendSetHomeButton(Player player, String command, String hoverText) {
        BaseComponent baseComponent = new TextComponent();
        baseComponent.addExtra("§6[Yes]");
        baseComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        baseComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
        player.spigot().sendMessage(baseComponent);
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        send(sender, plugin.getNoPerms(sender instanceof Player player ? player : null));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        send(sender, plugin.getOnlyPlayer(null));
        return null;
    }

    private String langOrDefault(CommandSender sender, String key, String defaultMessage) {
        String message = plugin.getLanguageConfig(sender).getString(key, defaultMessage);
        if (message == null) message = defaultMessage;
        return ReplaceCharConfig.replaceParagraph(message.replace('&', '§'));
    }

    private String formatHomeName(String message, String homeName) {
        return ReplaceCharConfig.replaceObjectWithData(message, "%Name%", homeName);
    }

    private String formatHoverHomeName(String message, String homeName) {
        return ReplaceCharConfig.replaceObjectWithData(message, "%Home%", homeName);
    }

    private void sendHomeNotExist(CommandSender sender) {
        send(sender, langOrDefault(sender, "HomeNotExist", "§cHome does not exist!"));
    }

    private boolean sendUsage(CommandSender sender) {
        send(sender, plugin.getWrongArgs("/sethome §coder §6/sethome <Name>"));
        send(sender, plugin.getWrongArgs("/home §coder §6/home <Name>"));
        return true;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    private String homePath(String playerName, String homeName) {
        return playerName + ".home." + homeName;
    }

    private String normalizeHomeName(String homeName) {
        return homeName.toLowerCase(Locale.ROOT);
    }

    private LocationsManager locations() {
        if (locationsManager == null) {
            locationsManager = new LocationsManager();
        }
        return locationsManager;
    }

    private void reloadLocations() {
        locationsManager = new LocationsManager();
        homes.clear();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1 && (commandName.equals(HOME) || commandName.equals(DEL_HOME))) {
            return matchingHomes(sender.getName(), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> matchingHomes(String playerName, String prefix) {
        return TabCompleteUtils.matchingStrings(collectHomes(playerName), prefix);
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(INVENTORY_TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String homeName = meta.getDisplayName().replaceFirst("^§6", "");
        if (!collectHomes(player.getName()).contains(homeName)) return;

        Location location = getHomeLocation(player.getName(), homeName);
        if (location == null) {
            sendHomeNotExist(player);
            return;
        }

        player.teleport(location);
        send(player, formatHomeName(langOrDefault(player, "HomeTeleportOther", "§aTeleported to %Name%."), homeName));
    }
}
