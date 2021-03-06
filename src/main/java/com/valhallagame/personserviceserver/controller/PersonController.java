package com.valhallagame.personserviceserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.valhallagame.common.JS;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.common.rabbitmq.RabbitSender;
import com.valhallagame.personserviceclient.message.*;
import com.valhallagame.personserviceserver.client.SteamClient;
import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.model.Session;
import com.valhallagame.personserviceserver.model.SteamUser;
import com.valhallagame.personserviceserver.service.PersonService;
import com.valhallagame.personserviceserver.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping(path = "/v1/person")
public class PersonController {
	private static final Logger logger = LoggerFactory.getLogger(PersonController.class);
	private static final String NOT_FOUND = "Unable to find a user with that username/password combination";

	@Autowired
	private RabbitSender rabbitSender;

	@Autowired
	private PersonService personService;

	@Autowired
	private SteamClient steamClient;

	@Autowired
	private SessionService sessionService;

	@RequestMapping(path = "/online-persons", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> onlinePersons() {
		logger.info("Online Persons called");
		List<Person> persons = personService.getOnlinePersons();
		return JS.message(HttpStatus.OK, persons);
	}

	@RequestMapping(path = "/get-person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getPerson(@Valid @RequestBody GetPersonParameter input) {
		logger.info("Get Persons called with {}", input);
		Optional<Person> optPerson = personService.getPerson(input.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "No person with that username was found!");
		}

		return JS.message(HttpStatus.OK, optPerson.get());
	}

    @RequestMapping(path = "/finished-tutorial", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<JsonNode> finishedTutorial(@Valid @RequestBody FinishedTutorialParameter input) {
        logger.info("Finished tutorial called with {}", input);
        Optional<Person> optPerson = personService.getPerson(input.getUsername());
        if (!optPerson.isPresent()) {
            return JS.message(HttpStatus.NOT_FOUND, "No person with that username was found!");
        }

        Person person = optPerson.get();
        person.setFinishedTutorial(true);
        personService.savePerson(person);

        return JS.message(HttpStatus.OK, optPerson.get());
    }

	@RequestMapping(path = "/signup", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> signup(@Valid @RequestBody SignupParameter input) {
		logger.info("Signup called for user {}", input.getDisplayUsername());
		if (input.getDisplayUsername().contains("#")) {
			return JS.message(HttpStatus.BAD_REQUEST, "# is not allowed in username");
		}

		Optional<Person> dbUserOpt = personService.getPerson(input.getDisplayUsername().toLowerCase());
		if (dbUserOpt.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "Username already taken.");
		} else {
			if (input.getPassword().isEmpty()) {
				return JS.message(HttpStatus.BAD_REQUEST, "Empty password.");
			}
            Person user = new Person(input.getDisplayUsername().toLowerCase(), input.getDisplayUsername(), input.getPassword());
			personService.savePerson(user);
			UUID randomUUID = UUID.randomUUID();
			Session session = new Session(randomUUID.toString(), user);
			sessionService.saveSession(session);

			return JS.message(HttpStatus.OK, session);
		}
	}

	@RequestMapping(path = "/steam-login", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> login(@Valid @RequestBody SteamLoginParameter input) throws IOException {
		logger.info("steam login called");
		Optional<String> steamIdOpt = steamClient.checkAuthSession(input.getAuthSessionTicket());
		if (!steamIdOpt.isPresent()) {
			return JS.message(HttpStatus.FORBIDDEN, "Steam authentication failure");
		}
		String steamId = steamIdOpt.get();
		Optional<Person> personOpt = personService.getPersonFromSteamId(steamId);

		if (personOpt.isPresent()) {
            Person person = personOpt.get();
            // lets always update the display username when login in.
            steamClient.getProfileName(steamId).ifPresent(steamProfileName -> {
                if (!person.getDisplayUsername().equals(steamProfileName)) {
                    personService.setDisplayUsername(person, steamProfileName);
                }
            });

			return createNewSession(personOpt.get());
		}

		logger.info("Creating new steam login");

		Optional<Person> dbUserOpt = personService.getPerson(steamId);
		if (dbUserOpt.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "Username already taken.");
		}

        Optional<String> steamProfileName = steamClient.getProfileName(steamId);
        String displayUsername = steamProfileName.orElse(steamId);

        Person user = new Person("steam:" + steamId, displayUsername, UUID.randomUUID().toString());
		user = personService.savePerson(user);
		SteamUser steamUser = new SteamUser(user.getId(), steamId);
		personService.saveSteamUser(steamUser);
		UUID randomUUID = UUID.randomUUID();
		Session session = new Session(randomUUID.toString(), user);
		sessionService.saveSession(session);
		return JS.message(HttpStatus.OK, session);
	}

    private void deleteSession(Person person) {
		Optional<Session> sessionOpt = sessionService.getSessionFromPerson(person);
		sessionOpt.ifPresent(session -> sessionService.deleteSession(session));
	}

	@RequestMapping(path = "/login", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> login(@Valid @RequestBody LoginParameter input) {
		logger.info("Login called for user {}", input.getDisplayUsername());
		Optional<Person> personOpt = personService.getPerson(input.getDisplayUsername().toLowerCase());

		if (!personOpt.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "Could not find person with username: " + input.getDisplayUsername());
		}

		Person person = personOpt.get();

		if (person.validatePassword(input.getPassword())) {
			person.setDisplayUsername(input.getDisplayUsername());
			return createNewSession(person);
		} else {
			return JS.message(HttpStatus.FORBIDDEN, "User not found or wrong password.");
		}
	}

	private ResponseEntity<JsonNode> createNewSession(Person person) {
        deleteSession(person); // ensure that we do not have multiple sessions
		Session session = new Session(UUID.randomUUID().toString(), person);
		sessionService.saveSession(session);
		personService.setPersonOnline(person);
		personService.savePerson(person);
		return JS.message(HttpStatus.OK, session);
	}

	@RequestMapping(path = "/logout", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> logout(@Valid @RequestBody LogoutParameter input) {
		logger.info("Logout called with {}", input);
		Person user = personService.getPerson(input.getUsername()).orElse(null);
		if (user == null) {
			return JS.message(HttpStatus.NOT_FOUND, "Unable to find a user with username: " + input.getUsername());
		}
		sessionService.getSessionFromPerson(user).ifPresent(session -> sessionService.deleteSession(session));
		personService.setPersonOffline(user);

		return JS.message(HttpStatus.OK, "Logged out");
	}

	@RequestMapping(path = "/check-login", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> checkLogin(@Valid @RequestBody CheckLoginParameter input) {
		logger.info("Check Login called with {}", input);
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
	public ResponseEntity<JsonNode> userAvaliable(@Valid @RequestBody UsernameAvailableParameter input) {
		logger.info("Username Available called with {}", input);
		if (input.getDisplayUsername().length() > 30) {
			return JS.message(HttpStatus.CONFLICT, "Username too long.");
		}
		Optional<Person> userOpt = personService.getPerson(input.getDisplayUsername().toLowerCase());
		if (userOpt.isPresent()) {
			return JS.message(HttpStatus.CONFLICT, "Username is not available.");
		} else {
			return JS.message(HttpStatus.OK, "Username is available.");
		}
	}

	@RequestMapping(path = "/get-session-from-token", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getSessionFromToken(@Valid @RequestBody GetSessionFromTokenParameter input) {
		logger.info("Get Session From Token called with {}", input);
		Optional<Session> optSession = sessionService.getSessionFromId(input.getToken());
		if (optSession.isPresent()) {
			return JS.message(HttpStatus.OK, optSession.get());
		} else {
			return JS.message(HttpStatus.NOT_FOUND, "No session with that token was found!");
		}
	}

	@RequestMapping(path = "/validate-credentials", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> validateCredentials(@Valid @RequestBody ValidateCredentialsParameter input) {
		logger.info("Validate Credentials called with {}", input);
		Optional<Person> optPerson = personService.getPerson(input.getDisplayUsername().toLowerCase());
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
	public ResponseEntity<JsonNode> deletePerson(@Valid @RequestBody DeletePersonParameter input) {
		logger.info("Delete Person called with {}", input);
		Optional<Person> optPerson = personService.getPerson(input.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, NOT_FOUND);
		}

		personService.deletePerson(optPerson.get());
		rabbitSender.sendMessage(RabbitMQRouting.Exchange.PERSON, RabbitMQRouting.Person.DELETE.name(),
				new NotificationMessage(input.getUsername(), "deleted person"));

		return JS.message(HttpStatus.OK, "credentials valid");
	}

	@RequestMapping(path = "/create-debug-person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> createDebugPerson(@Valid @RequestBody CreateDebugPersonParameter input) {
		logger.info("Create Debug Person called with {}", input);
		Person debugPerson = personService.createNewDebugPerson(input.getSingleton());

		rabbitSender.sendMessage(RabbitMQRouting.Exchange.PERSON, RabbitMQRouting.Person.CREATE.name(),
				new NotificationMessage(debugPerson.getUsername(), "created debug person"));

		sessionService.getSessionFromPerson(debugPerson).ifPresent(session -> sessionService.deleteSession(session));

		Session debugSession = sessionService.saveSession(new Session(input.getToken(), Instant.now(), debugPerson));

		personService.setPersonOnline(debugPerson);
		
		return JS.message(HttpStatus.OK, debugSession);
	}

	@RequestMapping(path = "/heartbeat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> heartbeat(@Valid @RequestBody HeartbeatParameter input) {
		logger.info("Heartbeat called with {}", input);
		Optional<Person> optPerson = personService.getPerson(input.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, NOT_FOUND);
		}

		Person person = optPerson.get();
		person.setLastHeartbeat(Instant.now());
		personService.savePerson(person);
		personService.setPersonOnline(person);

		return JS.message(HttpStatus.OK, "heartbeaten");
	}
}
