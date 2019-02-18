package com.valhallagame.personserviceserver.service;

import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.model.Session;
import com.valhallagame.personserviceserver.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SessionService {
	private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

	@Autowired
	private SessionRepository sessionRepository;

	public Session saveSession(Session session) {
		logger.info("Saving session {}", session);
		return sessionRepository.save(session);
	}

	public void deleteSession(Session session) {
		logger.info("Deleting session {}", session);
		sessionRepository.delete(session);
	}

	public Optional<Session> getSessionFromId(String id) {
		logger.info("Getting session with id {}", id);
		return sessionRepository.findById(id);
	}

	public Optional<Session> getSessionFromPerson(Person person) {
		logger.info("Getting session for person {}", person);
		return sessionRepository.findByPerson(person);
	}

}
