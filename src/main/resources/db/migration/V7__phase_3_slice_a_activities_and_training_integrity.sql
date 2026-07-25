CREATE UNIQUE INDEX uk_trainings_id_organization
    ON trainings (id, organization_id);

ALTER TABLE training_versions
    ADD COLUMN organization_id UUID,
    ADD COLUMN training_name_snapshot VARCHAR(150),
    ADD COLUMN training_code_snapshot VARCHAR(50),
    ADD COLUMN training_description_snapshot VARCHAR(2000),
    ADD COLUMN training_category_snapshot VARCHAR(150),
    ADD COLUMN regulatory_standard_snapshot BOOLEAN,
    ADD COLUMN content_snapshot JSONB;

UPDATE training_versions version
SET organization_id = training.organization_id,
    training_name_snapshot = training.name,
    training_code_snapshot = training.code,
    training_description_snapshot = training.description,
    training_category_snapshot = training.category,
    regulatory_standard_snapshot = training.is_regulatory_standard
FROM trainings training
WHERE training.id = version.training_id;

UPDATE training_versions version
SET content_snapshot = jsonb_build_object(
    'training', jsonb_build_object(
        'name', version.training_name_snapshot,
        'code', version.training_code_snapshot,
        'description', version.training_description_snapshot,
        'category', version.training_category_snapshot,
        'regulatoryStandard', version.regulatory_standard_snapshot
    ),
    'version', to_jsonb(version) - 'content_snapshot',
    'modules', COALESCE((
        SELECT jsonb_agg(
            to_jsonb(module_row) || jsonb_build_object(
                'videos', COALESCE((
                    SELECT jsonb_agg(to_jsonb(video_row) ORDER BY video_row.display_order)
                    FROM training_videos video_row
                    WHERE video_row.module_id = module_row.id
                ), '[]'::jsonb),
                'questionnaire', (
                    SELECT to_jsonb(questionnaire_row) || jsonb_build_object(
                        'questions', COALESCE((
                            SELECT jsonb_agg(
                                to_jsonb(question_row) || jsonb_build_object(
                                    'options', COALESCE((
                                        SELECT jsonb_agg(to_jsonb(option_row) ORDER BY option_row.display_order)
                                        FROM answer_options option_row
                                        WHERE option_row.question_id = question_row.id
                                    ), '[]'::jsonb)
                                ) ORDER BY question_row.display_order
                            )
                            FROM questions question_row
                            WHERE question_row.questionnaire_id = questionnaire_row.id
                        ), '[]'::jsonb)
                    )
                    FROM questionnaires questionnaire_row
                    WHERE questionnaire_row.module_id = module_row.id
                )
            ) ORDER BY module_row.display_order
        )
        FROM training_modules module_row
        WHERE module_row.training_version_id = version.id
    ), '[]'::jsonb)
)
WHERE version.status IN ('PUBLISHED', 'ARCHIVED');

UPDATE training_versions
SET passing_score = 70
WHERE passing_score < 70;

UPDATE questionnaires
SET passing_score = 70
WHERE passing_score < 70;

ALTER TABLE training_versions
    ALTER COLUMN organization_id SET NOT NULL,
    ALTER COLUMN training_name_snapshot SET NOT NULL,
    ALTER COLUMN training_code_snapshot SET NOT NULL,
    ALTER COLUMN regulatory_standard_snapshot SET NOT NULL,
    ADD CONSTRAINT fk_training_versions_training_organization
        FOREIGN KEY (training_id, organization_id) REFERENCES trainings (id, organization_id),
    ADD CONSTRAINT ck_training_versions_passing_score_mvp CHECK (passing_score BETWEEN 70 AND 100),
    ADD CONSTRAINT ck_training_versions_published_snapshot CHECK (
        status <> 'PUBLISHED' OR (published_at IS NOT NULL AND content_snapshot IS NOT NULL)
    );

ALTER TABLE questionnaires
    ADD CONSTRAINT ck_questionnaires_passing_score_mvp CHECK (passing_score BETWEEN 70 AND 100);

CREATE UNIQUE INDEX uk_training_versions_id_training_organization
    ON training_versions (id, training_id, organization_id);
CREATE INDEX idx_training_versions_organization_status
    ON training_versions (organization_id, status);
CREATE UNIQUE INDEX uk_answer_options_one_active_correct
    ON answer_options (question_id)
    WHERE status = 'ACTIVE' AND is_correct;

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_activities_id_organization ON activities (id, organization_id);
CREATE UNIQUE INDEX uk_activities_organization_name ON activities (organization_id, LOWER(name));
CREATE INDEX idx_activities_organization_status_name ON activities (organization_id, status, name);

CREATE TABLE job_activities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    job_id UUID NOT NULL,
    activity_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    unlinked_at TIMESTAMP WITH TIME ZONE,
    linked_by_user_id UUID NOT NULL,
    unlinked_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_job_activities_job_organization
        FOREIGN KEY (job_id, organization_id) REFERENCES jobs (id, organization_id),
    CONSTRAINT fk_job_activities_activity_organization
        FOREIGN KEY (activity_id, organization_id) REFERENCES activities (id, organization_id),
    CONSTRAINT ck_job_activities_dates CHECK (
        (status = 'ACTIVE' AND unlinked_at IS NULL AND unlinked_by_user_id IS NULL)
        OR (status = 'INACTIVE' AND unlinked_at IS NOT NULL AND unlinked_by_user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_job_activities_active
    ON job_activities (organization_id, job_id, activity_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_job_activities_id_organization ON job_activities (id, organization_id);
CREATE INDEX idx_job_activities_job_status ON job_activities (organization_id, job_id, status);
CREATE INDEX idx_job_activities_activity_status ON job_activities (organization_id, activity_id, status);

CREATE TABLE employee_activities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    employee_id UUID NOT NULL,
    activity_id UUID NOT NULL,
    origin VARCHAR(16) NOT NULL CHECK (origin IN ('JOB', 'MANUAL')),
    source_job_activity_id UUID,
    reason VARCHAR(1000),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    assigned_by_user_id UUID NOT NULL,
    deactivated_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_employee_activities_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT fk_employee_activities_activity_organization
        FOREIGN KEY (activity_id, organization_id) REFERENCES activities (id, organization_id),
    CONSTRAINT fk_employee_activities_job_link_organization
        FOREIGN KEY (source_job_activity_id, organization_id) REFERENCES job_activities (id, organization_id),
    CONSTRAINT ck_employee_activities_origin_source CHECK (
        (origin = 'JOB' AND source_job_activity_id IS NOT NULL AND reason IS NULL)
        OR (origin = 'MANUAL' AND source_job_activity_id IS NULL)
    ),
    CONSTRAINT ck_employee_activities_dates CHECK (
        (status = 'ACTIVE' AND deactivated_at IS NULL AND deactivated_by_user_id IS NULL)
        OR (status = 'INACTIVE' AND deactivated_at IS NOT NULL AND deactivated_by_user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_employee_activities_active_manual
    ON employee_activities (organization_id, employee_id, activity_id)
    WHERE status = 'ACTIVE' AND origin = 'MANUAL';
CREATE UNIQUE INDEX uk_employee_activities_active_job_source
    ON employee_activities (organization_id, employee_id, activity_id, source_job_activity_id)
    WHERE status = 'ACTIVE' AND origin = 'JOB';
CREATE INDEX idx_employee_activities_employee_status
    ON employee_activities (organization_id, employee_id, status, activity_id);
CREATE INDEX idx_employee_activities_activity_status
    ON employee_activities (organization_id, activity_id, status, employee_id);
CREATE INDEX idx_employee_activities_source_job_link
    ON employee_activities (source_job_activity_id) WHERE status = 'ACTIVE';

CREATE TABLE activity_training_requirements (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    activity_id UUID NOT NULL,
    training_id UUID NOT NULL,
    version_policy VARCHAR(32) NOT NULL CHECK (version_policy IN ('LATEST_PUBLISHED', 'FIXED_VERSION')),
    training_version_id UUID,
    required BOOLEAN NOT NULL DEFAULT TRUE CHECK (required),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    linked_by_user_id UUID NOT NULL,
    deactivated_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_requirements_activity_organization
        FOREIGN KEY (activity_id, organization_id) REFERENCES activities (id, organization_id),
    CONSTRAINT fk_requirements_training_organization
        FOREIGN KEY (training_id, organization_id) REFERENCES trainings (id, organization_id),
    CONSTRAINT fk_requirements_version_training_organization
        FOREIGN KEY (training_version_id, training_id, organization_id)
        REFERENCES training_versions (id, training_id, organization_id),
    CONSTRAINT ck_requirements_version_policy CHECK (
        (version_policy = 'LATEST_PUBLISHED' AND training_version_id IS NULL)
        OR (version_policy = 'FIXED_VERSION' AND training_version_id IS NOT NULL)
    ),
    CONSTRAINT ck_requirements_dates CHECK (
        (status = 'ACTIVE' AND deactivated_at IS NULL AND deactivated_by_user_id IS NULL)
        OR (status = 'INACTIVE' AND deactivated_at IS NOT NULL AND deactivated_by_user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_activity_training_requirements_active
    ON activity_training_requirements (organization_id, activity_id, training_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_requirements_activity_status
    ON activity_training_requirements (organization_id, activity_id, status);
CREATE INDEX idx_requirements_training_status
    ON activity_training_requirements (organization_id, training_id, status);
CREATE INDEX idx_requirements_fixed_version
    ON activity_training_requirements (training_version_id) WHERE training_version_id IS NOT NULL;

CREATE FUNCTION prevent_published_training_version_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IN ('PUBLISHED', 'ARCHIVED') AND (
        NEW.training_id IS DISTINCT FROM OLD.training_id
        OR NEW.organization_id IS DISTINCT FROM OLD.organization_id
        OR NEW.version_number IS DISTINCT FROM OLD.version_number
        OR NEW.workload_minutes IS DISTINCT FROM OLD.workload_minutes
        OR NEW.validity_type IS DISTINCT FROM OLD.validity_type
        OR NEW.validity_value IS DISTINCT FROM OLD.validity_value
        OR NEW.passing_score IS DISTINCT FROM OLD.passing_score
        OR NEW.max_attempts IS DISTINCT FROM OLD.max_attempts
        OR NEW.retry_interval_minutes IS DISTINCT FROM OLD.retry_interval_minutes
        OR NEW.published_at IS DISTINCT FROM OLD.published_at
        OR NEW.training_name_snapshot IS DISTINCT FROM OLD.training_name_snapshot
        OR NEW.training_code_snapshot IS DISTINCT FROM OLD.training_code_snapshot
        OR NEW.training_description_snapshot IS DISTINCT FROM OLD.training_description_snapshot
        OR NEW.training_category_snapshot IS DISTINCT FROM OLD.training_category_snapshot
        OR NEW.regulatory_standard_snapshot IS DISTINCT FROM OLD.regulatory_standard_snapshot
        OR NEW.content_snapshot IS DISTINCT FROM OLD.content_snapshot
    ) THEN
        RAISE EXCEPTION 'published training version is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_training_versions_published_immutable
    BEFORE UPDATE ON training_versions
    FOR EACH ROW EXECUTE FUNCTION prevent_published_training_version_mutation();
