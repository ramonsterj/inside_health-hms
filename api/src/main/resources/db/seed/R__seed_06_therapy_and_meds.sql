-- ============================================================================
-- SEED FILE 06: Psychotherapy Activities, Medication Administrations,
--               Inventory Movements
-- ============================================================================

SET session_replication_role = replica;

-- ============================================================================
-- PSYCHOTHERAPY ACTIVITIES (~25 records)
-- 1-3 per hospitalized patient, created by psych1/psych2
-- Categories: Taller, Sesión individual, Terapia con mascotas, Pilates,
--   Meditación guiada, Terapia grupal, Arte terapia, Musicoterapia,
--   Terapia ocupacional, Otra
-- ============================================================================

-- Juan Pérez González (14d, MDD + suicide attempt, 3 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Paciente participa en sesión grupal sobre manejo de emociones. Inicialmente reticente, gradualmente se integra al grupo. Comparte brevemente sobre sentimientos de desesperanza. Recibe retroalimentación positiva del grupo.',
  (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Terapia grupal';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión individual: Trabajo con reestructuración cognitiva. Se identifican distorsiones de pensamiento (catastrofización, pensamiento todo-o-nada). Paciente muestra insight parcial. Tarea: registro de pensamientos automáticos.',
  (a.admission_date + INTERVAL '8 days' + TIME '14:00'), (a.admission_date + INTERVAL '8 days' + TIME '14:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Sesión individual';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Participación en sesión de arte terapia. Realizo pintura expresando emociones. Colores oscuros predominantes pero con algunos elementos de luz. Verbalizo que le ayudo a expresar lo que no puede con palabras.',
  (a.admission_date + INTERVAL '11 days' + TIME '10:00'), (a.admission_date + INTERVAL '11 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Arte terapia';

-- Maria Santos López (12d, Bipolar I manic, 2 activities - started late due to acute mania)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Primera sesión grupal post-estabilización. Paciente participa activamente, tendencia a monopolizar la conversacion. Se trabaja limites y turnos. Responde bien a la estructura grupal.',
  (a.admission_date + INTERVAL '8 days' + TIME '10:00'), (a.admission_date + INTERVAL '8 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Terapia grupal';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión de psicoeducación individual sobre trastorno bipolar. Se explica importancia de medicación, señales de alarma de mania, higiene del sueño. Paciente receptiva, muestra preocupacion por futuras recaídas.',
  (a.admission_date + INTERVAL '10 days' + TIME '15:00'), (a.admission_date + INTERVAL '10 days' + TIME '15:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Sesión individual';

-- Pedro García Hernández (10d, Alcohol withdrawal, 3 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión grupal de prevención de recaídas. Paciente comparte historia de consumo de 20 años. Grupo ofrece apoyo y validación. Se trabajan estrategias de afrontamiento ante craving.',
  (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Terapia grupal';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Taller de manejo del estrés y prevención de recaídas. Se practican técnicas de relajación muscular progresiva y visualización. Paciente participó activamente.',
  (a.admission_date + INTERVAL '7 days' + TIME '10:00'), (a.admission_date + INTERVAL '7 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Taller';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Actividad de terapia ocupacional: jardineria terapéutica. Paciente disfruto actividad al aire libre. Comenta que le recuerda a su infancia en el campo. Efecto positivo en ánimo.',
  (a.admission_date + INTERVAL '9 days' + TIME '11:00'), (a.admission_date + INTERVAL '9 days' + TIME '11:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Terapia ocupacional';

-- Ana Martínez Ruiz (8d, Schizophrenia, 2 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión de musicoterapia. Paciente inicialmente observadora, gradualmente participa tocando pandereta. Disminucion visible de tension. No verbalizo durante sesión pero se mostró más relajada.',
  (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Musicoterapia';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Participación en terapia grupal. Asistio por primera vez. Escucha atentamente pero participa poco verbalmente. Hizo un comentario breve sobre su experiencia. Progreso significativo dada su condición.',
  (a.admission_date + INTERVAL '7 days' + TIME '10:00'), (a.admission_date + INTERVAL '7 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Terapia grupal';

-- Luis Morales Castro (7d, Polysubstance, 2 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión grupal de prevención de recaídas. Paciente comparte sobre su historia de consumo. Muestra motivación ambivalente para cambio. Se aplica entrevista motivacional en formato grupal.',
  (a.admission_date + INTERVAL '4 days' + TIME '10:00'), (a.admission_date + INTERVAL '4 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Terapia grupal';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión individual de entrevista motivacional. Se explora ambivalencia sobre consumo. Paciente identifica pros y contras. Verbaliza deseo de cambio pero duda de su capacidad. Se refuerzan autoeficacia y factores protectores.',
  (a.admission_date + INTERVAL '6 days' + TIME '14:00'), (a.admission_date + INTERVAL '6 days' + TIME '14:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Sesión individual';

-- Carmen Flores Mejía (5d, PTSD, 2 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión de meditación guiada enfocada en grounding y regulación emocional. Paciente con dificultad inicial para cerrar ojos (hipervigilancia). Se adapta con ojos abiertos. Logra 10 minutos de relajación. Reporta disminución de ansiedad post-sesión.',
  (a.admission_date + INTERVAL '2 days' + TIME '11:00'), (a.admission_date + INTERVAL '2 days' + TIME '11:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Meditación guiada';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión individual de procesamiento de trauma con técnicas de estabilización. Se trabaja lugar seguro y contenedor. Paciente se emociona pero logra regularse. No se realiza exposición por estar en fase aguda.',
  (a.admission_date + INTERVAL '4 days' + TIME '14:00'), (a.admission_date + INTERVAL '4 days' + TIME '14:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Sesión individual';

-- Roberto Díaz Vargas (4d, Bipolar II depressive, 1 activity)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Actividad de terapia ocupacional adaptada. Paciente realiza armado de rompecabezas simple. Movilidad limitada por condición cardiaca. Tolero 30 minutos. Expresión facial más animada al final de la actividad.',
  (a.admission_date + INTERVAL '3 days' + TIME '10:00'), (a.admission_date + INTERVAL '3 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Terapia ocupacional';

-- Sofia Ramírez Paz (3d, BPD, 2 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión individual de habilidades DBT: tolerancia al malestar. Se practican técnicas TIPP (temperatura, ejercicio intenso, respiración acompasada, relajación muscular). Paciente receptiva, identifica sus señales de alarma previas a la autolesión.',
  (a.admission_date + INTERVAL '1 day' + TIME '14:00'), (a.admission_date + INTERVAL '1 day' + TIME '14:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Sesión individual';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión de arte terapia. Paciente dibuja autorretrato. Trabajo significativo sobre autoimagen. Verbaliza que se siente "rota pero juntando piezas". Se explora metafora terapéuticamente.',
  (a.admission_date + INTERVAL '2 days' + TIME '10:00'), (a.admission_date + INTERVAL '2 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE' AND pc.name = 'Arte terapia';

-- Miguel Torres Luna (DISCHARGED, 14d stay, 2 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Terapia ocupacional: actividades de vida diaria. Paciente práctica higiene personal y organizacion de espacio. Mejoría funciónal notable desde ingreso. Se trabaja rutina diaria para el egreso.',
  (a.admission_date + INTERVAL '8 days' + TIME '10:00'), (a.admission_date + INTERVAL '8 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED' AND pc.name = 'Terapia ocupacional';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión grupal. Paciente escucha atentamente, participa brevemente cuando se le invita. Afecto aplanado pero funciónal. Se normaliza la experiencia psicótica en contexto grupal.',
  (a.admission_date + INTERVAL '10 days' + TIME '10:00'), (a.admission_date + INTERVAL '10 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED' AND pc.name = 'Terapia grupal';

-- Elena Sánchez Rivas (DISCHARGED, 11d stay, 3 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión de meditación guiada: respiración diafragmática y escaneo corporal. Paciente ansiosa al inicio, logra relajarse hacia el final. Se asigna práctica diaria de 10 minutos.',
  (a.admission_date + INTERVAL '2 days' + TIME '10:00'), (a.admission_date + INTERVAL '2 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED' AND pc.name = 'Meditación guiada';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión individual TCC: psicoeducación sobre modelo cognitivo de la ansiedad. Se identifican pensamientos catastróficos automáticos. Paciente comprende bien el modelo y muestra motivación.',
  (a.admission_date + INTERVAL '5 days' + TIME '14:00'), (a.admission_date + INTERVAL '5 days' + TIME '14:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED' AND pc.name = 'Sesión individual';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Taller de técnicas de relajación y manejo de ansiedad. Paciente demuestra dominio de técnicas aprendidas. Comparte con otros pacientes su experiencia de mejoría. Rol modelo positivo para el grupo.',
  (a.admission_date + INTERVAL '8 days' + TIME '10:00'), (a.admission_date + INTERVAL '8 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED' AND pc.name = 'Taller';

-- Francisco Mendoza Aguilar (DISCHARGED, 11d stay, 2 activities)
INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Terapia ocupacional adaptada para adulto mayor. Actividades de estimulación cognitiva y motricidad fina. Paciente coopera aunque con lentitud. Se adaptan actividades a su movilidad limitada.',
  (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'psych2')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED' AND pc.name = 'Terapia ocupacional';

INSERT INTO psychotherapy_activities (admission_id, category_id, description, created_at, updated_at, created_by)
SELECT a.id, pc.id, 'Sesión de musicoterapia. Paciente escucha música de su epoca (marimbas guatemaltecas). Expresión emocional notable, lloro al recordar momentos felices. Efecto terapéutico positivo sobre el ánimo.',
  (a.admission_date + INTERVAL '8 days' + TIME '11:00'), (a.admission_date + INTERVAL '8 days' + TIME '11:00'), (SELECT id FROM users WHERE username = 'psych1')
FROM admissions a JOIN patients p ON a.patient_id = p.id, psychotherapy_categories pc
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED' AND pc.name = 'Musicoterapia';


-- ============================================================================
-- MEDICATION ADMINISTRATIONS (~100 records)
-- 2-5 per medication order, distributed across admission days
-- Status: ~90% GIVEN, ~5% REFUSED, ~3% HELD, ~2% MISSED
-- ============================================================================

-- === Juan Pérez González (Sertralina BID, Quetiapina HS, Clonazepam PRN) ===
-- Sertralina 50mg BID - 5 administrations (days 0,2,4,7,10)
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada con alimentos, tolerada sin efectos adversos',
  a.admission_date + INTERVAL '8 hours', a.admission_date + INTERVAL '8 hours', a.admission_date + INTERVAL '8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Dosis matutina administrada sin novedad',
  a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada sin novedad',
  a.admission_date + INTERVAL '4 days 20 hours', a.admission_date + INTERVAL '4 days 20 hours', a.admission_date + INTERVAL '4 days 20 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Paciente reporta leve mejoría en ánimo',
  a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Tolerancia adecuada, continúa esquema',
  a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';

-- Quetiapina 300mg HS - 4 administrations (days 0,3,6,10)
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada a las 21:00. Paciente logró dormir 4 horas.',
  a.admission_date + INTERVAL '21 hours', a.admission_date + INTERVAL '21 hours', a.admission_date + INTERVAL '21 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño mejorado a 5 horas.',
  a.admission_date + INTERVAL '3 days 21 hours', a.admission_date + INTERVAL '3 days 21 hours', a.admission_date + INTERVAL '3 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño de 6 horas, refiere somnolencia matutina leve.',
  a.admission_date + INTERVAL '6 days 21 hours', a.admission_date + INTERVAL '6 days 21 hours', a.admission_date + INTERVAL '6 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño regular de 7 horas.',
  a.admission_date + INTERVAL '10 days 21 hours', a.admission_date + INTERVAL '10 days 21 hours', a.admission_date + INTERVAL '10 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';

-- Clonazepam 2mg PRN - 3 administrations (days 1,5,8 - decreasing frequency)
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: ansiedad severa con agitación. Se calmó en 30 min.',
  a.admission_date + INTERVAL '1 day 14 hours', a.admission_date + INTERVAL '1 day 14 hours', a.admission_date + INTERVAL '1 day 14 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: crisis de llanto con ansiedad. Efecto en 20 min.',
  a.admission_date + INTERVAL '5 days 16 hours', a.admission_date + INTERVAL '5 days 16 hours', a.admission_date + INTERVAL '5 days 16 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'REFUSED', 'Paciente indica que ya no necesita PRN, se siente más tranquilo.',
  a.admission_date + INTERVAL '8 days 15 hours', a.admission_date + INTERVAL '8 days 15 hours', a.admission_date + INTERVAL '8 days 15 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';

-- === Maria Santos López (Quetiapina BID, Biperiden BID, Midazolam PRN) ===
-- Quetiapina 300mg BID - 5 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada VO, paciente agitada pero acepta.',
  a.admission_date + INTERVAL '8 hours', a.admission_date + INTERVAL '8 hours', a.admission_date + INTERVAL '8 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'REFUSED', 'Paciente se niega, dice que no necesita medicación porque esta bien.',
  a.admission_date + INTERVAL '1 day 8 hours', a.admission_date + INTERVAL '1 day 8 hours', a.admission_date + INTERVAL '1 day 8 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Acepta medicación tras intervención de enfermera jefe.',
  a.admission_date + INTERVAL '3 days 20 hours', a.admission_date + INTERVAL '3 days 20 hours', a.admission_date + INTERVAL '3 days 20 hours',
  (SELECT id FROM users WHERE username = 'chiefnurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada sin resistencia, paciente más cooperativa.',
  a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Buena adherencia. Paciente reconoce beneficio de la medicación.',
  a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';

-- Biperiden 2mg BID - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrado con quetiapina.',
  a.admission_date + INTERVAL '8 hours 5 minutes', a.admission_date + INTERVAL '8 hours 5 minutes', a.admission_date + INTERVAL '8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sin efectos extrapiramidales.',
  a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrado sin novedad.',
  a.admission_date + INTERVAL '7 days 20 hours', a.admission_date + INTERVAL '7 days 20 hours', a.admission_date + INTERVAL '7 days 20 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Continúa sin efectos extrapiramidales.',
  a.admission_date + INTERVAL '10 days 8 hours 5 minutes', a.admission_date + INTERVAL '10 days 8 hours 5 minutes', a.admission_date + INTERVAL '10 days 8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';

-- Midazolam 15mg PRN - 2 administrations (acute phase only)
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: agitación severa al ingreso. Efecto en 15 min. Paciente se calmó y durmió.',
  a.admission_date + INTERVAL '1 hour', a.admission_date + INTERVAL '1 hour', a.admission_date + INTERVAL '1 hour',
  (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Midazolam 15mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: agitación nocturna día 2. Se calmó en 20 min.',
  a.admission_date + INTERVAL '2 days 2 hours', a.admission_date + INTERVAL '2 days 2 hours', a.admission_date + INTERVAL '2 days 2 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.status = 'ACTIVE' AND mo.medication = 'Midazolam 15mg' AND mo.category = 'MEDICAMENTOS';

-- === Pedro García Hernández (Diazepam PRN, Pregabalina BID) ===
-- Diazepam 10mg PRN - 5 administrations (frequent first 3 days, then tapering)
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'CIWA-Ar 22. Administrado IM. Temblor grueso, diaforesis.',
  a.admission_date + INTERVAL '1 hour', a.admission_date + INTERVAL '1 hour', a.admission_date + INTERVAL '1 hour',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Diazepam 10mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'CIWA-Ar 18. Segunda dosis. Temblor moderado persistente.',
  a.admission_date + INTERVAL '6 hours', a.admission_date + INTERVAL '6 hours', a.admission_date + INTERVAL '6 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Diazepam 10mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'CIWA-Ar 15. Temblor fino. Diaforesis mejorando.',
  a.admission_date + INTERVAL '1 day 4 hours', a.admission_date + INTERVAL '1 day 4 hours', a.admission_date + INTERVAL '1 day 4 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Diazepam 10mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'CIWA-Ar 12. Síntomas disminuyendo.',
  a.admission_date + INTERVAL '2 days 4 hours', a.admission_date + INTERVAL '2 days 4 hours', a.admission_date + INTERVAL '2 days 4 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Diazepam 10mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'CIWA-Ar 10. Última dosis, protocolo de destete.',
  a.admission_date + INTERVAL '3 days 8 hours', a.admission_date + INTERVAL '3 days 8 hours', a.admission_date + INTERVAL '3 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Diazepam 10mg' AND mo.category = 'MEDICAMENTOS';

-- Pregabalina 75mg BID - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Primera dosis, paciente tolera bien.',
  a.admission_date + INTERVAL '12 hours', a.admission_date + INTERVAL '12 hours', a.admission_date + INTERVAL '12 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Pregabalina 75mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Refiere disminución de ansiedad.',
  a.admission_date + INTERVAL '3 days 8 hours', a.admission_date + INTERVAL '3 days 8 hours', a.admission_date + INTERVAL '3 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Pregabalina 75mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Tolerancia adecuada, sueño mejorado.',
  a.admission_date + INTERVAL '6 days 20 hours', a.admission_date + INTERVAL '6 days 20 hours', a.admission_date + INTERVAL '6 days 20 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Pregabalina 75mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sin efectos adversos. Continúa esquema.',
  a.admission_date + INTERVAL '9 days 8 hours', a.admission_date + INTERVAL '9 days 8 hours', a.admission_date + INTERVAL '9 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.status = 'ACTIVE' AND mo.medication = 'Pregabalina 75mg' AND mo.category = 'MEDICAMENTOS';

-- === Ana Martínez Ruiz (Olanzapina BID, Biperiden BID, Clonazepam PRN) ===
-- Olanzapina 5mg BID - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Paciente suspicaz pero acepta medicación.',
  a.admission_date + INTERVAL '9 hours', a.admission_date + INTERVAL '9 hours', a.admission_date + INTERVAL '9 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'HELD', 'NPO por estudios de laboratorio matutinos.',
  a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Menos suspicaz, acepta sin resistencia.',
  a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Buena adherencia. Síntomas psicóticos mejorando.',
  a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';

-- Biperiden 2mg BID (Ana) - 3 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrado con olanzapina.',
  a.admission_date + INTERVAL '9 hours 5 minutes', a.admission_date + INTERVAL '9 hours 5 minutes', a.admission_date + INTERVAL '9 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sin signos de extrapiramidales.',
  a.admission_date + INTERVAL '4 days 8 hours 5 minutes', a.admission_date + INTERVAL '4 days 8 hours 5 minutes', a.admission_date + INTERVAL '4 days 8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Continúa sin efectos extrapiramidales.',
  a.admission_date + INTERVAL '7 days 8 hours 5 minutes', a.admission_date + INTERVAL '7 days 8 hours 5 minutes', a.admission_date + INTERVAL '7 days 8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';

-- Clonazepam PRN (Ana) - 2 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: agitación por ideación paranoide.',
  a.admission_date + INTERVAL '1 day 15 hours', a.admission_date + INTERVAL '1 day 15 hours', a.admission_date + INTERVAL '1 day 15 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: ansiedad nocturna.',
  a.admission_date + INTERVAL '3 days 22 hours', a.admission_date + INTERVAL '3 days 22 hours', a.admission_date + INTERVAL '3 days 22 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';

-- === Luis Morales Castro (Quetiapina 100mg BID, Biperiden BID) ===
-- Quetiapina 100mg BID - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada. Paciente desorientado pero acepta.',
  a.admission_date + INTERVAL '4 hours', a.admission_date + INTERVAL '4 hours', a.admission_date + INTERVAL '4 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'MISSED', 'Paciente dormido, no se despertó para dosis nocturna.',
  a.admission_date + INTERVAL '1 day 20 hours', a.admission_date + INTERVAL '1 day 20 hours', a.admission_date + INTERVAL '1 day 20 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Alucinaciones resueltas. Cooperativo.',
  a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Estable. Continúa esquema.',
  a.admission_date + INTERVAL '6 days 20 hours', a.admission_date + INTERVAL '6 days 20 hours', a.admission_date + INTERVAL '6 days 20 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';

-- Biperiden 2mg BID (Luis) - 3 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrado junto con quetiapina.',
  a.admission_date + INTERVAL '4 hours 5 minutes', a.admission_date + INTERVAL '4 hours 5 minutes', a.admission_date + INTERVAL '4 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sin efectos adversos.',
  a.admission_date + INTERVAL '4 days 8 hours 5 minutes', a.admission_date + INTERVAL '4 days 8 hours 5 minutes', a.admission_date + INTERVAL '4 days 8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Tolerancia adecuada.',
  a.admission_date + INTERVAL '6 days 20 hours 5 minutes', a.admission_date + INTERVAL '6 days 20 hours 5 minutes', a.admission_date + INTERVAL '6 days 20 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.status = 'ACTIVE' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';

-- === Carmen Flores Mejía (Sertralina QD, Clonazepam HS) ===
-- Sertralina 50mg QD - 3 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Primera dosis ISRS. Se explica período de latencia.',
  a.admission_date + INTERVAL '18 hours', a.admission_date + INTERVAL '18 hours', a.admission_date + INTERVAL '18 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Tolerada sin efectos adversos.',
  a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Continúa sin novedad.',
  a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';

-- Clonazepam 2mg HS (Carmen) - 3 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada HS. Pesadillas reportadas al despertar.',
  a.admission_date + INTERVAL '21 hours', a.admission_date + INTERVAL '21 hours', a.admission_date + INTERVAL '21 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño de 5 horas, pesadillas menos intensas.',
  a.admission_date + INTERVAL '2 days 21 hours', a.admission_date + INTERVAL '2 days 21 hours', a.admission_date + INTERVAL '2 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño de 6 horas mejorado.',
  a.admission_date + INTERVAL '4 days 21 hours', a.admission_date + INTERVAL '4 days 21 hours', a.admission_date + INTERVAL '4 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.status = 'ACTIVE' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';

-- === Roberto Díaz Vargas (Quetiapina 300mg HS) ===
-- Quetiapina 300mg HS - 3 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Primera dosis HS. Sueño de 4 horas.',
  a.admission_date + INTERVAL '10 hours', a.admission_date + INTERVAL '10 hours', a.admission_date + INTERVAL '10 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño mejorado a 6 horas. Somnolencia matutina.',
  a.admission_date + INTERVAL '1 day 21 hours', a.admission_date + INTERVAL '1 day 21 hours', a.admission_date + INTERVAL '1 day 21 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño regular. PA estable.',
  a.admission_date + INTERVAL '3 days 21 hours', a.admission_date + INTERVAL '3 days 21 hours', a.admission_date + INTERVAL '3 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 300mg' AND mo.category = 'MEDICAMENTOS';

-- === Sofia Ramírez Paz (Sertralina QD, Quetiapina 100mg HS) ===
-- Sertralina 50mg QD - 2 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Primera dosis. Paciente cooperativa.',
  a.admission_date + INTERVAL '14 hours', a.admission_date + INTERVAL '14 hours', a.admission_date + INTERVAL '14 hours',
  (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Tolerada. Sin náuseas.',
  a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.status = 'ACTIVE' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';

-- Quetiapina 100mg HS (Sofia) - 2 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada HS. Paciente logró dormir 5 horas.',
  a.admission_date + INTERVAL '5 hours', a.admission_date + INTERVAL '5 hours', a.admission_date + INTERVAL '5 hours',
  (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño mejorado. Sin autolesiónes.',
  a.admission_date + INTERVAL '2 days 21 hours', a.admission_date + INTERVAL '2 days 21 hours', a.admission_date + INTERVAL '2 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.status = 'ACTIVE' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';

-- === Discharged: Miguel Torres Luna ===
-- Olanzapina 10mg IM PRN - 2 administrations (acute phase)
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: agitación psicomotora severa. IM administrada.',
  a.admission_date + INTERVAL '1 hour', a.admission_date + INTERVAL '1 hour', a.admission_date + INTERVAL '1 hour',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Olanzapina 10mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: segunda dosis por agitación persistente día 1.',
  a.admission_date + INTERVAL '1 day 3 hours', a.admission_date + INTERVAL '1 day 3 hours', a.admission_date + INTERVAL '1 day 3 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Olanzapina 10mg' AND mo.category = 'MEDICAMENTOS';

-- Olanzapina 5mg oral BID - 5 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Transición a via oral. Acepta con asistencia.',
  a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'REFUSED', 'Paciente dice que medicación lo envenena. Se reporta a médico.',
  a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours', a.admission_date + INTERVAL '4 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Acepta tras intervención médica.',
  a.admission_date + INTERVAL '6 days 8 hours', a.admission_date + INTERVAL '6 days 8 hours', a.admission_date + INTERVAL '6 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Adherente. Mejoría clínica notable.',
  a.admission_date + INTERVAL '9 days 8 hours', a.admission_date + INTERVAL '9 days 8 hours', a.admission_date + INTERVAL '9 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Última dosis previa al egreso.',
  a.admission_date + INTERVAL '13 days 8 hours', a.admission_date + INTERVAL '13 days 8 hours', a.admission_date + INTERVAL '13 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Olanzapina 5mg' AND mo.category = 'MEDICAMENTOS';

-- Biperiden 2mg BID (Miguel) - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrado junto con olanzapina oral.',
  a.admission_date + INTERVAL '2 days 8 hours 5 minutes', a.admission_date + INTERVAL '2 days 8 hours 5 minutes', a.admission_date + INTERVAL '2 days 8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sin extrapiramidales.',
  a.admission_date + INTERVAL '6 days 8 hours 5 minutes', a.admission_date + INTERVAL '6 days 8 hours 5 minutes', a.admission_date + INTERVAL '6 days 8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Continúa tolerando bien.',
  a.admission_date + INTERVAL '9 days 20 hours', a.admission_date + INTERVAL '9 days 20 hours', a.admission_date + INTERVAL '9 days 20 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Dosis de egreso.',
  a.admission_date + INTERVAL '13 days 8 hours 5 minutes', a.admission_date + INTERVAL '13 days 8 hours 5 minutes', a.admission_date + INTERVAL '13 days 8 hours 5 minutes',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED' AND mo.medication = 'Biperiden 2mg' AND mo.category = 'MEDICAMENTOS';

-- === Discharged: Elena Sánchez Rivas ===
-- Sertralina 50mg QD - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Primera dosis. Paciente ansiosa pero cooperativa.',
  a.admission_date + INTERVAL '9 hours', a.admission_date + INTERVAL '9 hours', a.admission_date + INTERVAL '9 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Leve náusea reportada, tolerada.',
  a.admission_date + INTERVAL '3 days 8 hours', a.admission_date + INTERVAL '3 days 8 hours', a.admission_date + INTERVAL '3 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Náusea resuelta. Buen efecto terapéutico.',
  a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours', a.admission_date + INTERVAL '7 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Última dosis hospitalaria. Continúa ambulatoriamente.',
  a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';

-- Clonazepam 2mg PRN (Elena) - 3 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: crisis de pánico en la noche.',
  a.admission_date + INTERVAL '22 hours', a.admission_date + INTERVAL '22 hours', a.admission_date + INTERVAL '22 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'PRN: ataque de pánico día 3.',
  a.admission_date + INTERVAL '3 days 14 hours', a.admission_date + INTERVAL '3 days 14 hours', a.admission_date + INTERVAL '3 days 14 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'HELD', 'Destete de benzodiacepinas iniciado. Se ofrece técnica de respiración.',
  a.admission_date + INTERVAL '7 days 20 hours', a.admission_date + INTERVAL '7 days 20 hours', a.admission_date + INTERVAL '7 days 20 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED' AND mo.medication = 'Clonazepam 2mg' AND mo.category = 'MEDICAMENTOS';

-- === Discharged: Francisco Mendoza Aguilar ===
-- Sertralina 50mg QD - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Primera dosis. Paciente con anorexia, se da con jugo.',
  a.admission_date + INTERVAL '11 hours', a.admission_date + INTERVAL '11 hours', a.admission_date + INTERVAL '11 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'MISSED', 'Paciente dormido profundamente, no se despertó. Se reporta.',
  a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours', a.admission_date + INTERVAL '2 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Ingesta mejorando, tolera con alimentos.',
  a.admission_date + INTERVAL '5 days 8 hours', a.admission_date + INTERVAL '5 days 8 hours', a.admission_date + INTERVAL '5 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Dosis previa al egreso. Mejoría notable en ánimo.',
  a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours', a.admission_date + INTERVAL '10 days 8 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Sertralina 50mg' AND mo.category = 'MEDICAMENTOS';

-- Quetiapina 100mg HS (Francisco) - 4 administrations
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Administrada HS. Logró dormir 4 horas.',
  a.admission_date + INTERVAL '21 hours', a.admission_date + INTERVAL '21 hours', a.admission_date + INTERVAL '21 hours',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño de 5 horas.',
  a.admission_date + INTERVAL '3 days 21 hours', a.admission_date + INTERVAL '3 days 21 hours', a.admission_date + INTERVAL '3 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Sueño mejorado a 6 horas.',
  a.admission_date + INTERVAL '7 days 21 hours', a.admission_date + INTERVAL '7 days 21 hours', a.admission_date + INTERVAL '7 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';
INSERT INTO medication_administrations (medical_order_id, admission_id, status, notes, administered_at, created_at, updated_at, created_by)
SELECT mo.id, a.id, 'GIVEN', 'Dosis previa al egreso. Patron de sueño estabilizado.',
  a.admission_date + INTERVAL '10 days 21 hours', a.admission_date + INTERVAL '10 days 21 hours', a.admission_date + INTERVAL '10 days 21 hours',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM medical_orders mo JOIN admissions a ON mo.admission_id = a.id JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED' AND mo.medication = 'Quetiapina 100mg' AND mo.category = 'MEDICAMENTOS';


-- ============================================================================
-- INVENTORY MOVEMENTS (1 EXIT per GIVEN medication administration)
-- Uses window function to calculate running stock levels
-- ============================================================================

WITH ranked_admins AS (
  SELECT ma.id as ma_id, ma.admission_id, ma.administered_at, ma.created_by,
    mo.inventory_item_id, mo.medication,
    ROW_NUMBER() OVER (PARTITION BY mo.inventory_item_id ORDER BY ma.administered_at) as rn,
    COUNT(*) OVER (PARTITION BY mo.inventory_item_id) as total_exits
  FROM medication_administrations ma
  JOIN medical_orders mo ON ma.medical_order_id = mo.id
  WHERE ma.status = 'GIVEN' AND ma.deleted_at IS NULL AND mo.deleted_at IS NULL
    AND mo.inventory_item_id IS NOT NULL AND mo.category = 'MEDICAMENTOS'
)
INSERT INTO inventory_movements (item_id, admission_id, movement_type, quantity, previous_quantity, new_quantity, notes, created_at, updated_at, created_by)
SELECT ra.inventory_item_id, ra.admission_id, 'EXIT', 1,
  ii.quantity + ra.total_exits - ra.rn + 1,
  ii.quantity + ra.total_exits - ra.rn,
  'Administración de ' || ra.medication,
  ra.administered_at, ra.administered_at, ra.created_by
FROM ranked_admins ra
JOIN inventory_items ii ON ra.inventory_item_id = ii.id;


SET session_replication_role = DEFAULT;
