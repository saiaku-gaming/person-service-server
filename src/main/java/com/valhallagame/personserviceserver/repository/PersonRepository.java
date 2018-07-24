package com.valhallagame.personserviceserver.repository;

import com.valhallagame.personserviceserver.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {
	public Optional<Person> findByUsername(String username);

	public List<Person> findByOnline(boolean online);

	public void deleteByUsername(String username);
}
