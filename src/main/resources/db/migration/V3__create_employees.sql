CREATE UNIQUE INDEX uk_units_id_organization
    ON units (id, organization_id);
CREATE UNIQUE INDEX uk_sectors_id_unit_organization
    ON sectors (id, unit_id, organization_id);
CREATE UNIQUE INDEX uk_jobs_id_organization
    ON jobs (id, organization_id);

CREATE TABLE employees (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(150) NOT NULL,
    registration VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    job_id UUID NOT NULL,
    sector_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    photo_url VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_employees_job_organization
        FOREIGN KEY (job_id, organization_id) REFERENCES jobs (id, organization_id),
    CONSTRAINT fk_employees_sector_unit_organization
        FOREIGN KEY (sector_id, unit_id, organization_id)
        REFERENCES sectors (id, unit_id, organization_id),
    CONSTRAINT fk_employees_unit_organization
        FOREIGN KEY (unit_id, organization_id) REFERENCES units (id, organization_id)
);

CREATE UNIQUE INDEX uk_employees_organization_registration
    ON employees (organization_id, LOWER(registration));
CREATE INDEX idx_employees_organization_status
    ON employees (organization_id, status);
CREATE INDEX idx_employees_unit
    ON employees (unit_id);
CREATE INDEX idx_employees_sector
    ON employees (sector_id);
CREATE INDEX idx_employees_job
    ON employees (job_id);
