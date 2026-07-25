CREATE UNIQUE INDEX uk_questions_id_questionnaire
    ON questions (id, questionnaire_id);
CREATE UNIQUE INDEX uk_answer_options_id_question
    ON answer_options (id, question_id);
CREATE UNIQUE INDEX uk_assignments_execution_identity
    ON training_assignments (id, employee_id, training_id, training_version_id, organization_id);

CREATE TABLE assessment_attempts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    assignment_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    training_id UUID NOT NULL,
    training_version_id UUID NOT NULL,
    questionnaire_id UUID NOT NULL REFERENCES questionnaires (id),
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    score NUMERIC(5, 2) NOT NULL CHECK (score BETWEEN 0 AND 100),
    passing_score NUMERIC(5, 2) NOT NULL CHECK (passing_score BETWEEN 70 AND 100),
    result VARCHAR(16) NOT NULL CHECK (result IN ('APPROVED', 'FAILED')),
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_attempt_assignment_execution_identity
        FOREIGN KEY (assignment_id, employee_id, training_id, training_version_id, organization_id)
        REFERENCES training_assignments (id, employee_id, training_id, training_version_id, organization_id),
    CONSTRAINT fk_attempt_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT fk_attempt_training_organization
        FOREIGN KEY (training_id, organization_id) REFERENCES trainings (id, organization_id),
    CONSTRAINT fk_attempt_version_training_organization
        FOREIGN KEY (training_version_id, training_id, organization_id)
        REFERENCES training_versions (id, training_id, organization_id),
    CONSTRAINT ck_attempt_result_score CHECK (
        (result = 'APPROVED' AND score >= passing_score)
        OR (result = 'FAILED' AND score < passing_score)
    )
);

CREATE UNIQUE INDEX uk_assessment_attempts_id_organization
    ON assessment_attempts (id, organization_id);
CREATE UNIQUE INDEX uk_assessment_attempts_number
    ON assessment_attempts (organization_id, assignment_id, questionnaire_id, attempt_number);
CREATE UNIQUE INDEX uk_assessment_attempts_idempotency
    ON assessment_attempts (organization_id, assignment_id, questionnaire_id, idempotency_key);
CREATE INDEX idx_assessment_attempts_assignment_submitted
    ON assessment_attempts (organization_id, assignment_id, submitted_at DESC, id DESC);
CREATE INDEX idx_assessment_attempts_employee_training_latest
    ON assessment_attempts (organization_id, employee_id, training_id, submitted_at DESC, id DESC);
CREATE INDEX idx_assessment_attempts_questionnaire_result
    ON assessment_attempts (organization_id, assignment_id, questionnaire_id, result, submitted_at DESC);

CREATE TABLE attempt_answers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    attempt_id UUID NOT NULL,
    questionnaire_id UUID NOT NULL,
    question_id UUID NOT NULL,
    selected_option_id UUID NOT NULL,
    question_statement_snapshot VARCHAR(2000) NOT NULL,
    selected_option_text_snapshot VARCHAR(1000) NOT NULL,
    correct BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_attempt_answers_attempt_organization
        FOREIGN KEY (attempt_id, organization_id) REFERENCES assessment_attempts (id, organization_id),
    CONSTRAINT fk_attempt_answers_question_questionnaire
        FOREIGN KEY (question_id, questionnaire_id) REFERENCES questions (id, questionnaire_id),
    CONSTRAINT fk_attempt_answers_option_question
        FOREIGN KEY (selected_option_id, question_id) REFERENCES answer_options (id, question_id)
);

CREATE UNIQUE INDEX uk_attempt_answers_question
    ON attempt_answers (organization_id, attempt_id, question_id);
CREATE INDEX idx_attempt_answers_attempt
    ON attempt_answers (organization_id, attempt_id);

CREATE TABLE training_completions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    employee_id UUID NOT NULL,
    training_id UUID NOT NULL,
    training_version_id UUID NOT NULL,
    source_assignment_id UUID,
    completion_date DATE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_form VARCHAR(16) NOT NULL CHECK (completion_form IN ('AUTOMATIC', 'MANUAL')),
    final_score NUMERIC(5, 2) CHECK (final_score IS NULL OR final_score BETWEEN 0 AND 100),
    applied_validity_type VARCHAR(16) NOT NULL CHECK (applied_validity_type IN ('DAYS', 'MONTHS', 'INDEFINITE')),
    applied_validity_value INTEGER,
    expiration_date DATE,
    responsible_user_id UUID,
    notes VARCHAR(2000),
    external_evidence_file_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_completion_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT fk_completion_training_organization
        FOREIGN KEY (training_id, organization_id) REFERENCES trainings (id, organization_id),
    CONSTRAINT fk_completion_version_training_organization
        FOREIGN KEY (training_version_id, training_id, organization_id)
        REFERENCES training_versions (id, training_id, organization_id),
    CONSTRAINT fk_completion_assignment_execution_identity
        FOREIGN KEY (source_assignment_id, employee_id, training_id, training_version_id, organization_id)
        REFERENCES training_assignments (id, employee_id, training_id, training_version_id, organization_id),
    CONSTRAINT fk_completion_responsible_organization
        FOREIGN KEY (responsible_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT fk_completion_evidence_organization
        FOREIGN KEY (external_evidence_file_id, organization_id) REFERENCES uploaded_files (id, organization_id),
    CONSTRAINT ck_completion_validity CHECK (
        (applied_validity_type = 'INDEFINITE' AND applied_validity_value IS NULL AND expiration_date IS NULL)
        OR (applied_validity_type IN ('DAYS', 'MONTHS') AND applied_validity_value > 0 AND expiration_date IS NOT NULL)
    ),
    CONSTRAINT ck_completion_form CHECK (
        (completion_form = 'AUTOMATIC' AND source_assignment_id IS NOT NULL
            AND responsible_user_id IS NULL AND external_evidence_file_id IS NULL)
        OR (completion_form = 'MANUAL' AND responsible_user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_training_completions_id_organization
    ON training_completions (id, organization_id);
CREATE UNIQUE INDEX uk_training_completions_automatic_assignment
    ON training_completions (organization_id, source_assignment_id)
    WHERE completion_form = 'AUTOMATIC';
CREATE INDEX idx_training_completions_employee_training_latest
    ON training_completions (organization_id, employee_id, training_id, completed_at DESC, id DESC);
CREATE INDEX idx_training_completions_training_latest
    ON training_completions (organization_id, training_id, completed_at DESC, id DESC);
CREATE INDEX idx_training_completions_expiration
    ON training_completions (organization_id, expiration_date)
    WHERE expiration_date IS NOT NULL;

CREATE TABLE completion_expiration_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    completion_id UUID NOT NULL,
    previous_expiration_date DATE,
    recalculated_expiration_date DATE,
    validity_type VARCHAR(16) NOT NULL CHECK (validity_type IN ('DAYS', 'MONTHS', 'INDEFINITE')),
    validity_value INTEGER,
    responsible_user_id UUID NOT NULL,
    reason VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_completion_expiration_history_completion
        FOREIGN KEY (completion_id, organization_id) REFERENCES training_completions (id, organization_id),
    CONSTRAINT fk_completion_expiration_history_responsible
        FOREIGN KEY (responsible_user_id, organization_id) REFERENCES users (id, organization_id),
    CONSTRAINT ck_completion_expiration_history_validity CHECK (
        (validity_type = 'INDEFINITE' AND validity_value IS NULL AND recalculated_expiration_date IS NULL)
        OR (validity_type IN ('DAYS', 'MONTHS') AND validity_value > 0 AND recalculated_expiration_date IS NOT NULL)
    )
);

CREATE INDEX idx_completion_expiration_history_completion
    ON completion_expiration_history (organization_id, completion_id, created_at DESC, id DESC);

CREATE FUNCTION prevent_phase_4_slice_b_history_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'phase 4 slice B history records are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_assessment_attempts_immutable
    BEFORE UPDATE OR DELETE ON assessment_attempts
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_4_slice_b_history_mutation();
CREATE TRIGGER trg_attempt_answers_immutable
    BEFORE UPDATE OR DELETE ON attempt_answers
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_4_slice_b_history_mutation();
CREATE TRIGGER trg_training_completions_immutable
    BEFORE UPDATE OR DELETE ON training_completions
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_4_slice_b_history_mutation();
CREATE TRIGGER trg_completion_expiration_history_immutable
    BEFORE UPDATE OR DELETE ON completion_expiration_history
    FOR EACH ROW EXECUTE FUNCTION prevent_phase_4_slice_b_history_mutation();
