package com.valhallagame.personserviceclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
	private String username;
	private String displayUsername;
	private String password;
	private boolean online;
	private String clientVersion;
}
