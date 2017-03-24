package com.winthier.massstorage.sql;

import com.winthier.massstorage.Item;
import com.winthier.massstorage.MassStoragePlugin;
import com.winthier.massstorage.NamedItem;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"owner", "item_type", "item_data"}))
@Getter
@Setter
@NoArgsConstructor
public final class SQLItem {
    @Id private Integer id;
    @Version private Date version;
    @Column(nullable = false) private UUID owner;
    @Column(nullable = false) private Integer itemType;
    @Column(nullable = false) private Integer itemData;
    @Column(nullable = false) private Integer amount;

    public static SQLItem of(UUID owner, Item item) {
        SQLItem result = new SQLItem();
        result.setOwner(owner);
        result.setItemType(item.getType());
        result.setItemData(item.getData());
        result.setAmount(0);
        return result;
    }

    public static List<SQLItem> find(UUID uuid) {
        return MassStoragePlugin.getInstance().getDb().find(SQLItem.class).where().eq("owner", uuid).findList();
    }

    public Item getItem() {
        return new Item(getItemType(), getItemData());
    }

    public NamedItem getNamedItem() {
        return new NamedItem(getItemType(), getItemData(), getAmount());
    }
}
