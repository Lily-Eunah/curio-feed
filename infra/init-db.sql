-- Initialization script for PostgreSQL
-- (Users and DB are created by POSTGRES_USER and POSTGRES_DB in docker-compose.yml)
-- You can add additional schema initialization here if needed.

CREATE TABLE IF NOT EXISTS example_table (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
