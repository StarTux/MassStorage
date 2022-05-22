package com.cavetale.ms.sql;

import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data @Table(name = "players")
public final class SQLPlayer implements SQLRow {
    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false)
    private boolean auto;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private Date created;

    public SQLPlayer() { }

    public SQLPlayer(final UUID uuid) {
        this.uuid = uuid;
        this.created = new Date();
    }
}
