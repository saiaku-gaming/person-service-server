DELETE FROM person WHERE person_id IN (
	SELECT person_id FROM (
		SELECT
			person_id, 
			ROW_NUMBER() OVER 
			(
				partition BY username ORDER BY person_id
			) AS num 
		FROM person
	) p WHERE p.num > 1
);
ALTER TABLE person ADD UNIQUE (username);