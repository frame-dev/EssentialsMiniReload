/**
 * Dies ist ein Plugin von FrameDev
 * Bitte nichts §ndern, @Copyright by FrameDev
 */
package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.managers.InventoryManager;
import ch.framedev.essentialsmini.managers.ItemBuilder;
import ch.framedev.essentialsmini.managers.LocationsManager;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.simplejavautils.TextUtils;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author DHZoc
 */
public class HomeCMD extends CommandListenerBase {

    private final Main plugin;

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (command.getName().equalsIgnoreCase("homegui")) {
                InventoryManager inventoryManager = new InventoryManager("§aHomes");
                inventoryManager.setSize(3);
                inventoryManager.create();
                List<String> homes = new ArrayList<>();
                ConfigurationSection cs = new LocationsManager().getCfg().getConfigurationSection(sender.getName() + ".home");
                if (new LocationsManager().getCfg().contains(sender.getName() + ".home")) {
                    if (cs != null) {
                        for (String s : cs.getKeys(false)) {
                            if (s != null)
                                if (new LocationsManager().getCfg().get(sender.getName() + ".home." + s) != null) {
                                    if (!new LocationsManager().getCfg().get(sender.getName() + ".home." + s).equals(" ")) {
                                        homes.add(s);
                                    }
                                }
                        }
                    }
                }
                for (int i = 0; i < homes.size(); i++) {
                    inventoryManager.setItem(i, new ItemBuilder(Material.BLACK_BED).setDisplayName("§6" + homes.get(i)).setLore("§aTeleport to Home §6" + homes.get(i)).build());
                }
                inventoryManager.fillNull();
                if (sender instanceof Player) {
                    ((Player) sender).openInventory(inventoryManager.getInventory());
                }
            }
            if (command.getName().equalsIgnoreCase("sethome")) {
                if (sender instanceof Player) {
                    if (sender.hasPermission("essentialsmini.sethome")) {
                        new LocationsManager(sender.getName() + ".home.home").setLocation(((Player) sender).getLocation());
                        String homeSet = plugin.getLanguageConfig(sender).getString("HomeSet");
                        if(homeSet == null) {
                            sender.sendMessage(plugin.getPrefix() + "§cConfig 'HomeSet' not found! Please contact the Admin!");
                            return true;
                        }
                        if (homeSet.contains("&"))
                            homeSet = homeSet.replace('&', '§');
                        sender.sendMessage(plugin.getPrefix() + homeSet);
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            }
            if (command.getName().equalsIgnoreCase("home")) {
                try {
                    if (sender instanceof Player) {
                        if (sender.hasPermission(plugin.getPermissionBase() + "home")) {
                            if (new LocationsManager().getCfg().contains(sender.getName() + ".home.home") && !new LocationsManager().getCfg().get(sender.getName() + ".home.home").equals(" ")) {
                                ((Player) sender).teleport(new LocationsManager(sender.getName() + ".home.home").getLocation());
                                String homeTeleport = plugin.getLanguageConfig(sender).getString("HomeTeleport");
                                if(homeTeleport == null) {
                                    sender.sendMessage(plugin.getPrefix() + "§cConfig 'HomeTeleport' not found! Please contact the Admin!");
                                    return true;
                                }
                                if (homeTeleport.contains("&"))
                                    homeTeleport = homeTeleport.replace('&', '§');
                                sender.sendMessage(plugin.getPrefix() + homeTeleport);
                                homes.clear();
                            } else {
                                String homeExist = plugin.getLanguageConfig(sender).getString("HomeNotExist");
                                homeExist = ReplaceCharConfig.replaceParagraph(homeExist);
                                sender.sendMessage(plugin.getPrefix() + homeExist);
                                sender.sendMessage(plugin.getPrefix() + "§aHome setzen?");
                                BaseComponent baseComponent = new TextComponent();
                                baseComponent.addExtra("§6[Yes]");
                                baseComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sethome"));
                                baseComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aSet Home!")));
                                sender.spigot().sendMessage(baseComponent);
                            }
                        } else {
                            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                        }

                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                } catch (IllegalArgumentException x) {
                    String homeExist = plugin.getLanguageConfig(sender).getString("HomeNotExist");
                    homeExist = ReplaceCharConfig.replaceParagraph(homeExist);
                    sender.sendMessage(plugin.getPrefix() + homeExist);
                    String homeButton = plugin.getLanguageConfig(sender).getString("HomeButton");
                    homeButton = new TextUtils().replaceAndWithParagraph(homeButton);
                    sender.sendMessage(plugin.getPrefix() + homeButton);
                    BaseComponent baseComponent = new TextComponent();
                    baseComponent.addExtra("§6[Yes]");
                    baseComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sethome"));
                    String showText = plugin.getLanguageConfig(sender).getString("ShowTextHover");
                    showText = new TextUtils().replaceAndWithParagraph(showText);
                    baseComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(showText)));
                    sender.spigot().sendMessage(baseComponent);
                }
            }
            if (command.getName().equalsIgnoreCase("delhome")) {
                try {
                    if (sender instanceof Player) {
                        this.locationsManager = new LocationsManager(sender.getName() + ".home.home");
                        locationsManager.getCfg().set(sender.getName() + ".home.home", " ");
                        locationsManager.saveCfg();
                        String homeDeleted = plugin.getLanguageConfig(sender).getString("HomeDelete");
                        if(homeDeleted == null) {
                            sender.sendMessage(plugin.getPrefix() + "§cConfig 'HomeDelete' not found! Please contact the Admin!");
                            return true;
                        }
                        if (homeDeleted.contains("&"))
                            homeDeleted = ReplaceCharConfig.replaceParagraph(homeDeleted);
                        sender.sendMessage(plugin.getPrefix() + homeDeleted);
                        homes.clear();
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                    }
                } catch (IllegalArgumentException ex) {
                    String homeExist = plugin.getLanguageConfig(sender).getString("HomeNotExist");
                    homeExist = ReplaceCharConfig.replaceParagraph(homeExist);
                    sender.sendMessage(plugin.getPrefix() + homeExist);
                }
            }
        } else if (args.length == 1) {
            if (command.getName().equalsIgnoreCase("sethome")) {
                if (sender instanceof Player) {
                    String name = args[0].toLowerCase();
                    if (sender.hasPermission("essentialsmini.sethome")) {
                        if (!new LocationsManager(sender.getName() + ".home." + name).getCfg().contains(sender.getName() + ".home." + name) || new LocationsManager().getCfg().getString(sender.getName() + ".home." + name).equalsIgnoreCase(" ")) {
                            new LocationsManager(sender.getName() + ".home." + name)
                                    .setLocation(((Player) sender).getLocation());
                            String homeSet = plugin.getLanguageConfig(sender).getString("HomeSetOther");
                            homeSet = ReplaceCharConfig.replaceParagraph(homeSet);
                            homeSet = ReplaceCharConfig.replaceObjectWithData(homeSet, "%Name%", name);
                            sender.sendMessage(plugin.getPrefix() + homeSet);
                            homes.clear();
                        } else {
                            String exist = plugin.getLanguageConfig(sender).getString("HomeExist");
                            exist = ReplaceCharConfig.replaceParagraph(exist);
                            sender.sendMessage(plugin.getPrefix() + exist);
                        }
                        return true;
                    } else {
                        sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            }
            if (command.getName().equalsIgnoreCase("home")) {
                if (sender instanceof Player) {
                    String name = args[0].toLowerCase();
                    try {
                        if (sender.hasPermission(plugin.getPermissionBase() + "home")) {
                            ((Player) sender)
                                    .teleport(new LocationsManager(sender.getName() + ".home." + name).getLocation());
                            String homeTeleport = plugin.getLanguageConfig(sender).getString("HomeTeleportOther");
                            homeTeleport = ReplaceCharConfig.replaceParagraph(homeTeleport);
                            homeTeleport = ReplaceCharConfig.replaceObjectWithData(homeTeleport, "%Name%", name);
                            sender.sendMessage(plugin.getPrefix() + homeTeleport);
                            homes.clear();
                        } else {
                            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                        }
                    } catch (IllegalArgumentException ex) {
                        String homeExist = plugin.getLanguageConfig(sender).getString("HomeNotExist");
                        homeExist = ReplaceCharConfig.replaceParagraph(homeExist);
                        sender.sendMessage(plugin.getPrefix() + homeExist);
                        String homeButton = plugin.getLanguageConfig(sender).getString("HomeButton");
                        homeButton = new TextUtils().replaceAndWithParagraph(homeButton);
                        sender.sendMessage(plugin.getPrefix() + homeButton);
                        BaseComponent baseComponent = new TextComponent();
                        baseComponent.addExtra("§6[Yes]");
                        baseComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sethome " + name));
                        String showText = plugin.getLanguageConfig(sender).getString("ShowTextHoverOther");
                        showText = new TextUtils().replaceAndWithParagraph(showText);
                        showText = new TextUtils().replaceObject(showText, "%Home%", name);
                        baseComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(showText)));
                        sender.spigot().sendMessage(baseComponent);
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            }
            if (command.getName().equalsIgnoreCase("delhome")) {
                if (sender instanceof Player) {
                    String name = args[0].toLowerCase();
                    try {
                        this.locationsManager = new LocationsManager(sender.getName() + ".home." + name);
                        locationsManager.getCfg().set(sender.getName() + ".home." + name, " ");
                        locationsManager.saveCfg();
                        String homeDelete = plugin.getLanguageConfig(sender).getString("HomeDeleteOther");
                        homeDelete = ReplaceCharConfig.replaceParagraph(homeDelete);
                        homeDelete = ReplaceCharConfig.replaceObjectWithData(homeDelete, "%Name%", name);
                        sender.sendMessage(plugin.getPrefix() + homeDelete);
                        homes.clear();
                    } catch (IllegalArgumentException ex) {
                        String homeExist = plugin.getLanguageConfig(sender).getString("HomeNotExist");
                        homeExist = ReplaceCharConfig.replaceParagraph(homeExist);
                        sender.sendMessage(plugin.getPrefix() + homeExist);
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                }
            }
        } else if (args.length == 2) {
            if (command.getName().equalsIgnoreCase("delotherhome")) {
                if (sender.hasPermission(new Permission("essentialsmini.deletehome.others", PermissionDefault.OP))) {
                    OfflinePlayer target = PlayerUtils.getOfflinePlayerByName(args[1]);
                    String name = args[0];
                    try {
                        if (!new LocationsManager().getCfg().getString(target.getName() + ".home." + name).equalsIgnoreCase(" ")) {
                            this.locationsManager = new LocationsManager(target.getName() + ".home." + name);
                            locationsManager.getCfg().set(target.getName() + ".home." + name, " ");
                            locationsManager.saveCfg();
                            sender.sendMessage(plugin.getPrefix() + "§aDen Home von §6" + target.getName() + " §amit dem Namen §6" + name + " §awurde §centfernt!");
                            homes.clear();
                        }
                    } catch (IllegalArgumentException ex) {
                        sender.sendMessage(plugin.getPrefix() + "§cDieses Home wurde noch nicht gesetzt!");
                        sender.sendMessage(plugin.getPrefix() + "§cOder wurde schon entfernt!");
                    }
                } else {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            }
        } else {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/sethome §coder §6/sethome <Name>"));
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/home §coder §6/home <Name>"));
        }
        return false;
    }

    final ArrayList<String> homes = new ArrayList<>();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("home")) {
            ArrayList<String> homes = new ArrayList<>();
            if (args.length == 1) {
                if (new LocationsManager().getCfg().contains(sender.getName() + ".home")) {
                    ConfigurationSection cs = new LocationsManager().getCfg().getConfigurationSection(sender.getName() + ".home");
                    if (cs != null) {
                        for (String s : cs.getKeys(false)) {
                            if (new LocationsManager().getCfg().get(sender.getName() + ".home." + s) != null) {
                                if (!new LocationsManager().getCfg().get(sender.getName() + ".home." + s).equals(" ")) {
                                    if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                                        if (!s.equalsIgnoreCase("home")) {
                                            homes.add(s);
                                        }
                                    }
                                }
                            }
                        }
                        Collections.sort(homes);
                        return homes;
                    }
                }
            }
        }
        if (command.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 1) {
                if (new LocationsManager().getCfg().contains(sender.getName() + ".home")) {
                    ConfigurationSection cs = new LocationsManager().getCfg().getConfigurationSection(sender.getName() + ".home");
                    if (cs != null) {
                        for (String s : cs.getKeys(false)) {
                            if (new LocationsManager().getCfg().get(sender.getName() + ".home." + s) != null) {
                                if (!new LocationsManager().getCfg().get(sender.getName() + ".home." + s).equals(" ")) {
                                    if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                                        homes.add(s);
                                    }
                                }
                            }
                        }
                        Collections.sort(homes);
                        return homes;
                    }
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase("§aHomes")) {
            Player player = (Player) event.getWhoClicked();
            List<String> homes = new ArrayList<>();
            ConfigurationSection cs = new LocationsManager().getCfg().getConfigurationSection(player.getName() + ".home");
            if (new LocationsManager().getCfg().contains(player.getName() + ".home")) {
                if (cs != null) {
                    for (String s : cs.getKeys(false)) {
                        if (s != null)
                            if (new LocationsManager().getCfg().get(player.getName() + ".home." + s) != null) {
                                if (!new LocationsManager().getCfg().get(player.getName() + ".home." + s).equals(" ")) {
                                    homes.add(s);
                                }
                            }
                    }
                }
            }
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            if (!event.getCurrentItem().hasItemMeta()) return;
            if (event.getCurrentItem().getItemMeta() == null) return;
            if (!event.getCurrentItem().getItemMeta().hasDisplayName()) return;
            for (String s : homes) {
                if (event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase("§6" + s)) {
                    player.teleport(new LocationsManager(player.getName() + ".home." + s).getLocation());
                    String homeTeleport = plugin.getLanguageConfig(player).getString("HomeTeleportOther");
                    if(homeTeleport == null) {
                        event.getWhoClicked().sendMessage(plugin.getPrefix() + "§cCould not find HomeTeleportOther in messages");
                        return;
                    }
                    homeTeleport = ReplaceCharConfig.replaceParagraph(homeTeleport);
                    homeTeleport = ReplaceCharConfig.replaceObjectWithData(homeTeleport, "%Name%", s);
                    player.sendMessage(plugin.getPrefix() + homeTeleport);
                }
            }
        }
    }
}