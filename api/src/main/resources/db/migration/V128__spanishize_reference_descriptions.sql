-- ============================================================================
-- V128: Spanishize lingering English/mixed reference-data text (defense-in-depth)
-- ============================================================================
-- The UI now renders all coded reference data through i18n keys (see
-- docs/architecture/I18N.md), but the DB rows themselves were seeded in English
-- by historical migrations (V005/V015/V031/V021/V046/V040/V119, …). This forward,
-- idempotent migration converges those rows to Spanish so the i18n FALLBACK, the
-- PDF export path, and any direct API consumer also read Spanish — and removes the
-- old seed/migration English-vs-Spanish drift.
--
-- Every UPDATE is guarded by the stable key (code, or name for the admin-renamable
-- categories), so re-application simply re-sets the same value (no-op). Historical
-- migrations keep their original English text and Flyway checksums untouched.
-- ============================================================================

-- ---- roles.description -----------------------------------------------------
UPDATE roles SET description = 'Acceso total al sistema con todos los permisos' WHERE code = 'ADMINISTRADOR';
UPDATE roles SET description = 'Usuario básico con permisos limitados' WHERE code = 'USUARIO';
UPDATE roles SET description = 'Personal de recepción y registro' WHERE code = 'PERSONAL_ADMINISTRATIVO';
UPDATE roles SET description = 'Médicos' WHERE code = 'MEDICO';
UPDATE roles SET description = 'Personal de enfermería' WHERE code = 'ENFERMERO';
UPDATE roles SET description = 'Jefe del departamento de enfermería' WHERE code = 'JEFE_ENFERMERIA';
UPDATE roles SET description = 'Profesionales de salud mental' WHERE code = 'PSICOLOGO';
UPDATE roles SET description = 'Médico residente a cargo de las admisiones' WHERE code = 'MEDICO_RESIDENTE';
UPDATE roles SET description = 'Auxiliar de enfermería — solo notas y signos vitales' WHERE code = 'AUXILIAR_ENFERMERIA';
UPDATE roles SET description = 'Mantenimiento — gestiona bodegas de mantenimiento, traslada insumos y carga consumibles no médicos' WHERE code = 'MANTENIMIENTO';

-- ---- permissions.name + permissions.description ----------------------------
UPDATE permissions SET name = 'Crear Usuario', description = 'Crear nuevas cuentas de usuario' WHERE code = 'user:create';
UPDATE permissions SET name = 'Ver Usuario', description = 'Ver detalles de usuario' WHERE code = 'user:read';
UPDATE permissions SET name = 'Actualizar Usuario', description = 'Modificar información de usuario' WHERE code = 'user:update';
UPDATE permissions SET name = 'Eliminar Usuario', description = 'Eliminar cuentas de usuario' WHERE code = 'user:delete';
UPDATE permissions SET name = 'Restablecer Contraseña', description = 'Restablecer la contraseña de cualquier usuario' WHERE code = 'user:reset-password';
UPDATE permissions SET name = 'Listar Usuarios Eliminados', description = 'Ver cuentas de usuario eliminadas' WHERE code = 'user:list-deleted';
UPDATE permissions SET name = 'Restaurar Usuario', description = 'Restaurar cuentas de usuario eliminadas' WHERE code = 'user:restore';
UPDATE permissions SET name = 'Crear Rol', description = 'Crear nuevos roles' WHERE code = 'role:create';
UPDATE permissions SET name = 'Ver Rol', description = 'Ver detalles de rol' WHERE code = 'role:read';
UPDATE permissions SET name = 'Actualizar Rol', description = 'Modificar información de rol' WHERE code = 'role:update';
UPDATE permissions SET name = 'Eliminar Rol', description = 'Eliminar roles que no son del sistema' WHERE code = 'role:delete';
UPDATE permissions SET name = 'Asignar Permisos', description = 'Modificar los permisos de un rol' WHERE code = 'role:assign-permissions';
UPDATE permissions SET name = 'Ver Auditoría', description = 'Ver entradas del registro de auditoría' WHERE code = 'audit:read';
UPDATE permissions SET name = 'Crear Paciente', description = 'Registrar nuevos pacientes' WHERE code = 'patient:create';
UPDATE permissions SET name = 'Ver Paciente', description = 'Ver información del paciente' WHERE code = 'patient:read';
UPDATE permissions SET name = 'Actualizar Paciente', description = 'Modificar información del paciente' WHERE code = 'patient:update';
UPDATE permissions SET name = 'Subir Documento de Identidad', description = 'Subir el documento de identidad del paciente' WHERE code = 'patient:upload-id';
UPDATE permissions SET name = 'Ver Documento de Identidad', description = 'Ver el documento de identidad del paciente' WHERE code = 'patient:view-id';
UPDATE permissions SET name = 'Eliminar Paciente', description = 'Eliminar pacientes (eliminación lógica)' WHERE code = 'patient:delete';
UPDATE permissions SET name = 'Crear Código de Triage', description = 'Crear nuevos códigos de triage' WHERE code = 'triage-code:create';
UPDATE permissions SET name = 'Ver Código de Triage', description = 'Ver códigos de triage' WHERE code = 'triage-code:read';
UPDATE permissions SET name = 'Actualizar Código de Triage', description = 'Modificar códigos de triage' WHERE code = 'triage-code:update';
UPDATE permissions SET name = 'Eliminar Código de Triage', description = 'Eliminar códigos de triage' WHERE code = 'triage-code:delete';
UPDATE permissions SET name = 'Crear Habitación', description = 'Crear nuevas habitaciones' WHERE code = 'room:create';
UPDATE permissions SET name = 'Ver Habitación', description = 'Ver habitaciones' WHERE code = 'room:read';
UPDATE permissions SET name = 'Actualizar Habitación', description = 'Modificar habitaciones' WHERE code = 'room:update';
UPDATE permissions SET name = 'Eliminar Habitación', description = 'Eliminar habitaciones' WHERE code = 'room:delete';
UPDATE permissions SET name = 'Ver Ocupación de Camas', description = 'Ver la pantalla de ocupación de camas del hospital' WHERE code = 'room:occupancy-view';
UPDATE permissions SET name = 'Crear Admisión', description = 'Registrar nuevas admisiones' WHERE code = 'admission:create';
UPDATE permissions SET name = 'Ver Admisión', description = 'Ver admisiones' WHERE code = 'admission:read';
UPDATE permissions SET name = 'Actualizar Admisión', description = 'Modificar admisiones' WHERE code = 'admission:update';
UPDATE permissions SET name = 'Eliminar Admisión', description = 'Eliminar admisiones' WHERE code = 'admission:delete';
UPDATE permissions SET name = 'Subir Consentimiento', description = 'Subir documentos de consentimiento' WHERE code = 'admission:upload-consent';
UPDATE permissions SET name = 'Ver Consentimiento', description = 'Ver documentos de consentimiento' WHERE code = 'admission:view-consent';
UPDATE permissions SET name = 'Ver Documentos de Admisión', description = 'Ver documentos de las admisiones' WHERE code = 'admission:view-documents';
UPDATE permissions SET name = 'Subir Documentos de Admisión', description = 'Subir documentos a las admisiones' WHERE code = 'admission:upload-documents';
UPDATE permissions SET name = 'Descargar Documentos de Admisión', description = 'Descargar documentos de las admisiones' WHERE code = 'admission:download-documents';
UPDATE permissions SET name = 'Eliminar Documentos de Admisión', description = 'Eliminar documentos de las admisiones' WHERE code = 'admission:delete-documents';
UPDATE permissions SET name = 'Exportar PDF de Admisión', description = 'Generar y descargar el PDF completo de la admisión' WHERE code = 'admission:export-pdf';
UPDATE permissions SET name = 'Crear Tipo de Documento', description = 'Crear nuevos tipos de documento' WHERE code = 'document-type:create';
UPDATE permissions SET name = 'Ver Tipo de Documento', description = 'Ver tipos de documento' WHERE code = 'document-type:read';
UPDATE permissions SET name = 'Actualizar Tipo de Documento', description = 'Modificar tipos de documento' WHERE code = 'document-type:update';
UPDATE permissions SET name = 'Eliminar Tipo de Documento', description = 'Eliminar tipos de documento' WHERE code = 'document-type:delete';
UPDATE permissions SET name = 'Crear Historia Clínica', description = 'Crear la historia clínica de las admisiones' WHERE code = 'clinical-history:create';
UPDATE permissions SET name = 'Ver Historia Clínica', description = 'Ver la historia clínica de las admisiones' WHERE code = 'clinical-history:read';
UPDATE permissions SET name = 'Actualizar Historia Clínica', description = 'Modificar la historia clínica de las admisiones' WHERE code = 'clinical-history:update';
UPDATE permissions SET name = 'Crear Nota de Evolución', description = 'Crear notas de evolución para las admisiones' WHERE code = 'progress-note:create';
UPDATE permissions SET name = 'Ver Nota de Evolución', description = 'Ver notas de evolución de las admisiones' WHERE code = 'progress-note:read';
UPDATE permissions SET name = 'Actualizar Nota de Evolución', description = 'Modificar notas de evolución de las admisiones' WHERE code = 'progress-note:update';
UPDATE permissions SET name = 'Crear Orden Médica', description = 'Crear órdenes médicas para las admisiones' WHERE code = 'medical-order:create';
UPDATE permissions SET name = 'Ver Orden Médica', description = 'Ver órdenes médicas de las admisiones' WHERE code = 'medical-order:read';
UPDATE permissions SET name = 'Actualizar Orden Médica', description = 'Modificar órdenes médicas de las admisiones' WHERE code = 'medical-order:update';
UPDATE permissions SET name = 'Suspender Orden Médica', description = 'Suspender órdenes médicas activas' WHERE code = 'medical-order:discontinue';
UPDATE permissions SET name = 'Subir Documento de Orden Médica', description = 'Subir documentos a las órdenes médicas' WHERE code = 'medical-order:upload-document';
UPDATE permissions SET name = 'Eliminar Documento de Orden Médica', description = 'Eliminar documentos de las órdenes médicas' WHERE code = 'medical-order:delete-document';
UPDATE permissions SET name = 'Autorizar Orden Médica', description = 'Aprobar o rechazar órdenes médicas' WHERE code = 'medical-order:authorize';
UPDATE permissions SET name = 'Autorización de Emergencia de Orden Médica', description = 'Autoautorización del médico para escenarios de crisis o fuera de horario' WHERE code = 'medical-order:emergency-authorize';
UPDATE permissions SET name = 'Marcar Orden Médica En Proceso', description = 'Marcar una orden con resultados como ejecutada (muestra tomada / referido / administrado)' WHERE code = 'medical-order:mark-in-progress';
UPDATE permissions SET name = 'Ver Nota de Enfermería', description = 'Ver notas de enfermería' WHERE code = 'nursing-note:read';
UPDATE permissions SET name = 'Crear Nota de Enfermería', description = 'Crear notas de enfermería' WHERE code = 'nursing-note:create';
UPDATE permissions SET name = 'Actualizar Nota de Enfermería', description = 'Actualizar notas de enfermería' WHERE code = 'nursing-note:update';
UPDATE permissions SET name = 'Ver Signos Vitales', description = 'Ver signos vitales' WHERE code = 'vital-sign:read';
UPDATE permissions SET name = 'Registrar Signos Vitales', description = 'Registrar signos vitales' WHERE code = 'vital-sign:create';
UPDATE permissions SET name = 'Actualizar Signos Vitales', description = 'Actualizar signos vitales' WHERE code = 'vital-sign:update';
UPDATE permissions SET name = 'Crear Actividad de Psicoterapia', description = 'Registrar actividades de psicoterapia' WHERE code = 'psychotherapy-activity:create';
UPDATE permissions SET name = 'Ver Actividad de Psicoterapia', description = 'Ver actividades de psicoterapia' WHERE code = 'psychotherapy-activity:read';
UPDATE permissions SET name = 'Eliminar Actividad de Psicoterapia', description = 'Eliminar actividades de psicoterapia' WHERE code = 'psychotherapy-activity:delete';
UPDATE permissions SET name = 'Crear Categoría de Psicoterapia', description = 'Crear categorías de actividades' WHERE code = 'psychotherapy-category:create';
UPDATE permissions SET name = 'Ver Categoría de Psicoterapia', description = 'Ver categorías de actividades' WHERE code = 'psychotherapy-category:read';
UPDATE permissions SET name = 'Actualizar Categoría de Psicoterapia', description = 'Modificar categorías de actividades' WHERE code = 'psychotherapy-category:update';
UPDATE permissions SET name = 'Eliminar Categoría de Psicoterapia', description = 'Eliminar categorías de actividades' WHERE code = 'psychotherapy-category:delete';
UPDATE permissions SET name = 'Crear Categoría de Inventario', description = 'Crear categorías de inventario' WHERE code = 'inventory-category:create';
UPDATE permissions SET name = 'Ver Categoría de Inventario', description = 'Ver categorías de inventario' WHERE code = 'inventory-category:read';
UPDATE permissions SET name = 'Actualizar Categoría de Inventario', description = 'Modificar categorías de inventario' WHERE code = 'inventory-category:update';
UPDATE permissions SET name = 'Eliminar Categoría de Inventario', description = 'Eliminar categorías de inventario' WHERE code = 'inventory-category:delete';
UPDATE permissions SET name = 'Crear Artículo de Inventario', description = 'Crear artículos de inventario' WHERE code = 'inventory-item:create';
UPDATE permissions SET name = 'Ver Artículo de Inventario', description = 'Ver artículos de inventario' WHERE code = 'inventory-item:read';
UPDATE permissions SET name = 'Actualizar Artículo de Inventario', description = 'Modificar artículos de inventario' WHERE code = 'inventory-item:update';
UPDATE permissions SET name = 'Eliminar Artículo de Inventario', description = 'Eliminar artículos de inventario' WHERE code = 'inventory-item:delete';
UPDATE permissions SET name = 'Crear Movimiento de Inventario', description = 'Registrar movimientos de existencias' WHERE code = 'inventory-movement:create';
UPDATE permissions SET name = 'Ver Movimiento de Inventario', description = 'Ver el historial de movimientos de existencias' WHERE code = 'inventory-movement:read';
UPDATE permissions SET name = 'Ver Lotes de Inventario', description = 'Listar y ver los lotes de un artículo' WHERE code = 'inventory-lot:read';
UPDATE permissions SET name = 'Crear Lote de Inventario', description = 'Registrar un nuevo lote (entrada)' WHERE code = 'inventory-lot:create';
UPDATE permissions SET name = 'Actualizar Lote de Inventario', description = 'Editar metadatos del lote, retirar un lote, anular FEFO' WHERE code = 'inventory-lot:update';
UPDATE permissions SET name = 'Ver Facturación', description = 'Ver cargos y saldos del paciente' WHERE code = 'billing:read';
UPDATE permissions SET name = 'Crear Cargo de Facturación', description = 'Crear cargos manuales al paciente' WHERE code = 'billing:create';
UPDATE permissions SET name = 'Crear Ajuste de Facturación', description = 'Crear ajustes y créditos de facturación' WHERE code = 'billing:adjust';
UPDATE permissions SET name = 'Configurar Facturación', description = 'Configurar parámetros de facturación a nivel de sistema' WHERE code = 'billing:configure';
UPDATE permissions SET name = 'Ver Factura', description = 'Ver facturas' WHERE code = 'invoice:read';
UPDATE permissions SET name = 'Crear Factura', description = 'Generar facturas' WHERE code = 'invoice:create';
UPDATE permissions SET name = 'Registrar Administración de Medicamento', description = 'Registrar una administración de medicamento' WHERE code = 'medication-administration:create';
UPDATE permissions SET name = 'Ver Administraciones de Medicamentos', description = 'Ver registros de administración de medicamentos' WHERE code = 'medication-administration:read';
UPDATE permissions SET name = 'Ver Medicamentos', description = 'Ver el catálogo estructurado de medicamentos' WHERE code = 'medication:read';
UPDATE permissions SET name = 'Crear Medicamento', description = 'Asociar detalles de medicamento a un artículo de tipo medicamento' WHERE code = 'medication:create';
UPDATE permissions SET name = 'Actualizar Medicamento', description = 'Editar los campos de detalle del medicamento' WHERE code = 'medication:update';
UPDATE permissions SET name = 'Ver Reporte de Vencimientos', description = 'Ver el panel de vencimientos por colores' WHERE code = 'medication:expiry-report';
UPDATE permissions SET name = 'Ver Tesorería', description = 'Ver registros de tesorería' WHERE code = 'treasury:read';
UPDATE permissions SET name = 'Editar Tesorería', description = 'Crear y editar registros de tesorería' WHERE code = 'treasury:write';
UPDATE permissions SET name = 'Eliminar Tesorería', description = 'Eliminar registros de tesorería' WHERE code = 'treasury:delete';
UPDATE permissions SET name = 'Configurar Tesorería', description = 'Configurar cuentas bancarias y empleados' WHERE code = 'treasury:configure';
UPDATE permissions SET name = 'Conciliar Tesorería', description = 'Cargar y conciliar estados de cuenta bancarios' WHERE code = 'treasury:reconcile';
UPDATE permissions SET name = 'Reportes de Tesorería', description = 'Ver reportes financieros' WHERE code = 'treasury:report';
UPDATE permissions SET name = 'Ver Bodegas', description = 'Listar bodegas y ver existencias por bodega' WHERE code = 'warehouse:read';
UPDATE permissions SET name = 'Crear Bodega', description = 'Crear una nueva bodega' WHERE code = 'warehouse:create';
UPDATE permissions SET name = 'Actualizar Bodega', description = 'Renombrar / activar / desactivar una bodega' WHERE code = 'warehouse:update';
UPDATE permissions SET name = 'Eliminar Bodega', description = 'Eliminar una bodega (solo si no tiene existencias)' WHERE code = 'warehouse:delete';
UPDATE permissions SET name = 'Crear Traslado', description = 'Emitir un traslado desde una bodega a la que tiene acceso' WHERE code = 'warehouse-transfer:create';
UPDATE permissions SET name = 'Ver Traslados', description = 'Ver el historial de traslados' WHERE code = 'warehouse-transfer:read';
UPDATE permissions SET name = 'Recibir Traslado', description = 'Confirmar la recepción de un traslado entrante' WHERE code = 'warehouse-transfer:receive';
UPDATE permissions SET name = 'Crear Cargo de Bodega', description = 'Cargar un consumible no médico a una admisión' WHERE code = 'warehouse-charge:create';
UPDATE permissions SET name = 'Ver Catálogo de Laboratorio', description = 'Ver proveedores, pruebas, pruebas por proveedor y paneles de laboratorio' WHERE code = 'lab-catalog:read';
UPDATE permissions SET name = 'Gestionar Catálogo de Laboratorio', description = 'CRUD completo de proveedores, pruebas, pruebas por proveedor y paneles de laboratorio' WHERE code = 'lab-catalog:manage';

-- ---- document_types.name + document_types.description ----------------------
UPDATE document_types SET name = 'Consentimiento de Admisión', description = 'Formulario general de consentimiento de admisión' WHERE code = 'CONSENT_ADMISSION';
UPDATE document_types SET name = 'Consentimiento de Aislamiento', description = 'Consentimiento para aislamiento/reclusión del paciente' WHERE code = 'CONSENT_ISOLATION';
UPDATE document_types SET name = 'Consentimiento de Inmovilización', description = 'Consentimiento para sujeción física (inmovilización)' WHERE code = 'CONSENT_RESTRAINT';
UPDATE document_types SET name = 'Consentimiento de Sedación', description = 'Consentimiento para sedación/administración de medicamentos' WHERE code = 'CONSENT_SEDATION';
UPDATE document_types SET name = 'Lista de Inventario', description = 'Inventario escrito de las pertenencias del paciente' WHERE code = 'INVENTORY_LIST';
UPDATE document_types SET name = 'Fotos de Inventario', description = 'Fotos de las pertenencias del paciente' WHERE code = 'INVENTORY_PHOTO';
UPDATE document_types SET name = 'Otro Documento', description = 'Otros documentos relacionados con la admisión' WHERE code = 'OTHER';

-- ---- triage_codes.description ----------------------------------------------
UPDATE triage_codes SET description = 'Crítico - Atención inmediata requerida' WHERE code = 'A';
UPDATE triage_codes SET description = 'Urgente - Requiere atención pronta' WHERE code = 'B';
UPDATE triage_codes SET description = 'Menos Urgente - Puede esperar para atención' WHERE code = 'C';
UPDATE triage_codes SET description = 'No Urgente - Problemas menores' WHERE code = 'D';
UPDATE triage_codes SET description = 'Referencia - Admisión programada' WHERE code = 'E';

-- ---- warehouses.description ------------------------------------------------
UPDATE warehouses SET description = 'Bodega maestra / de recepción. Las entregas llegan aquí.' WHERE code = 'ADMINISTRACION';
UPDATE warehouses SET description = 'Bodega de enfermería.' WHERE code = 'ENFERMERIA';
UPDATE warehouses SET description = 'Bodega de mantenimiento 1.' WHERE code = 'MANTENIMIENTO_1';
UPDATE warehouses SET description = 'Bodega de mantenimiento 2.' WHERE code = 'MANTENIMIENTO_2';
UPDATE warehouses SET description = 'Bodega de cocina.' WHERE code = 'COCINA';
UPDATE warehouses SET description = 'Bodega de psicología.' WHERE code = 'PSICOLOGIA';

-- ---- inventory_categories.description (admin-renamable; keyed by name) ------
UPDATE inventory_categories SET description = 'Medicamentos e insumos farmacéuticos' WHERE name = 'Medicamentos';
UPDATE inventory_categories SET description = 'Materiales y equipo' WHERE name = 'Material y Equipo';
UPDATE inventory_categories SET description = 'Servicios e insumos de laboratorio' WHERE name = 'Laboratorios';
UPDATE inventory_categories SET description = 'Servicios hospitalarios' WHERE name = 'Servicios';
UPDATE inventory_categories SET description = 'Servicios de personal especializado' WHERE name = 'Personal Especial';
UPDATE inventory_categories SET description = 'Ingredientes de cocina' WHERE name = 'Ingredientes de Cocina';
UPDATE inventory_categories SET description = 'Alimentos servidos a los pacientes' WHERE name = 'Alimentación';

-- ---- psychotherapy_categories.description (admin-renamable; keyed by name) --
UPDATE psychotherapy_categories SET description = 'Talleres terapéuticos grupales' WHERE name = 'Taller';
UPDATE psychotherapy_categories SET description = 'Sesiones individuales de psicoterapia' WHERE name = 'Sesión individual';
UPDATE psychotherapy_categories SET description = 'Terapia asistida con animales' WHERE name = 'Terapia con mascotas';
UPDATE psychotherapy_categories SET description = 'Sesiones de pilates terapéutico' WHERE name = 'Pilates';
UPDATE psychotherapy_categories SET description = 'Sesiones de meditación y mindfulness' WHERE name = 'Meditación guiada';
UPDATE psychotherapy_categories SET description = 'Sesiones de terapia grupal' WHERE name = 'Terapia grupal';
UPDATE psychotherapy_categories SET description = 'Terapia a través del arte y expresión creativa' WHERE name = 'Arte terapia';
UPDATE psychotherapy_categories SET description = 'Terapia a través de la música' WHERE name = 'Musicoterapia';
UPDATE psychotherapy_categories SET description = 'Actividades de terapia ocupacional' WHERE name = 'Terapia ocupacional';
UPDATE psychotherapy_categories SET description = 'Otras actividades terapéuticas' WHERE name = 'Otra';
