package com.valhallagame.personserviceserver.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.valhallagame.personserviceserver.model.Person;
import com.valhallagame.personserviceserver.repository.PersonRepository;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class PersonService {
	@Autowired
	private PersonRepository personRepository;

	public Person savePerson(Person person) {
		return personRepository.save(person);
	}

	public void deletePerson(Person person) {
		personRepository.delete(person);
	}

	public Optional<Person> getPerson(String username) {
		return personRepository.findByUsername(username);
	}

	public Person createNewDebugPerson() {
		String name = "I am broken. Please fix!";
		try {
			name = "debug-" + getRandomName();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String sha1HexPass = DigestUtils.sha1Hex("debug").toUpperCase();
		Optional<Person> personOpt = getPerson(name);
		Person person;
		if (!personOpt.isPresent()) {
			person = new Person(name, sha1HexPass);
			personRepository.save(person);
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
}
