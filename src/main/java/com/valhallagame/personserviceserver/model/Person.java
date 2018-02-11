package com.valhallagame.personserviceserver.model;

import java.time.Instant;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.mindrot.jbcrypt.BCrypt;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "person")
public class Person {
	@Id
	@SequenceGenerator(name = "person_person_id_seq", sequenceName = "person_person_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_person_id_seq")
	@Column(name = "person_id", updatable = false)
	private Integer id;

	@Column(name = "username")
	private String username;

	@Column(name = "display_username")
	private String displayUsername;

	@Column(name = "password")
	private String password;

	@Column(name = "online")
	private boolean online;

	@Column(name = "client_version")
	private String clientVersion;

	@Column(name = "last_heartbeat")
	@Basic
	private Instant lastHeartbeat;

	public Person(String displayUsername, String plaintextPassword) {
		this.setUsername(displayUsername.toLowerCase());
		this.setDisplayUsername(displayUsername);
		this.password = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());
		this.lastHeartbeat = Instant.now();
	}

	public boolean validatePassword(String plaintextPassword) {
		return BCrypt.checkpw(plaintextPassword, getPassword());
	}

	public void setPassword(String password) {
		this.password = BCrypt.hashpw(password, BCrypt.gensalt());
	}
}
