package com.valhallagame.personserviceclient;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpStatus;

import com.valhallagame.common.DefaultServicePortMappings;
import com.valhallagame.common.RestCaller;
import com.valhallagame.common.RestResponse;
import com.valhallagame.personserviceclient.model.Person;
import com.valhallagame.personserviceclient.model.Session;
import com.valhallagame.personserviceclient.model.TokenParameter;
import com.valhallagame.personserviceclient.model.UsernameParameter;
import com.valhallagame.personserviceclient.model.UsernamePasswordParameter;

public class PersonServiceClient {
	private static PersonServiceClient personServiceClient;

	private String personServiceServerUrl = "http://localhost:" + DefaultServicePortMappings.PERSON_SERVICE_PORT;
	private RestCaller restCaller;

	private PersonServiceClient() {
		restCaller = new RestCaller();
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

	public RestResponse<Person> getPerson(String username) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/get-person",
					new UsernameParameter(username), Person.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}

	public RestResponse<Session> signup(String username, String password) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/signup",
					new UsernamePasswordParameter(username, password), Session.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}

	public RestResponse<Session> login(String username, String password) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/login",
					new UsernamePasswordParameter(username, password), Session.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}

	public RestResponse<String> logout(String username) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/logout", new UsernameParameter(username),
					String.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}

	public RestResponse<String> checkLogin(String username) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/check-login",
					new UsernameParameter(username), String.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}

	public RestResponse<String> isUsernameAvailable(String username) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/username-available",
					new UsernameParameter(username), String.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}

	public RestResponse<Session> getSessionFromToken(String token) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/get-session-from-token",
					new TokenParameter(token), Session.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}

	// TODO fix this with character service
	public RestResponse<String> createDebugPerson() {
		return new RestResponse<>(HttpStatus.OK, Optional.of("THINGS DID IT"));
	}

	public RestResponse<String> validateCredentials(String username, String password) {
		try {
			return restCaller.postCall(personServiceServerUrl + "/v1/person/validate-credentials",
					new UsernamePasswordParameter(username, password), String.class);
		} catch (IOException e) {
			e.printStackTrace();
			return RestResponse.errorResponse(e);
		}
	}
}
