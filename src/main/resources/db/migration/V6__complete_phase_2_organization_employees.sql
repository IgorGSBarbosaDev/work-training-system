CREATE TABLE organization_settings (
    organization_id UUID PRIMARY KEY REFERENCES organizations (id) ON DELETE CASCADE,
    expiring_soon_days INTEGER NOT NULL DEFAULT 30 CHECK (expiring_soon_days BETWEEN 1 AND 3650),
    default_passing_score INTEGER NOT NULL DEFAULT 70 CHECK (default_passing_score BETWEEN 70 AND 100),
    default_required_video_percentage INTEGER NOT NULL DEFAULT 80 CHECK (default_required_video_percentage = 80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO organization_settings (
    organization_id,
    expiring_soon_days,
    default_passing_score,
    default_required_video_percentage,
    created_at,
    updated_at
)
SELECT id, 30, 70, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM organizations;

ALTER TABLE employees
    ADD COLUMN photo_object_key VARCHAR(1024),
    ADD COLUMN photo_content_type VARCHAR(100),
    ADD COLUMN photo_size_bytes BIGINT;

ALTER TABLE employees
    ADD CONSTRAINT ck_employee_photo_metadata CHECK (
        (photo_object_key IS NULL AND photo_content_type IS NULL AND photo_size_bytes IS NULL)
        OR (photo_object_key IS NOT NULL AND photo_content_type IS NOT NULL AND photo_size_bytes > 0)
    );

CREATE UNIQUE INDEX uk_employees_organization_email_lower
    ON employees (organization_id, LOWER(email));

CREATE TABLE employee_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    employee_id UUID NOT NULL,
    change_type VARCHAR(32) NOT NULL CHECK (change_type IN (
        'CREATED', 'PROFILE_UPDATED', 'STATUS_CHANGED', 'JOB_CHANGED', 'PHOTO_UPDATED', 'PHOTO_REMOVED'
    )),
    responsible_user_id UUID NOT NULL,
    before_state JSONB,
    after_state JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_employee_history_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id)
);

CREATE INDEX idx_employee_history_employee_created
    ON employee_history (employee_id, created_at DESC, id DESC);
CREATE INDEX idx_employee_history_actor_created
    ON employee_history (responsible_user_id, created_at DESC);

CREATE FUNCTION prevent_employee_history_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'employee_history is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_employee_history_append_only
    BEFORE UPDATE OR DELETE ON employee_history
    FOR EACH ROW EXECUTE FUNCTION prevent_employee_history_mutation();

CREATE INDEX idx_employees_scope_active
    ON employees (organization_id, status, unit_id, sector_id, job_id);
