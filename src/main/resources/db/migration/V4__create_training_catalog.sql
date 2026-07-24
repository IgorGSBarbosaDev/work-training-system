CREATE TABLE trainings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(2000),
    category VARCHAR(150),
    is_regulatory_standard BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_trainings_organization_code
    ON trainings (organization_id, LOWER(code));
CREATE INDEX idx_trainings_organization_status
    ON trainings (organization_id, status);

CREATE TABLE training_versions (
    id UUID PRIMARY KEY,
    training_id UUID NOT NULL REFERENCES trainings (id),
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    workload_minutes INTEGER NOT NULL CHECK (workload_minutes > 0),
    validity_type VARCHAR(16) NOT NULL CHECK (validity_type IN ('DAYS', 'MONTHS', 'INDEFINITE')),
    validity_value INTEGER,
    passing_score NUMERIC(5, 2) NOT NULL CHECK (passing_score >= 0 AND passing_score <= 100),
    max_attempts INTEGER CHECK (max_attempts IS NULL OR max_attempts > 0),
    retry_interval_minutes INTEGER NOT NULL CHECK (retry_interval_minutes >= 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_training_versions_validity_value CHECK (
        (validity_type = 'INDEFINITE' AND validity_value IS NULL)
        OR (validity_type IN ('DAYS', 'MONTHS') AND validity_value IS NOT NULL AND validity_value > 0)
    )
);

CREATE UNIQUE INDEX uk_training_versions_number
    ON training_versions (training_id, version_number);
CREATE UNIQUE INDEX uk_training_versions_one_published
    ON training_versions (training_id)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_training_versions_training_status
    ON training_versions (training_id, status);

CREATE TABLE training_modules (
    id UUID PRIMARY KEY,
    training_version_id UUID NOT NULL REFERENCES training_versions (id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_training_modules_version_order
    ON training_modules (training_version_id, display_order);
CREATE INDEX idx_training_modules_version_status
    ON training_modules (training_version_id, status);

CREATE TABLE training_videos (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES training_modules (id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    duration_seconds INTEGER NOT NULL CHECK (duration_seconds > 0),
    storage_object_key VARCHAR(2048) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_training_videos_module_order
    ON training_videos (module_id, display_order);
CREATE INDEX idx_training_videos_module_status
    ON training_videos (module_id, status);

CREATE TABLE questionnaires (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL UNIQUE REFERENCES training_modules (id),
    title VARCHAR(150) NOT NULL,
    passing_score NUMERIC(5, 2) NOT NULL CHECK (passing_score >= 0 AND passing_score <= 100),
    max_attempts INTEGER CHECK (max_attempts IS NULL OR max_attempts > 0),
    retry_interval_minutes INTEGER NOT NULL CHECK (retry_interval_minutes >= 0),
    shuffle_questions BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE questions (
    id UUID PRIMARY KEY,
    questionnaire_id UUID NOT NULL REFERENCES questionnaires (id),
    statement VARCHAR(2000) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_questions_questionnaire_order
    ON questions (questionnaire_id, display_order);
CREATE INDEX idx_questions_questionnaire_status
    ON questions (questionnaire_id, status);

CREATE TABLE answer_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES questions (id),
    text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_answer_options_question_order
    ON answer_options (question_id, display_order);
CREATE INDEX idx_answer_options_question_status
    ON answer_options (question_id, status);
