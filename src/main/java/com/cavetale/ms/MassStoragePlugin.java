package com.cavetale.ms;

import com.cavetale.ms.util.Gui;
import com.winthier.sql.SQLDatabase;
import org.bukkit.plugin.java.JavaPlugin;

public final class MassStoragePlugin extends JavaPlugin {
    protected static MassStoragePlugin instance;
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
        command.enable();
        insertCommand.enable();
        adminCommand.enable();
        StorableCategory.initialize(this);
        Gui.enable(this);
        sessions.enable();
    }
}
