package com.cavetale.ms.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("players")
public final class SQLPlayer implements SQLRow {
    @Id private Integer id;
    @Unique private UUID uuid;
    @Default("0") private boolean auto;
    @Default("0") private int sortOrder;
    @Default("NOW()") private Date created;

    public SQLPlayer() { }

    public SQLPlayer(final UUID uuid) {
        this.uuid = uuid;
        this.created = new Date();
    }
}
