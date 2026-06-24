package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandListenerBase;
import ch.framedev.essentialsmini.main.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelpCMD extends CommandListenerBase {

    private static final String COMMAND_NAME = "emhelp";
    private static final String HELP_PERMISSION = "essentialsmini.help";
    private static final String TITLE_PREFIX = "§aEssentialsMini Help §8";
    private static final int INVENTORY_SIZE = 54;
    private static final int COMMAND_SLOTS = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int TEXT_PAGE_SIZE = 10;

    private static final Map<String, String> USAGES = createUsages();
    private static final Map<String, String> DETAILS = createDetails();

    private final Main plugin;
    private final NamespacedKey commandKey;
    private final NamespacedKey actionKey;

    public HelpCMD(Main plugin) {
        super(plugin, COMMAND_NAME);
        this.plugin = plugin;
        this.commandKey = new NamespacedKey(plugin, "help_command");
        this.actionKey = new NamespacedKey(plugin, "help_action");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(HELP_PERMISSION)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms(sender instanceof Player player ? player : null));
            return true;
        }

        int page = parsePage(args);
        if (sender instanceof Player player && !hasTextFlag(args)) {
            openHelpGui(player, page);
            return true;
        }

        sendTextHelp(sender, page);
        return true;
    }

    private void openHelpGui(Player player, int requestedPage) {
        List<HelpEntry> entries = getEntries();
        int maxPage = getMaxPage(entries.size(), COMMAND_SLOTS);
        int page = Math.min(Math.max(requestedPage, 1), maxPage);
        HelpHolder holder = new HelpHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, TITLE_PREFIX + "§7" + page + "/" + maxPage);
        holder.setInventory(inventory);

        int start = (page - 1) * COMMAND_SLOTS;
        int end = Math.min(entries.size(), start + COMMAND_SLOTS);
        for (int slot = 0, index = start; index < end; slot++, index++) {
            inventory.setItem(slot, createCommandItem(entries.get(index), player));
        }

        fillBorder(inventory);
        inventory.setItem(PREVIOUS_SLOT, createActionItem(Material.ARROW, "previous", "§ePrevious Page", "§7Go to page " + Math.max(1, page - 1), page > 1));
        inventory.setItem(CLOSE_SLOT, createActionItem(Material.BARRIER, "close", "§cClose", "§7Close this help menu", true));
        inventory.setItem(NEXT_SLOT, createActionItem(Material.ARROW, "next", "§eNext Page", "§7Go to page " + Math.min(maxPage, page + 1), page < maxPage));

        player.openInventory(inventory);
    }

    private ItemStack createCommandItem(HelpEntry entry, Player player) {
        boolean allowed = entry.permission() == null || player.hasPermission(entry.permission());
        ItemStack item = new ItemStack(allowed ? materialFor(entry.name()) : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName((allowed ? "§a/" : "§c/") + entry.name());
        meta.setLore(wrapLore(entry, allowed));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(commandKey, PersistentDataType.STRING, entry.name());
        item.setItemMeta(meta);
        return item;
    }

    private List<String> wrapLore(HelpEntry entry, boolean allowed) {
        List<String> lore = new ArrayList<>();
        lore.add("§7" + entry.description());
        lore.add("");
        lore.add("§eUsage:");
        for (String line : splitLongLine(entry.usage())) {
            lore.add("§f" + line);
        }

        if (!entry.aliases().isEmpty()) {
            lore.add("");
            lore.add("§eAliases: §7" + String.join(", ", entry.aliases()));
        }

        lore.add("");
        lore.add("§ePermission: §7" + (entry.permission() == null ? "none" : entry.permission()));
        lore.add(allowed ? "§aYou can use this command." : "§cYou do not have this permission.");

        String detail = DETAILS.get(entry.name());
        if (detail != null) {
            lore.add("");
            lore.add("§eInfo:");
            for (String line : splitLongLine(detail)) {
                lore.add("§7" + line);
            }
        }

        lore.add("");
        lore.add("§8Click for chat help.");
        return lore;
    }

    private ItemStack createActionItem(Material material, String action, String name, String lore, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? material : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(enabled ? name : "§8" + ChatColor.stripColor(name));
        meta.setLore(List.of(lore));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, enabled ? action : "disabled");
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

        for (int slot = COMMAND_SLOTS; slot < INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void sendTextHelp(CommandSender sender, int requestedPage) {
        List<HelpEntry> entries = getEntries();
        int maxPage = getMaxPage(entries.size(), TEXT_PAGE_SIZE);
        int page = Math.min(Math.max(requestedPage, 1), maxPage);
        int start = (page - 1) * TEXT_PAGE_SIZE;
        int end = Math.min(entries.size(), start + TEXT_PAGE_SIZE);

        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§aEssentialsMini Help §7Page §e" + page + "§7/§e" + maxPage);
        sender.sendMessage("§7Use §e/emhelp <page>§7. Players can use §e/emhelp§7 for the GUI.");
        sender.sendMessage("");

        for (int i = start; i < end; i++) {
            HelpEntry entry = entries.get(i);
            sender.sendMessage("§a" + entry.usage() + " §8- §7" + entry.description());
            sender.sendMessage("  §8Permission: §7" + (entry.permission() == null ? "none" : entry.permission()));
        }

        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendEntryMessage(Player player, HelpEntry entry) {
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§a/" + entry.name());
        player.sendMessage("§7" + entry.description());
        player.sendMessage("§eUsage: §f" + entry.usage());
        if (!entry.aliases().isEmpty()) {
            player.sendMessage("§eAliases: §7" + String.join(", ", entry.aliases()));
        }
        player.sendMessage("§ePermission: §7" + (entry.permission() == null ? "none" : entry.permission()));

        TextComponent component = new TextComponent("§aClick here to paste the command into chat");
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, entry.usage().split(" ")[0]));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Suggest " + entry.usage())));
        player.spigot().sendMessage(component);
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private List<HelpEntry> getEntries() {
        Map<String, Map<String, Object>> commands = plugin.getDescription().getCommands();
        if (commands.isEmpty()) {
            return Collections.emptyList();
        }

        List<HelpEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> command : commands.entrySet()) {
            String name = command.getKey().toLowerCase(Locale.ROOT);
            Map<String, Object> data = command.getValue() == null ? Map.of() : command.getValue();
            entries.add(new HelpEntry(
                    name,
                    getDescription(name, data),
                    USAGES.getOrDefault(name, "/" + name),
                    getPermission(name, data),
                    getAliases(data)
            ));
        }

        entries.sort(Comparator.comparing(HelpEntry::name));
        return entries;
    }

    private String getDescription(String name, Map<String, Object> data) {
        Object description = data.get("description");
        if (description != null && !String.valueOf(description).isBlank()) {
            return String.valueOf(description);
        }
        return DETAILS.getOrDefault(name, "No description available yet.");
    }

    private String getPermission(String name, Map<String, Object> data) {
        Object configuredPermission = data.get("permission");
        if (configuredPermission != null && !String.valueOf(configuredPermission).isBlank()) {
            return String.valueOf(configuredPermission);
        }

        String inferred = "essentialsmini." + name;
        Permission permission = Bukkit.getPluginManager().getPermission(inferred);
        return permission == null ? null : inferred;
    }

    private List<String> getAliases(Map<String, Object> data) {
        Object aliases = data.get("aliases");
        if (aliases instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .filter(alias -> !alias.isBlank())
                    .collect(Collectors.toList());
        }
        if (aliases instanceof String alias && !alias.isBlank()) {
            return List.of(alias);
        }
        return List.of();
    }

    private Material materialFor(String name) {
        if (name.contains("ban") || name.contains("mute")) return Material.REDSTONE_BLOCK;
        if (name.contains("warp") || name.contains("tp") || name.equals("back") || name.equals("top")) return Material.ENDER_PEARL;
        if (name.contains("home") || name.equals("spawn")) return Material.COMPASS;
        if (name.contains("weather") || name.equals("sun") || name.equals("rain") || name.equals("thunder")) return Material.SUNFLOWER;
        if (name.contains("time") || name.equals("day") || name.equals("night")) return Material.CLOCK;
        if (name.contains("eco") || name.contains("balance") || name.equals("pay") || name.equals("bank")) return Material.GOLD_INGOT;
        if (name.contains("kit") || name.equals("item") || name.equals("repair") || name.equals("enchant")) return Material.DIAMOND_PICKAXE;
        return switch (name) {
            case "heal", "feed", "godmode" -> Material.GOLDEN_APPLE;
            case "mail", "msg", "r", "staffchat" -> Material.WRITABLE_BOOK;
            case "emhelp", "essentialsmini" -> Material.BOOK;
            default -> Material.PAPER;
        };
    }

    private int parsePage(String[] args) {
        if (args == null) {
            return 1;
        }

        for (String arg : args) {
            try {
                return Math.max(1, Integer.parseInt(arg));
            } catch (NumberFormatException ignored) {
                // Keep looking for a numeric page argument.
            }
        }
        return 1;
    }

    private boolean hasTextFlag(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if ("text".equalsIgnoreCase(arg) || "list".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private int getMaxPage(int entries, int pageSize) {
        return Math.max(1, (entries + pageSize - 1) / pageSize);
    }

    private List<String> splitLongLine(String input) {
        if (input == null || input.length() <= 34) {
            return List.of(input == null ? "" : input);
        }

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : input.split(" ")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > 34) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(HELP_PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return Stream.of("1", "2", "3", "text", "list")
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof HelpHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if ("close".equals(action)) {
            player.closeInventory();
            return;
        }
        if ("previous".equals(action)) {
            openHelpGui(player, holder.page() - 1);
            return;
        }
        if ("next".equals(action)) {
            openHelpGui(player, holder.page() + 1);
            return;
        }

        String commandName = meta.getPersistentDataContainer().get(commandKey, PersistentDataType.STRING);
        if (commandName == null) {
            return;
        }

        getEntries().stream()
                .filter(entry -> entry.name().equals(commandName))
                .findFirst()
                .ifPresent(entry -> sendEntryMessage(player, entry));
    }

    private static Map<String, String> createUsages() {
        Map<String, String> usages = new LinkedHashMap<>();
        usages.put("afk", "/afk");
        usages.put("anvil", "/anvil");
        usages.put("back", "/back");
        usages.put("backpack", "/backpack [player]");
        usages.put("balance", "/balance [player]");
        usages.put("balancetop", "/balancetop");
        usages.put("bank", "/bank <create|balance|deposit|withdraw|transfer|info|members>");
        usages.put("book", "/book");
        usages.put("cartographytable", "/cartographytable");
        usages.put("chatclear", "/chatclear");
        usages.put("copybook", "/copybook");
        usages.put("createkit", "/createkit <name>");
        usages.put("day", "/day [world]");
        usages.put("delhome", "/delhome <name>");
        usages.put("delotherhome", "/delotherhome <player> <home>");
        usages.put("delwarp", "/delwarp <name>");
        usages.put("eban", "/eban <player> <reason>");
        usages.put("eco", "/eco <add|remove|set> <amount> [player] or /eco <add|remove|set> <player> <amount>");
        usages.put("enchant", "/enchant <enchantment> [level]");
        usages.put("enderchest", "/enderchest [player]");
        usages.put("essentialsmini", "/essentialsmini <reload|help|economy|version|about>");
        usages.put("eunban", "/eunban <player>");
        usages.put("feed", "/feed [player]");
        usages.put("firework", "/firework");
        usages.put("fly", "/fly [player]");
        usages.put("flyspeed", "/flyspeed <0-10|reset> [player]");
        usages.put("gamemode", "/gamemode <survival|creative|adventure|spectator> [player]");
        usages.put("globalmute", "/globalmute");
        usages.put("godmode", "/godmode [player]");
        usages.put("grindstone", "/grindstone");
        usages.put("heal", "/heal [player]");
        usages.put("emhelp", "/emhelp [page|text]");
        usages.put("home", "/home <name>");
        usages.put("homegui", "/homegui");
        usages.put("invsee", "/invsee <player>");
        usages.put("item", "/item <material> [amount]");
        usages.put("killall", "/killall");
        usages.put("kits", "/kits [kit]");
        usages.put("lightningstrike", "/lightningstrike [player]");
        usages.put("loom", "/loom");
        usages.put("mail", "/mail <read|send|clear> [player] [message]");
        usages.put("maintenance", "/maintenance <on|off>");
        usages.put("msg", "/msg <player> <message>");
        usages.put("msgtoggle", "/msgtoggle");
        usages.put("mute", "/mute <player> <reason>");
        usages.put("muteforplayer", "/muteforplayer <player> <target>");
        usages.put("muteinfo", "/muteinfo [player]");
        usages.put("mysql", "/mysql");
        usages.put("near", "/near [radius]");
        usages.put("nick", "/nick <nickname> <skin>");
        usages.put("nicklist", "/nicklist");
        usages.put("night", "/night [world]");
        usages.put("offline", "/offline");
        usages.put("online", "/online");
        usages.put("pay", "/pay <amount> <player> or /pay <player> <amount>");
        usages.put("playerheads", "/playerheads <player>");
        usages.put("playerweather", "/playerweather <sun|rain|thunder> [player]");
        usages.put("pltime", "/pltime <day|night|ticks> [player]");
        usages.put("plweather", "/plweather <sun|rain|thunder> [player]");
        usages.put("position", "/position");
        usages.put("rain", "/rain [world]");
        usages.put("r", "/r <message>");
        usages.put("removetempban", "/removetempban <player>");
        usages.put("removetempmute", "/removetempmute <player>");
        usages.put("renameitem", "/renameitem <name>");
        usages.put("repair", "/repair [player]");
        usages.put("resetplayerweather", "/resetplayerweather [player]");
        usages.put("resetpltime", "/resetpltime [player]");
        usages.put("resetplweather", "/resetplweather [player]");
        usages.put("retrieve", "/retrieve");
        usages.put("sethome", "/sethome <name>");
        usages.put("setspawn", "/setspawn");
        usages.put("setwarp", "/setwarp <name> [cost]");
        usages.put("showrecipe", "/showrecipe [material]");
        usages.put("signitem", "/signitem");
        usages.put("signremove", "/signremove");
        usages.put("silent", "/silent");
        usages.put("sleep", "/sleep");
        usages.put("smithingtable", "/smithingtable");
        usages.put("spawn", "/spawn");
        usages.put("spawnmob", "/spawnmob <type> [amount]");
        usages.put("spy", "/spy");
        usages.put("staffchat", "/staffchat <message>");
        usages.put("stonecutter", "/stonecutter");
        usages.put("suicid", "/suicid");
        usages.put("sun", "/sun [world]");
        usages.put("tempban", "/tempban <player> <time> <unit> <reason>");
        usages.put("tempmute", "/tempmute <player> <time> <unit> <reason>");
        usages.put("thunder", "/thunder [world]");
        usages.put("timeplayed", "/timeplayed [player]");
        usages.put("top", "/top");
        usages.put("tpa", "/tpa <player>");
        usages.put("tpaaccept", "/tpaaccept");
        usages.put("tpadeny", "/tpadeny");
        usages.put("tpahere", "/tpahere <player>");
        usages.put("tpahereaccept", "/tpahereaccept");
        usages.put("tpaheredeny", "/tpaheredeny");
        usages.put("tphereall", "/tphereall");
        usages.put("tptoggle", "/tptoggle");
        usages.put("trash", "/trash");
        usages.put("unban", "/unban <player>");
        usages.put("vanish", "/vanish [player]");
        usages.put("walkspeed", "/walkspeed <speed> [player]");
        usages.put("warp", "/warp <name>");
        usages.put("warps", "/warps");
        usages.put("workbench", "/workbench");
        usages.put("xp", "/xp <add|set|remove> <amount> [player]");
        return usages;
    }

    private static Map<String, String> createDetails() {
        Map<String, String> details = new HashMap<>();
        details.put("emhelp", "Opens this paged command GUI. Use /emhelp text for a chat version.");
        details.put("nick", "Changes your display name and, when ProtocolLib is available, refreshes your visible skin.");
        details.put("nicklist", "Shows players currently marked as nicked.");
        details.put("maintenance", "Toggles maintenance mode so only allowed players can join.");
        details.put("playerweather", "Alias of /plweather.");
        details.put("resetplayerweather", "Alias of /resetplweather.");
        details.put("essentialsmini", "Main administration command for reload, version, and feature toggles.");
        return details;
    }

    private record HelpEntry(String name, String description, String usage, String permission, List<String> aliases) {
    }

    private static final class HelpHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private HelpHolder(int page) {
            this.page = page;
        }

        private int page() {
            return page;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        @NotNull
        public Inventory getInventory() {
            return inventory;
        }
    }
}
