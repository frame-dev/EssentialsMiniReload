package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.SkinApplier;
import ch.framedev.essentialsmini.utils.SkinChanger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class NickCMD extends CommandBase {

    private final File file;
    private final FileConfiguration fileConfiguration;
    private final List<String> nickList;

    public NickCMD(Main plugin) {
        super(plugin, "nick");

        this.file = new File(plugin.getDataFolder(), "nicks.yml");
        this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
        this.nickList = fileConfiguration.getStringList("nicks");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPrefix() + getPlugin().getOnlyPlayer(null));
            return true;
        }

        if (!player.hasPermission(getPlugin().getPermissionBase() + "nick")) {
            player.sendMessage(getPrefix() + getPlugin().getNoPerms(player));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(getPrefix() + "Usage: /nick <nickname> <skin>");
            return true;
        }

        if(isPaperLike()) {
            player.sendMessage(getPrefix() + "§cThis command is not supported on Paper-like servers due to API limitations.");
            return true;
        }

        String skinName = args[0];
        boolean isNicked = nickList.contains(player.getName());

        if(!isNicked) {
            SkinChanger.fetchByUsername(getPlugin(), skinName).whenCompleteAsync((tex, err) -> {
                if (err != null) {
                    player.sendMessage("§cFailed: " + err.getMessage());
                    return;
                }
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    try {
                        getPlugin().getSkinService().apply(player, tex.value(), tex.signature());
                        player.sendMessage("§aSkin applied! If you don’t see it, re-log or wait a few seconds.");
                    } catch (Exception e) {
                        player.sendMessage("§cApply failed: " + e.getMessage());
                    }
                });
            });
            nickList.add(player.getName());
        } else {
            getPlugin().getSkinService().clear(player);
            player.sendMessage("§aYour skin has been reset to your original skin.");
            nickList.remove(player.getName());
        }

        fileConfiguration.set("nicks", nickList);
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            player.sendMessage(getPrefix() + "§cFailed to save nick configuration.");
            return true;
        }

        return true;
    }

    public boolean isPaperLike() {
        String serverType = Bukkit.getServer().getClass().getPackage().getName();
        return serverType.contains("paper") || serverType.contains("purpur") || serverType.contains("folia");
    }
}