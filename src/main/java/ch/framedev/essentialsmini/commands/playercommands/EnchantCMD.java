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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.bukkit.Material.AIR;

public class EnchantCMD extends CommandBase {

    private final Main plugin;

    public EnchantCMD(Main plugin) {
        super(plugin, "enchant");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 2) {
            if (sender instanceof Player player) {
                if (player.hasPermission(plugin.getPermissionBase() + "enchant")) {
                    if (player.getInventory().getItemInMainHand().getType() != AIR) {
                        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
                        if(meta == null) {
                            player.sendMessage(plugin.getPrefix() + "§cThis Item can't be enchanted!");
                            return true;
                        }
                        if (args[0].equalsIgnoreCase("unbreakable")) {
                            if (args[1].equalsIgnoreCase("true")) {
                                meta.setUnbreakable(true);
                            } else if (args[1].equalsIgnoreCase("false")) {
                                meta.setUnbreakable(false);
                            }
                            player.getInventory().getItemInMainHand().setItemMeta(meta);
                        } else if (Enchantments.getByName(args[0]) != null) {
                            meta.addEnchant(Enchantments.getByName(args[0]), Integer.parseInt(args[1]), true);
                            player.getInventory().getItemInMainHand().setItemMeta(meta);
                        } else {
                            String message = plugin.getLanguageConfig(player).getString("EnchantNotExist");
                            if (message != null) {
                                message = new TextUtils().replaceAndWithParagraph(message);
                            }
                            sender.sendMessage(plugin.getPrefix() + message);
                        }
                    } else {
                        String noItemInHand = plugin.getLanguageConfig(player).getString("NoItemFoundInHand");
                        noItemInHand = ReplaceCharConfig.replaceParagraph(noItemInHand);
                        player.sendMessage(plugin.getPrefix() + noItemInHand);
                    }
                } else {
                    player.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer());
            }
        } else if (args.length == 3) {
            if (sender.hasPermission(plugin.getPermissionBase() + "enchant.others")) {
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    if (target.getInventory().getItemInMainHand().getType() != AIR) {
                        ItemMeta meta = target.getInventory().getItemInMainHand().getItemMeta();
                        if(meta == null) {
                            sender.sendMessage(plugin.getPrefix() + "§cThis Item can't be enchanted!");
                            return true;
                        }
                        if (args[0].equalsIgnoreCase("unbreakable")) {
                            if (args[1].equalsIgnoreCase("true")) {
                                meta.setUnbreakable(true);
                            } else if (args[1].equalsIgnoreCase("false")) {
                                meta.setUnbreakable(false);
                            }
                            target.getInventory().getItemInMainHand().setItemMeta(meta);
                        } else if (Enchantments.getByName(args[0]) != null) {
                            meta.addEnchant(Enchantments.getByName(args[0]), Integer.parseInt(args[1]), true);
                            target.getInventory().getItemInMainHand().setItemMeta(meta);
                        } else {
                            String message = plugin.getLanguageConfig(sender).getString("EnchantNotExist");
                            if (message != null) {
                                message = new TextUtils().replaceAndWithParagraph(message);
                            }
                            sender.sendMessage(plugin.getPrefix() + message);
                        }
                    } else {
                        String message = plugin.getLanguageConfig(sender).getString("NoItemFoundInHand");
                        if (message != null) {
                            message = new TextUtils().replaceAndWithParagraph(message);
                        }
                        sender.sendMessage(plugin.getPrefix() + message);
                    }
                } else {
                    String message = plugin.getVariables().getPlayerNameNotOnline(args[2]);
                    sender.sendMessage(plugin.getPrefix() + message);
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
            }
        } else {
            if (sender.hasPermission(plugin.getPermissionBase() + "enchant")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/enchant <Enchantment Name> <Level>"));
            }
            if (sender.hasPermission(plugin.getPermissionBase() + "enchant.others")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getWrongArgs("/enchant <Enchantment Name> <Level> <Player Name>"));
            }

        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission(plugin.getPermissionBase() + "enchant") || sender.hasPermission(plugin.getPermissionBase() + "enchant.others")) {
                ArrayList<String> empty = new ArrayList<>();
                for (Map.Entry<String, Enchantment> s : Enchantments.entrySet()) {
                    if (s.getKey().toLowerCase().startsWith(args[0])) {
                        empty.add(s.getKey());
                    }
                }
                Collections.sort(empty);
                return empty;
            }
        }
        return null;
    }

    public static class Enchantments {

        private static final Map<String, Enchantment> ENCHANTMENTS = new HashMap<>();
        private static final Map<String, Enchantment> ALIASENCHANTMENTS = new HashMap<>();

        static {

            // Depth Strider
            ENCHANTMENTS.put("depthstrider", Enchantment.DEPTH_STRIDER);
            ALIASENCHANTMENTS.put("dswim", Enchantment.DEPTH_STRIDER);

            // Soul Speed
            ENCHANTMENTS.put("soulspeed", Enchantment.SOUL_SPEED);
            ALIASENCHANTMENTS.put("ss", Enchantment.SOUL_SPEED);

            // Power
            ENCHANTMENTS.put("power", Enchantment.POWER);
            ALIASENCHANTMENTS.put("p", Enchantment.POWER);

            // Swift Sneak
            ENCHANTMENTS.put("swiftsneak", Enchantment.SWIFT_SNEAK);
            ALIASENCHANTMENTS.put("sneak", Enchantment.SWIFT_SNEAK);

            // Punch
            ENCHANTMENTS.put("punch", Enchantment.PUNCH);
            ALIASENCHANTMENTS.put("pu", Enchantment.PUNCH);

            // Flame
            ENCHANTMENTS.put("flame", Enchantment.FLAME);
            ALIASENCHANTMENTS.put("f", Enchantment.FLAME);

            // Damage-related Enchantments
            ENCHANTMENTS.put("alldamage", Enchantment.SHARPNESS);
            ALIASENCHANTMENTS.put("alldmg", Enchantment.SHARPNESS);
            ENCHANTMENTS.put("sharpness", Enchantment.SHARPNESS);
            ALIASENCHANTMENTS.put("sharp", Enchantment.SHARPNESS);
            ALIASENCHANTMENTS.put("dal", Enchantment.SHARPNESS);

            // Smite
            ENCHANTMENTS.put("undeaddamage", Enchantment.SMITE);
            ENCHANTMENTS.put("smite", Enchantment.SMITE);
            ALIASENCHANTMENTS.put("du", Enchantment.SMITE);

            // Bane of Arthropods
            ENCHANTMENTS.put("baneofarthropods", Enchantment.BANE_OF_ARTHROPODS);
            ALIASENCHANTMENTS.put("arthropod", Enchantment.BANE_OF_ARTHROPODS);
            ALIASENCHANTMENTS.put("dar", Enchantment.BANE_OF_ARTHROPODS);

            // Efficiency
            ENCHANTMENTS.put("digspeed", Enchantment.EFFICIENCY);
            ENCHANTMENTS.put("efficiency", Enchantment.EFFICIENCY);
            ALIASENCHANTMENTS.put("minespeed", Enchantment.EFFICIENCY);
            ALIASENCHANTMENTS.put("cutspeed", Enchantment.EFFICIENCY);
            ALIASENCHANTMENTS.put("ds", Enchantment.EFFICIENCY);

            // Unbreaking
            ENCHANTMENTS.put("durability", Enchantment.UNBREAKING);
            ENCHANTMENTS.put("unbreaking", Enchantment.UNBREAKING);
            ALIASENCHANTMENTS.put("dura", Enchantment.UNBREAKING);
            ALIASENCHANTMENTS.put("d", Enchantment.UNBREAKING);

            // Thorns
            ENCHANTMENTS.put("thorns", Enchantment.THORNS);
            ALIASENCHANTMENTS.put("thorn", Enchantment.THORNS);
            ALIASENCHANTMENTS.put("t", Enchantment.THORNS);

            // Fire Aspect
            ENCHANTMENTS.put("fireaspect", Enchantment.FIRE_ASPECT);
            ALIASENCHANTMENTS.put("fa", Enchantment.FIRE_ASPECT);
            ALIASENCHANTMENTS.put("fire", Enchantment.FIRE_ASPECT);

            // Knockback
            ENCHANTMENTS.put("knockback", Enchantment.KNOCKBACK);
            ALIASENCHANTMENTS.put("kback", Enchantment.KNOCKBACK);
            ALIASENCHANTMENTS.put("kb", Enchantment.KNOCKBACK);

            // Protection
            ENCHANTMENTS.put("protection", Enchantment.PROTECTION);
            ALIASENCHANTMENTS.put("prot", Enchantment.PROTECTION);

            // Respiration
            ENCHANTMENTS.put("respiration", Enchantment.RESPIRATION);
            ALIASENCHANTMENTS.put("oxygen", Enchantment.RESPIRATION);
            ALIASENCHANTMENTS.put("o", Enchantment.RESPIRATION);

            // Feather Falling
            ENCHANTMENTS.put("featherfalling", Enchantment.FEATHER_FALLING);
            ALIASENCHANTMENTS.put("pfa", Enchantment.FEATHER_FALLING);

            // Silk Touch
            ENCHANTMENTS.put("silktouch", Enchantment.SILK_TOUCH);
            ALIASENCHANTMENTS.put("st", Enchantment.SILK_TOUCH);

            // Aqua Affinity
            ENCHANTMENTS.put("aquaaffinity", Enchantment.AQUA_AFFINITY);
            ALIASENCHANTMENTS.put("waterworker", Enchantment.AQUA_AFFINITY);

            // Infinity
            ENCHANTMENTS.put("infinity", Enchantment.INFINITY);
            ALIASENCHANTMENTS.put("infinite", Enchantment.INFINITY);
            ALIASENCHANTMENTS.put("ai", Enchantment.INFINITY);

            // Mending
            ENCHANTMENTS.put("mending", Enchantment.MENDING);

            // Frost Walker
            ENCHANTMENTS.put("frostwalker", Enchantment.FROST_WALKER);
            ALIASENCHANTMENTS.put("frost", Enchantment.FROST_WALKER);

            // Curse of Binding
            ENCHANTMENTS.put("bindingcurse", Enchantment.BINDING_CURSE);
            ALIASENCHANTMENTS.put("bindcurse", Enchantment.BINDING_CURSE);
            ALIASENCHANTMENTS.put("bind", Enchantment.BINDING_CURSE);

            // Curse of Vanishing
            ENCHANTMENTS.put("vanishingcurse", Enchantment.VANISHING_CURSE);
            ALIASENCHANTMENTS.put("vanishcurse", Enchantment.VANISHING_CURSE);

            // Sweeping Edge
            ENCHANTMENTS.put("sweepingedge", Enchantment.SWEEPING_EDGE);
            ALIASENCHANTMENTS.put("se", Enchantment.SWEEPING_EDGE);

            // Looting
            ENCHANTMENTS.put("looting", Enchantment.LOOTING);

            // Fortune
            ENCHANTMENTS.put("fortune", Enchantment.FORTUNE);

            // PROJECTILE_PROTECTION
            ENCHANTMENTS.put("projectileprotection", Enchantment.PROJECTILE_PROTECTION);
            ALIASENCHANTMENTS.put("projprot", Enchantment.PROJECTILE_PROTECTION);

            // Blast Protection
            ENCHANTMENTS.put("blastprotection", Enchantment.BLAST_PROTECTION);
            ALIASENCHANTMENTS.put("blastprot", Enchantment.BLAST_PROTECTION);

            // Fire Protection
            ENCHANTMENTS.put("fireprotection", Enchantment.FIRE_PROTECTION);
            ALIASENCHANTMENTS.put("fireprot", Enchantment.FIRE_PROTECTION);

            // Loyalty
            ENCHANTMENTS.put("loyalty", Enchantment.LOYALTY);
            ALIASENCHANTMENTS.put("return", Enchantment.LOYALTY);

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
            ALIASENCHANTMENTS.put("luck", Enchantment.LUCK_OF_THE_SEA);
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
                enchantment = ALIASENCHANTMENTS.get(normalized);
            }

            return enchantment;
        }

        public static Set<Map.Entry<String, Enchantment>> entrySet() {
            return ENCHANTMENTS.entrySet();
        }
    }
}
