package ch.framedev.essentialsmini.managers;

import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.NotFoundException;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 03.07.2020 20:00
 */
@SuppressWarnings("unused")
public class LocationManager {

    private String name;
    private final File fileBackup = new File(Main.getInstance().getDataFolder(), "locationsBackup.yml");
    private final File fileBackupJson = new File(Main.getInstance().getDataFolder(), "locationsBackupJson.json");
    private final File file = new File(Main.getInstance().getDataFolder(), "locations.yml");
    private final FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
    private final FileConfiguration cfgBackup = YamlConfiguration.loadConfiguration(fileBackup);

    private final Main instance = Main.getInstance();

    public LocationManager(String name) {
        this.name = name;
    }

    public LocationManager() {
    }

    /**
     * Save Locations Config
     */
    public void saveCfg() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    /**
     * @return returns the Locations Config
     */
    public FileConfiguration getCfg() {
        return cfg;
    }

    public boolean exists(String name) {
        if (name == null || !cfg.contains(name)) {
            return false;
        }

        Object value = cfg.get(name);
        if (value instanceof String stringValue) {
            return !stringValue.isBlank();
        }
        return value != null;
    }

    /**
     * @param name     the Location Name
     * @param location the Location to save
     */
    public void setLocation(String name, Location location) {
        World world = location.getWorld();
        if (world == null) return;
        cfg.set(name + ".world", world.getName());
        cfg.set(name + ".x", location.getX());
        cfg.set(name + ".y", location.getY());
        cfg.set(name + ".z", location.getZ());
        cfg.set(name + ".yaw", location.getYaw());
        cfg.set(name + ".pitch", location.getPitch());
        cfg.set(name + ".createdAt", System.currentTimeMillis());
        saveCfg();
    }

    /**
     * use the Name variable from Constructor
     *
     * @param location the Location to Save
     */
    public void setLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        cfg.set(name + ".world", world.getName());
        cfg.set(name + ".x", location.getX());
        cfg.set(name + ".y", location.getY());
        cfg.set(name + ".z", location.getZ());
        cfg.set(name + ".yaw", location.getYaw());
        cfg.set(name + ".pitch", location.getPitch());
        cfg.set(name + ".createdAt", System.currentTimeMillis());
        saveCfg();
    }

    public String getCreatedAt(String name) {
        if (cfg.contains(name + ".createdAt")) {
            Date date = new Date(cfg.getLong(name + ".createdAt"));
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
            return simpleDateFormat.format(date);
        }
        return "";
    }

    /**
     * remove the Json Location
     *
     * @param name the Location Name
     */
    public void removeLocation(String name) {
        if (cfg.contains(name)) {
            cfg.set(name, " ");
            saveCfg();
        }
    }

    /**
     * @param s the Location Name
     * @return returns the Location from the File
     * @throws NullPointerException throw a NullPointerException if the Location was not found
     */
    public Location getLocation(String s) throws NotFoundException {
        return readLocation(s);
    }

    public void setWarp(String warpName, Location location) {
        setLocation("warps." + warpName, location);
        saveCfg();
    }

    public void setWarp(String warpName, Location location, double cost) {
        setWarp(warpName, location);
        cfg.set("warps." + warpName + ".cost", cost);
        saveCfg();
    }

    public boolean costWarp(String warpName) {
        return cfg.contains("warps." + warpName + ".cost");
    }

    public double getWarpCost(String warpName) {
        if (cfg.contains("warps." + warpName + ".cost"))
            return cfg.getDouble("warps." + warpName + ".cost");
        return 0.0;
    }

    public Location getWarp(String warpName) {
        if (cfg.contains("warps." + warpName)) {
            return getLocation("warps." + warpName);
        }
        return null;
    }

    public FileConfiguration getCfgBackup() {
        return cfgBackup;
    }

    public File getFileBackup() {
        return fileBackup;
    }

    public File getFile() {
        return file;
    }

    /**
     * uses the Variable Name from the Constructor
     *
     * @return return the Location from the file
     */
    public Location getLocation() {
        return readLocation(name);
    }

    /**
     * throw NullPointerException if the World is null
     *
     * @param location the Location to convert to an string
     * @return the Location converted to String
     */
    public static String locationToString(Location location) {
        String s = "";
        if (location == null || location.getWorld() == null) {
            return null;
        }
        s += location.getWorld().getName() + ";";
        s += location.getX() + ";";
        s += location.getY() + ";";
        s += location.getZ() + ";";
        s += location.getYaw() + ";";
        s += location.getPitch();
        /* World;10;63;12;160;-24 */
        return s;
    }

    /**
     * @param string the converted StringLocation
     * @return returns a completed Location from the String
     */
    public static Location locationFromString(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        String[] s = string.split(";");
        if (s.length < 6) {
            return null;
        }
        World world = Bukkit.getWorld(s[0]);
        if (world == null) {
            return null;
        }
        try {
            return new Location(world, Double.parseDouble(s[1]), Double.parseDouble(s[2]), Double.parseDouble(s[3]), Float.parseFloat(s[4]), Float.parseFloat(s[5]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public List<Location> getWarps() {
        ConfigurationSection cs = cfg.getConfigurationSection("warps");
        List<Location> warps = new ArrayList<>();
        if (cs == null) return warps;
        for (String s : cs.getKeys(false)) {
            Location location = getLocation("warps." + s);
            if (location != null) {
                warps.add(location);
            }
        }
        return warps;
    }

    public List<String> getWarpNames() {
        ConfigurationSection cs = cfg.getConfigurationSection("warps");
        List<String> warps = new ArrayList<>();
        if (cs == null) return warps;
        warps.addAll(cs.getKeys(false));
        return warps;
    }

    /**
     * Saves the Backups
     */
    public void saveBackupCfg() {
        try {
            cfgBackup.save(fileBackup);
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    private final HashMap<String, String> backups = new HashMap<>();

    public void saveBackup() {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String ss = offlinePlayer.getName();
            if (ss != null) {
                ConfigurationSection cs = getCfg().getConfigurationSection(ss + ".home");
                if (cs != null) {
                    for (String s : cs.getKeys(false)) {
                        if (getCfg().get(ss + ".home." + s) != null && !getCfg().get(ss + ".home." + s).equals(" ")) {
                            Location location = getLocation(ss + ".home." + s);
                            String serializedLocation = locationToString(location);
                            if (serializedLocation != null) {
                                cfgBackup.set(ss + ".home." + s, serializedLocation);
                                backups.put(ss + ".home." + s, serializedLocation);
                            }
                        }
                    }
                }
            }
        }
        saveBackupCfg();
        // saveBackUps();
    }

    /**
     * Deletes the Position Locations every Reload or restart
     */
    public void deleteLocations() {
        ConfigurationSection cs = new LocationManager().getCfg().getConfigurationSection("position");
        if (cs != null) {
            for (String s : cs.getKeys(false)) {
                if (s != null) {
                    getCfg().set("position." + s, " ");
                    try {
                        getCfg().save(getFile());
                    } catch (IOException e) {
                        Main.getInstance().getLogger4J().error(e);
                    }
                }
            }
        }
    }

    public void saveBackUps() {
        try {
            FileWriter fileWriter = new FileWriter(fileBackupJson);
            fileWriter.write(new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(backups));
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Main.getInstance().getLogger4J().error(e);
        }
    }

    private Location readLocation(String path) {
        if (path == null || !cfg.contains(path)) {
            return null;
        }

        String worldName = cfg.getString(path + ".world");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
