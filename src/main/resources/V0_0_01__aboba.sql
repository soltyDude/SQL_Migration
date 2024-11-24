-- Create a new table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL
);

INSERT INTO users (name, email) VALUES ('2', 'jane@exasfasfdample.com');
INSERT INTO users (name, email) VALUES ('2', 'jane@wadexaamasfasfple.com');
INSERT INTO users (name, email) VALUES ('b', 'janeq@awdawdexampleasfasf.com');
INSERT INTO users (name, email) VALUES ('a', 'janew@example.comasfafaf');