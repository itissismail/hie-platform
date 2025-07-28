--DROP SCHEMA public CASCADE;
--CREATE SCHEMA public;
-- GRANT ALL ON SCHEMA public TO hie_user;
-- GRANT ALL ON SCHEMA public TO hie_user;
-- Create audit table
create TABLE message_audit (
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
    response_payload TEXT,
    previous_message_id UUID ,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create message state table
create TABLE message_state (
    id BIGSERIAL PRIMARY KEY,
    message_id UUID UNIQUE NOT NULL,
    correlation_id UUID NOT NULL,
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
create index idx_message_audit_correlation on message_audit(correlation_id);
create index idx_message_audit_status on message_audit(status);
create index idx_message_audit_message_id on message_audit(message_id);
create index idx_message_audit_service_name on message_audit(service_name);
create index idx_message_audit_step_name on message_audit(step_name);
create index idx_message_audit_created_at on message_audit(created_at);
create index idx_message_state_correlation on message_state(message_id);
create index idx_message_state_status on message_state(current_status);
create index idx_message_state_updated_at on message_state(updated_at);
create index idx_message_state_patient_id on message_state(patient_id);

-- Create quarantine table
create TABLE quarantine_messages (
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
create TABLE clients (
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
create index idx_clients_client_id on clients(client_id);
create index idx_clients_active on clients(is_active);

-- Create sequence for step ordering
create sequence IF NOT EXISTS audit_step_sequence START 1;

