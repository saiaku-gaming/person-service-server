package com.valhallagame.personserviceserver.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.model.Session;
import com.valhallagame.personserviceserver.repository.SessionRepository;

@Service
public class SessionService {

	@Autowired
	private SessionRepository sessionRepository;

	public Session saveSession(Session session) {
		return sessionRepository.save(session);
	}

	public void deleteSession(Session session) {
		sessionRepository.delete(session);
	}

	public Optional<Session> getSessionFromPerson(Person person) {
		return sessionRepository.findByPerson(person);
	}

}
