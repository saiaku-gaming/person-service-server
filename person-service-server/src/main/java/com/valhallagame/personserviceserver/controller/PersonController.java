package com.valhallagame.personserviceserver.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.valhallagame.common.JS;
import com.valhallagame.personserviceserver.message.UsernameParameter;
import com.valhallagame.personserviceserver.message.UsernamePasswordParameter;
import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.model.Session;
import com.valhallagame.personserviceserver.service.PersonService;
import com.valhallagame.personserviceserver.service.SessionService;

@Controller
@RequestMapping(path = "/v1/person")
public class PersonController {

	@Autowired
	private PersonService personService;

	@Autowired
	private SessionService sessionService;

	@RequestMapping(path = "/get-person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> getPerson(@RequestBody UsernameParameter username) {
		Optional<Person> optPerson = personService.getPerson(username.getUsername());
		if (!optPerson.isPresent()) {
			return JS.message(HttpStatus.NOT_FOUND, "No person with that username was found!");
		}

		return JS.message(HttpStatus.OK, optPerson.get());
	}

	@RequestMapping(path = "/signup", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> signup(@RequestBody UsernamePasswordParameter input) {

		if (input == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty input.");
		} else if (input.getUsername() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty username input.");
		} else if (input.getPassword() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty password input.");
		}

		Optional<Person> dbUserOpt = personService.getPerson(input.getUsername());
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
	public ResponseEntity<?> login(@RequestBody UsernamePasswordParameter input) {
		if (input == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty input.");
		} else if (input.getUsername() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty username input.");
		} else if (input.getUsername() == null) {
			return JS.message(HttpStatus.BAD_REQUEST, "Empty password input.");
		}

		Optional<Person> personOpt = personService.getPerson(input.getUsername());

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
			// TODO add notification here
			// notificationService.notifyPartyAndFriends(person, "Online");
			return JS.message(HttpStatus.OK, session);
		} else {
			return JS.message(HttpStatus.FORBIDDEN, "User not found or wrong password.");
		}
	}

	// @RequestMapping(path = "/check-login", method = RequestMethod.POST)
	// @ResponseBody
	// public ResponseEntity<?> login() {
	// return JS.message(OK, "User is logged in.");
	// }
}
