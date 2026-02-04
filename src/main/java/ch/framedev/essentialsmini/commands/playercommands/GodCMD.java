package ch.framedev.essentialsmini.commands.playercommands;


/*
 * de.framedev.essentialsmini.commands
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 20.09.2020 18:26
 */

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GodCMD extends CommandBase {

    public GodCMD(Main plugin) {
        super(plugin, "god");
        setupTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer());
                return true;
            }
            if (!player.hasPermission(getPlugin().getPermissionBase() + "god")) {
                player.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                return true;
            }
            toggleGod(player, sender, false);
            return true;
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                if (!sender.hasPermission(getPlugin().getPermissionBase() + "god.others")) {
                    sender.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms());
                    return true;
                }
                toggleGod(target, sender, true);
            } else {
                sender.sendMessage(getPlugin().getPrefix() + getPlugin().getVariables().getPlayerNameNotOnline(args[0]));
            }
            return true;
        } else {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/god"));
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs("/god <SpielerName>"));
            return true;
        }
    }

    private void toggleGod(Player target, CommandSender sender, boolean notifySender) {
        boolean enabling = !target.isInvulnerable();
        if (enabling) {
            target.setInvulnerable(true);
            if (!Main.getSilent().contains(sender.getName())) {
                String msg = getPlugin().getLanguageConfig(target).getString("God.Self.Activated");
                if (msg == null) msg = "§aYou are now invulnerable!";
                if (msg.contains("&")) msg = msg.replace('&', '§');
                target.sendMessage(getPlugin().getPrefix() + ReplaceCharConfig.replaceParagraph(msg));
            }
            if (notifySender) {
                String other = getPlugin().getLanguageConfig(sender).getString("God.Other.Activated");
                if (other == null) other = "§aEnabled godmode for %Player%";
                other = other.replace("%Player%", target.getName());
                if (other.contains("&")) other = other.replace('&', '§');
                sender.sendMessage(getPlugin().getPrefix() + ReplaceCharConfig.replaceParagraph(other));
            }
        } else {
            target.setInvulnerable(false);
            if (!Main.getSilent().contains(sender.getName())) {
                String msg = getPlugin().getLanguageConfig(target).getString("God.Self.Deactivated");
                if (msg == null) msg = "§cYou are no longer invulnerable!";
                if (msg.contains("&")) msg = msg.replace('&', '§');
                target.sendMessage(getPlugin().getPrefix() + ReplaceCharConfig.replaceParagraph(msg));
            }
            if (notifySender) {
                String other = getPlugin().getLanguageConfig(sender).getString("God.Other.Deactivated");
                if (other == null) other = "§cDisabled godmode for %Player%";
                other = other.replace("%Player%", target.getName());
                if (other.contains("&")) other = other.replace('&', '§');
                sender.sendMessage(getPlugin().getPrefix() + ReplaceCharConfig.replaceParagraph(other));
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) names.add(p.getName());
            }
            Collections.sort(names);
            return names;
        }
        return super.onTabComplete(sender, command, label, args);
    }
}
