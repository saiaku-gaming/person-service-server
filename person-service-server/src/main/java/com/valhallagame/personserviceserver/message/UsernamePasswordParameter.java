package com.valhallagame.personserviceserver.message;

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
