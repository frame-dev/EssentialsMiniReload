package ch.framedev.essentialsmini.commands.playercommands;


/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 13.08.2020 19:44
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EnchantCMD extends CommandBase {

    private static final String PERM_ENCHANT = "enchant";
    private static final String PERM_ENCHANT_OTHERS = "enchant.others";
    private static final String ENCHANT_UNBREAKABLE = "unbreakable";

    private final Main plugin;

    public EnchantCMD(Main plugin) {
        super(plugin, "enchant");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
                return true;
            }
            if (!player.hasPermission(plugin.getPermissionBase() + PERM_ENCHANT)) {
                player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }
            return applyEnchant(sender, player, args[0], args[1]);
        }

        if (args.length == 3) {
            if (!sender.hasPermission(plugin.getPermissionBase() + PERM_ENCHANT_OTHERS)) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }

            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix() + plugin.getVariables().getPlayerNameNotOnline(args[2]));
                return true;
            }

            return applyEnchant(sender, target, args[0], args[1]);
        }

        if (sender.hasPermission(plugin.getPermissionBase() + PERM_ENCHANT)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/enchant <Enchantment Name> <Level|true|false>"));
        }
        if (sender.hasPermission(plugin.getPermissionBase() + PERM_ENCHANT_OTHERS)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/enchant <Enchantment Name> <Level|true|false> <Player Name>"));
        }
        return true;
    }

    private boolean applyEnchant(CommandSender sender, Player target, String enchantName, String value) {
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            String noItemInHand = plugin.getLanguageConfig(target).getString("NoItemFoundInHand");
            noItemInHand = noItemInHand == null ? "§cNo item found in hand." : ReplaceCharConfig.replaceParagraph(noItemInHand);
            sender.sendMessage(plugin.getPrefix() + noItemInHand);
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            sender.sendMessage(plugin.getPrefix() + "§cThis item can't be enchanted!");
            return true;
        }

        if (ENCHANT_UNBREAKABLE.equalsIgnoreCase(enchantName)) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/enchant unbreakable <true|false> [Player]"));
                return true;
            }
            meta.setUnbreakable(Boolean.parseBoolean(value));
            item.setItemMeta(meta);
            return true;
        }

        Enchantment enchantment = Enchantments.getByName(enchantName);
        if (enchantment == null) {
            String message = plugin.getLanguageConfig(sender).getString("EnchantNotExist");
            message = message == null ? "§cThis enchantment does not exist." : new TextUtils().replaceAndWithParagraph(message);
            sender.sendMessage(plugin.getPrefix() + message);
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/enchant <Enchantment Name> <Level> [Player Name]"));
            return true;
        }

        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission(plugin.getPermissionBase() + PERM_ENCHANT) || sender.hasPermission(plugin.getPermissionBase() + PERM_ENCHANT_OTHERS)) {
                ArrayList<String> suggestions = new ArrayList<>();
                suggestions.add(ENCHANT_UNBREAKABLE);
                String prefix = args[0].toLowerCase(Locale.ROOT);
                for (Map.Entry<String, Enchantment> entry : Enchantments.entrySet()) {
                    if (entry.getKey().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        suggestions.add(entry.getKey());
                    }
                }
                for (String alias : Enchantments.aliases()) {
                    if (alias.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        suggestions.add(alias);
                    }
                }
                Collections.sort(suggestions);
                return suggestions;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase(ENCHANT_UNBREAKABLE)) {
            return filter(List.of("true", "false"), args[1]);
        } else if (args.length == 3 && sender.hasPermission(plugin.getPermissionBase() + PERM_ENCHANT_OTHERS)) {
            ArrayList<String> players = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                players.add(online.getName());
            }
            return filter(players, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> source, String startsWith) {
        String lower = startsWith.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(value);
            }
        }
        Collections.sort(out);
        return out;
    }

    public static class Enchantments {

        private static final Map<String, Enchantment> ENCHANTMENTS = new HashMap<>();
        private static final Map<String, Enchantment> ALIAS_ENCHANTMENTS = new HashMap<>();

        static {

            // Depth Strider
            ENCHANTMENTS.put("depthstrider", Enchantment.DEPTH_STRIDER);
            ALIAS_ENCHANTMENTS.put("dswim", Enchantment.DEPTH_STRIDER);

            // Soul Speed
            ENCHANTMENTS.put("soulspeed", Enchantment.SOUL_SPEED);
            ALIAS_ENCHANTMENTS.put("ss", Enchantment.SOUL_SPEED);

            // Power
            ENCHANTMENTS.put("power", Enchantment.POWER);
            ALIAS_ENCHANTMENTS.put("p", Enchantment.POWER);

            // Swift Sneak
            ENCHANTMENTS.put("swiftsneak", Enchantment.SWIFT_SNEAK);
            ALIAS_ENCHANTMENTS.put("sneak", Enchantment.SWIFT_SNEAK);

            // Punch
            ENCHANTMENTS.put("punch", Enchantment.PUNCH);
            ALIAS_ENCHANTMENTS.put("pu", Enchantment.PUNCH);

            // Flame
            ENCHANTMENTS.put("flame", Enchantment.FLAME);
            ALIAS_ENCHANTMENTS.put("f", Enchantment.FLAME);

            // Damage-related Enchantments
            ENCHANTMENTS.put("alldamage", Enchantment.SHARPNESS);
            ALIAS_ENCHANTMENTS.put("alldmg", Enchantment.SHARPNESS);
            ENCHANTMENTS.put("sharpness", Enchantment.SHARPNESS);
            ALIAS_ENCHANTMENTS.put("sharp", Enchantment.SHARPNESS);
            ALIAS_ENCHANTMENTS.put("dal", Enchantment.SHARPNESS);

            // Smite
            ENCHANTMENTS.put("undeaddamage", Enchantment.SMITE);
            ENCHANTMENTS.put("smite", Enchantment.SMITE);
            ALIAS_ENCHANTMENTS.put("du", Enchantment.SMITE);

            // Bane of Arthropods
            ENCHANTMENTS.put("baneofarthropods", Enchantment.BANE_OF_ARTHROPODS);
            ALIAS_ENCHANTMENTS.put("arthropod", Enchantment.BANE_OF_ARTHROPODS);
            ALIAS_ENCHANTMENTS.put("dar", Enchantment.BANE_OF_ARTHROPODS);

            // Efficiency
            ENCHANTMENTS.put("digspeed", Enchantment.EFFICIENCY);
            ENCHANTMENTS.put("efficiency", Enchantment.EFFICIENCY);
            ALIAS_ENCHANTMENTS.put("minespeed", Enchantment.EFFICIENCY);
            ALIAS_ENCHANTMENTS.put("cutspeed", Enchantment.EFFICIENCY);
            ALIAS_ENCHANTMENTS.put("ds", Enchantment.EFFICIENCY);

            // Unbreaking
            ENCHANTMENTS.put("durability", Enchantment.UNBREAKING);
            ENCHANTMENTS.put("unbreaking", Enchantment.UNBREAKING);
            ALIAS_ENCHANTMENTS.put("dura", Enchantment.UNBREAKING);
            ALIAS_ENCHANTMENTS.put("d", Enchantment.UNBREAKING);

            // Thorns
            ENCHANTMENTS.put("thorns", Enchantment.THORNS);
            ALIAS_ENCHANTMENTS.put("thorn", Enchantment.THORNS);
            ALIAS_ENCHANTMENTS.put("t", Enchantment.THORNS);

            // Fire Aspect
            ENCHANTMENTS.put("fireaspect", Enchantment.FIRE_ASPECT);
            ALIAS_ENCHANTMENTS.put("fa", Enchantment.FIRE_ASPECT);
            ALIAS_ENCHANTMENTS.put("fire", Enchantment.FIRE_ASPECT);

            // Knockback
            ENCHANTMENTS.put("knockback", Enchantment.KNOCKBACK);
            ALIAS_ENCHANTMENTS.put("kback", Enchantment.KNOCKBACK);
            ALIAS_ENCHANTMENTS.put("kb", Enchantment.KNOCKBACK);

            // Protection
            ENCHANTMENTS.put("protection", Enchantment.PROTECTION);
            ALIAS_ENCHANTMENTS.put("prot", Enchantment.PROTECTION);

            // Respiration
            ENCHANTMENTS.put("respiration", Enchantment.RESPIRATION);
            ALIAS_ENCHANTMENTS.put("oxygen", Enchantment.RESPIRATION);
            ALIAS_ENCHANTMENTS.put("o", Enchantment.RESPIRATION);

            // Feather Falling
            ENCHANTMENTS.put("featherfalling", Enchantment.FEATHER_FALLING);
            ALIAS_ENCHANTMENTS.put("pfa", Enchantment.FEATHER_FALLING);

            // Silk Touch
            ENCHANTMENTS.put("silktouch", Enchantment.SILK_TOUCH);
            ALIAS_ENCHANTMENTS.put("st", Enchantment.SILK_TOUCH);

            // Aqua Affinity
            ENCHANTMENTS.put("aquaaffinity", Enchantment.AQUA_AFFINITY);
            ALIAS_ENCHANTMENTS.put("waterworker", Enchantment.AQUA_AFFINITY);

            // Infinity
            ENCHANTMENTS.put("infinity", Enchantment.INFINITY);
            ALIAS_ENCHANTMENTS.put("infinite", Enchantment.INFINITY);
            ALIAS_ENCHANTMENTS.put("ai", Enchantment.INFINITY);

            // Mending
            ENCHANTMENTS.put("mending", Enchantment.MENDING);

            // Frost Walker
            ENCHANTMENTS.put("frostwalker", Enchantment.FROST_WALKER);
            ALIAS_ENCHANTMENTS.put("frost", Enchantment.FROST_WALKER);

            // Curse of Binding
            ENCHANTMENTS.put("bindingcurse", Enchantment.BINDING_CURSE);
            ALIAS_ENCHANTMENTS.put("bindcurse", Enchantment.BINDING_CURSE);
            ALIAS_ENCHANTMENTS.put("bind", Enchantment.BINDING_CURSE);

            // Curse of Vanishing
            ENCHANTMENTS.put("vanishingcurse", Enchantment.VANISHING_CURSE);
            ALIAS_ENCHANTMENTS.put("vanishcurse", Enchantment.VANISHING_CURSE);

            // Sweeping Edge
            ENCHANTMENTS.put("sweepingedge", Enchantment.SWEEPING_EDGE);
            ALIAS_ENCHANTMENTS.put("se", Enchantment.SWEEPING_EDGE);

            // Looting
            ENCHANTMENTS.put("looting", Enchantment.LOOTING);

            // Fortune
            ENCHANTMENTS.put("fortune", Enchantment.FORTUNE);

            // PROJECTILE_PROTECTION
            ENCHANTMENTS.put("projectileprotection", Enchantment.PROJECTILE_PROTECTION);
            ALIAS_ENCHANTMENTS.put("projprot", Enchantment.PROJECTILE_PROTECTION);

            // Blast Protection
            ENCHANTMENTS.put("blastprotection", Enchantment.BLAST_PROTECTION);
            ALIAS_ENCHANTMENTS.put("blastprot", Enchantment.BLAST_PROTECTION);

            // Fire Protection
            ENCHANTMENTS.put("fireprotection", Enchantment.FIRE_PROTECTION);
            ALIAS_ENCHANTMENTS.put("fireprot", Enchantment.FIRE_PROTECTION);

            // Loyalty
            ENCHANTMENTS.put("loyalty", Enchantment.LOYALTY);
            ALIAS_ENCHANTMENTS.put("return", Enchantment.LOYALTY);

            // Impaling
            ENCHANTMENTS.put("impaling", Enchantment.IMPALING);

            // Riptide
            ENCHANTMENTS.put("riptide", Enchantment.RIPTIDE);

            // Channeling
            ENCHANTMENTS.put("channeling", Enchantment.CHANNELING);

            // Lure
            ENCHANTMENTS.put("lure", Enchantment.LURE);

            // Luck of the Sea
            ENCHANTMENTS.put("luckofthesea", Enchantment.LUCK_OF_THE_SEA);
            ALIAS_ENCHANTMENTS.put("luck", Enchantment.LUCK_OF_THE_SEA);
        }

        /**
         * Retrieve an Enchantment by name or alias.
         *
         * @param name The name or alias of the enchantment.
         * @return Enchantment if found, null otherwise.
         */
        public static Enchantment getByName(String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }

            String normalized = name.toLowerCase(Locale.ENGLISH);

            // Check primary enchantments
            Enchantment enchantment = ENCHANTMENTS.get(normalized);

            // Check alias enchantments
            if (enchantment == null) {
                enchantment = ALIAS_ENCHANTMENTS.get(normalized);
            }

            return enchantment;
        }

        public static Set<Map.Entry<String, Enchantment>> entrySet() {
            return ENCHANTMENTS.entrySet();
        }

        public static Set<String> aliases() {
            return ALIAS_ENCHANTMENTS.keySet();
        }
    }
}
