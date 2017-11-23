CREATE TABLE person (
    person_id serial NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    display_username text NOT NULL,
    last_heartbeat timestamp with time zone DEFAULT now() NOT NULL,
    online boolean DEFAULT false NOT NULL,
    client_version text
);