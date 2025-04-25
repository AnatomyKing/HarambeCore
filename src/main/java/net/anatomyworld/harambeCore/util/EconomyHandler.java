package net.anatomyworld.harambeCore.util;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHandler {

    private static Economy economy;

    public static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getLogger().severe("[EconomyHandler] Vault plugin not found.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Bukkit.getLogger().severe("[EconomyHandler] No Vault-compatible economy detected.");
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static boolean withdrawBalance(Player player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.withdrawPlayer(player, amount);
        if (!r.transactionSuccess())
            Bukkit.getLogger().warning("[EconomyHandler] Withdraw failed: " + r.errorMessage);
        return r.transactionSuccess();
    }

    public static boolean depositBalance(Player player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.depositPlayer(player, amount);
        if (!r.transactionSuccess())
            Bukkit.getLogger().warning("[EconomyHandler] Deposit failed: " + r.errorMessage);
        return r.transactionSuccess();
    }

    public static boolean hasEnoughBalance(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public static double getBalance(Player player) {
        return economy == null ? 0.0 : economy.getBalance(player);
    }
}
