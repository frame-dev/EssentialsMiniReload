/**
 * Dies ist ein Plugin von FrameDev
 * Bitte nichts §ndern, @Copyright by FrameDev
 */
package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.managers.InventoryManager;
import ch.framedev.essentialsmini.managers.ItemBuilder;
import ch.framedev.essentialsmini.managers.LocationsManager;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author DHZoc
 */
public class HomeCMD extends CommandListenerBase {

    private final Main plugin;
    private final String inventoryTitle = "§aHomes";

    public HomeCMD(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        if (plugin.isHomeTP()) {
            setup("home", this);
            setup("sethome", this);
            setup("delhome", this);
            setup("delotherhome", this);
            setup("homegui", this);
            plugin.getTabCompleters().put("home", this);
            plugin.getTabCompleters().put("delhome", this);
            this.locationsManager = new LocationsManager();
        }
    }

    private LocationsManager locationsManager;

    // cached homes list (kept for compatibility with existing calls to homes.clear())
    final ArrayList<String> homes = new ArrayList<>();

    // Helper: safely get language string for sender with a default and paragraph/color replacements
    private String langOrDefault(CommandSender sender, String key, String defaultMsg) {
        String s = plugin.getLanguageConfig(sender).getString(key);
        if (s == null) return defaultMsg;
        // replace & with § when present and apply paragraph replacement
        if (s.contains("&")) s = s.replace('&', '§');
        return ReplaceCharConfig.replaceParagraph(s);
    }

    // Helper: collect homes for a player name
    private List<String> collectHomes(String playerName) {
        List<String> list = new ArrayList<>();
        if (locationsManager == null) locationsManager = new LocationsManager();
        ConfigurationSection cs = locationsManager.getCfg().getConfigurationSection(playerName + ".home");
        if (cs != null) {
            for (String s : cs.getKeys(false)) {
                if (s == null) continue;
                Object val = locationsManager.getCfg().get(playerName + ".home." + s);
                if (val != null && !String.valueOf(val).equals(" ")) {
                    if (!s.equalsIgnoreCase("home")) list.add(s);
                }
            }
        }
        Collections.sort(list);
        // update cached homes to keep older code that calls homes.clear()
        homes.clear();
        homes.addAll(list);
        return list;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (command.getName().equalsIgnoreCase("homegui")) {
                if (!(sender instanceof Player pSender)) return true;
                InventoryManager inventoryManager = new InventoryManager(inventoryTitle);
                inventoryManager.setSize(3);
                inventoryManager.create();

                List<String> homesList = collectHomes(pSender.getName());

                for (int i = 0; i < homesList.size(); i++) {
                    inventoryManager.setItem(i, new ItemBuilder(Material.BLACK_BED)
                            .setDisplayName("§6" + homesList.get(i))
                            .setLore("§aTeleport to Home §6" + homesList.get(i)).build());
                }
                inventoryManager.fillNull();
                pSender.openInventory(inventoryManager.getInventory());
            }

            if (command.getName().equalsIgnoreCase("sethome")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                if (!sender.hasPermission("essentialsmini.sethome")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                // default home name 'home'
                new LocationsManager(player.getName() + ".home.home").setLocation(player.getLocation());
                String homeSet = langOrDefault(sender, "HomeSet", "§aHome set!");
                sender.sendMessage(plugin.getPrefix() + homeSet);
            }

            if (command.getName().equalsIgnoreCase("home")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                if (!sender.hasPermission(plugin.getPermissionBase() + "home")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                try {
                    if (locationsManager.getCfg().contains(player.getName() + ".home.home") && !String.valueOf(locationsManager.getCfg().get(player.getName() + ".home.home")).equals(" ")) {
                        player.teleport(new LocationsManager(player.getName() + ".home.home").getLocation());
                        String homeTeleport = langOrDefault(sender, "HomeTeleport", "§aTeleported to home.");
                        sender.sendMessage(plugin.getPrefix() + homeTeleport);
                    } else {
                        String homeExist = langOrDefault(sender, "HomeNotExist", "§cHome does not exist!");
                        sender.sendMessage(plugin.getPrefix() + homeExist);

                        BaseComponent baseComponent = new TextComponent();
                        baseComponent.addExtra("§6[Yes]");
                        baseComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sethome"));
                        baseComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aSet Home!")));
                        sender.spigot().sendMessage(baseComponent);
                    }
                } catch (IllegalArgumentException x) {
                    String homeExist = langOrDefault(sender, "HomeNotExist", "§cHome does not exist!");
                    sender.sendMessage(plugin.getPrefix() + homeExist);
                    sender.sendMessage(plugin.getPrefix() + langOrDefault(sender, "HomeButton", "§aSet a home?"));
                    BaseComponent baseComponent = new TextComponent();
                    baseComponent.addExtra("§6[Yes]");
                    baseComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sethome"));
                    String showText = langOrDefault(sender, "ShowTextHover", "§aSet Home!");
                    baseComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(showText)));
                    sender.spigot().sendMessage(baseComponent);
                }
            }

            if (command.getName().equalsIgnoreCase("delhome")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                try {
                    this.locationsManager = new LocationsManager(player.getName() + ".home.home");
                    locationsManager.getCfg().set(player.getName() + ".home.home", " ");
                    locationsManager.saveCfg();
                    String homeDeleted = langOrDefault(sender, "HomeDelete", "§aHome deleted.");
                    sender.sendMessage(plugin.getPrefix() + homeDeleted);
                    homes.clear();
                } catch (IllegalArgumentException ex) {
                    String homeExist = langOrDefault(sender, "HomeNotExist", "§cHome does not exist!");
                    sender.sendMessage(plugin.getPrefix() + homeExist);
                }
            }

        } else if (args.length == 1) {
            if (command.getName().equalsIgnoreCase("sethome")) {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                String name = args[0].toLowerCase();
                if (!sender.hasPermission("essentialsmini.sethome")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                LocationsManager lm = new LocationsManager(sender.getName() + ".home." + name);
                if (!locationsManager.getCfg().contains(sender.getName() + ".home." + name) || locationsManager.getCfg().getString(sender.getName() + ".home." + name).equalsIgnoreCase(" ")) {
                    lm.setLocation(p.getLocation());
                    String homeSet = langOrDefault(sender, "HomeSetOther", "§aHome %Name% set.");
                    homeSet = ReplaceCharConfig.replaceObjectWithData(homeSet, "%Name%", name);
                    sender.sendMessage(plugin.getPrefix() + homeSet);
                    homes.clear();
                } else {
                    String exist = langOrDefault(sender, "HomeExist", "§cHome already exists.");
                    sender.sendMessage(plugin.getPrefix() + exist);
                }
                return true;
            }

            if (command.getName().equalsIgnoreCase("home")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                String name = args[0].toLowerCase();
                try {
                    if (!sender.hasPermission(plugin.getPermissionBase() + "home")) {
                        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                        return true;
                    }
                    player.teleport(new LocationsManager(player.getName() + ".home." + name).getLocation());
                    String homeTeleport = langOrDefault(sender, "HomeTeleportOther", "§aTeleported to %Name%.");
                    homeTeleport = ReplaceCharConfig.replaceObjectWithData(homeTeleport, "%Name%", name);
                    sender.sendMessage(plugin.getPrefix() + homeTeleport);
                    homes.clear();
                } catch (IllegalArgumentException ex) {
                    String homeExist = langOrDefault(sender, "HomeNotExist", "§cHome does not exist!");
                    sender.sendMessage(plugin.getPrefix() + homeExist);
                    sender.sendMessage(plugin.getPrefix() + langOrDefault(sender, "HomeButton", "§aSet a home?"));
                    BaseComponent baseComponent = new TextComponent();
                    baseComponent.addExtra("§6[Yes]");
                    baseComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sethome " + name));
                    String showText = langOrDefault(sender, "ShowTextHoverOther", "§aSet Home!");
                    showText = ReplaceCharConfig.replaceObjectWithData(showText, "%Home%", name);
                    baseComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(showText)));
                    sender.spigot().sendMessage(baseComponent);
                }
            }

            if (command.getName().equalsIgnoreCase("delhome")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    return true;
                }
                String name = args[0].toLowerCase();
                try {
                    this.locationsManager = new LocationsManager(sender.getName() + ".home." + name);
                    locationsManager.getCfg().set(sender.getName() + ".home." + name, " ");
                    locationsManager.saveCfg();
                    String homeDelete = langOrDefault(sender, "HomeDeleteOther", "§aHome %Name% deleted.");
                    homeDelete = ReplaceCharConfig.replaceObjectWithData(homeDelete, "%Name%", name);
                    sender.sendMessage(plugin.getPrefix() + homeDelete);
                    homes.clear();
                } catch (IllegalArgumentException ex) {
                    String homeExist = langOrDefault(sender, "HomeNotExist", "§cHome does not exist!");
                    sender.sendMessage(plugin.getPrefix() + homeExist);
                }
            }

        } else if (args.length == 2) {
            if (command.getName().equalsIgnoreCase("delotherhome")) {
                if (!sender.hasPermission(new Permission("essentialsmini.deletehome.others", PermissionDefault.OP))) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                OfflinePlayer target = PlayerUtils.getOfflinePlayerByName(args[1]);
                String name = args[0];
                try {
                    String val = locationsManager.getCfg().getString(target.getName() + ".home." + name, " ");
                    if (!val.equalsIgnoreCase(" ")) {
                        this.locationsManager = new LocationsManager(target.getName() + ".home." + name);
                        locationsManager.getCfg().set(target.getName() + ".home." + name, " ");
                        locationsManager.saveCfg();
                        sender.sendMessage(plugin.getPrefix() + "§aDen Home von §6" + target.getName() + " §amit dem Namen §6" + name + " §awurde §centfernt!");
                        homes.clear();
                    } else {
                        sender.sendMessage(plugin.getPrefix() + "§cDieses Home wurde noch nicht gesetzt!");
                        sender.sendMessage(plugin.getPrefix() + "§cOder wurde schon entfernt!");
                    }
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(plugin.getPrefix() + "§cDieses Home wurde noch nicht gesetzt!");
                    sender.sendMessage(plugin.getPrefix() + "§cOder wurde schon entfernt!");
                }
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/sethome §coder §6/sethome <Name>"));
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/home §coder §6/home <Name>"));
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("home")) {
            ArrayList<String> homes = new ArrayList<>();
            if (args.length == 1) {
                List<String> list = collectHomes(sender.getName());
                for (String s : list) {
                    if (s.toLowerCase().startsWith(args[0].toLowerCase())) homes.add(s);
                }
                Collections.sort(homes);
                return homes;
            }
        }
        if (command.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 1) {
                List<String> list = collectHomes(sender.getName());
                ArrayList<String> homes = new ArrayList<>();
                for (String s : list) {
                    if (s.toLowerCase().startsWith(args[0].toLowerCase())) homes.add(s);
                }
                Collections.sort(homes);
                return homes;
            }
        }
        return null;
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(inventoryTitle)) {
            Player player = (Player) event.getWhoClicked();
            List<String> homesList = collectHomes(player.getName());
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            if (!event.getCurrentItem().hasItemMeta()) return;
            if (event.getCurrentItem().getItemMeta() == null) return;
            if (!event.getCurrentItem().getItemMeta().hasDisplayName()) return;
            for (String s : homesList) {
                if (event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase("§6" + s)) {
                    player.teleport(new LocationsManager(player.getName() + ".home." + s).getLocation());
                    String homeTeleport = langOrDefault(player, "HomeTeleportOther", "§aTeleported to %Name%.");
                    homeTeleport = ReplaceCharConfig.replaceObjectWithData(homeTeleport, "%Name%", s);
                    player.sendMessage(plugin.getPrefix() + homeTeleport);
                }
            }
        }
    }
}