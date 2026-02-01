-- V028__add_storage_path_to_document_tables.sql
-- Move file storage from database BYTEA to file system
-- Files will be stored at: {base-path}/patients/{patientId}/{document-type}/{uuid}_{filename}

-- Patient ID Documents
-- Add storage_path column
ALTER TABLE patient_id_documents
    ADD COLUMN storage_path VARCHAR(500);

-- Drop file_data column (no existing production data to migrate)
ALTER TABLE patient_id_documents
    DROP COLUMN file_data;

-- Make storage_path NOT NULL
ALTER TABLE patient_id_documents
    ALTER COLUMN storage_path SET NOT NULL;


-- Admission Consent Documents
-- Add storage_path column
ALTER TABLE admission_consent_documents
    ADD COLUMN storage_path VARCHAR(500);

-- Drop file_data column
ALTER TABLE admission_consent_documents
    DROP COLUMN file_data;

-- Make storage_path NOT NULL
ALTER TABLE admission_consent_documents
    ALTER COLUMN storage_path SET NOT NULL;
