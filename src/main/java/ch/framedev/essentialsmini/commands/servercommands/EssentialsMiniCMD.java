package ch.framedev.essentialsmini.commands.servercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EssentialsMiniCMD extends CommandBase {

    public EssentialsMiniCMD(Main plugin) {
        super(plugin, "essentialsmini");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§aEssentialsMini §7by FrameDev");
            sender.sendMessage("§7Version: §a" + getPlugin().getDescription().getVersion());
            sender.sendMessage("§7Website: §ahttps://github.com/FrameDev/EssentialsMini");
            sender.sendMessage("§7Commands:");
            sender.sendMessage("§a/essentialsmini reload §7- Reload the EssentialsMini configuration");
            sender.sendMessage("§a/essentialsmini help §7- Show this help message");
            sender.sendMessage("§a/essentialsmini economy <true|false> §7- Enable or disable the economy feature");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("essentialsmini.reload")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            getPlugin().reloadConfig();
            sender.sendMessage("§aEssentialsMini configuration reloaded.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("economy")) {
            if (!sender.hasPermission("essentialsmini.economy")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            if (!args[1].equalsIgnoreCase("true") && !args[1].equalsIgnoreCase("false")) {
                sender.sendMessage("§cUsage: /essentialsmini economy <true|false>");
                return true;
            }
            boolean activate = args[1].equalsIgnoreCase("true");
            getPlugin().getConfig().set("Economy.Activate", activate);
            getPlugin().saveConfig();
            sender.sendMessage("§aEconomy feature has been set to §e" + args[1].toLowerCase() + "§a.");
            if (activate) {
                sender.sendMessage("§aEconomy feature is now enabled. Please ensure you have an economy plugin installed.");
            } else {
                sender.sendMessage("§aEconomy feature is now disabled.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            if (!sender.hasPermission("essentialsmini.help")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            sender.sendMessage("§aEssentialsMini Help:");
            sender.sendMessage("§7Use §a/essentialsmini §7to see this help message.");
            sender.sendMessage("§7Use §a/essentialsmini reload §7to reload the configuration.");
            sender.sendMessage("§7Use §a/essentialsmini economy <true|false> §7to enable or disable the economy feature.");
            return true;
        }

        sender.sendMessage("§cUnknown command. Use §a/essentialsmini help §cfor a list of commands.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> sub = Arrays.asList("reload", "help", "economy");
            for (String s : sub) {
                if (s.startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("economy")) {
            if ("true".startsWith(args[1].toLowerCase())) completions.add("true");
            if ("false".startsWith(args[1].toLowerCase())) completions.add("false");
        }
        return completions;
    }
}