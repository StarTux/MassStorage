package com.cavetale.ms;

import com.cavetale.ms.util.Gui;
import com.winthier.sql.SQLDatabase;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class MassStoragePlugin extends JavaPlugin {
    protected static MassStoragePlugin instance;
    protected final StorableItemIndex index = new StorableItemIndex();
    protected final MassStorageSessions sessions = new MassStorageSessions(this);
    protected final MassStorageCommand command = new MassStorageCommand(this);
    protected final MassStorageInsertCommand insertCommand = new MassStorageInsertCommand(this);
    protected final SQLDatabase database = new SQLDatabase(this);

    @Override
    public void onEnable() {
        instance = this;
        database.registerTable(SQLMassStorage.class);
        if (!database.createAllTables()) {
            throw new IllegalStateException("Database creation failed!");
        }
        index.populate();
        command.enable();
        insertCommand.enable();
        StorableCategory.initialize(this);
        Gui.enable(this);
        sessions.enable();
    }

    public boolean insert(Player player, ItemStack itemStack, Consumer<Boolean> callback) {
        if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() < 1) return false;
        StorableItem storable = index.get(itemStack);
        if (!storable.isValid()) return false;
        return sessions.apply(player, session -> {
                session.insertAsync(storable, itemStack.getAmount(), callback);
            });
    }

    public void find(UUID uuid, Consumer<List<SQLMassStorage>> callback) {
        database.find(SQLMassStorage.class)
            .eq("owner", uuid)
            .findListAsync(callback);
    }

    public List<SQLMassStorage> find(UUID uuid) {
        return database.find(SQLMassStorage.class)
            .eq("owner", uuid)
            .findList();
    }
}
