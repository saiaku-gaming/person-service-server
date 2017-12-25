package com.valhallagame.personserviceserver.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.valhallagame.common.JS;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.personserviceclient.message.TokenParameter;
import com.valhallagame.personserviceclient.message.UsernameParameter;
import com.valhallagame.personserviceclient.message.UsernamePasswordParameter;
import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.model.Session;
import com.valhallagame.personserviceserver.service.PersonService;
import com.valhallagame.personserviceserver.service.SessionService;

@Controller
@RequestMapping(path = "/v1/person")
public class PersonController {

	private static final String NOT_FOUND = "Unable to find a user with that username/password combination";

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private PersonService personService;

	@Autowired
	private SessionService sessionService;

	@RequestMapping(path = "/online-persons", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> onlinePersons() {
		List<Person> persons = personService.getOnlinePersons();
		return JS.message(HttpStatus.OK, persons);
	}
	
	@RequestMapping(path = "/get-person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getPerson(@Valid @RequestBody UsernameParameter username) {
		Optional<Person> optPerson = personService.getPerson(username.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "No person with that username was found!");
		}

		return JS.message(HttpStatus.OK, optPerson.get());
	}

	@RequestMapping(path = "/signup", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> signup(@Valid @RequestBody UsernamePasswordParameter input) {
		if (input == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty input.");
		} else if (input.getUsername() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty username input.");
		} else if (input.getPassword() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty password input.");
		} else if (input.getUsername().contains("#")) {
			return JS.message(HttpStatus.BAD_REQUEST, "# is not allowed in username");
		}

		Optional<Person> dbUserOpt = personService.getPerson(input.getUsername().toLowerCase());
		if (dbUserOpt.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "Username already taken.");
		} else {
			if (input.getPassword().isEmpty()) {
				return JS.message(HttpStatus.BAD_REQUEST, "Empty password.");
			}
			Person user = new Person(input.getUsername(), input.getPassword());
			personService.savePerson(user);
			UUID randomUUID = UUID.randomUUID();
			Session session = new Session(randomUUID.toString(), user);
			sessionService.saveSession(session);

			return JS.message(HttpStatus.OK, session);
		}
	}

	@RequestMapping(path = "/login", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> login(@Valid @RequestBody UsernamePasswordParameter input) {
		if (input == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty input.");
		} else if (input.getUsername() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty username input.");
		} else if (input.getPassword() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty password input.");
		}

		Optional<Person> personOpt = personService.getPerson(input.getUsername().toLowerCase());

		if (!personOpt.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username: " + input.getUsername());
		}

		Person person = personOpt.get();

		if (person.validatePassword(input.getPassword())) {
			Optional<Session> sessionOpt = sessionService.getSessionFromPerson(person);
			// Log out the old one. This is used so only one can be active at
			// the same time.
			sessionOpt.ifPresent(session -> sessionService.deleteSession(session));

			UUID randomUUID = UUID.randomUUID();
			Session session = new Session(randomUUID.toString(), person);
			sessionService.saveSession(session);

			person.setDisplayUsername(input.getUsername());
			person.setOnline(true);
			personService.savePerson(person);
			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.PERSON.name(), RabbitMQRouting.Person.ONLINE.name(),
					new NotificationMessage(person.getUsername(), "Online"));
			return JS.message(HttpStatus.OK, session);
		} else {
			return JS.message(HttpStatus.FORBIDDEN, "User not found or wrong password.");
		}
	}

	@RequestMapping(path = "/logout", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> logout(@Valid @RequestBody UsernameParameter input) {
		Person user = personService.getPerson(input.getUsername()).orElse(null);
		if (user == null) {
			return JS.message(HttpStatus.NOT_FOUND, "Unable to find a user with username: " + input.getUsername());
		}

		user.setOnline(false);
		user = personService.savePerson(user);
		Optional<Session> optSession = sessionService.getSessionFromPerson(user);
		if (optSession.isPresent()) {
			sessionService.deleteSession(optSession.get());
		}

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.PERSON.name(), RabbitMQRouting.Person.OFFLINE.name(),
				new NotificationMessage(input.getUsername(), "Offline"));
		return JS.message(HttpStatus.OK, "Logged out");
	}

	@RequestMapping(path = "/check-login", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> checkLogin(@Valid @RequestBody UsernameParameter input) {
		Person user = personService.getPerson(input.getUsername()).orElse(null);
		if (user == null) {
			return JS.message(HttpStatus.NOT_FOUND, "Unable to find a user with username: " + input.getUsername());
		}
		Optional<Session> optSession = sessionService.getSessionFromPerson(user);
		if (optSession.isPresent()) {
			return JS.message(HttpStatus.OK, "User is logged in.");
		} else {
			return JS.message(HttpStatus.NOT_FOUND, "User is not logged in.");
		}
	}

	@RequestMapping(path = "/username-available", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> userAvaliable(@Valid @RequestBody UsernameParameter input) {
		if (input.getUsername().length() > 30) {
			return JS.message(HttpStatus.CONFLICT, "Username too long.");
		}
		Optional<Person> userOpt = personService.getPerson(input.getUsername());
		if (userOpt.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "Username is not available.");
		} else {
			return JS.message(HttpStatus.OK, "Username is available.");
		}
	}

	@RequestMapping(path = "/get-session-from-token", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getSessionFromToken(@Valid @RequestBody TokenParameter input) {
		Optional<Session> optSession = sessionService.getSessionFromId(input.getToken());
		if (optSession.isPresent()) {
			return JS.message(HttpStatus.OK, optSession.get());
		} else {
			return JS.message(HttpStatus.NOT_FOUND, "No session with that token was found!");
		}
	}

	@RequestMapping(path = "/validate-credentials", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> validateCredentials(@Valid @RequestBody UsernamePasswordParameter input) {
		Optional<Person> optPerson = personService.getPerson(input.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, NOT_FOUND);
		}
		if (optPerson.get().validatePassword(input.getPassword())) {
			return JS.message(HttpStatus.OK, "credentials valid");
		} else {
			return JS.message(HttpStatus.NOT_FOUND, NOT_FOUND);
		}
	}

	@RequestMapping(path = "/delete-person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> deletePerson(@Valid @RequestBody UsernameParameter input) {
		Optional<Person> optPerson = personService.getPerson(input.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, NOT_FOUND);
		}

		personService.deletePerson(optPerson.get());
		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.PERSON.name(), RabbitMQRouting.Person.DELETE.name(),
				new NotificationMessage(input.getUsername(), "deleted person"));

		return JS.message(HttpStatus.OK, "credentials valid");
	}

	@RequestMapping(path = "/create-debug-person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> createDebugPerson(@Valid @RequestBody TokenParameter input) {
		Person debugPerson = personService.createNewDebugPerson();

		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.PERSON.name(), RabbitMQRouting.Person.CREATE.name(),
				new NotificationMessage(debugPerson.getUsername(), "created debug person"));

		Session debugSession = sessionService.saveSession(new Session(input.getToken(), Instant.now(), debugPerson));

		return JS.message(HttpStatus.OK, debugSession);
	}

	@RequestMapping(path = "/heartbeat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> heartbeat(@Valid @RequestBody UsernameParameter input) {
		Optional<Person> optPerson = personService.getPerson(input.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, NOT_FOUND);
		}

		Person person = optPerson.get();
		person.setLastHeartbeat(Instant.now());
		personService.savePerson(person);

		return JS.message(HttpStatus.OK, "heartbeaten");
	}
}
