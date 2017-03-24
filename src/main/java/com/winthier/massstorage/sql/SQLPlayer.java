package com.winthier.massstorage.sql;

import com.winthier.massstorage.MassStoragePlugin;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Entity
@Table(name = "players",
       uniqueConstraints = @UniqueConstraint(columnNames = {"uuid"}))
@Getter
@Setter
@NoArgsConstructor
public class SQLPlayer {
    @Id Integer id;
    @Version Date version;
    @Column(nullable = false) UUID uuid;
    String name;
    @Column(nullable = false) Integer capacity;

    public static SQLPlayer get(UUID uuid) {
        SQLPlayer result = MassStoragePlugin.getInstance().getDb().find(SQLPlayer.class).where().eq("uuid", uuid).findUnique();
        if (result == null) {
            result = new SQLPlayer();
            result.setUuid(uuid);
            result.setCapacity(MassStoragePlugin.getInstance().getConfig().getInt("DefaultCapacity", 0));
            MassStoragePlugin.getInstance().getDb().save(result);
        }
        return result;
    }

    public static SQLPlayer find(String name) {
        return MassStoragePlugin.getInstance().getDb().find(SQLPlayer.class).where().eq("name", name).findUnique();
    }
}
