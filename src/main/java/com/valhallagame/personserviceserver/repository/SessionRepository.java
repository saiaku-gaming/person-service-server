package com.valhallagame.personserviceserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.model.Session;

public interface SessionRepository extends JpaRepository<Session, String> {
	Optional<Session> findById(String id);

	Optional<Session> findByPerson(Person person);
}
