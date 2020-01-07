package com.winthier.massstorage;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
class Category {
    final String name;
    final ItemStack icon;
    final boolean misc;
    final Set<Material> materials;
}
