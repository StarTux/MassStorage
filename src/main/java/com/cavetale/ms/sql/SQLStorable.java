package com.cavetale.ms.sql;

import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorageType;
import com.cavetale.mytems.Mytems;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import org.bukkit.Material;

@Data
@Table(name = "storage",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {
                   "owner",
                   "type",
                   "name",
               }),
       })
public final class SQLStorable {
    @Id
    private Integer id;

    @Column(nullable = false)
    private UUID owner;

    @Column(nullable = false)
    private int type; // StorageType

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private boolean auto;

    @Column(nullable = false)
    private int favorite;

    @Column(nullable = false)
    private Date updated;

    public SQLStorable() { }

    public SQLStorable(final UUID uuid, final Material material) {
        this.owner = uuid;
        this.type = StorageType.BUKKIT.id;
        this.name = material.name().toLowerCase();
        this.updated = new Date();
    }

    public SQLStorable(final UUID uuid, final Mytems mytems) {
        this.owner = uuid;
        this.type = StorageType.MYTEMS.id;
        this.name = mytems.name().toLowerCase();
        this.updated = new Date();
    }

    public SQLStorable(final UUID uuid, final StorableItem storable) {
        this.owner = uuid;
        this.type = storable.getStorageType().id;
        this.name = storable.getSqlName();
        this.updated = new Date();
    }

    public StorageType getStorageType() {
        return StorageType.of(type);
    }
}
