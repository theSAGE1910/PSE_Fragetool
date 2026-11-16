-- Erstes Schema. Spätere Änderungen kommen als V2__..., V3__... dazu; diese Datei nicht mehr ändern.

CREATE TABLE dozent (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE question (
    id                 UUID PRIMARY KEY,
    type               VARCHAR(32)   NOT NULL,
    owner_id           UUID          NOT NULL REFERENCES dozent (id) ON DELETE CASCADE,
    text               VARCHAR(2000) NOT NULL,
    time_limit_seconds INTEGER       NOT NULL,
    quick_start        BOOLEAN       NOT NULL,
    correct_value      NUMERIC(30, 10),
    tolerance          NUMERIC(30, 10),
    image_id           UUID
);
CREATE INDEX idx_question_owner ON question (owner_id);

CREATE TABLE question_tag (
    question_id UUID         NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    tag         VARCHAR(100) NOT NULL,
    PRIMARY KEY (question_id, tag)
);

CREATE TABLE answer_option (
    id          UUID PRIMARY KEY,
    question_id UUID         NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    text        VARCHAR(500) NOT NULL,
    correct     BOOLEAN      NOT NULL,
    sort_index  INTEGER      NOT NULL
);
CREATE INDEX idx_answer_option_question ON answer_option (question_id);

-- /MK155/: höchstens ein Bild pro Frage; question.image_id zeigt auf das aktuelle Bild
CREATE TABLE question_image (
    id           UUID PRIMARY KEY,
    question_id  UUID        NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    content_type VARCHAR(32) NOT NULL,
    data         BYTEA       NOT NULL,
    width        INTEGER     NOT NULL,
    height       INTEGER     NOT NULL
);

CREATE TABLE session (
    id                 UUID PRIMARY KEY,
    owner_id           UUID         NOT NULL REFERENCES dozent (id) ON DELETE CASCADE,
    name               VARCHAR(200) NOT NULL,
    join_code          VARCHAR(16)  NOT NULL UNIQUE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    scoreboard_visible BOOLEAN      NOT NULL
);
CREATE INDEX idx_session_owner ON session (owner_id);

-- /MK360/: über Teilnehmer wird nur gespeichert, was Teilnahme, Wiedererkennung und Punkte erfordern.
CREATE TABLE participant (
    id           UUID PRIMARY KEY,
    session_id   UUID        NOT NULL REFERENCES session (id) ON DELETE CASCADE,
    display_name VARCHAR(16) NOT NULL,
    token        VARCHAR(64) NOT NULL UNIQUE,
    points       INTEGER     NOT NULL,
    joined_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_participant_name UNIQUE (session_id, display_name)
);

CREATE TABLE question_run (
    id                 UUID PRIMARY KEY,
    session_id         UUID        NOT NULL REFERENCES session (id) ON DELETE CASCADE,
    question_id        UUID        NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    started_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at          TIMESTAMP WITH TIME ZONE,
    status             VARCHAR(16) NOT NULL,
    free_text_released BOOLEAN     NOT NULL
);
CREATE INDEX idx_question_run_session ON question_run (session_id, started_at);

CREATE TABLE answer (
    id             UUID PRIMARY KEY,
    run_id         UUID NOT NULL REFERENCES question_run (id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES participant (id) ON DELETE CASCADE,
    numeric_value  NUMERIC(30, 10),
    text           VARCHAR(500),
    correct        BOOLEAN,
    submitted_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    -- /MK240/: höchstens eine Antwort pro Teilnehmer und Frage
    CONSTRAINT uq_answer_once UNIQUE (run_id, participant_id)
);

CREATE TABLE answer_selection (
    answer_id UUID NOT NULL REFERENCES answer (id) ON DELETE CASCADE,
    option_id UUID NOT NULL,
    PRIMARY KEY (answer_id, option_id)
);
