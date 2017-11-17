package com.valhallagame.personserviceclient;

import java.util.Optional;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.valhallagame.personserviceclient.model.Person;
import com.valhallagame.personserviceclient.model.Session;
import com.valhallagame.personserviceclient.model.UsernameParameter;
import com.valhallagame.personserviceclient.model.UsernamePasswordParameter;

public class PersonServiceClient {
	private static PersonServiceClient personServiceClient;

	private String personServiceServerUrl = "http://localhost:1235";

	private PersonServiceClient() {
	}

	public static void init(String personServiceServerUrl) {
		PersonServiceClient client = get();
		client.personServiceServerUrl = personServiceServerUrl;
	}

	public static PersonServiceClient get() {
		if (personServiceClient == null) {
			personServiceClient = new PersonServiceClient();
		}

		return personServiceClient;
	}

	public Optional<Person> getPerson(String username) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			return Optional.ofNullable(restTemplate.postForObject(personServiceServerUrl + "/v1/person/get-person",
					new UsernameParameter(username), Person.class));
		} catch (RestClientException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public Optional<Session> signup(String username, String password) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			return Optional.ofNullable(restTemplate.postForObject(personServiceServerUrl + "/v1/person/signup",
					new UsernamePasswordParameter(username, password), Session.class));
		} catch (RestClientException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public Optional<Session> login(String username, String password) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			return Optional.ofNullable(restTemplate.postForObject(personServiceServerUrl + "/v1/person/login",
					new UsernamePasswordParameter(username, password), Session.class));
		} catch (RestClientException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public boolean logout(String username) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			restTemplate.postForObject(personServiceServerUrl + "/v1/person/logout", new UsernameParameter(username),
					String.class);
			return true;
		} catch (RestClientException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkLogin(String username) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			restTemplate.postForObject(personServiceServerUrl + "/v1/person/check-login",
					new UsernameParameter(username), String.class);
			return true;
		} catch (RestClientException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean isUsernameAvailable(String username) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			restTemplate.postForObject(personServiceServerUrl + "/v1/person/username-available",
					new UsernameParameter(username), String.class);
			return true;
		} catch (RestClientException e) {
			e.printStackTrace();
			return false;
		}
	}
}
