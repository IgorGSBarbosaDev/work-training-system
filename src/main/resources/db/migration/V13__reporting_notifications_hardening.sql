ALTER TABLE email_deliveries ADD COLUMN body VARCHAR(4000);
UPDATE email_deliveries delivery
SET body = COALESCE(notification.message, delivery.subject)
FROM notifications notification
WHERE notification.id = delivery.notification_id
  AND notification.organization_id = delivery.organization_id;
UPDATE email_deliveries SET body = subject WHERE body IS NULL;
ALTER TABLE email_deliveries ALTER COLUMN body SET NOT NULL;

ALTER TABLE notifications ADD COLUMN deduplication_key VARCHAR(240);
UPDATE notifications SET deduplication_key = id::text WHERE deduplication_key IS NULL;
ALTER TABLE notifications ALTER COLUMN deduplication_key SET NOT NULL;
CREATE UNIQUE INDEX uk_notifications_deduplication
    ON notifications (organization_id, user_id, type, deduplication_key);

CREATE INDEX idx_email_deliveries_filter
    ON email_deliveries (organization_id, status, recipient, created_at DESC, id DESC);
CREATE INDEX idx_assignments_reporting_period
    ON training_assignments (organization_id, assigned_date, employee_id, training_id, status);
CREATE INDEX idx_completions_reporting
    ON training_completions (organization_id, completed_at, employee_id, training_id);
CREATE INDEX idx_attempts_reporting_latest
    ON assessment_attempts (organization_id, assignment_id, submitted_at DESC, id DESC, score, result);
