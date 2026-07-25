ALTER TABLE training_assignments
    ADD COLUMN recertification_of_completion_id UUID;

ALTER TABLE training_assignments DROP CONSTRAINT ck_assignment_recertification;
ALTER TABLE training_assignments ADD CONSTRAINT fk_assignment_recertification_completion
    FOREIGN KEY (recertification_of_completion_id, organization_id)
    REFERENCES training_completions (id, organization_id);
ALTER TABLE training_assignments ADD CONSTRAINT ck_assignment_recertification CHECK (
    (recertification AND origin = 'RECERTIFICATION'
        AND (recertification_of_assignment_id IS NOT NULL OR recertification_of_completion_id IS NOT NULL))
    OR (NOT recertification AND origin <> 'RECERTIFICATION'
        AND recertification_of_assignment_id IS NULL AND recertification_of_completion_id IS NULL)
);

CREATE TABLE recertifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    completion_id UUID NOT NULL,
    assignment_id UUID NOT NULL,
    trigger_type VARCHAR(16) NOT NULL CHECK (trigger_type IN ('AUTOMATIC', 'MANUAL')),
    responsible_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_recertification_completion_organization
        FOREIGN KEY (completion_id, organization_id) REFERENCES training_completions (id, organization_id),
    CONSTRAINT fk_recertification_assignment_organization
        FOREIGN KEY (assignment_id, organization_id) REFERENCES training_assignments (id, organization_id),
    CONSTRAINT fk_recertification_responsible_organization
        FOREIGN KEY (responsible_user_id, organization_id) REFERENCES users (id, organization_id)
);
CREATE UNIQUE INDEX uk_recertifications_id_organization ON recertifications (id, organization_id);
CREATE UNIQUE INDEX uk_recertifications_completion ON recertifications (organization_id, completion_id);
CREATE UNIQUE INDEX uk_recertifications_assignment ON recertifications (organization_id, assignment_id);
CREATE INDEX idx_recertifications_created ON recertifications (organization_id, created_at DESC, id DESC);

CREATE TABLE completion_expiration_states (
    completion_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('EXPIRING_SOON', 'EXPIRED')),
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (completion_id, organization_id),
    CONSTRAINT fk_expiration_state_completion_organization
        FOREIGN KEY (completion_id, organization_id) REFERENCES training_completions (id, organization_id)
);

CREATE TABLE completion_expiration_status_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    completion_id UUID NOT NULL,
    previous_status VARCHAR(16) CHECK (previous_status IN ('EXPIRING_SOON', 'EXPIRED')),
    new_status VARCHAR(16) NOT NULL CHECK (new_status IN ('EXPIRING_SOON', 'EXPIRED')),
    effective_expiration_date DATE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_expiration_history_completion_organization
        FOREIGN KEY (completion_id, organization_id) REFERENCES training_completions (id, organization_id)
);
CREATE INDEX idx_expiration_history_completion
    ON completion_expiration_status_history (organization_id, completion_id, recorded_at DESC, id DESC);

CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    completion_id UUID NOT NULL,
    type VARCHAR(16) NOT NULL CHECK (type IN ('INTERNAL', 'EXTERNAL')),
    validation_code VARCHAR(64) NOT NULL,
    object_key VARCHAR(1024) NOT NULL,
    issued_date DATE NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED')),
    responsible_user_id UUID NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by_user_id UUID,
    revocation_reason VARCHAR(1000),
    previous_certificate_id UUID,
    generation_number INTEGER NOT NULL CHECK (generation_number > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_certificates_id_organization UNIQUE (id, organization_id),
    CONSTRAINT fk_certificate_completion_organization
        FOREIGN KEY (completion_id, organization_id) REFERENCES training_completions (id, organization_id),
    CONSTRAINT fk_certificate_responsible_organization
        FOREIGN KEY (responsible_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT fk_certificate_revoker_organization
        FOREIGN KEY (revoked_by_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT fk_certificate_previous_organization
        FOREIGN KEY (previous_certificate_id, organization_id) REFERENCES certificates (id, organization_id),
    CONSTRAINT ck_certificate_revocation CHECK (
        (status = 'ACTIVE' AND revoked_at IS NULL AND revoked_by_user_id IS NULL AND revocation_reason IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL AND revoked_by_user_id IS NOT NULL
            AND revocation_reason IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uk_certificates_validation_code ON certificates (validation_code);
CREATE UNIQUE INDEX uk_certificates_active_completion_type
    ON certificates (organization_id, completion_id, type) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_certificates_generation
    ON certificates (organization_id, completion_id, type, generation_number);
CREATE INDEX idx_certificates_completion ON certificates (organization_id, completion_id, issued_at DESC);
CREATE INDEX idx_certificates_issued ON certificates (organization_id, issued_at DESC, id DESC);

CREATE TABLE certificate_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    certificate_id UUID NOT NULL,
    event_type VARCHAR(16) NOT NULL CHECK (event_type IN ('ISSUED', 'REVOKED', 'REGENERATED')),
    responsible_user_id UUID NOT NULL,
    related_certificate_id UUID,
    reason VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_certificate_history_certificate_organization
        FOREIGN KEY (certificate_id, organization_id) REFERENCES certificates (id, organization_id),
    CONSTRAINT fk_certificate_history_related_organization
        FOREIGN KEY (related_certificate_id, organization_id) REFERENCES certificates (id, organization_id),
    CONSTRAINT fk_certificate_history_responsible_organization
        FOREIGN KEY (responsible_user_id, organization_id) REFERENCES users (id, organization_id)
);
CREATE INDEX idx_certificate_history_certificate
    ON certificate_history (organization_id, certificate_id, occurred_at DESC, id DESC);

CREATE TABLE certificate_generation_jobs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    completion_id UUID NOT NULL,
    certificate_type VARCHAR(16) NOT NULL CHECK (certificate_type IN ('INTERNAL', 'EXTERNAL')),
    requested_by_user_id UUID NOT NULL,
    replaces_certificate_id UUID,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_error VARCHAR(1000),
    certificate_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_certificate_job_completion_organization
        FOREIGN KEY (completion_id, organization_id) REFERENCES training_completions (id, organization_id),
    CONSTRAINT fk_certificate_job_requester_organization
        FOREIGN KEY (requested_by_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT fk_certificate_job_replaced_organization
        FOREIGN KEY (replaces_certificate_id, organization_id) REFERENCES certificates (id, organization_id),
    CONSTRAINT fk_certificate_job_result_organization
        FOREIGN KEY (certificate_id, organization_id) REFERENCES certificates (id, organization_id)
);
CREATE UNIQUE INDEX uk_certificate_jobs_id_organization ON certificate_generation_jobs (id, organization_id);
CREATE UNIQUE INDEX uk_certificate_jobs_open_completion_type
    ON certificate_generation_jobs (organization_id, completion_id, certificate_type)
    WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX idx_certificate_jobs_retry ON certificate_generation_jobs (status, updated_at);

CREATE TABLE employee_qr_codes (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    employee_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_ciphertext VARCHAR(512) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED')),
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    generated_by_user_id UUID NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by_user_id UUID,
    revocation_reason VARCHAR(1000),
    CONSTRAINT fk_employee_qr_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT fk_employee_qr_generator_organization
        FOREIGN KEY (generated_by_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT fk_employee_qr_revoker_organization
        FOREIGN KEY (revoked_by_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT ck_employee_qr_revocation CHECK (
        (status = 'ACTIVE' AND revoked_at IS NULL AND revoked_by_user_id IS NULL AND revocation_reason IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL AND revoked_by_user_id IS NOT NULL
            AND revocation_reason IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uk_employee_qr_id_organization ON employee_qr_codes (id, organization_id);
CREATE UNIQUE INDEX uk_employee_qr_token_hash ON employee_qr_codes (token_hash);
CREATE UNIQUE INDEX uk_employee_qr_active_employee
    ON employee_qr_codes (organization_id, employee_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_employee_qr_employee_history
    ON employee_qr_codes (organization_id, employee_id, generated_at DESC, id DESC);

CREATE TABLE qr_code_access_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    qr_code_id UUID,
    queried_by_user_id UUID NOT NULL,
    queried_at TIMESTAMP WITH TIME ZONE NOT NULL,
    result VARCHAR(24) NOT NULL CHECK (result IN ('VALID', 'REVOKED', 'UNKNOWN', 'OUT_OF_SCOPE')),
    request_id VARCHAR(128) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    technical_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_qr_access_qr_organization
        FOREIGN KEY (qr_code_id, organization_id) REFERENCES employee_qr_codes (id, organization_id),
    CONSTRAINT fk_qr_access_user_organization
        FOREIGN KEY (queried_by_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT ck_qr_access_metadata CHECK (jsonb_typeof(technical_metadata) = 'object')
);
CREATE INDEX idx_qr_access_qr_time
    ON qr_code_access_logs (organization_id, qr_code_id, queried_at DESC, id DESC);
CREATE INDEX idx_qr_access_token_time
    ON qr_code_access_logs (organization_id, token_hash, queried_at DESC, id DESC);
CREATE INDEX idx_qr_access_user_time
    ON qr_code_access_logs (organization_id, queried_by_user_id, queried_at DESC);

CREATE FUNCTION prevent_phase_5_slice_a_history_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'phase 5 slice A history records are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recertifications_immutable
    BEFORE UPDATE OR DELETE ON recertifications
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_5_slice_a_history_mutation();
CREATE TRIGGER trg_expiration_status_history_immutable
    BEFORE UPDATE OR DELETE ON completion_expiration_status_history
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_5_slice_a_history_mutation();
CREATE TRIGGER trg_certificate_history_immutable
    BEFORE UPDATE OR DELETE ON certificate_history
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_5_slice_a_history_mutation();
CREATE TRIGGER trg_qr_access_logs_immutable
    BEFORE UPDATE OR DELETE ON qr_code_access_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_5_slice_a_history_mutation();
