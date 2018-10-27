DELETE FROM session;
ALTER TABLE session ADD UNIQUE (person_id, session_id);
