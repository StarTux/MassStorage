package com.cavetale.ms;

import com.cavetale.ms.session.MassStorageSessions;
import com.cavetale.ms.sql.SQLMassStorage;
import com.cavetale.ms.storable.StorableCategory;
import com.cavetale.ms.storable.StorableItemIndex;
import com.cavetale.ms.util.Gui;
import com.winthier.sql.SQLDatabase;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class MassStoragePlugin extends JavaPlugin {
    @Getter protected static MassStoragePlugin instance;
    protected final StorableItemIndex index = new StorableItemIndex();
    protected final MassStorageSessions sessions = new MassStorageSessions(this);
    protected final MassStorageCommand command = new MassStorageCommand(this);
    protected final MassStorageInsertCommand insertCommand = new MassStorageInsertCommand(this);
    protected final MassStorageAdminCommand adminCommand = new MassStorageAdminCommand(this);
    protected final SQLDatabase database = new SQLDatabase(this);

    @Override
    public void onEnable() {
        instance = this;
        database.registerTable(SQLMassStorage.class);
        if (!database.createAllTables()) {
            throw new IllegalStateException("Database creation failed!");
        }
        index.populate();
        StorableCategory.initialize(this);
        Gui.enable(this);
        command.enable();
        insertCommand.enable();
        adminCommand.enable();
        sessions.enable();
    }
}
