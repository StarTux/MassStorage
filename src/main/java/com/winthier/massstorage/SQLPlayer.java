package com.winthier.massstorage;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Table(name = "players")
@Getter @Setter @NoArgsConstructor @ToString
public final class SQLPlayer {
    @Id Integer id;
    Date version;
    UUID uuid;
    String name;
    Integer capacity;
}
