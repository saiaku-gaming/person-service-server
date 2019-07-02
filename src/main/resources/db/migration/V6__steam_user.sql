CREATE TABLE steam_user
(
    steam_user_id serial  NOT NULL,
    person_id     integer NOT NULL,
    steam_id      text    NOT NULL
);