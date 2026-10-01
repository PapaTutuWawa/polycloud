-- V1: Initial setup of the upload tables.
CREATE TABLE IF NOT EXISTS uploads (
    id          UUID NOT NULL PRIMARY KEY,
    size        BIGINT NOT NULL,
    "user"      TEXT NOT NULL,
    path        TEXT NOT NULL,
    "offset"    BIGINT NOT NULL
);
