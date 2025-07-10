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
    metadata JSONB
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_message_audit_correlation ON message_audit(correlation_id);
CREATE INDEX idx_message_audit_status ON message_audit(status);
CREATE INDEX idx_message_state_correlation ON message_state(message_id);
CREATE INDEX idx_message_state_status ON message_state(current_status);

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
