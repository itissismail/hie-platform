-- Create audit table
CREATE TABLE message_audit (
    id BIGSERIAL PRIMARY KEY,
    message_id UUID NOT NULL,
    correlation_id UUID NOT NULL,
    service_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    processing_time_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT,
    step_name VARCHAR(100) NOT NULL,
    step_sequence INTEGER DEFAULT 0,
    request_payload TEXT,
    response_payload TEXT
);

-- Create message state table
CREATE TABLE message_state (
    id BIGSERIAL PRIMARY KEY,
    message_id UUID UNIQUE NOT NULL,
    current_status VARCHAR(20) NOT NULL,
    source_organization VARCHAR(100) NOT NULL,
    message_type VARCHAR(10) NOT NULL,
    patient_id VARCHAR(50),
    global_patient_id VARCHAR(50),
    s3_location VARCHAR(500),
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_processed_by VARCHAR(50),
    total_processing_time_ms BIGINT DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_message_audit_correlation ON message_audit(correlation_id);
CREATE INDEX idx_message_audit_status ON message_audit(status);
CREATE INDEX idx_message_audit_message_id ON message_audit(message_id);
CREATE INDEX idx_message_audit_service_name ON message_audit(service_name);
CREATE INDEX idx_message_audit_step_name ON message_audit(step_name);
CREATE INDEX idx_message_audit_created_at ON message_audit(created_at);
CREATE INDEX idx_message_state_correlation ON message_state(message_id);
CREATE INDEX idx_message_state_status ON message_state(current_status);
CREATE INDEX idx_message_state_updated_at ON message_state(updated_at);
CREATE INDEX idx_message_state_patient_id ON message_state(patient_id);

-- Create quarantine table
CREATE TABLE quarantine_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id UUID NOT NULL,
    reason VARCHAR(255) NOT NULL,
    raw_message TEXT NOT NULL,
    quarantine_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed BOOLEAN DEFAULT FALSE,
    reviewer VARCHAR(100),
    review_date TIMESTAMP,
    review_action VARCHAR(50)
);

-- V1__create_clients_table.sql
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(255) UNIQUE NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    client_roles TEXT NOT NULL, -- Comma-separated roles
    grant_types VARCHAR(255) DEFAULT 'client_credentials',
    scopes VARCHAR(255) DEFAULT 'read,write',
    access_token_validity INTEGER DEFAULT 3600, -- 1 hour
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster lookups
CREATE INDEX idx_clients_client_id ON clients(client_id);
CREATE INDEX idx_clients_active ON clients(is_active);

-- Create sequence for step ordering
CREATE SEQUENCE IF NOT EXISTS audit_step_sequence START 1;

