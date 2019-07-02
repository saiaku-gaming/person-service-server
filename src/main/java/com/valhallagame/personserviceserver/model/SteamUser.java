package com.valhallagame.personserviceserver.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
@Table(name = "steam_user")
public class SteamUser {
    @Id
    @SequenceGenerator(name = "steam_user_steam_user_id_seq", sequenceName = "steam_user_steam_user_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "steam_user_steam_user_id_seq")
    @Column(name = "steam_user_id", updatable = false)
    private Integer id;

    @Column(name = "person_id")
    private int personId;

    @Column(name = "steam_id")
    private String steamId;

    public SteamUser(int personId, String steamId) {
        this.personId = personId;
        this.steamId = steamId;
    }
}
