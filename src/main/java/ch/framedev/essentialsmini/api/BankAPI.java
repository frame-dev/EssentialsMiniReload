package ch.framedev.essentialsmini.api;

import ch.framedev.essentialsmini.main.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Not in use!
 * / This Plugin was Created by FrameDev
 * / Package: de.framedev.essentialsmini.api
 * / ClassName BankAPI
 * / Date: 27.06.21
 * / Project: EssentialsMini
 * / Copyrighted by FrameDev
 */
@SuppressWarnings("unused")
public class BankAPI {

    File file = new File(Main.getInstance().getDataFolder() + "/money", "bankAccounts.yml");
    FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

    private static BankAPI instance;
    private static Main mainInstance;

    public BankAPI() {
        instance = this;
        mainInstance = Main.getInstance();
    }

    public static BankAPI getInstance() {
        return instance;
    }

    private void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            mainInstance.getLogger4J().error(e.getMessage(), e);
        }
    }

    public boolean createBankAccount(String player, String bankName) {
        return cfg.contains("Banks." + player + "." + bankName);
    }

    public double getBankBalance(String player, String bankName) {
        if (hasBankAccount(player))
            return cfg.getDouble("Banks." + player + "." + bankName + ".balance");
        return 0d;
    }

    public boolean depositMoney(String player, String bankName, double amount) {
        if (!hasBankAccount(player))
            return false;
        double balance = getBankBalance(player, bankName);
        balance += amount;
        return setBankBalance(player, bankName, balance);
    }

    public boolean withdrawMoney(String player, String bankName, double amount) {
        if (!hasBankAccount(player))
            return false;
        double balance = getBankBalance(player, bankName);
        balance -= amount;
        return setBankBalance(player, bankName, balance);
    }

    public boolean setBankBalance(String player, String bankName, double money) {
        if (!hasBankAccount(player))
            return false;
        cfg.set("Banks." + player + "." + bankName + ".balance", money);
        save();
        return true;
    }

    public boolean hasBankAccount(String player) {
        return cfg.contains("Banks." + player) && cfg.get("banks." + player) != null;
    }

    public String getBankName(String player) {
        if (!hasBankAccount(player)) return null;
        return cfg.getString("Banks." + player);
    }
}
