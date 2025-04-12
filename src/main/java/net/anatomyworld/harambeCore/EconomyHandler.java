package net.anatomyworld.harambeCore;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHandler {

    private static Economy economy;

    public static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getLogger().severe("[EconomyHandler] Vault plugin not found. Economy cannot be initialized.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Bukkit.getLogger().severe("[EconomyHandler] No Vault-compatible economy plugin found.");
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    public static boolean hasEnoughBalance(Player player, double amount) {
        if (economy == null) {
            Bukkit.getLogger().warning("[EconomyHandler] Economy not initialized. Cannot check balance.");
            return false;
        }
        return economy.has(player, amount);
    }

    public static boolean withdrawBalance(Player player, double amount) {
        if (economy == null) {
            Bukkit.getLogger().warning("[EconomyHandler] Economy not initialized. Cannot withdraw balance.");
            return false;
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            Bukkit.getLogger().warning("[EconomyHandler] Failed to withdraw " + amount + " from " + player.getName() + ". Reason: " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    public static double getBalance(Player player) {
        if (economy == null) {
            Bukkit.getLogger().warning("[EconomyHandler] Economy not initialized. Returning balance as 0.");
            return 0.0;
        }
        return economy.getBalance(player);
    }

    public static Economy getEconomy() {
        return economy;
    }
}
