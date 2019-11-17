package com.valhallagame.personserviceserver.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

import javax.persistence.*;
import java.time.Instant;

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

    @Column(name = "finished_tutorial")
    private boolean finishedTutorial;

	public Person(String username, String displayUsername, String plaintextPassword) {
		this.setUsername(username);
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
