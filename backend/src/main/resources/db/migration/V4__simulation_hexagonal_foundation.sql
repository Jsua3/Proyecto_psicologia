-- V4: Fundacion hexagonal para simulaciones DAG, versionamiento, bitacoras y auditoria.

CREATE TABLE simulation_cases (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(80)  NOT NULL UNIQUE,
    title        VARCHAR(220) NOT NULL,
    description  TEXT,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by   BIGINT       NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE case_versions (
    id                 BIGSERIAL PRIMARY KEY,
    simulation_case_id BIGINT       NOT NULL REFERENCES simulation_cases(id),
    semantic_version   VARCHAR(32)  NOT NULL,
    status             VARCHAR(24)  NOT NULL CHECK (status IN ('DRAFT', 'IN_REVIEW', 'PUBLISHED', 'ARCHIVED')),
    narrative_context  TEXT,
    cloned_from_id     BIGINT       REFERENCES case_versions(id),
    published_at       TIMESTAMP,
    created_by         BIGINT       NOT NULL REFERENCES users(id),
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (simulation_case_id, semantic_version)
);

CREATE TABLE simulation_nodes (
    id                     BIGSERIAL PRIMARY KEY,
    case_version_id         BIGINT       NOT NULL REFERENCES case_versions(id) ON DELETE CASCADE,
    node_key                VARCHAR(120) NOT NULL,
    title                   VARCHAR(220) NOT NULL,
    narrative               TEXT         NOT NULL,
    support_resources_json  TEXT         NOT NULL DEFAULT '[]',
    required_tools_json     TEXT         NOT NULL DEFAULT '[]',
    sensitive_content       BOOLEAN      NOT NULL DEFAULT FALSE,
    safe_exit_required      BOOLEAN      NOT NULL DEFAULT FALSE,
    warning_message         TEXT,
    start_node              BOOLEAN      NOT NULL DEFAULT FALSE,
    terminal_node           BOOLEAN      NOT NULL DEFAULT FALSE,
    position_x              INTEGER,
    position_y              INTEGER,
    UNIQUE (case_version_id, node_key)
);

CREATE TABLE decision_options (
    id                    BIGSERIAL PRIMARY KEY,
    case_version_id        BIGINT       NOT NULL REFERENCES case_versions(id) ON DELETE CASCADE,
    option_key             VARCHAR(120) NOT NULL,
    source_node_id         BIGINT       NOT NULL REFERENCES simulation_nodes(id) ON DELETE CASCADE,
    target_node_id         BIGINT       NOT NULL REFERENCES simulation_nodes(id) ON DELETE CASCADE,
    text                   TEXT         NOT NULL,
    classification         VARCHAR(24)  NOT NULL CHECK (classification IN ('ADEQUATE', 'RISKY', 'INADEQUATE')),
    score_delta            INTEGER      NOT NULL DEFAULT 0,
    stress_delta           INTEGER      NOT NULL DEFAULT 0,
    prohibited_penalty     INTEGER      NOT NULL DEFAULT -50,
    immediate_feedback     TEXT         NOT NULL,
    prohibited_conduct     BOOLEAN      NOT NULL DEFAULT FALSE,
    prohibition_reason     TEXT,
    UNIQUE (case_version_id, option_key)
);

CREATE TABLE simulation_attempts_v2 (
    id                 UUID PRIMARY KEY,
    attempt_token_hash VARCHAR(128) NOT NULL UNIQUE,
    case_version_id     BIGINT       NOT NULL REFERENCES case_versions(id),
    student_id          BIGINT       NOT NULL REFERENCES users(id),
    current_node_id     BIGINT       NOT NULL REFERENCES simulation_nodes(id),
    status              VARCHAR(24)  NOT NULL CHECK (status IN ('IN_PROGRESS', 'SAFE_EXITED', 'COMPLETED')),
    accumulated_score   INTEGER      NOT NULL DEFAULT 0,
    stress_index        INTEGER      NOT NULL DEFAULT 0,
    started_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    ended_at            TIMESTAMP,
    locked_at           TIMESTAMP
);

CREATE TABLE attempt_events (
    id                 BIGSERIAL PRIMARY KEY,
    attempt_id          UUID         NOT NULL REFERENCES simulation_attempts_v2(id) ON DELETE CASCADE,
    event_type          VARCHAR(48)  NOT NULL,
    node_id             BIGINT       REFERENCES simulation_nodes(id),
    decision_option_id  BIGINT       REFERENCES decision_options(id),
    score_delta         INTEGER      NOT NULL DEFAULT 0,
    stress_delta        INTEGER      NOT NULL DEFAULT 0,
    detail              TEXT,
    occurred_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE reflection_journals (
    id                  BIGSERIAL PRIMARY KEY,
    attempt_id           UUID        NOT NULL REFERENCES simulation_attempts_v2(id) ON DELETE CASCADE,
    node_id              BIGINT      NOT NULL REFERENCES simulation_nodes(id),
    encrypted_text       TEXT        NOT NULL,
    encryption_key_ref   VARCHAR(120) NOT NULL,
    locked               BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (attempt_id, node_id)
);

CREATE TABLE rubrics (
    id              BIGSERIAL PRIMARY KEY,
    case_version_id  BIGINT       REFERENCES case_versions(id),
    name            VARCHAR(180) NOT NULL,
    description     TEXT,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      BIGINT       NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE rubric_criteria (
    id             BIGSERIAL PRIMARY KEY,
    rubric_id       BIGINT       NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    competency      VARCHAR(80)  NOT NULL,
    title           VARCHAR(180) NOT NULL,
    description     TEXT,
    max_score       INTEGER      NOT NULL DEFAULT 5,
    display_order   INTEGER      NOT NULL DEFAULT 1
);

CREATE TABLE rubric_evaluations (
    id              BIGSERIAL PRIMARY KEY,
    attempt_id       UUID        NOT NULL REFERENCES simulation_attempts_v2(id),
    rubric_id        BIGINT      NOT NULL REFERENCES rubrics(id),
    instructor_id    BIGINT      NOT NULL REFERENCES users(id),
    total_score      NUMERIC(6,2) NOT NULL DEFAULT 0,
    comment          TEXT,
    evaluated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (attempt_id, rubric_id, instructor_id)
);

CREATE TABLE publication_checklists (
    id               BIGSERIAL PRIMARY KEY,
    case_version_id   BIGINT      NOT NULL REFERENCES case_versions(id) ON DELETE CASCADE,
    submitted_by      BIGINT      NOT NULL REFERENCES users(id),
    completion_ratio  NUMERIC(5,2) NOT NULL DEFAULT 0,
    status            VARCHAR(24) NOT NULL CHECK (status IN ('PENDING', 'COMPLETE', 'REJECTED')),
    submitted_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP
);

CREATE TABLE publication_checklist_items (
    id              BIGSERIAL PRIMARY KEY,
    checklist_id     BIGINT       NOT NULL REFERENCES publication_checklists(id) ON DELETE CASCADE,
    code            VARCHAR(80)  NOT NULL,
    label           VARCHAR(220) NOT NULL,
    required        BOOLEAN      NOT NULL DEFAULT TRUE,
    fulfilled       BOOLEAN      NOT NULL DEFAULT FALSE,
    evidence_note   TEXT,
    UNIQUE (checklist_id, code)
);

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_id         BIGINT       REFERENCES users(id),
    actor_role       VARCHAR(40),
    action           VARCHAR(120) NOT NULL,
    resource_type    VARCHAR(80),
    resource_id      VARCHAR(120),
    context_json     TEXT         NOT NULL DEFAULT '{}',
    ip_address       VARCHAR(80),
    user_agent       TEXT,
    occurred_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    retention_until  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_case_versions_case_status ON case_versions(simulation_case_id, status);
CREATE INDEX idx_simulation_nodes_version ON simulation_nodes(case_version_id);
CREATE INDEX idx_decision_options_source ON decision_options(source_node_id);
CREATE INDEX idx_attempts_student_status ON simulation_attempts_v2(student_id, status);
CREATE INDEX idx_attempt_events_attempt_time ON attempt_events(attempt_id, occurred_at);
CREATE INDEX idx_reflection_attempt ON reflection_journals(attempt_id);
CREATE INDEX idx_audit_logs_time ON audit_logs(occurred_at);
CREATE INDEX idx_audit_logs_retention ON audit_logs(retention_until);
