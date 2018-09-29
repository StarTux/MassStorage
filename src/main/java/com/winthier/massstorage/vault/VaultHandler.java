package com.winthier.massstorage.vault;

import com.winthier.massstorage.Item;
import com.winthier.massstorage.MassStoragePlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHandler {
    private Economy economy = null;

    // Economy

    public Economy getEconomy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) economy = economyProvider.getProvider();
        }
        return economy;
    }

    public boolean hasMoney(Player player, double money) {
        if (money < 0.0) return false;
        return getEconomy().has(player, money);
    }

    public boolean takeMoney(Player player, double money) {
        if (money < 0.0) return false;
        EconomyResponse resp = getEconomy().withdrawPlayer(player, money);
        MassStoragePlugin.getInstance().getLogger().info(String.format("Took %s from %s: %s", formatMoney(money), player.getName(), resp.transactionSuccess()));
        return resp.transactionSuccess();
    }

    public String formatMoney(double money) {
        return getEconomy().format(money);
    }

    // Item

    public String getItemName(Item item) {
        return getItemName(item.toItemStack());
    }

    public String getItemName(ItemStack itemStack) {
        String name = itemStack.getType().name();
        String[] arr = name.split("_");
        if (arr.length == 0) return name;
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = arr[i].substring(0, 1) + arr[i].substring(1).toLowerCase();
        }
        StringBuilder sb = new StringBuilder(arr[0]);
        for (int i = 1; i < arr.length; ++i) {
            sb.append(" ").append(arr[i]);
        }
        return sb.toString();
    }
}
