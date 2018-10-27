DELETE FROM session;
ALTER TABLE session ADD UNIQUE (person_id);
ALTER TABLE session DROP CONSTRAINT session_person_id_session_id_key;
