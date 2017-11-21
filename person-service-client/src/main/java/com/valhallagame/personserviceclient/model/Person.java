package com.valhallagame.personserviceclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
	@JsonProperty("username")
	private String username;

	@JsonProperty("displayUsername")
	private String displayUsername;

	@JsonProperty("online")
	private boolean online;

	@JsonProperty("clientVersion")
	private String clientVersion;
}
