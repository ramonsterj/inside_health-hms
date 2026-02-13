-- ============================================================================
-- SEED FILE 05: Nursing Notes, Vital Signs
-- ============================================================================

SET session_replication_role = replica;

-- ============================================================================
-- NURSING NOTES (~35 records)
-- 2-4 per hospitalized patient, created by nurse1-nurse4, chiefnurse1-2
-- ============================================================================

-- === Active Patients ===

-- Juan Pérez González (MDD + suicide attempt, 14d, 4 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente masculino de 45 años ingresa por intento suicida (sobreingesta de benzodiacepinas). Consciente, orientado, Glasgow 15. Se activan precauciones suicidas: monitoreo cada 15 min, retiro de objetos cortopunzantes, supervisión en baño. Signos vitales estables. Canalizado con SSN 0.9%. Familia presente, esposa muy angustiada.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Turno nocturno día 3: Paciente con insomnio, solicitó quetiapina a las 21:00, se administró según orden médica. Verbaliza culpa y desesperanza. Se realizó escucha activa. Negó ideación suicida activa al preguntar directamente. Ingesta alimentaria 40%. Continúa con precauciones suicidas.', (a.admission_date + INTERVAL '3 days' + TIME '22:00'), (a.admission_date + INTERVAL '3 days' + TIME '22:00'), (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Ronda de seguridad día 7: Paciente más comunicativo, participó en terapia grupal por primera vez. Sueño mejorado con quetiapina (6 horas). Ingesta 60%. Higiene personal adecuada. Se reduce monitoreo suicida a cada 30 minutos previa autorización médica.', (a.admission_date + INTERVAL '7 days' + TIME '14:00'), (a.admission_date + INTERVAL '7 days' + TIME '14:00'), (SELECT id FROM users WHERE username = 'chiefnurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 12: Mejoría notable en ánimo y participación. Ingesta alimentaria completa. Sueño de 7 horas. Participa en actividades terapéuticas diarias. Esposa reporta conversaciones positivas sobre planes futuros. Continúa adherente a medicación.', (a.admission_date + INTERVAL '12 days' + TIME '10:00'), (a.admission_date + INTERVAL '12 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Maria Santos López (Bipolar I manic, 12d, 4 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente femenina de 28 años ingresa por episodio maníaco con psicosis. Agitación psicomotora marcada, verborrea, ideas delirantes de misión religiosa. Se ubica en habitación individual con ambiente de baja estimulación. Se administra midazolam 15mg IM según indicacion. Familia no presente, se contacta madre por teléfono.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Turno diurno día 2: Continúa agitada pero menos intensa. Durmio 3 horas fragmentadas. Acepta medicación oral (quetiapina). Habla rápido sobre proyectos grandiosos. Se limitan estímulos. Ingesta parcial, come de pie y caminando. Hidratacion adecuada.', (a.admission_date + INTERVAL '2 days' + TIME '08:00'), (a.admission_date + INTERVAL '2 days' + TIME '08:00'), (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 6: Disminucion significativa de agitación. Sueño de 5 horas. Ideas delirantes menos prominentes. Acepta sentarse a comer. Coopera con higiene personal. Madre llego de Quetzaltenango, visita le hizo bien. Adherente a quetiapina y biperiden.', (a.admission_date + INTERVAL '6 days' + TIME '15:00'), (a.admission_date + INTERVAL '6 days' + TIME '15:00'), (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 10: Paciente estabilizada. Sueño de 7 horas. Sin ideas delirantes activas. Reconoce haber estado enferma. Participa en terapia grupal. Come en comedor. Ánimo eutimico. Se inicia psicoeducación sobre trastorno bipolar.', (a.admission_date + INTERVAL '10 days' + TIME '09:30'), (a.admission_date + INTERVAL '10 days' + TIME '09:30'), (SELECT id FROM users WHERE username = 'chiefnurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Pedro García Hernández (Alcohol withdrawal, 10d, 4 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso emergencia nocturna: Paciente masculino de 52 años con síndrome de abstinencia alcohólica severa. Temblor grueso, diaforesis, taquicardia (FC 110), HTA (PA 155/95). CIWA-Ar: 22 puntos. Se administra diazepam 10mg IM. Proteccion de via aerea, barandales arriba, precauciones anticonvulsivas. Se canaliza SSN.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Monitoreo CIWA día 2: CIWA-Ar cada 4 horas. Puntajes descendentes: 18-15-12. Temblor disminuyendo, sudoración leve. FC 98, PA 142/88. Se administró segunda dosis diazepam por puntaje >10. Control de glucosa capilar: 145 mg/dL. Dieta diabética tolerada 50%.', (a.admission_date + INTERVAL '2 days' + TIME '06:30'), (a.admission_date + INTERVAL '2 days' + TIME '06:30'), (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 5: CIWA-Ar: 6 puntos, se suspende protocolo. Sin temblor. SV normalizándose. Come dieta diabética completa. Glucosas capilares en rango (110-140). Paciente tranquilo, expresa deseo de no volver a beber. Se refiere a grupo de apoyo interno.', (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 8: Estable, sin síntomas de abstinencia. Participa activamente en terapia grupal y sesiones de prevención de recaídas. Sueño regular (7 horas). Glucosas capilares controladas con dieta. Familia lo visita, esposa e hijos.', (a.admission_date + INTERVAL '8 days' + TIME '14:00'), (a.admission_date + INTERVAL '8 days' + TIME '14:00'), (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Ana Martínez Ruiz (Schizophrenia paranoid, 8d, 3 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente femenina de 35 años con esquizofrenia paranoide descompensada. Suspicaz, evita contacto visual. Refiere que la comida esta envenenada. Se ofrece comida sellada, acepta parcialmente. PA 138/88 (HTA conocida). Olanzapina y biperiden administrados. Se monitorea conducta paranoide.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Turno diurno día 3: Menos suspicaz, acepta comida regular. Aun evita areas comunes. PA 130/82 (mejorando). Acepta medicación sin resistencia. Higiene personal con asistencia. Sin alucinaciones aparentes durante turno.', (a.admission_date + INTERVAL '3 days' + TIME '08:30'), (a.admission_date + INTERVAL '3 days' + TIME '08:30'), (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 6: Mejoría notable. Come en comedor con supervisión. PA 128/80. Ideas paranoides menos intensas. Participa brevemente en terapia grupal (observa más que participa). Sueño de 6 horas con quetiapina. Adherente a medicación.', (a.admission_date + INTERVAL '6 days' + TIME '15:00'), (a.admission_date + INTERVAL '6 days' + TIME '15:00'), (SELECT id FROM users WHERE username = 'chiefnurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Luis Morales Castro (Polysubstance + psychosis, 7d, 3 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso madrugada: Paciente masculino de 30 años ingresa a las 03:20 por psicosis inducida por sustancias. Agitado, desorientado, alucinaciones visuales (ve insectos). Se administra quetiapina oral, acepta con dificultad. Hidratacion IV. Sin evidencia de consumo activo. Monitoreo continuo.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 3: Alucinaciones resueltas. Orientado en persona y lugar, confuso en tiempo. Reporta craving intenso por sustancias. Se aplica protocolo de manejo de craving. Come 50% de racion. Sueño fragmentado (4 horas).', (a.admission_date + INTERVAL '3 days' + TIME '10:00'), (a.admission_date + INTERVAL '3 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 6: Orientado en 3 esferas. Craving disminuido. Come dieta completa. Sueño de 6 horas. Participa en grupo de prevención de recaídas. Cooperativo con personal. Verbaliza motivación para rehabilitación.', (a.admission_date + INTERVAL '6 days' + TIME '14:30'), (a.admission_date + INTERVAL '6 days' + TIME '14:30'), (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Carmen Flores Mejía (PTSD + suicidal ideation, 5d, 3 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente femenina de 32 años con TEPT e ideación suicida. Llanto frecuente, hipervigilante, se sobresalta con ruidos. Precauciones suicidas activadas. Se ofrece ambiente tranquilo. Alergia a penicilina registrada en expediente. Acepta sertralina oral.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Turno nocturno día 2: Pesadillas frecuentes, despertó gritando a las 02:00. Se acompañó hasta que se calmó. Se administró clonazepam PRN. Logró dormir 4 horas adicionales. Por la mañana reporta flashbacks. Se acompaña a actividades de relajación.', (a.admission_date + INTERVAL '2 days' + TIME '07:00'), (a.admission_date + INTERVAL '2 days' + TIME '07:00'), (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 4: Leve mejoría en ánimo. Pesadillas persisten pero menos intensas. Participó en meditación guiada. Niega ideación suicida activa. Ingesta 60%. Contacto telefónico con hermana fue positivo.', (a.admission_date + INTERVAL '4 days' + TIME '16:00'), (a.admission_date + INTERVAL '4 days' + TIME '16:00'), (SELECT id FROM users WHERE username = 'chiefnurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Roberto Díaz Vargas (Bipolar II depressive, 4d, 2 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente masculino de 58 años con depresión bipolar. Enlentecido, anhedonia severa, insomnio. Antecedentes: cardiopatía isquémica, hipotiroidismo. Se toman SV: PA 132/84, FC 68, regular. Dieta baja en sodio indicada. Se administra quetiapina 300mg HS.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 3: Sueño mejorado (6 horas con quetiapina). Persiste enlentecimiento pero coopera con actividades basicas. PA 128/80. Ingesta 50%. Acepta participar en terapia ocupacional. Esposa lo visito, interacción breve pero positiva.', (a.admission_date + INTERVAL '3 days' + TIME '11:00'), (a.admission_date + INTERVAL '3 days' + TIME '11:00'), (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Sofia Ramírez Paz (BPD + self-harm, 3d, 2 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso nocturna: Paciente femenina de 24 años con TLP, ingresa por crisis con autolesiónes (cortes superficiales en antebrazos). Heridas limpias, no requieren sutura. Curación con antiséptico. Protocolo de seguridad: revisión de pertenencias, retiro de objetos cortopunzantes. Llanto y rabia alternantes. Se ofrece contención verbal.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 2: Más calmada. Reconoce patrón de crisis. Sin nuevos episodios de autolesión. Heridas en cicatrización adecuada. Acepta sertralina y quetiapina. Participó en sesión individual con psicóloga. Sueño de 5 horas. Ingesta regular.', (a.admission_date + INTERVAL '2 days' + TIME '09:00'), (a.admission_date + INTERVAL '2 days' + TIME '09:00'), (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Discharged Patients ===

-- Miguel Torres Luna (Schizophrenia, 14d stay, 3 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente masculino de 40 años con esquizofrenia paranoide descompensada. Agitación severa, alucinaciones auditivas imperativas. Se administra olanzapina 10mg IM. Requirió contención verbal. Se monitorea de cerca.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 5: Disminucion de agitación. Acepta medicación oral. Alucinaciones persisten pero menos imperativas. Come con supervisión. Higiene con asistencia. Esposa capacitada sobre supervisión de medicación.', (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de egreso día 13: Paciente estable para egreso. Sin síntomas psicóticos activos. Funciónalidad basal recuperada. Esposa recibió psicoeducación sobre medicación y señales de recaida. Cita de control en 2 semanas.', (a.admission_date + INTERVAL '13 days' + TIME '12:00'), (a.admission_date + INTERVAL '13 days' + TIME '12:00'), (SELECT id FROM users WHERE username = 'chiefnurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Elena Sánchez Rivas (GAD + Panic, 11d stay, 3 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente femenina de 26 años con crisis de pánico múltiples. Temblor, taquicardia, sensacion de muerte inminente. Se calma con técnicas de respiración. Se explica plan de tratamiento. Acepta sertralina oral.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 5: Ataques de pánico reducidos significativamente. Practica técnicas de respiración de forma independiente. Sueño de 6 horas. Come completo. Participa activamente en TCC grupal.', (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (a.admission_date + INTERVAL '5 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de egreso día 10: Remisión completa de ataques de pánico. Paciente confiada en herramientas aprendidas. Se entrega plan de seguimiento ambulatorio con TCC semanal y control farmacológico en 2 semanas.', (a.admission_date + INTERVAL '10 days' + TIME '10:00'), (a.admission_date + INTERVAL '10 days' + TIME '10:00'), (SELECT id FROM users WHERE username = 'chiefnurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Francisco Mendoza Aguilar (MDD geriatric, 11d stay, 3 notes)
INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de ingreso: Paciente masculino de 72 años con depresión severa. Enlentecimiento marcado, anorexia. Movilidad limitada por artritis, requiere andador. Se inicia suplemento nutricional. Asistencia para higiene y movilizacion. Riesgo de caidas: alto.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Evaluación día 5: Leve mejoría en ingesta (60% de racion + suplemento). Durmio 5 horas con quetiapina. Movilizacion con andador y asistencia. Se incorpora a terapia ocupacional adaptada. Interacción verbal mínima pero cooperativo.', (a.admission_date + INTERVAL '5 days' + TIME '09:00'), (a.admission_date + INTERVAL '5 days' + TIME '09:00'), (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

INSERT INTO nursing_notes (admission_id, description, created_at, updated_at, created_by)
SELECT a.id, 'Nota de egreso día 10: Mejoría significativa. Come completo. Afecto reactivo. Disfruta visitas de hijos. Deambula con andador independiente. Se coordina atención domiciliaria para artritis. Hijos comprometidos con supervisión.', (a.admission_date + INTERVAL '10 days' + TIME '14:00'), (a.admission_date + INTERVAL '10 days' + TIME '14:00'), (SELECT id FROM users WHERE username = 'chiefnurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';


-- ============================================================================
-- VITAL SIGNS (2 per day per patient: morning 06:00, evening 20:00)
-- Using generate_series for efficient bulk generation
-- ============================================================================

-- === Juan Pérez González (14d, normal vitals, stable depression) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  118 + (gs.d % 7) - 3,
  76 + (gs.d % 5) - 2,
  72 + (gs.d % 6) - 2,
  16 + (gs.d % 3) - 1,
  36.4 + (gs.d % 4) * 0.1,
  97 + (gs.d % 3),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 14))::INT) AS gs(d)
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  120 + (gs.d % 5) - 2,
  78 + (gs.d % 4) - 2,
  74 + (gs.d % 5) - 2,
  15 + (gs.d % 3),
  36.5 + (gs.d % 3) * 0.1,
  97 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 14))::INT) AS gs(d)
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Maria Santos López (12d, slightly elevated HR during mania, normalizing) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  122 + (gs.d % 6) - 3,
  78 + (gs.d % 5) - 2,
  CASE WHEN gs.d < 4 THEN 92 + (gs.d % 5) - 2
       WHEN gs.d < 8 THEN 82 + (gs.d % 5) - 2
       ELSE 72 + (gs.d % 5) - 2 END,
  CASE WHEN gs.d < 4 THEN 18 + (gs.d % 3) - 1 ELSE 16 + (gs.d % 3) - 1 END,
  36.5 + (gs.d % 3) * 0.1,
  97 + (gs.d % 3),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 12))::INT) AS gs(d)
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  124 + (gs.d % 5) - 2,
  80 + (gs.d % 4) - 2,
  CASE WHEN gs.d < 4 THEN 95 + (gs.d % 4) - 2
       WHEN gs.d < 8 THEN 85 + (gs.d % 4) - 2
       ELSE 74 + (gs.d % 4) - 2 END,
  CASE WHEN gs.d < 4 THEN 18 + (gs.d % 2) ELSE 16 + (gs.d % 2) END,
  36.5 + (gs.d % 4) * 0.1,
  97 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 12))::INT) AS gs(d)
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Pedro García Hernández (10d, ALCOHOL WITHDRAWAL - elevated first 3d, normalizing) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, other, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  CASE WHEN gs.d < 3 THEN 148 + (gs.d % 7) - 3
       WHEN gs.d < 6 THEN 135 + (gs.d % 6) - 3
       ELSE 122 + (gs.d % 5) - 2 END,
  CASE WHEN gs.d < 3 THEN 94 + (gs.d % 5) - 2
       WHEN gs.d < 6 THEN 86 + (gs.d % 4) - 2
       ELSE 78 + (gs.d % 4) - 2 END,
  CASE WHEN gs.d < 3 THEN 105 + (gs.d % 8) - 3
       WHEN gs.d < 6 THEN 88 + (gs.d % 6) - 3
       ELSE 74 + (gs.d % 5) - 2 END,
  CASE WHEN gs.d < 3 THEN 20 + (gs.d % 3) - 1 ELSE 16 + (gs.d % 3) - 1 END,
  CASE WHEN gs.d < 3 THEN 37.2 + (gs.d % 3) * 0.1
       WHEN gs.d < 6 THEN 36.9 + (gs.d % 3) * 0.1
       ELSE 36.4 + (gs.d % 3) * 0.1 END,
  CASE WHEN gs.d < 3 THEN 95 + (gs.d % 3) ELSE 97 + (gs.d % 3) END,
  CASE WHEN gs.d < 3 THEN 'CIWA-Ar: ' || (22 - gs.d * 4)::TEXT || '. Temblor ' || CASE WHEN gs.d = 0 THEN 'grueso' WHEN gs.d = 1 THEN 'moderado' ELSE 'fino' END || ', diaforesis.'
       WHEN gs.d < 6 THEN 'CIWA-Ar: ' || (10 - (gs.d - 3) * 2)::TEXT || '. Sin temblor.'
       ELSE NULL END,
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 10))::INT) AS gs(d)
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, other, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  CASE WHEN gs.d < 3 THEN 152 + (gs.d % 5) - 2
       WHEN gs.d < 6 THEN 138 + (gs.d % 5) - 2
       ELSE 124 + (gs.d % 5) - 2 END,
  CASE WHEN gs.d < 3 THEN 96 + (gs.d % 4) - 2
       WHEN gs.d < 6 THEN 88 + (gs.d % 4) - 2
       ELSE 80 + (gs.d % 4) - 2 END,
  CASE WHEN gs.d < 3 THEN 108 + (gs.d % 6) - 3
       WHEN gs.d < 6 THEN 90 + (gs.d % 5) - 2
       ELSE 76 + (gs.d % 5) - 2 END,
  CASE WHEN gs.d < 3 THEN 19 + (gs.d % 2) ELSE 16 + (gs.d % 2) END,
  CASE WHEN gs.d < 3 THEN 37.3 + (gs.d % 3) * 0.1
       WHEN gs.d < 6 THEN 37.0 + (gs.d % 2) * 0.1
       ELSE 36.5 + (gs.d % 3) * 0.1 END,
  CASE WHEN gs.d < 3 THEN 95 + (gs.d % 2) ELSE 97 + (gs.d % 2) END,
  CASE WHEN gs.d < 3 THEN 'Glucosa capilar: ' || (150 - gs.d * 5)::TEXT || ' mg/dL. Diaforesis nocturna.' ELSE NULL END,
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 10))::INT) AS gs(d)
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Ana Martínez Ruiz (8d, HTA controlled, normalizing) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  CASE WHEN gs.d < 3 THEN 138 + (gs.d % 5) - 2 ELSE 128 + (gs.d % 5) - 2 END,
  CASE WHEN gs.d < 3 THEN 88 + (gs.d % 4) - 2 ELSE 80 + (gs.d % 4) - 2 END,
  70 + (gs.d % 6) - 2,
  16 + (gs.d % 3) - 1,
  36.4 + (gs.d % 4) * 0.1,
  97 + (gs.d % 3),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 8))::INT) AS gs(d)
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  CASE WHEN gs.d < 3 THEN 140 + (gs.d % 4) - 2 ELSE 130 + (gs.d % 4) - 2 END,
  CASE WHEN gs.d < 3 THEN 90 + (gs.d % 3) - 1 ELSE 82 + (gs.d % 3) - 1 END,
  72 + (gs.d % 5) - 2,
  16 + (gs.d % 2),
  36.5 + (gs.d % 3) * 0.1,
  97 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 8))::INT) AS gs(d)
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Luis Morales Castro (7d, slightly elevated first 2d from substance use) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  CASE WHEN gs.d < 2 THEN 132 + (gs.d % 5) - 2 ELSE 120 + (gs.d % 5) - 2 END,
  CASE WHEN gs.d < 2 THEN 84 + (gs.d % 4) - 2 ELSE 76 + (gs.d % 4) - 2 END,
  CASE WHEN gs.d < 2 THEN 88 + (gs.d % 5) - 2 ELSE 74 + (gs.d % 5) - 2 END,
  16 + (gs.d % 3) - 1,
  36.4 + (gs.d % 4) * 0.1,
  97 + (gs.d % 3),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 7))::INT) AS gs(d)
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  CASE WHEN gs.d < 2 THEN 134 + (gs.d % 4) - 2 ELSE 122 + (gs.d % 4) - 2 END,
  CASE WHEN gs.d < 2 THEN 86 + (gs.d % 3) - 1 ELSE 78 + (gs.d % 3) - 1 END,
  CASE WHEN gs.d < 2 THEN 90 + (gs.d % 4) - 2 ELSE 76 + (gs.d % 4) - 2 END,
  16 + (gs.d % 2),
  36.5 + (gs.d % 3) * 0.1,
  97 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 7))::INT) AS gs(d)
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Carmen Flores Mejía (5d, normal vitals) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  112 + (gs.d % 6) - 2,
  72 + (gs.d % 5) - 2,
  70 + (gs.d % 6) - 2,
  15 + (gs.d % 3),
  36.3 + (gs.d % 4) * 0.1,
  98 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 5))::INT) AS gs(d)
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  114 + (gs.d % 5) - 2,
  74 + (gs.d % 4) - 2,
  72 + (gs.d % 5) - 2,
  16 + (gs.d % 2),
  36.4 + (gs.d % 3) * 0.1,
  97 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 5))::INT) AS gs(d)
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Roberto Díaz Vargas (4d, cardiac patient, slightly lower HR) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  130 + (gs.d % 5) - 2,
  82 + (gs.d % 4) - 2,
  66 + (gs.d % 5) - 2,
  16 + (gs.d % 3) - 1,
  36.3 + (gs.d % 4) * 0.1,
  96 + (gs.d % 3),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 4))::INT) AS gs(d)
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  132 + (gs.d % 4) - 2,
  84 + (gs.d % 3) - 1,
  68 + (gs.d % 4) - 2,
  16 + (gs.d % 2),
  36.4 + (gs.d % 3) * 0.1,
  96 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 4))::INT) AS gs(d)
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Sofia Ramírez Paz (3d, young female, normal vitals) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  110 + (gs.d % 5) - 2,
  70 + (gs.d % 4) - 2,
  74 + (gs.d % 5) - 2,
  16 + (gs.d % 3) - 1,
  36.4 + (gs.d % 3) * 0.1,
  98 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 3))::INT) AS gs(d)
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  112 + (gs.d % 4) - 2,
  72 + (gs.d % 3) - 1,
  76 + (gs.d % 4) - 2,
  15 + (gs.d % 3),
  36.5 + (gs.d % 3) * 0.1,
  98 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (CURRENT_DATE - 1 - (CURRENT_DATE - 3))::INT) AS gs(d)
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Discharged: Miguel Torres Luna (14d stay, normal vitals) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  120 + (gs.d % 6) - 3,
  78 + (gs.d % 5) - 2,
  74 + (gs.d % 6) - 2,
  16 + (gs.d % 3) - 1,
  36.4 + (gs.d % 4) * 0.1,
  97 + (gs.d % 3),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  122 + (gs.d % 5) - 2,
  80 + (gs.d % 4) - 2,
  76 + (gs.d % 5) - 2,
  16 + (gs.d % 2),
  36.5 + (gs.d % 3) * 0.1,
  97 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- === Discharged: Elena Sánchez Rivas (11d stay, slightly tachycardic initially) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  116 + (gs.d % 6) - 3,
  74 + (gs.d % 5) - 2,
  CASE WHEN gs.d < 3 THEN 86 + (gs.d % 5) - 2 ELSE 72 + (gs.d % 5) - 2 END,
  16 + (gs.d % 3) - 1,
  36.4 + (gs.d % 3) * 0.1,
  98 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse2')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  118 + (gs.d % 5) - 2,
  76 + (gs.d % 4) - 2,
  CASE WHEN gs.d < 3 THEN 88 + (gs.d % 4) - 2 ELSE 74 + (gs.d % 4) - 2 END,
  16 + (gs.d % 2),
  36.5 + (gs.d % 3) * 0.1,
  98 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse4')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- === Discharged: Francisco Mendoza Aguilar (11d stay, elderly, slightly higher BP) ===
-- Morning readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '06:00',
  134 + (gs.d % 6) - 3,
  82 + (gs.d % 5) - 2,
  68 + (gs.d % 6) - 2,
  16 + (gs.d % 3) - 1,
  36.3 + (gs.d % 4) * 0.1,
  96 + (gs.d % 3),
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (a.admission_date::DATE + gs.d) + TIME '06:15',
  (SELECT id FROM users WHERE username = 'nurse3')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
-- Evening readings
INSERT INTO vital_signs (admission_id, recorded_at, systolic_bp, diastolic_bp, heart_rate, respiratory_rate, temperature, oxygen_saturation, created_at, updated_at, created_by)
SELECT a.id,
  (a.admission_date::DATE + gs.d) + TIME '20:00',
  136 + (gs.d % 5) - 2,
  84 + (gs.d % 4) - 2,
  70 + (gs.d % 5) - 2,
  16 + (gs.d % 2),
  36.4 + (gs.d % 3) * 0.1,
  96 + (gs.d % 2),
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (a.admission_date::DATE + gs.d) + TIME '20:15',
  (SELECT id FROM users WHERE username = 'nurse1')
FROM admissions a JOIN patients p ON a.patient_id = p.id
CROSS JOIN generate_series(0, (a.discharge_date::DATE - 1 - a.admission_date::DATE)::INT) AS gs(d)
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

SET session_replication_role = DEFAULT;
