package com.winthier.massstorage.vault;

import com.winthier.massstorage.Item;
import com.winthier.massstorage.MassStoragePlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHandler {
    Economy economy = null;

    public Economy getEconomy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) economy = economyProvider.getProvider();
        }
        return economy;
    }

    // Economy

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

    public List<Item> findItems(String searchTerm) {
        List<Item> result = new ArrayList<>();
        ItemInfo info = Items.itemByName(searchTerm);
        if (info != null) {
            Item item = Item.of(info.toStack());
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public String getItemName(Item item) {
        return Items.itemByStack(item.toItemStack()).getName();
    }
}
