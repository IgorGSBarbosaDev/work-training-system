CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO organizations (id, name, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Organizacao principal',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

CREATE TABLE units (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_units_organization_name
    ON units (organization_id, LOWER(name));
CREATE UNIQUE INDEX uk_units_organization_code
    ON units (organization_id, LOWER(code))
    WHERE code IS NOT NULL;
CREATE INDEX idx_units_organization_status
    ON units (organization_id, status);

CREATE TABLE sectors (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    unit_id UUID NOT NULL REFERENCES units (id),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sectors_unit_name
    ON sectors (unit_id, LOWER(name));
CREATE UNIQUE INDEX uk_sectors_unit_code
    ON sectors (unit_id, LOWER(code))
    WHERE code IS NOT NULL;
CREATE INDEX idx_sectors_organization_status
    ON sectors (organization_id, status);
CREATE INDEX idx_sectors_unit
    ON sectors (unit_id);

CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_jobs_organization_name
    ON jobs (organization_id, LOWER(name));
CREATE INDEX idx_jobs_organization_status
    ON jobs (organization_id, status);
