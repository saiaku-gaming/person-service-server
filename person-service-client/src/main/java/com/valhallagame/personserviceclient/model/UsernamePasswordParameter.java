package com.valhallagame.personserviceclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsernamePasswordParameter {
	private String username;
	private String password;
}
