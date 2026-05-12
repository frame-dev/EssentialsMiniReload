package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.KitManager;
import ch.framedev.essentialsmini.utils.Cooldown;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import ch.framedev.essentialsmini.utils.TabCompleteUtils;
import ch.framedev.essentialsmini.utils.TextUtils;
import ch.framedev.essentialsmini.utils.Variables;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class KitCMD extends CommandBase {

    private static final String KITS = "kits";
    private static final String CREATE_KIT = "createkit";
    private static final String KITS_USAGE = "§6/kits <kitname>";
    private static final String CREATE_KIT_USAGE = "§6/createkit <KitName> [cooldown] [cost]";
    private static final String KIT_ROOT = "Items";
    private static final String NOT_ENOUGH_MONEY_KEY = Variables.MONEY_MESSAGE + ".MSG.NotEnough";
    private static final String DEFAULT_NOT_ENOUGH_MONEY = "§cYou don't have enough money §6:%Money%";

    private final Main plugin;
    public final Map<UUID, Map<String, Cooldown>> cooldowns = new HashMap<>();

    public KitCMD(Main plugin) {
        super(plugin, KITS, CREATE_KIT);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        return switch (commandName) {
            case KITS -> handleKits(player, args);
            case CREATE_KIT -> handleCreateKit(player, args);
            default -> false;
        };
    }

    private boolean handleKits(Player player, String[] args) {
        if (args.length != 1) {
            sendWrongArgs(player, KITS_USAGE);
            return true;
        }

        String kitName = args[0];
        if (!hasPermission(player, plugin.getPermissionBase() + "kits." + kitName)) return true;

        KitManager kitManager = new KitManager();
        if (!kitExists(kitName)) {
            send(player, "§cDieses Kit existiert nicht!");
            return true;
        }

        KitOptions options = new KitOptions(
                Math.max(0, kitManager.getCost(kitName)),
                Math.max(0, kitManager.getCooldown(kitName))
        );

        if (isOnCooldown(player, kitName)) return true;
        if (!chargeKitCost(player, options.cost())) return true;

        if (options.cooldownSeconds() > 0) {
            setCooldown(player, kitName, options.cooldownSeconds());
        }
        kitManager.loadKits(kitName, player);
        return true;
    }

    private boolean handleCreateKit(Player player, String[] args) {
        if (!hasPermission(player, plugin.getPermissionBase() + "createkit")) return true;

        if (args.length < 1 || args.length > 3) {
            sendWrongArgs(player, CREATE_KIT_USAGE);
            return true;
        }

        KitOptions options = parseCreateKitOptions(player, args);
        if (options == null) return true;

        ItemStack[] contents = player.getInventory().getContents();
        KitManager kitManager = new KitManager();
        if (options.cost() > 0) {
            kitManager.createKit(args[0], contents, options.cooldownSeconds(), options.cost());
        } else if (options.cooldownSeconds() > 0) {
            kitManager.createKit(args[0], contents, options.cooldownSeconds());
        } else {
            kitManager.createKit(args[0], contents);
        }

        send(player, "§aKit Created §6" + args[0]);
        player.getInventory().clear();
        return true;
    }

    private KitOptions parseCreateKitOptions(Player player, String[] args) {
        int cooldown = 0;
        int cost = 0;

        if (args.length >= 2) {
            Integer parsedCooldown = parseNonNegativeInt(args[1]);
            if (parsedCooldown == null) {
                sendWrongArgs(player, CREATE_KIT_USAGE);
                return null;
            }
            cooldown = parsedCooldown;
        }

        if (args.length == 3) {
            Integer parsedCost = parseNonNegativeInt(args[2]);
            if (parsedCost == null) {
                sendWrongArgs(player, CREATE_KIT_USAGE);
                return null;
            }
            cost = parsedCost;
        }

        return new KitOptions(cost, cooldown);
    }

    private boolean chargeKitCost(Player player, int cost) {
        if (cost <= 0) return true;

        if (plugin.getVaultManager() == null || plugin.getVaultManager().getEconomy() == null) {
            send(player, "§cEconomy not enabled!");
            return false;
        }

        if (!plugin.getVaultManager().getEconomy().has(player, cost)) {
            sendNotEnoughMoney(player);
            return false;
        }

        plugin.getVaultManager().getEconomy().withdrawPlayer(player, cost);
        return true;
    }

    private boolean isOnCooldown(Player player, String kitName) {
        Map<String, Cooldown> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Cooldown cooldown = playerCooldowns.get(kitName.toLowerCase(Locale.ROOT));
        if (cooldown == null || cooldown.check()) {
            if (cooldown != null) {
                playerCooldowns.remove(kitName.toLowerCase(Locale.ROOT));
            }
            return false;
        }

        send(player, "§cYou can't use that command for another " + formatDuration(cooldown.getSecondsLeft()) + "!");
        return true;
    }

    private void setCooldown(Player player, String kitName, int seconds) {
        cooldowns
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(kitName.toLowerCase(Locale.ROOT), new Cooldown(seconds, System.currentTimeMillis()));
    }

    private String formatDuration(long totalSeconds) {
        long safeSeconds = Math.max(0, totalSeconds);
        return String.format(Locale.ROOT, "%02d:%02d", safeSeconds / 60, safeSeconds % 60);
    }

    private boolean kitExists(String kitName) {
        FileConfiguration config = KitManager.getCustomConfig();
        return config != null && config.contains(KIT_ROOT + "." + kitName);
    }

    private Integer parseNonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void sendNotEnoughMoney(Player player) {
        String message = plugin.getLanguageConfig(player).getString(NOT_ENOUGH_MONEY_KEY, DEFAULT_NOT_ENOUGH_MONEY);
        if (message == null) message = DEFAULT_NOT_ENOUGH_MONEY;

        double balance = plugin.getVaultManager() == null || plugin.getVaultManager().getEco() == null
                ? 0D
                : plugin.getVaultManager().getEco().getBalance(player);
        message = new TextUtils().replaceAndWithParagraph(message);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Money%", balance + plugin.getCurrencySymbol());
        send(player, message);
    }

    private boolean hasPermission(Player player, String permission) {
        if (player.hasPermission(permission)) return true;

        send(player, plugin.getNoPerms(player));
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;

        sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer(null));
        return null;
    }

    private void sendWrongArgs(CommandSender sender, String usage) {
        send(sender, plugin.getWrongArgs(sender instanceof Player player ? player : null, usage));
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(plugin.getPrefix() + message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals(KITS) && args.length == 1) {
            return matchingKits(sender, args[0]);
        }

        if (commandName.equals(CREATE_KIT) && args.length == 1) {
            return Collections.singletonList("Kit_Name");
        }

        return Collections.emptyList();
    }

    private List<String> matchingKits(CommandSender sender, String prefix) {
        FileConfiguration config = KitManager.getCustomConfig();
        if (config == null) return Collections.emptyList();

        ConfigurationSection section = config.getConfigurationSection(KIT_ROOT);
        if (section == null) return Collections.emptyList();

        List<String> visibleKits = new ArrayList<>();
        for (String kitName : section.getKeys(false)) {
            if (sender.hasPermission(plugin.getPermissionBase() + "kits." + kitName)) {
                visibleKits.add(kitName);
            }
        }
        return TabCompleteUtils.matchingStrings(visibleKits, prefix);
    }

    private record KitOptions(int cost, int cooldownSeconds) {
    }
}
