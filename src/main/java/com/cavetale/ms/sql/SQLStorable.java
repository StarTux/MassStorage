package com.cavetale.ms.sql;

import com.cavetale.ms.storable.StorableItem;
import com.cavetale.ms.storable.StorageType;
import com.cavetale.mytems.Mytems;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Material;

@Data @NotNull @Name("storage")
@SQLRow.UniqueKey({"owner", "type", "name"})
public final class SQLStorable implements SQLRow {
    @Id private Integer id;
    private UUID owner;
    private int type; // StorageType
    @VarChar(40) private String name;
    @Default("0") private int amount;
    @Default("0") private boolean auto;
    @Default("0") private int favorite;
    @Default("NOW()") private Date updated;

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
