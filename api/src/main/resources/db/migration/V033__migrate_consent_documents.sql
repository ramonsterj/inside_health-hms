-- Migrate existing consent documents to new admission_documents table
-- These will be migrated as CONSENT_ADMISSION type
INSERT INTO admission_documents (
    admission_id,
    document_type_id,
    display_name,
    file_name,
    content_type,
    file_size,
    storage_path,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    acd.admission_id,
    (SELECT id FROM document_types WHERE code = 'CONSENT_ADMISSION'),
    acd.file_name,
    acd.file_name,
    acd.content_type,
    acd.file_size,
    acd.storage_path,
    acd.created_at,
    acd.updated_at,
    acd.created_by,
    acd.updated_by
FROM admission_consent_documents acd
WHERE acd.deleted_at IS NULL;
