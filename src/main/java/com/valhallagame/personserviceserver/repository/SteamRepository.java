package com.valhallagame.personserviceserver.repository;

import com.valhallagame.personserviceserver.model.SteamUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.Optional;

public interface SteamRepository extends JpaRepository<SteamUser, Integer> {
    Optional<SteamUser> findBySteamId(String steamId);

    @Modifying
    @Transactional
    void deleteByPersonId(int personId);
}
