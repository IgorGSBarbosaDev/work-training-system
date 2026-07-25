CREATE UNIQUE INDEX uk_employees_id_organization
    ON employees (id, organization_id);
CREATE UNIQUE INDEX uk_sectors_id_organization
    ON sectors (id, organization_id);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'SUPERVISOR', 'EMPLOYEE')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    employee_id UUID,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_users_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT ck_employee_role_link
        CHECK (role <> 'EMPLOYEE' OR employee_id IS NOT NULL)
);

CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));
CREATE UNIQUE INDEX uk_users_id_organization ON users (id, organization_id);
CREATE UNIQUE INDEX uk_users_employee ON users (employee_id) WHERE employee_id IS NOT NULL;
CREATE INDEX idx_users_organization_role_status ON users (organization_id, role, status);

CREATE TABLE login_attempt_states (
    id UUID PRIMARY KEY,
    email_hash VARCHAR(64) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_login_attempt_states_email_hash ON login_attempt_states (email_hash);
CREATE INDEX idx_login_attempt_states_locked_until ON login_attempt_states (locked_until)
    WHERE locked_until IS NOT NULL;

CREATE TABLE refresh_token_families (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revocation_reason VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_refresh_token_families_user ON refresh_token_families (user_id);
CREATE INDEX idx_refresh_token_families_expiry ON refresh_token_families (expires_at);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES refresh_token_families (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_id UUID REFERENCES refresh_tokens (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expires_at);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_password_reset_tokens_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_expiry ON password_reset_tokens (expires_at);

CREATE TABLE user_permissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    permission VARCHAR(64) NOT NULL CHECK (permission IN ('ASSIGN_TRAINING')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_permissions UNIQUE (user_id, permission)
);

CREATE TABLE access_scope_grants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    scope_type VARCHAR(16) NOT NULL CHECK (scope_type IN ('UNIT', 'SECTOR', 'EMPLOYEE')),
    unit_id UUID,
    sector_id UUID,
    employee_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_scope_user_organization
        FOREIGN KEY (user_id, organization_id) REFERENCES users (id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_scope_unit_organization
        FOREIGN KEY (unit_id, organization_id) REFERENCES units (id, organization_id),
    CONSTRAINT fk_scope_sector_organization
        FOREIGN KEY (sector_id, organization_id) REFERENCES sectors (id, organization_id),
    CONSTRAINT fk_scope_employee_organization
        FOREIGN KEY (employee_id, organization_id) REFERENCES employees (id, organization_id),
    CONSTRAINT ck_scope_exact_target CHECK (
        (scope_type = 'UNIT' AND unit_id IS NOT NULL AND sector_id IS NULL AND employee_id IS NULL)
        OR (scope_type = 'SECTOR' AND unit_id IS NULL AND sector_id IS NOT NULL AND employee_id IS NULL)
        OR (scope_type = 'EMPLOYEE' AND unit_id IS NULL AND sector_id IS NULL AND employee_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_scope_grants_unit ON access_scope_grants (user_id, unit_id)
    WHERE scope_type = 'UNIT';
CREATE UNIQUE INDEX uk_scope_grants_sector ON access_scope_grants (user_id, sector_id)
    WHERE scope_type = 'SECTOR';
CREATE UNIQUE INDEX uk_scope_grants_employee ON access_scope_grants (user_id, employee_id)
    WHERE scope_type = 'EMPLOYEE';
CREATE INDEX idx_scope_grants_user_active ON access_scope_grants (user_id, active);
