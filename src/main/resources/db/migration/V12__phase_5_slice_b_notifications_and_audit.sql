CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_id VARCHAR(128),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_audit_user_organization
        FOREIGN KEY (user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT ck_audit_details_object CHECK (jsonb_typeof(details) = 'object')
);
CREATE INDEX idx_audit_logs_filter ON audit_logs
    (organization_id, occurred_at DESC, id DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs
    (organization_id, entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_user ON audit_logs
    (organization_id, user_id, occurred_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    user_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    related_entity_type VARCHAR(100),
    related_entity_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_notifications_id_organization UNIQUE (id, organization_id),
    CONSTRAINT fk_notification_user_organization
        FOREIGN KEY (user_id, organization_id) REFERENCES users (id, organization_id)
);
CREATE INDEX idx_notifications_user_state ON notifications
    (organization_id, user_id, archived_at, read_at, created_at DESC, id DESC);

CREATE TABLE email_deliveries (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    user_id UUID NOT NULL,
    notification_id UUID,
    recipient VARCHAR(254) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_error VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_email_user_organization
        FOREIGN KEY (user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT fk_email_notification_organization
        FOREIGN KEY (notification_id, organization_id) REFERENCES notifications (id, organization_id)
);
CREATE INDEX idx_email_deliveries_status ON email_deliveries
    (organization_id, status, updated_at DESC);

CREATE FUNCTION prevent_phase_5_slice_b_history_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'phase 5 slice B history records are immutable';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_5_slice_b_history_mutation();
