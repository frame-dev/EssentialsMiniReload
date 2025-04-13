package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.NameTagChanger;
import ch.framedev.essentialsmini.utils.SkinChanger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

        String newName = args[0];
        String skinName = args[1];
        boolean isNicked = nickList.contains(player.getName());
        SkinChanger changer = new SkinChanger();

        if (!isNicked) {
            // Apply nickname and skin
            changer.changeSkin(player, skinName);
            player.setDisplayName(newName);
            player.setPlayerListName(newName); // tab list name
            nickList.add(player.getName());
            Map<String, String> skin = NameTagChanger.getSkin(skinName);
            fileConfiguration.set("nick." + player.getName() + ".name", player.getName());
            player.sendMessage(getPrefix() + "You are now nicked as §e" + newName);
            NameTagChanger.changeNameAndSkin(player, newName, skin.get("value"), skin.get("signature"));
        } else {
            // Revert nickname and skin
            changer.changeSkin(player, player.getName());
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            nickList.remove(player.getName());
            Map<String, String> skin = NameTagChanger.getSkin(player.getName());
            NameTagChanger.changeNameAndSkin(player, player.getName(), skin.get("value"), skin.get("signature"));

            fileConfiguration.set("nick." + player.getName(), null);
            player.sendMessage(getPrefix() + "Your nickname was §cremoved§7.");
        }

        fileConfiguration.set("nicks", nickList);
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            player.sendMessage(getPrefix() + "§cFailed to save nick configuration.");
            e.printStackTrace();
        }

        return true;
    }
}