package com.winthier.massstorage;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Material;

@Table(name = "items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"owner", "material"}))
@Getter @Setter @NoArgsConstructor @ToString
public final class SQLItem {
    @Id Integer id;
    @Column(nullable = false) UUID owner;
    @Column(nullable = false, length = 64) String material;
    @Column(nullable = false) Integer amount;
    transient Material mat;

    public static SQLItem of(UUID owner, Material mat) {
        SQLItem result = new SQLItem();
        result.setOwner(owner);
        result.setMaterial(mat.name().toLowerCase());
        result.setAmount(0);
        result.mat = mat;
        return result;
    }

    public boolean prep(MassStoragePlugin plugin) {
        mat = plugin.materialOf(material.toUpperCase());
        return mat != null;
    }

    public static List<SQLItem> find(MassStoragePlugin plugin, UUID uuid) {
        return plugin.getDb().find(SQLItem.class)
            .eq("owner", uuid).findList().stream()
            .filter(row -> row.prep(plugin))
            .collect(Collectors.toList());
    }
}
