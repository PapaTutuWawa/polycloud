CREATE TABLE IF NOT EXISTS refresh_token(
    token_hash  TEXT NOT NULL PRIMARY KEY,
    username    TEXT NOT NULL
);