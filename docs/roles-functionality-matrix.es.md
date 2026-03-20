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
| Documento de consentimiento de admision | Full | Work | Read | Read | Read | Read | None | El endpoint de descarga solo se protege por permiso. |
| Documentos de admision | Full | Work | Read | Read | Read | Read | None | Staff puede subir; los roles clinicos pueden ver y descargar. |
| Historia clinica | Full | None | Work | None | Read | Read | None | Solo admin tiene permiso de actualizacion por defecto. Doctor puede crear y leer. Nurse y chief nurse son solo lectura. |
| Notas de evolucion | Full | None | Work | None | Work | Work | None | Chief nurse agrega permiso de actualizacion. |
| Ordenes medicas | Full | None | Work | None | Read | Read | None | Doctor puede crear y descontinuar. Solo admin tiene `medical-order:update` por defecto. |
| Documentos de orden medica | Full | None | Work | None | Work | Read | None | Doctor y nurse pueden subir. Chief nurse puede leer y descargar por `medical-order:read`, pero no subir por defecto. No existe un permiso de descarga separado; la descarga se valida con `medical-order:read`. |
| Notas de enfermeria | Full | None | Scoped | None | Scoped | Scoped | None | Los usuarios no admin solo pueden editar sus propios registros, dentro de 24 horas y mientras la admision siga activa. |
| Signos vitales | Full | None | Scoped | None | Scoped | Scoped | None | Tienen las mismas restricciones de edicion que las notas de enfermeria. |
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
- La actualizacion de notas de enfermeria y signos vitales es mas estricta que el nombre del permiso. Los usuarios no admin solo pueden editar sus propios registros, dentro de 24 horas y mientras la admision siga activa.
- La actualizacion de notas de evolucion es mas amplia que en enfermeria o signos vitales. Si un rol tiene `progress-note:update`, el servicio no agrega restricciones por autor o ventana de tiempo.
- Las actualizaciones de ordenes medicas no forman parte de los roles por defecto de doctor, nurse o chief nurse. Doctor puede crear y descontinuar, pero solo admin tiene permiso de actualizacion por defecto.
- Chief nurse no es solo una etiqueta. En los roles integrados extiende a nurse con `patient:update`, `admission:update` y `progress-note:update`.

## Desajustes y Huecos a Seguir

- Desajuste del rol `USER`: `V007__remove_user_read_from_user_role.sql` elimina `user:read` de `USER`, pero `R__seed_01_reset_and_base.sql` lo vuelve a asignar durante el reseed. En entornos con seed, el rol `USER` puede llamar a la API `/api/users`, pero el frontend sigue ocultando la administracion de usuarios detras de `requiresAdmin`, asi que no tiene efecto practico en la UI.
- Desajuste de facturacion para `ADMINISTRATIVE_STAFF`: el seed otorga `billing:read` e `invoice:read`, pero la migracion versionada de billing solo otorga lectura de billing a doctor y nurse, y acceso total de billing e invoice a admin.
- Desajuste de tesoreria para `ADMINISTRATIVE_STAFF`: `V084__seed_treasury_permissions.sql` otorga acceso total de tesoreria a staff, pero el archivo de reseed reconstruye los permisos sin volver a aplicar esos grants. En entornos dev o acceptance con seed, el acceso a tesoreria puede perderse despues de un reseed.
- Los tipos de documento son accesibles por ruta y permiso, pero `web/src/layout/AppMenu.vue` no expone un enlace en el sidebar.
- `CHIEF_NURSE` no puede subir documentos de orden medica a pesar de ser un rol que extiende a nurse. `V073__add_medical_order_document_permissions.sql` otorga `medical-order:upload-document` a `NURSE` pero no a `CHIEF_NURSE`.
- La creacion de actividades de psicoterapia valida el nombre del rol `PSYCHOLOGIST` a nivel de servicio (`PsychotherapyActivityService`), no solo el permiso `psychotherapy-activity:create`. Un rol personalizado con ese permiso pero sin el nombre de rol `PSYCHOLOGIST` seria rechazado.
