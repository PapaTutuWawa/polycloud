-- V1: Initial setup of the calendar tables.
CREATE TABLE IF NOT EXISTS calendar (
    id          UUID NOT NULL PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT,
    owner       TEXT NOT NULL,
    color       TEXT NOT NULL,
    public      BOOLEAN NOT NULL,
    CONSTRAINT uq_name
        UNIQUE (name, owner)
);

CREATE TABLE IF NOT EXISTS event (
    id                  UUID NOT NULL PRIMARY KEY,
    calendar            UUID NOT NULL,
    title               TEXT NOT NULL,
    description         TEXT,
    timeframe           TSTZRANGE NOT NULL,
    all_day             BOOLEAN NOT NULL,
    place               TEXT,
    repeat_mode         TEXT,
    repeat_times        INTEGER,
    repeat_until        TIMESTAMP WITH TIME ZONE,
    repeat_valid_until  TIMESTAMP WITH TIME ZONE,
    repeat_config       JSONB,
    CONSTRAINT fk_calendar
        FOREIGN KEY (calendar)
        REFERENCES calendar (id)
        ON DELETE CASCADE
);