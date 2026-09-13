-- V1: Initial setup of the calendar tables.
CREATE TABLE IF NOT EXISTS calendar (
    id          UUID NOT NULL PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT,
    owner       TEXT NOT NULL,
    color       TEXT NOT NULL,
    public      BOOLEAN NOT NULL
);