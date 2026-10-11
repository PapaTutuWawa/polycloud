-- V1: Initial setup of the WebDAV app password table.
CREATE TABLE IF NOT EXISTS webdav_app_password (
    secret_hash TEXT NOT NULL PRIMARY KEY,
    id          UUID NOT NULL UNIQUE,
    username    TEXT NOT NULL,
    name        TEXT NOT NULL,
    roles       TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);
