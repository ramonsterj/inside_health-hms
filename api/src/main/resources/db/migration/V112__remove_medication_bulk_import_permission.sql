-- V112: Drop the medication:bulk-import permission seeded by V104.
-- The bulk importer surface was removed in favor of the one-shot V111 loader.

DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'medication:bulk-import');

DELETE FROM permissions WHERE code = 'medication:bulk-import';
