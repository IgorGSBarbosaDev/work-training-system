CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    purpose VARCHAR(32) NOT NULL CHECK (purpose IN (
        'TRAINING_VIDEO', 'EMPLOYEE_PHOTO', 'EXTERNAL_CERTIFICATE', 'GENERATED_CERTIFICATE'
    )),
    state VARCHAR(16) NOT NULL CHECK (state IN ('REQUESTED', 'UPLOADED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    requested_by_user_id UUID NOT NULL,
    owner_employee_id UUID,
    original_file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(1024) NOT NULL,
    expected_content_type VARCHAR(100) NOT NULL,
    expected_size_bytes BIGINT NOT NULL CHECK (expected_size_bytes > 0),
    expected_checksum_sha256 VARCHAR(64),
    actual_content_type VARCHAR(100),
    actual_size_bytes BIGINT,
    actual_checksum_sha256 VARCHAR(64),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(1000),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    expired_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_uploaded_files_requester_organization
        FOREIGN KEY (requested_by_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT fk_uploaded_files_owner_organization
        FOREIGN KEY (owner_employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT ck_uploaded_files_checksum CHECK (
        expected_checksum_sha256 IS NULL OR expected_checksum_sha256 ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT ck_uploaded_files_actual_metadata CHECK (
        (state = 'UPLOADED' AND uploaded_at IS NOT NULL AND actual_content_type IS NOT NULL
            AND actual_size_bytes IS NOT NULL AND actual_size_bytes > 0)
        OR state <> 'UPLOADED'
    ),
    CONSTRAINT ck_uploaded_files_terminal_metadata CHECK (
        (state = 'FAILED' AND failed_at IS NOT NULL AND failure_reason IS NOT NULL)
        OR (state = 'CANCELLED' AND cancelled_at IS NOT NULL)
        OR (state = 'EXPIRED' AND expired_at IS NOT NULL)
        OR state IN ('REQUESTED', 'UPLOADED')
    )
);

CREATE UNIQUE INDEX uk_uploaded_files_id_organization ON uploaded_files (id, organization_id);
CREATE UNIQUE INDEX uk_uploaded_files_object_key ON uploaded_files (organization_id, object_key);
CREATE INDEX idx_uploaded_files_requester_state ON uploaded_files (organization_id, requested_by_user_id, state, created_at DESC);
CREATE INDEX idx_uploaded_files_owner_purpose ON uploaded_files (organization_id, owner_employee_id, purpose, state)
    WHERE owner_employee_id IS NOT NULL;
CREATE INDEX idx_uploaded_files_expiry ON uploaded_files (expires_at) WHERE state = 'REQUESTED';

ALTER TABLE training_videos
    ADD COLUMN organization_id UUID,
    ADD COLUMN file_id UUID;

UPDATE training_videos video
SET organization_id = version.organization_id
FROM training_modules module
JOIN training_versions version ON version.id = module.training_version_id
WHERE module.id = video.module_id;

ALTER TABLE training_videos
    ALTER COLUMN organization_id SET NOT NULL,
    ADD CONSTRAINT fk_training_videos_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    ADD CONSTRAINT fk_training_videos_file_organization
        FOREIGN KEY (file_id, organization_id) REFERENCES uploaded_files (id, organization_id);

CREATE UNIQUE INDEX uk_training_videos_id_organization ON training_videos (id, organization_id);
CREATE INDEX idx_training_videos_file ON training_videos (organization_id, file_id) WHERE file_id IS NOT NULL;
CREATE UNIQUE INDEX uk_assignments_id_version_organization
    ON training_assignments (id, training_version_id, organization_id);

CREATE TABLE video_progress (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    assignment_id UUID NOT NULL,
    training_version_id UUID NOT NULL,
    video_id UUID NOT NULL,
    position_seconds BIGINT NOT NULL DEFAULT 0 CHECK (position_seconds >= 0),
    watched_seconds NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (watched_seconds >= 0),
    percentage_watched NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (percentage_watched BETWEEN 0 AND 100),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_event_at TIMESTAMP WITH TIME ZONE,
    last_event_received_at TIMESTAMP WITH TIME ZONE,
    last_event_sequence BIGINT NOT NULL DEFAULT 0 CHECK (last_event_sequence >= 0),
    last_event_hash VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_video_progress_assignment_version_organization
        FOREIGN KEY (assignment_id, training_version_id, organization_id)
        REFERENCES training_assignments (id, training_version_id, organization_id),
    CONSTRAINT fk_video_progress_video_organization
        FOREIGN KEY (video_id, organization_id) REFERENCES training_videos (id, organization_id),
    CONSTRAINT ck_video_progress_completion CHECK (
        (completed AND percentage_watched >= 80.00 AND completed_at IS NOT NULL)
        OR (NOT completed AND completed_at IS NULL)
    ),
    CONSTRAINT ck_video_progress_event CHECK (
        (last_event_sequence = 0 AND last_event_at IS NULL AND last_event_received_at IS NULL AND last_event_hash IS NULL)
        OR (last_event_sequence > 0 AND last_event_at IS NOT NULL AND last_event_received_at IS NOT NULL AND last_event_hash IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_video_progress_id_organization ON video_progress (id, organization_id);
CREATE UNIQUE INDEX uk_video_progress_assignment_video ON video_progress (organization_id, assignment_id, video_id);
CREATE INDEX idx_video_progress_assignment_completion ON video_progress (organization_id, assignment_id, completed, video_id);
CREATE INDEX idx_video_progress_video ON video_progress (organization_id, video_id);

CREATE TABLE video_progress_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    progress_id UUID NOT NULL,
    assignment_id UUID NOT NULL,
    video_id UUID NOT NULL,
    event_identifier VARCHAR(200) NOT NULL,
    event_sequence BIGINT NOT NULL CHECK (event_sequence > 0),
    event_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    requested_position_seconds BIGINT NOT NULL CHECK (requested_position_seconds >= 0),
    requested_watched_seconds NUMERIC(14, 3) NOT NULL CHECK (requested_watched_seconds >= 0),
    reported_percentage NUMERIC(5, 2) NOT NULL CHECK (reported_percentage BETWEEN 0 AND 100),
    resulting_position_seconds BIGINT NOT NULL CHECK (resulting_position_seconds >= 0),
    resulting_watched_seconds NUMERIC(14, 3) NOT NULL CHECK (resulting_watched_seconds >= 0),
    resulting_percentage NUMERIC(5, 2) NOT NULL CHECK (resulting_percentage BETWEEN 0 AND 100),
    resulting_completed BOOLEAN NOT NULL,
    resulting_assignment_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_video_progress_events_progress_organization
        FOREIGN KEY (progress_id, organization_id) REFERENCES video_progress (id, organization_id),
    CONSTRAINT fk_video_progress_events_assignment_organization
        FOREIGN KEY (assignment_id, organization_id) REFERENCES training_assignments (id, organization_id),
    CONSTRAINT fk_video_progress_events_video_organization
        FOREIGN KEY (video_id, organization_id) REFERENCES training_videos (id, organization_id),
    CONSTRAINT ck_video_progress_events_assignment_status CHECK (resulting_assignment_status IN (
        'NOT_STARTED', 'IN_PROGRESS', 'AWAITING_ASSESSMENT', 'APPROVED', 'FAILED',
        'COMPLETED', 'EXPIRING_SOON', 'EXPIRED', 'CANCELLED', 'WAIVED'
    ))
);

CREATE UNIQUE INDEX uk_video_progress_events_identifier
    ON video_progress_events (organization_id, assignment_id, video_id, event_identifier);
CREATE UNIQUE INDEX uk_video_progress_events_sequence
    ON video_progress_events (organization_id, assignment_id, video_id, event_sequence);
CREATE INDEX idx_video_progress_events_progress_created ON video_progress_events (progress_id, created_at);

CREATE TABLE assignment_status_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    assignment_id UUID NOT NULL,
    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_assignment_status_events_assignment_organization
        FOREIGN KEY (assignment_id, organization_id) REFERENCES training_assignments (id, organization_id),
    CONSTRAINT ck_assignment_status_events_change CHECK (previous_status <> new_status)
);

CREATE INDEX idx_assignment_status_events_assignment ON assignment_status_events (organization_id, assignment_id, occurred_at);

CREATE FUNCTION prevent_phase_4_history_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'phase 4 history records are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_video_progress_events_immutable
    BEFORE UPDATE OR DELETE ON video_progress_events
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_4_history_mutation();
CREATE TRIGGER trg_assignment_status_events_immutable
    BEFORE UPDATE OR DELETE ON assignment_status_events
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_4_history_mutation();
