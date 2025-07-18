package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.managers.BanMuteManager;
import ch.framedev.essentialsmini.utils.DateUnit;
import ch.framedev.essentialsmini.utils.PlayerUtils;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.commands.playercommands
 * ClassName MuteCMD
 * Date: 15.05.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

public class MuteCMD extends CommandBase implements Listener {

    private final Main plugin;

    private final List<OfflinePlayer> muted;

    public static File file;
    public static FileConfiguration cfg;

    public MuteCMD(Main plugin) {
        super(plugin);
        setup("mute", this);
        setup("tempmute", this);
        setup("muteinfo", this);
        setup("removetempmute", this);
        setupTabCompleter("tempmute", this);
        this.plugin = plugin;
        plugin.getListeners().add(this);
        this.muted = plugin.getVariables().getMutedPlayers();
        file = new File(plugin.getDataFolder(), "tempMutes.yml");
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mute")) {
            if (args.length == 1) {
                if (!sender.hasPermission(plugin.getPermissionBase() + "mute")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }

                OfflinePlayer player = PlayerUtils.getOfflinePlayerByName(args[0]);
                if (muted.contains(player)) {
                    muted.remove(player);
                    if (player.isOnline()) {
                        String selfUnMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Deactivate");
                        selfUnMute = ReplaceCharConfig.replaceParagraph(selfUnMute);
                        ((Player) player).sendMessage(plugin.getPrefix() + selfUnMute);
                    }
                    String otherUnMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Deactivate");
                    otherUnMute = ReplaceCharConfig.replaceParagraph(otherUnMute);
                    otherUnMute = ReplaceCharConfig.replaceObjectWithData(otherUnMute, "%Player%", player.getName());
                    sender.sendMessage(plugin.getPrefix() + otherUnMute);
                } else {
                    muted.add(player);
                    if (player.isOnline()) {
                        String selfMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Activate");
                        selfMute = ReplaceCharConfig.replaceParagraph(selfMute);
                        ((Player) player).sendMessage(plugin.getPrefix() + selfMute);
                    }
                    String otherMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Activate");
                    otherMute = ReplaceCharConfig.replaceParagraph(otherMute);
                    otherMute = ReplaceCharConfig.replaceObjectWithData(otherMute, "%Player%", player.getName());
                    sender.sendMessage(plugin.getPrefix() + otherMute);
                }
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("tempmute")) {
            if (args.length == 5) {
                if (!sender.hasPermission(plugin.getPermissionBase() + "tempmute")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }
                if (args[0].equalsIgnoreCase("type")) {
                    MuteReason muteReason = MuteReason.valueOf(args[2].toUpperCase());
                    DateUnit unit = DateUnit.valueOf(args[4].toUpperCase());
                    long value = Long.parseLong(args[3]);
                    long current = System.currentTimeMillis();
                    long millis = value * unit.getToSec() * 1000;
                    long newValue = current + millis;
                    Date date = new Date(newValue);
                    OfflinePlayer player = PlayerUtils.getOfflinePlayerByName(args[1]);
                    if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
                        new BanMuteManager().setTempMute(player, muteReason, new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").format(date));
                        if (player.isOnline()) {
                            String selfMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Activate");
                            selfMute = ReplaceCharConfig.replaceParagraph(selfMute);
                            ((Player) player).sendMessage(plugin.getPrefix() + selfMute);
                        }
                        String otherMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Activate");
                        otherMute = ReplaceCharConfig.replaceParagraph(otherMute);
                        otherMute = ReplaceCharConfig.replaceObjectWithData(otherMute, "%Player%", player.getName());
                        sender.sendMessage(plugin.getPrefix() + otherMute);
                    } else {
                        cfg.set(player.getName() + ".reason", muteReason.getReason());
                        cfg.set(player.getName() + ".expire", date);
                        try {
                            cfg.save(file);
                        } catch (IOException e) {
                            plugin.getLogger4J().error(e);
                        }
                        if (player.isOnline()) {
                            String selfMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Activate");
                            selfMute = ReplaceCharConfig.replaceParagraph(selfMute);
                            ((Player) player).sendMessage(plugin.getPrefix() + selfMute);
                        }
                        String otherMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Activate");
                        otherMute = ReplaceCharConfig.replaceParagraph(otherMute);
                        otherMute = ReplaceCharConfig.replaceObjectWithData(otherMute, "%Player%", player.getName());
                        sender.sendMessage(plugin.getPrefix() + otherMute);
                    }
                }

                if (args[0].equalsIgnoreCase("own")) {
                    String muteReason = args[2];
                    DateUnit unit = DateUnit.valueOf(args[4].toUpperCase());
                    long value = Long.parseLong(args[3]);
                    long current = System.currentTimeMillis();
                    long millis = value * unit.getToSec() * 1000;
                    long newValue = current + millis;
                    Date date = new Date(newValue);
                    OfflinePlayer player = PlayerUtils.getOfflinePlayerByName(args[1]);
                    if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
                        new BanMuteManager().setTempMute(player, muteReason, new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").format(date));
                        if (player.isOnline()) {
                            String selfMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Activate");
                            selfMute = ReplaceCharConfig.replaceParagraph(selfMute);
                            ((Player) player).sendMessage(plugin.getPrefix() + selfMute);
                        }
                        String otherMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Activate");
                        otherMute = ReplaceCharConfig.replaceParagraph(otherMute);
                        otherMute = ReplaceCharConfig.replaceObjectWithData(otherMute, "%Player%", player.getName());
                        sender.sendMessage(plugin.getPrefix() + otherMute);
                    } else {
                        cfg.set(player.getName() + ".reason", muteReason);
                        cfg.set(player.getName() + ".expire", date);
                        try {
                            cfg.save(file);
                        } catch (IOException e) {
                            plugin.getLogger4J().error(e);
                        }
                        if (player.isOnline()) {
                            String selfMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Activate");
                            selfMute = ReplaceCharConfig.replaceParagraph(selfMute);
                            ((Player) player).sendMessage(plugin.getPrefix() + selfMute);
                        }
                        String otherMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Activate");
                        otherMute = ReplaceCharConfig.replaceParagraph(otherMute);
                        otherMute = ReplaceCharConfig.replaceObjectWithData(otherMute, "%Player%", player.getName());
                        sender.sendMessage(plugin.getPrefix() + otherMute);
                    }
                }

                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("removetempmute")) {
            if (args.length == 1) {
                if (!sender.hasPermission(plugin.getPermissionBase() + "tempmute")) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                    return true;
                }

                OfflinePlayer player = PlayerUtils.getOfflinePlayerByName(args[0]);
                if (player.getName() == null) {
                    System.out.println("Player Name is Null; MuteCMD.java:189");
                    return true;
                }
                if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
                    new BanMuteManager().removeTempMute(player);
                    if (player.isOnline()) {
                        String selfUnMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Deactivate");
                        selfUnMute = ReplaceCharConfig.replaceParagraph(selfUnMute);
                        ((Player) player).sendMessage(plugin.getPrefix() + selfUnMute);
                    }
                    String otherUnMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Deactivate");
                    otherUnMute = ReplaceCharConfig.replaceParagraph(otherUnMute);
                    otherUnMute = ReplaceCharConfig.replaceObjectWithData(otherUnMute, "%Player%", player.getName());
                    sender.sendMessage(plugin.getPrefix() + otherUnMute);
                } else {
                    if (cfg.contains(player.getName())) {
                        cfg.set(player.getName(), null);
                        try {
                            cfg.save(file);
                        } catch (IOException e) {
                            plugin.getLogger4J().error(e);
                        }
                        if (player.isOnline()) {
                            String selfUnMute = plugin.getLanguageConfig((Player) player).getString("Mute.Self.Deactivate");
                            selfUnMute = ReplaceCharConfig.replaceParagraph(selfUnMute);
                            ((Player) player).sendMessage(plugin.getPrefix() + selfUnMute);
                        }
                        String otherUnMute = plugin.getLanguageConfig(sender).getString("Mute.Other.Deactivate");
                        otherUnMute = ReplaceCharConfig.replaceParagraph(otherUnMute);
                        otherUnMute = ReplaceCharConfig.replaceObjectWithData(otherUnMute, "%Player%", player.getName());
                        sender.sendMessage(plugin.getPrefix() + otherUnMute);
                    }
                }
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("muteinfo")) {
            if (!sender.hasPermission(plugin.getPermissionBase() + "muteinfo")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getNoPerms());
                return true;
            }

            ArrayList<OfflinePlayer> players = new ArrayList<>();
            if (!getPlugin().isMysql() || !getPlugin().isSQL() || !getPlugin().isMongoDB()) {
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if(offlinePlayer == null) {
                        System.out.println("OfflinePlayer is Null; MuteCMD.java:238");
                        return true;
                    }
                    if (offlinePlayer.getName() == null) {
                        System.out.println("OfflinePlayer Name is Null; MuteCMD.java:240");
                        return true;
                    }
                    if (cfg.contains(offlinePlayer.getName())) {
                        players.add(offlinePlayer);
                    }
                }

                players.forEach(player -> {
                    sender.sendMessage("§6" + player.getName() + " §ais Muted while : §6" + cfg.getString(player.getName() + ".reason"));
                    sender.sendMessage("§aExpired at §6: " + cfg.getString(player.getName() + ".expire"));
                });
            } else {
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (new BanMuteManager().isTempMute(player))
                        players.add(player);
                }
                players.forEach(player -> {
                    new BanMuteManager().getTempMute(player).thenAccept(stringStringMap -> {
                        if(stringStringMap != null) {
                            sender.sendMessage("§6" + player.getName() + " §ais Muted while : §6" + stringStringMap.get(stringStringMap.keySet().iterator().next()));
                            try {
                                sender.sendMessage("§aExpired at §6: " + new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").parse(stringStringMap.values().iterator().next()));
                            } catch (ParseException e) {
                                plugin.getLogger4J().error(e);
                            }
                        }
                    });
                });
            }
        }
        return super.onCommand(sender, command, label, args);
    }

    public CompletableFuture<Boolean> isExpiredAsync(OfflinePlayer player) {
        if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
            BanMuteManager banMuteManager = new BanMuteManager();

            // Check if the player is temp-muted
            if (banMuteManager.isTempMute(player)) {
                return banMuteManager.getTempMute(player).thenApply(stringStringMap -> {
                    if (stringStringMap != null) {
                        String tempMute = stringStringMap.get("TempMute");
                        try {
                            Date muteDate = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").parse(tempMute);
                            return muteDate.getTime() < System.currentTimeMillis();
                        } catch (ParseException e) {
                            plugin.getLogger4J().error("Error parsing TempMute date for player: " + player.getName(), e);
                            return true; // Treat as expired if date parsing fails
                        }
                    }
                    return true; // Treat as expired if no TempMute data is found
                }).exceptionally(t -> {
                    plugin.getLogger4J().error("Error while checking TempMute expiration for player: " + player.getName(), t);
                    return true; // Treat as expired in case of an error
                });
            } else {
                return CompletableFuture.completedFuture(true); // Not temp-muted, treat as expired
            }
        } else {
            // Handle file-based configuration synchronously
            if (cfg.contains(player.getName() + ".reason")) {
                Date expireDate = (Date) cfg.get(player.getName() + ".expire");
                if (expireDate != null) {
                    return CompletableFuture.completedFuture(expireDate.getTime() < System.currentTimeMillis());
                }
            }
            return CompletableFuture.completedFuture(true); // Treat as expired if no data is found
        }
    }

    public boolean isExpired(OfflinePlayer player) {
        try {
            return isExpiredAsync(player).join();
        } catch (Exception e) {
            plugin.getLogger4J().error("Error while checking TempMute expiration for player: " + player.getName(), e);
            return true; // Treat as expired in case of an error
        }
    }


    @EventHandler
    public void onChatWrite(AsyncPlayerChatEvent event) {
        if (!isExpired(event.getPlayer())) {
            if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
                if (new BanMuteManager().isTempMute(event.getPlayer())) {
                    final Date[] date = {new Date()};
                    final String[] reason = {""};
                    new BanMuteManager().getTempMute(event.getPlayer()).thenAccept(stringStringMap -> {
                        if(stringStringMap != null) {
                            reason[0] = stringStringMap.get(stringStringMap.keySet().iterator().next());
                            try {
                                date[0] = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss").parse(stringStringMap.values().iterator().next());
                            } catch (ParseException e) {
                                plugin.getLogger4J().error(e);
                            }
                            event.getPlayer().sendMessage(plugin.getPrefix() + "§cYou are Muted! While §6" + reason[0] + " | §aExpired at : §6" + date[0].toString());
                        }
                    });
                }
            } else {
                Date date = (Date) cfg.get(event.getPlayer().getName() + ".expire");
                event.getPlayer().sendMessage(plugin.getPrefix() + "§cYou are Muted! While §6" + cfg.getString(event.getPlayer().getName() + ".reason") + " | §aExpired at : §6" + date.toString());
            }
            event.setCancelled(true);
        } else {
            Player player = event.getPlayer();
            if (getPlugin().isMysql() || getPlugin().isSQL() || getPlugin().isMongoDB()) {
                new BanMuteManager().removeTempMute(player);
            } else {
                if (cfg.contains(player.getName() + ".reason")) {
                    cfg.set(player.getName(), null);
                    try {
                        cfg.save(file);
                    } catch (IOException e) {
                        plugin.getLogger4J().error(e);
                    }
                }
            }
        }
        if (muted.contains(event.getPlayer())) {
            event.getPlayer().sendMessage(plugin.getPrefix() + "§cYou are Muted!");
            event.setCancelled(true);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            ArrayList<String> reason = new ArrayList<>();
            reason.add("own");
            reason.add("type");
            ArrayList<String> empty = new ArrayList<>();
            for (String s : reason) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase()))
                    empty.add(s);
            }
            Collections.sort(empty);
            return empty;
        }

        if (args.length == 2) {
            ArrayList<String> reason = new ArrayList<>();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                reason.add(offlinePlayer.getName());
            }
            ArrayList<String> empty = new ArrayList<>();
            for (String s : reason) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase()))
                    empty.add(s);
            }
            Collections.sort(empty);
            return empty;
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("type")) {
                ArrayList<String> reason = new ArrayList<>();
                Arrays.asList(MuteReason.values()).forEach(reasons -> reason.add(reasons.name()));
                ArrayList<String> empty = new ArrayList<>();
                for (String s : reason) {
                    if (s.toLowerCase().startsWith(args[2].toLowerCase()))
                        empty.add(s);
                }
                Collections.sort(empty);
                return empty;
            }
            if (args[0].equalsIgnoreCase("own")) {
                return new ArrayList<String>(Collections.singleton("your_Message"));
            }
        }
        if (args.length == 4) {
            return new ArrayList<String>(Collections.singletonList("Time"));
        }
        if (args.length == 5) {
            ArrayList<String> dateFormat = new ArrayList<>();
            Arrays.asList(DateUnit.values()).forEach(dateUnit -> dateFormat.add(dateUnit.name()));
            ArrayList<String> empty = new ArrayList<>();
            for (String s : dateFormat) {
                if (s.toLowerCase().startsWith(args[4].toLowerCase())) {
                    empty.add(s);
                }
            }
            Collections.sort(empty);
            return empty;
        }
        return super.onTabComplete(sender, command, label, args);
    }

    public static enum MuteReason {
        ADVERTISING("advertising"),
        CAPS("caps"),
        MILD_TOXICITY("mild toxicity"),
        TOXICITY("toxicity"),
        SPAMMING("spamming"),
        CURSING("cursing"),
        INSULTING("insulting"),
        NSFW("nfsw"),
        LEAKING_SENSITIVE_DATA("leaking sensitive Data"),
        VIOLATION_OF_THE_RULES("violation of the rules");

        private final String reason;

        MuteReason(String reason) {
            this.reason = reason;
        }

        public static MuteReason getMuteReason(String reason) {
            return valueOf(reason.toUpperCase());
        }

        public String getReason() {
            return reason;
        }
    }
}
