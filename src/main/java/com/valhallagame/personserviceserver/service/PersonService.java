package com.valhallagame.personserviceserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.repository.PersonRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class PersonService {

	private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

	private ConcurrentMap<String, String> singletonPersons = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Instant> debugPersons = new ConcurrentHashMap<>();

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private PersonRepository personRepository;

	public Person savePerson(Person person) {
		return personRepository.save(person);
	}

	public void deletePerson(Person person) {
		debugPersons.remove(person.getUsername());
		personRepository.delete(person);
	}

	public void deletePerson(String username) {
		personRepository.deleteByUsername(username);
	}

	public Optional<Person> getPerson(String username) {
		return personRepository.findByUsername(username);
	}

	public Person createNewDebugPerson(String singleton) {
		String displayUsername = singleton != null ? singletonPersons.get(singleton) : null;

		if(displayUsername == null) {
			displayUsername = "I am broken. Please fix!";
			try {
                displayUsername = "debug-" + getRandomName();
			} catch (IOException e) {
				logger.error("Could not get a random name", e);
			}

			displayUsername = displayUsername.chars()
					.mapToObj(c -> String.valueOf((char) c))
                    .map(c -> Math.random() < 0.1 ? c.toUpperCase() : c.toLowerCase())
					.collect(Collectors.joining());

			if(singleton != null) {
				singletonPersons.put(singleton, displayUsername);
			}
		}

		String sha1HexPass = DigestUtils.sha1Hex("debug").toUpperCase();
		Optional<Person> personOpt = getPerson(displayUsername.toLowerCase());
		Person person;
		if (!personOpt.isPresent()) {
			person = new Person(displayUsername, sha1HexPass);
			person.setOnline(true);
			personRepository.save(person);
			debugPersons.put(person.getUsername(), Instant.now());
		} else {
			person = personOpt.get();
		}
		return person;
	}

	private String getRandomName() throws IOException {
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder().url("https://randomuser.me/api/?nat=US").get().build();

		Response response = client.newCall(request).execute();

		String jsonBody = response.body().string();

		ObjectMapper mapper = new ObjectMapper();

		JsonNode node = mapper.readTree(jsonBody);
		ArrayNode results = (ArrayNode) node.get("results");
		ObjectNode user = (ObjectNode) results.get(0);
		ObjectNode name = (ObjectNode) user.get("name");
		ValueNode first = (ValueNode) name.get("first");
		return first.textValue();
	}

	public List<Person> getOnlinePersons() {
		return personRepository.findByOnline(true);
	}

	public void deleteOldDebugPersons() {
		for(Map.Entry<String, Instant> entry : debugPersons.entrySet()) {
			if(singletonPersons.values().stream().noneMatch(du -> du.toLowerCase().equals(entry.getKey())) &&
					entry.getValue().plus(1, ChronoUnit.HOURS).isBefore(Instant.now())) {
				deletePerson(entry.getKey());

				rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.PERSON.name(), RabbitMQRouting.Person.DELETE.name(),
						new NotificationMessage(entry.getKey(), "deleted person"));
			}
		}
	}
}
