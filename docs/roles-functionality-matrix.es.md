# Matriz de Roles y Funcionalidades

## Alcance

Esta matriz describe los roles del sistema que vienen definidos actualmente en el repositorio.

Se basa en:

- las validaciones de autorizacion del backend en controladores y servicios
- las asignaciones por defecto de roles en `api/src/main/resources/db/seed/R__seed_01_reset_and_base.sql`
- las migraciones versionadas de roles y permisos en `api/src/main/resources/db/migration`
- las restricciones de rutas y menu en el frontend dentro de `web/src/router/index.ts` y `web/src/layout/AppMenu.vue`

La plataforma permite crear roles personalizados, asi que este documento solo cubre los roles integrados y las asignaciones por defecto que vienen con el proyecto.

## Leyenda

| Valor | Significado |
| --- | --- |
| Full | Puede crear, actualizar, eliminar y administrar el area funcional |
| Work | Puede operar el flujo diario, pero sin administracion completa del sistema |
| Read | Solo puede ver o usar registros existentes |
| Scoped | El acceso tiene restricciones adicionales mas alla del permiso base |
| Self | Solo sobre su propio perfil o cuenta |
| None | No tiene acceso por defecto |
| * | Tiene `user:read` a nivel de API, pero el frontend oculta la pantalla detras de `requiresAdmin` |
| ** | Otorgado por `V084__seed_treasury_permissions.sql` pero se pierde tras reseed — ver seccion Desajustes |

## Matriz

| Funcionalidad | ADMIN | ADMINISTRATIVE_STAFF | DOCTOR | PSYCHOLOGIST | NURSE | CHIEF_NURSE | USER | Notas |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Perfil, contrasena y locale propios | Self | Self | Self | Self | Self | Self | Self | Los endpoints de `/api/users/me` estan disponibles para cualquier usuario autenticado. |
| Administracion de usuarios | Full | None* | None* | None | None* | None* | None* | La pantalla de gestion de usuarios en frontend es solo para admin. |
| Administracion de roles | Full | None | None | None | None | None | None | La pantalla de roles en frontend es solo para admin. |
| Logs de auditoria | Read | None | None | None | None | None | None | La pantalla de auditoria en frontend es solo para admin. |
| Pacientes | Full | Work | Scoped | Scoped | Read | Work | None | Staff puede crear y actualizar. Doctor esta limitado a sus pacientes asignados (medico tratante o consultante) en listado y detalle. Psychologist esta limitado a pacientes con admision activa en listado y detalle; psychologist tambien tiene `patient:update`. |
| Documentos de identificacion del paciente | Full | Work | None | None | None | None | None | Staff puede subir, ver y eliminar porque el borrado reutiliza `patient:upload-id`. |
| Admisiones | Full | Work | Scoped | Scoped | Read | Work | None | Staff y chief nurse pueden actualizar y dar egreso. El listado de admisiones para doctor se limita al medico tratante o consultante. Para psychologist, listados, busquedas y detalle son solo de activas. |
| Exportacion PDF de admision | Full | Work | None | None | None | None | None | `admission:export-pdf` solo se asigna a ADMIN y ADMINISTRATIVE_STAFF en V099. La exportacion se genera de forma sincrona, se transmite una sola vez y se audita con `ADMISSION_EXPORT`. |
| Documento de consentimiento de admision | Full | Work | Read | Read | Read | Read | None | El endpoint de descarga solo se protege por permiso. |
| Documentos de admision | Full | Work | Read | Read | Read | Read | None | Staff puede subir; los roles clinicos pueden ver y descargar. |
| Historia clinica | Full | None | Work | None | Read | Read | None | Solo admin tiene permiso de actualizacion por defecto. Doctor puede crear y leer. Nurse y chief nurse son solo lectura. |
| Notas de evolucion | Full | None | Work | None | Work | Work | None | Append-only para no admin. Doctor, nurse y chief nurse pueden `create` y `read`; solo ADMIN tiene `progress-note:update`. CHIEF_NURSE recibe `progress-note:create` en V096; el grant legado de `progress-note:update` para CHIEF_NURSE en el seed se revoca en V096. La proteccion al alta bloquea escrituras para todos, incluido ADMIN. |
| Ordenes medicas | Full | Read | Work | None | Read | Read | None | Doctor puede crear y descontinuar. Solo admin tiene `medical-order:update` por defecto. El estado inicial depende de la categoria: las ordenes directivas (dieta / movilidad / etc.) inician en `ACTIVA`; las que requieren autorizacion (medicamentos / laboratorios / referencias / pruebas psicometricas) inician en `SOLICITADO`. El staff administrativo recibe acceso de lectura para poder autorizar. |
| Autorizacion de ordenes medicas | Full | Work | None | None | None | None | None | El permiso `medical-order:authorize` permite la transicion `SOLICITADO → AUTORIZADO` o `NO_AUTORIZADO`. Solo aplica a categorias que requieren autorizacion. Asignado por defecto a admin y al staff administrativo. |
| Autorizacion de emergencia | Full | None | Work | None | None | None | None | El permiso `medical-order:emergency-authorize` permite a un doctor autorizar una orden por su cuenta cuando no hay personal administrativo (fuera de horario, paciente en crisis, familia no localizable). Requiere una razon estructurada (enum) y nota opcional (obligatoria cuando la razon es OTHER). Marca campos `emergency_*` y dispara facturacion igual que la autorizacion normal. Asignado por defecto a admin y doctor. |
| Marcar orden en proceso | Full | None | Work | None | Work | Work | None | El permiso `medical-order:mark-in-progress` transiciona `AUTORIZADO → EN_PROCESO` para laboratorios, referencias y pruebas psicometricas (muestra tomada / paciente referido / prueba administrada). Es la cola de trabajo de enfermeria. Despues de esta transicion ya no se puede descontinuar — la accion fue iniciada externamente. El estado terminal `RESULTADOS_RECIBIDOS` se alcanza implicitamente al subir un documento de resultados. Asignado por defecto a admin, enfermero(a), doctor y chief nurse. |
| Documentos de orden medica | Full | None | Work | None | Work | Read | None | Doctor y nurse pueden subir. Subir sobre `AUTORIZADO` o `EN_PROCESO` transiciona automaticamente a `RESULTADOS_RECIBIDOS`. Chief nurse puede leer y descargar por `medical-order:read`, pero no subir por defecto. No existe un permiso de descarga separado; la descarga se valida con `medical-order:read`. |
| Ordenes medicas por estado (dashboard) | Full | Read | Read | None | Read | Read | None | Pantalla de nivel superior en `/medical-orders`. Visible para cualquier rol con `medical-order:read`. El filtro por defecto selecciona los buckets que requieren accion (`SOLICITADO`, `AUTORIZADO`, `EN_PROCESO`). Los botones de accion siguen validados por el permiso correspondiente (autorizar / autorizacion de emergencia / marcar en proceso / descontinuar / subir documento). |
| Notas de enfermeria | Full | None | Work | None | Work | Work | None | Append-only para no admin. Doctor, nurse y chief nurse pueden `create` y `read`; solo ADMIN tiene `nursing-note:update` despues de V096. La proteccion al alta bloquea escrituras para todos, incluido ADMIN. |
| Signos vitales | Full | None | Work | None | Work | Work | None | Append-only para no admin. Doctor, nurse y chief nurse pueden `create` y `read`; solo ADMIN tiene `vital-sign:update` despues de V097. La proteccion al alta bloquea escrituras para todos, incluido ADMIN. |
| Administracion de medicamentos | Full | None | Read | None | Work | Work | None | Crear se limita a ordenes de medicamentos en admisiones activas y requiere una orden vinculada a inventario para estado `GIVEN`. |
| Actividades de psicoterapia | Full | None | Read | Work | Read | Read | None | Crear tambien requiere el rol real `PSYCHOLOGIST` y una admision de hospitalizacion. El borrado queda, en la practica, solo para admin en los roles integrados. |
| Categorias de psicoterapia | Full | None | Read | Read | Read | Read | None | La gestion de categorias es admin-only por defecto. |
| Cargos y saldos de facturacion | Full | Read | Read | Read | Read | Read | None | Los cargos manuales y ajustes son admin-only en los roles que vienen con el sistema. El acceso de lectura para staff y psychologist viene del seed, no de la migracion versionada de billing. |
| Facturas | Full | Read | None | None | None | None | None | La generacion de factura es admin-only en los roles integrados. El permiso de lectura de facturas para staff viene del seed, no de la migracion versionada de billing. |
| Codigos de triage | Full | Read | Read | Read | Read | Read | None | La gestion es admin-only por defecto. |
| Habitaciones | Full | Read | Read | Read | Read | Read | None | La gestion es admin-only por defecto. |
| Tipos de documento | Full | Read | None | None | None | None | None | Las rutas y vistas existen, pero no hay una entrada en el sidebar para tipos de documento. |
| Categorias, items y movimientos de inventario | Full | None | None | None | None | None | None | Los movimientos de inventario aun pueden crearse de forma indirecta desde el flujo de administracion de medicamentos. |
| Operacion de tesoreria | Full | Full** | None | None | None | None | None | Incluye cuentas bancarias, gastos, ingresos, empleados, planilla y honorarios medicos. |
| Reportes de tesoreria | Full | Full** | None | None | None | None | None | Incluye dashboard y reportes mensual, pagos proximos, resumen bancario, compensacion, indemnizacion y conciliacion. |
| Conciliacion de tesoreria | Full | Full** | None | None | None | None | None | Incluye estados de cuenta bancarios y el flujo de conciliacion. |

## Notas Importantes de Enforcement

- El frontend trata a `ADMIN` como superusuario. `web/src/stores/auth.ts` devuelve `true` para cualquier permiso cuando el usuario tiene el rol `ADMIN`.
- `/users`, `/roles` y `/audit-logs` estan protegidos en frontend con `requiresAdmin`, no solo con permisos directos.
- Nurse y chief nurse son redirigidos desde `/dashboard` hacia `/nursing-kardex`.
- El kardex de enfermeria usa `admission:read`, asi que staff, doctor, psychologist y admin tambien pueden abrirlo si navegan directamente.
- El alcance de doctor es parcial, no total. El listado y detalle de pacientes y el listado de admisiones se limitan al medico tratante **o** consultante. Pero `AdmissionService.getAdmission()` no valida que el detalle directo pertenezca al medico.
- El alcance de psychologist tambien es parcial. El codigo limita a activas en listado y detalle de pacientes, listado y detalle de admisiones, busqueda de pacientes, resumen de paciente y listado de medicos consultantes, pero las descargas de consentimiento y documentos de admision solo se protegen por permiso.
- Las notas de enfermeria, las notas de evolucion y los signos vitales son admin-only para `update`, tanto a nivel de role-permission (solo ADMIN tiene `nursing-note:update` / `progress-note:update` / `vital-sign:update` despues de V097) como a nivel de servicio (`*Service.updateXxx()` valida ADMIN de forma independiente). La proteccion al alta bloquea las actualizaciones para todos, incluido ADMIN. El campo `canEdit` en `NursingNoteResponse`, `ProgressNoteResponse` y `VitalSignResponse` se evalua como `isAdmin && admission.isActive`. Doctores, nurses y chief nurses deben solicitar la edicion a un administrador. Ver `docs/features/nursing-module.md` revision 1.4 para la justificacion sobre signos vitales.
- Las actualizaciones de ordenes medicas no forman parte de los roles por defecto de doctor, nurse o chief nurse. Doctor puede crear y descontinuar, pero solo admin tiene permiso de actualizacion por defecto.
- Las ordenes medicas tienen un flujo dependiente de la categoria. Las categorias directivas (por ejemplo `DIETA`) inician en `ACTIVA` y no pasan por autorizacion. Las categorias que requieren autorizacion (`MEDICAMENTOS`, `LABORATORIOS`, `REFERENCIAS_MEDICAS`, `PRUEBAS_PSICOMETRICAS`) inician en `SOLICITADO`. La autorizacion (`SOLICITADO → AUTORIZADO`/`NO_AUTORIZADO`) es solo para admin y staff administrativo. Los doctores pueden usar autorizacion de emergencia (`medical-order:emergency-authorize`) para autoautorizar ordenes en crisis o fuera de horario, con un motivo estructurado registrado en auditoria. Para categorias con resultados, enfermeria/admin/doctor pueden marcar una orden `AUTORIZADO` como `EN_PROCESO` (muestra tomada / paciente referido / prueba administrada); a partir de ese punto, ya no se puede descontinuar. El estado terminal `RESULTADOS_RECIBIDOS` no se puede asignar manualmente: se alcanza al subir un documento de resultados, que transiciona la orden en la misma transaccion (saltando `EN_PROCESO` si es necesario). El descontinuado (`DESCONTINUADO`) solo se permite desde `ACTIVA`, `SOLICITADO` o `AUTORIZADO`.
- Chief nurse no es solo una etiqueta. En los roles integrados extiende a nurse con `patient:update` y `admission:update`. El grant legado de `progress-note:update` para CHIEF_NURSE en el seed se revoca en V096; el grant de `vital-sign:update` que V045 daba a CHIEF_NURSE / NURSE / DOCTOR se revoca en V097. CHIEF_NURSE ahora respeta la politica de edicion por registro (admin-only para notas de enfermeria, notas de evolucion y signos vitales).

## Desajustes y Huecos a Seguir

- Desajuste del rol `USER`: `V007__remove_user_read_from_user_role.sql` elimina `user:read` de `USER`, pero `R__seed_01_reset_and_base.sql` lo vuelve a asignar durante el reseed. En entornos con seed, el rol `USER` puede llamar a la API `/api/users`, pero el frontend sigue ocultando la administracion de usuarios detras de `requiresAdmin`, asi que no tiene efecto practico en la UI.
- Desajuste de facturacion para `ADMINISTRATIVE_STAFF`: el seed otorga `billing:read` e `invoice:read`, pero la migracion versionada de billing solo otorga lectura de billing a doctor y nurse, y acceso total de billing e invoice a admin.
- Desajuste de tesoreria para `ADMINISTRATIVE_STAFF`: `V084__seed_treasury_permissions.sql` otorga acceso total de tesoreria a staff, pero el archivo de reseed reconstruye los permisos sin volver a aplicar esos grants. En entornos dev o acceptance con seed, el acceso a tesoreria puede perderse despues de un reseed.
- Los tipos de documento son accesibles por ruta y permiso, pero `web/src/layout/AppMenu.vue` no expone un enlace en el sidebar.
- `CHIEF_NURSE` no puede subir documentos de orden medica a pesar de ser un rol que extiende a nurse. `V073__add_medical_order_document_permissions.sql` otorga `medical-order:upload-document` a `NURSE` pero no a `CHIEF_NURSE`.
- La creacion de actividades de psicoterapia valida el nombre del rol `PSYCHOLOGIST` a nivel de servicio (`PsychotherapyActivityService`), no solo el permiso `psychotherapy-activity:create`. Un rol personalizado con ese permiso pero sin el nombre de rol `PSYCHOLOGIST` seria rechazado.
