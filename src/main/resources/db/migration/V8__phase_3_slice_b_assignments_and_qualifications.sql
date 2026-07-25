CREATE TABLE assignment_batches (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    requested_by_user_id UUID NOT NULL,
    idempotency_key VARCHAR(200),
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    requested_count INTEGER NOT NULL DEFAULT 0 CHECK (requested_count >= 0),
    created_count INTEGER NOT NULL DEFAULT 0 CHECK (created_count >= 0),
    skipped_count INTEGER NOT NULL DEFAULT 0 CHECK (skipped_count >= 0),
    failed_count INTEGER NOT NULL DEFAULT 0 CHECK (failed_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_assignment_batch_counts CHECK (
        requested_count = created_count + skipped_count + failed_count
        OR status = 'PROCESSING'
    )
);

CREATE UNIQUE INDEX uk_assignment_batches_id_organization
    ON assignment_batches (id, organization_id);
CREATE UNIQUE INDEX uk_assignment_batches_idempotency
    ON assignment_batches (organization_id, requested_by_user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_assignment_batches_requester_created
    ON assignment_batches (organization_id, requested_by_user_id, created_at DESC);

CREATE TABLE training_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    employee_id UUID NOT NULL,
    training_id UUID NOT NULL,
    training_version_id UUID NOT NULL,
    origin VARCHAR(32) NOT NULL CHECK (origin IN (
        'EMPLOYEE', 'JOB', 'ACTIVITY', 'SECTOR', 'UNIT', 'GROUP', 'RECERTIFICATION'
    )),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_date DATE NOT NULL,
    due_date DATE,
    status VARCHAR(32) NOT NULL CHECK (status IN (
        'NOT_STARTED', 'IN_PROGRESS', 'AWAITING_ASSESSMENT', 'APPROVED', 'FAILED',
        'COMPLETED', 'EXPIRING_SOON', 'EXPIRED', 'CANCELLED', 'WAIVED'
    )),
    priority VARCHAR(16) NOT NULL CHECK (priority IN ('NORMAL', 'HIGH', 'URGENT')),
    responsible_user_id UUID NOT NULL,
    recertification BOOLEAN NOT NULL DEFAULT FALSE,
    recertification_of_assignment_id UUID REFERENCES training_assignments (id),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by_user_id UUID,
    cancellation_reason VARCHAR(1000),
    waived_at TIMESTAMP WITH TIME ZONE,
    waived_by_user_id UUID,
    waiver_reason VARCHAR(1000),
    idempotency_key VARCHAR(200),
    request_hash VARCHAR(64),
    batch_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_assignments_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT fk_assignments_training_organization
        FOREIGN KEY (training_id, organization_id) REFERENCES trainings (id, organization_id),
    CONSTRAINT fk_assignments_version_training_organization
        FOREIGN KEY (training_version_id, training_id, organization_id)
        REFERENCES training_versions (id, training_id, organization_id),
    CONSTRAINT fk_assignments_batch_organization
        FOREIGN KEY (batch_id, organization_id) REFERENCES assignment_batches (id, organization_id),
    CONSTRAINT ck_assignment_due_date CHECK (due_date IS NULL OR due_date >= assigned_date),
    CONSTRAINT ck_assignment_recertification CHECK (
        (recertification AND origin = 'RECERTIFICATION' AND recertification_of_assignment_id IS NOT NULL)
        OR (NOT recertification AND origin <> 'RECERTIFICATION' AND recertification_of_assignment_id IS NULL)
    ),
    CONSTRAINT ck_assignment_cancellation CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by_user_id IS NOT NULL
            AND cancellation_reason IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancelled_at IS NULL AND cancelled_by_user_id IS NULL
            AND cancellation_reason IS NULL)
    ),
    CONSTRAINT ck_assignment_waiver CHECK (
        (status = 'WAIVED' AND waived_at IS NOT NULL AND waived_by_user_id IS NOT NULL
            AND waiver_reason IS NOT NULL)
        OR (status <> 'WAIVED' AND waived_at IS NULL AND waived_by_user_id IS NULL
            AND waiver_reason IS NULL)
    ),
    CONSTRAINT ck_assignment_idempotency_hash CHECK (
        (idempotency_key IS NULL AND request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND request_hash IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_training_assignments_id_organization
    ON training_assignments (id, organization_id);
CREATE UNIQUE INDEX uk_training_assignments_effective_active
    ON training_assignments (organization_id, employee_id, training_id, training_version_id)
    WHERE status IN ('NOT_STARTED', 'IN_PROGRESS', 'AWAITING_ASSESSMENT', 'APPROVED', 'FAILED');
CREATE UNIQUE INDEX uk_training_assignments_idempotency
    ON training_assignments (organization_id, responsible_user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_assignments_employee_status_due
    ON training_assignments (organization_id, employee_id, status, due_date, assigned_at DESC);
CREATE INDEX idx_assignments_training_status
    ON training_assignments (organization_id, training_id, status, assigned_at DESC);
CREATE INDEX idx_assignments_version ON training_assignments (training_version_id);
CREATE INDEX idx_assignments_batch ON training_assignments (batch_id) WHERE batch_id IS NOT NULL;

CREATE TABLE assignment_sources (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    assignment_id UUID NOT NULL,
    origin VARCHAR(32) NOT NULL CHECK (origin IN (
        'EMPLOYEE', 'JOB', 'ACTIVITY', 'SECTOR', 'UNIT', 'GROUP', 'RECERTIFICATION'
    )),
    source_reference_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_assignment_sources_assignment_organization
        FOREIGN KEY (assignment_id, organization_id)
        REFERENCES training_assignments (id, organization_id)
);

CREATE UNIQUE INDEX uk_assignment_sources_provenance
    ON assignment_sources (organization_id, assignment_id, origin, source_reference_id);
CREATE INDEX idx_assignment_sources_reference
    ON assignment_sources (organization_id, origin, source_reference_id);

CREATE TABLE assignment_batch_results (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    batch_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    result VARCHAR(16) NOT NULL CHECK (result IN ('CREATED', 'SKIPPED', 'FAILED')),
    assignment_id UUID,
    code VARCHAR(64),
    message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_assignment_batch_results_batch_organization
        FOREIGN KEY (batch_id, organization_id) REFERENCES assignment_batches (id, organization_id),
    CONSTRAINT fk_assignment_batch_results_assignment_organization
        FOREIGN KEY (assignment_id, organization_id) REFERENCES training_assignments (id, organization_id),
    CONSTRAINT ck_assignment_batch_result_details CHECK (
        (result = 'CREATED' AND assignment_id IS NOT NULL AND code IS NULL AND message IS NULL)
        OR (result IN ('SKIPPED', 'FAILED') AND assignment_id IS NULL AND code IS NOT NULL AND message IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_assignment_batch_results_employee
    ON assignment_batch_results (organization_id, batch_id, employee_id);
CREATE INDEX idx_assignment_batch_results_batch_result
    ON assignment_batch_results (batch_id, result, employee_id);

CREATE TABLE activity_qualifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    employee_id UUID NOT NULL,
    activity_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('AVAILABLE', 'EXPIRING', 'BLOCKED', 'NOT_ASSIGNED')),
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_expiration_date DATE,
    blocking_reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_qualifications_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT fk_qualifications_activity_organization
        FOREIGN KEY (activity_id, organization_id) REFERENCES activities (id, organization_id),
    CONSTRAINT ck_qualification_blocking_reasons CHECK (
        jsonb_typeof(blocking_reasons) = 'array'
        AND ((status = 'BLOCKED' AND jsonb_array_length(blocking_reasons) > 0)
            OR (status <> 'BLOCKED' AND jsonb_array_length(blocking_reasons) = 0))
    )
);

CREATE UNIQUE INDEX uk_activity_qualifications_employee_activity
    ON activity_qualifications (organization_id, employee_id, activity_id);
CREATE INDEX idx_qualifications_employee_status
    ON activity_qualifications (organization_id, employee_id, status, activity_id);
CREATE INDEX idx_qualifications_activity_status
    ON activity_qualifications (organization_id, activity_id, status, employee_id);
CREATE INDEX idx_qualifications_next_expiration
    ON activity_qualifications (organization_id, next_expiration_date)
    WHERE next_expiration_date IS NOT NULL;
