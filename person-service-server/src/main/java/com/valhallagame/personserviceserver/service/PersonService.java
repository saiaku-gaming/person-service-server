package com.valhallagame.personserviceserver.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.repository.PersonRepository;

@Service
public class PersonService {
	@Autowired
	private PersonRepository personRepository;

	public Optional<Person> getPerson(String username) {
		return personRepository.findByUsername(username);
	}
}
