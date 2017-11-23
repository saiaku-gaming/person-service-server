package com.valhallagame.personserviceserver.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "session")
public class Session {
	@Id
	@Column(name = "session_id", unique = true)
	private String id;

	@Column(name = "ts")
	private Instant timestamp;

	@OneToOne
	@JoinColumn(name = "person_id")
	private Person person;

	public Session(String id, Person person) {
		this.id = id;
		this.person = person;
		this.timestamp = Instant.now();
	}

	public void updateTimestamp() {
		this.timestamp = Instant.now();
	}
}
