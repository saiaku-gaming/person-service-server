package com.valhallagame.personserviceserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valhallagame.personserviceserver.model.Person;

public interface PersonRepository extends JpaRepository<Person, Integer> {
	public Optional<Person> findByUsername(String username);
}
