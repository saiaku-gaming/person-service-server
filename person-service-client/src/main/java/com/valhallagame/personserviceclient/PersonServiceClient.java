package com.valhallagame.personserviceclient;

import java.util.Optional;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.valhallagame.personserviceclient.model.Person;
import com.valhallagame.personserviceclient.model.UsernameParameter;

public class PersonServiceClient {
	private static final String SERVER_URL = "http://localhost:1235";

	public static Optional<Person> getPerson(String username) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			return Optional.ofNullable(restTemplate.postForObject(SERVER_URL + "/v1/person/get-person",
					new UsernameParameter(username), Person.class));
		} catch (RestClientException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
