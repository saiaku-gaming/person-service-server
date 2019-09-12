package com.valhallagame.personserviceserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.common.rabbitmq.RabbitSender;
import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.repository.PersonRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class PersonService {

	private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

	private ConcurrentMap<String, String> singletonPersons = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Instant> debugPersons = new ConcurrentHashMap<>();

	@Autowired
	private RabbitSender rabbitSender;

	@Autowired
	private PersonRepository personRepository;

	public Person savePerson(Person person) {
		logger.info("Saving person {}", person);
		return personRepository.save(person);
	}

	public void deletePerson(Person person) {
		logger.info("Deleting person {}", person);
		debugPersons.remove(person.getUsername());
		personRepository.delete(person);
	}

	public void deletePerson(String username) {
		logger.info("Deleting person with username {}", username);
		personRepository.deleteByUsername(username);
	}

	public Optional<Person> getPerson(String username) {
		logger.info("Getting person with username {}", username);
		return personRepository.findByUsername(username);
	}

	public Person createNewDebugPerson(String singleton) {
		logger.info("Creating new debug person singleton {} ", singleton);
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
		} else {
			person = personOpt.get();
		}

		debugPersons.put(person.getUsername(), Instant.now());

		return person;
	}

	private String getRandomName() throws IOException {
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder().url("https://randomuser.me/api/?nat=US").get().build();

		Response response = client.newCall(request).execute();

        if(response.body() == null) {
        	return getBackupName();
		}

        String jsonBody = response.body().string();

		ObjectMapper mapper = new ObjectMapper();

		JsonNode node = mapper.readTree(jsonBody);
		ArrayNode results = (ArrayNode) node.get("results");
		ObjectNode user = (ObjectNode) results.get(0);
		ObjectNode name = (ObjectNode) user.get("name");
		ValueNode first = (ValueNode) name.get("first");
		return first.textValue();
	}

	private String getBackupName() {
		String[] names = {
				"Juliann",
				"Lino",
				"Gale",
				"Betsy",
				"Lovella",
				"Anton",
				"Ilana",
				"Artie",
				"Janell",
				"Marita",
				"Aleen",
				"Quiana",
				"Kandra",
				"Clemencia",
				"Ana",
				"Von",
				"Mirella",
				"Albert",
				"Josefina",
				"Vita",
				"Gordon",
				"Bree",
				"Donnette",
				"Andrea",
				"Sydney",
				"Cari",
				"Hisako",
				"Terrance",
				"Annetta",
				"Jocelyn"
		};

		Random random = new Random();

		return names[random.nextInt(names.length)];
	}

	public List<Person> getOnlinePersons() {
		logger.info("Getting online persons");
		return personRepository.findByOnline(true);
	}

	public void deleteOldDebugPersons() {
		logger.info("Deleting old debug persons");
		List<String> keysToRemove = new ArrayList<>();
		for(Map.Entry<String, Instant> entry : debugPersons.entrySet()) {
			if(singletonPersons.values().stream().noneMatch(du -> du.toLowerCase().equals(entry.getKey())) &&
					entry.getValue().plus(1, ChronoUnit.HOURS).isBefore(Instant.now())) {
				deletePerson(entry.getKey());
				keysToRemove.add(entry.getKey());

				rabbitSender.sendMessage(RabbitMQRouting.Exchange.PERSON, RabbitMQRouting.Person.DELETE.name(),
						new NotificationMessage(entry.getKey(), "deleted person"));
			}
		}

		for(String key : keysToRemove) {
			debugPersons.remove(key);
		}
	}

    public void markPersonsOffline() {
        getOnlinePersons()
                .stream()
                .filter(person -> person.getLastHeartbeat().isBefore(Instant.now().minusSeconds(120)))
                .forEach(this::setPersonOffline);
    }

    public void setPersonOffline(Person person) {
        if (person.isOnline()) {
            person.setOnline(false);
            person = savePerson(person);
            rabbitSender.sendMessage(
                    RabbitMQRouting.Exchange.PERSON,
                    RabbitMQRouting.Person.OFFLINE.name(),
                    new NotificationMessage(person.getUsername(), "Offline")
            );
        }
    }

    public void setPersonOnline(Person person) {
        if (!person.isOnline()) {
            person.setOnline(true);
            person = savePerson(person);
            rabbitSender.sendMessage(
                    RabbitMQRouting.Exchange.PERSON,
                    RabbitMQRouting.Person.ONLINE.name(),
                    new NotificationMessage(person.getUsername(), "Online")
            );
        }
    }
}
