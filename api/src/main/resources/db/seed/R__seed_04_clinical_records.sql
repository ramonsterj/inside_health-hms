-- ============================================================================
-- SEED FILE 04: Clínical Histories, Progress Notes, Medical Orders
-- ============================================================================

SET session_replication_role = replica;

-- ============================================================================
-- CLINICAL HISTORIES (11 records - 1 per hospitalized admission)
-- ============================================================================

-- Juan Pérez González (MDD severe + suicide attempt)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Intento suicida por sobreingesta de benzodiacepinas. Hallado inconsciente por esposa, trasladado a emergencia.',
  'Paciente masculino de 45 años con TDM severo recurrente, presenta empeoramiento progresivo de síntomas depresivos en últimas 6 semanas. Anhedonia severa, insomnio terminal, pérdida de 8 kg. Realizo intento suicida ingiriendo 20 tabletas de clonazepam.',
  'Dos hospitalizaciones psiquiátricas previas por episodios depresivos severos. Primer episodio a los 30 años. Tratamiento previo con fluoxetina y venlafaxina con respuesta parcial.',
  'Alergia a sulfonamidas. Sin otras patologías médicas relevantes. Niega cirugías previas.',
  'Padre con antecedente de trastorno depresivo mayor. Tia materna con intento suicida. Madre sana.',
  'Nacido en Guatemala City, tercero de cuatro hermanos. Casado desde hace 20 años, dos hijos adolescentes.',
  'Niega consumo de alcohol, tabaco o sustancias ilícitas.',
  'Sin antecedentes legales relevantes.',
  'Vive con esposa e hijos. Red de apoyo familiar presente pero limitada. Conflictos conyugales recientes por pérdida de empleo.',
  'Desarrollo psicomotor normal. Sin antecedentes de maltrato infantil.',
  'Licenciatura en contaduría. Desempleado desde hace 3 meses, factor precipitante del episodio actual.',
  'Vida sexual activa con pareja estable. Niega disfunción sexual previa al episodio actual.',
  'Catolico practicante, refiere crisis de fe durante episodio actual.',
  'Consciente, orientado en 3 esferas. Afecto aplanado, ánimo deprimido severo. Pensamiento con contenido de desesperanza y culpa. Ideación suicida activa con plan estructurado. Juicio deteriorado. Insight parcial.',
  'PA 125/82, FC 78, FR 16, T 36.5. Pupilas isocóricas reactivas. Sin signos de intoxicación aguda residual. Cicatrices antiguas en antebrazos.',
  'F32.2 Episodio depresivo grave sin síntomas psicóticos (CIE-10). F33.2 Trastorno depresivo recurrente, episodio actual grave. Intento suicida reciente (X61).',
  'Hospitalización en unidad cerrada. Precauciones suicidas estrictas. Iniciar Sertralina 50mg BID. Quetiapina 300mg HS para insomnio. Psicoterapia individual y grupal. Evaluación por trabajo social.',
  'Riesgo suicida ALTO. Intento reciente con método letal accesible. Factores de riesgo: sexo masculino, desempleo, aislamiento, ideación persistente. Factor protector: familia presente.',
  'Reservado a corto plazo. Favorable a mediano plazo con adherencia al tratamiento y resolución de estresores psicosociales.',
  'Paciente y esposa informados sobre diagnóstico, plan de tratamiento y riesgos. Consentimiento informado firmado por esposa como responsable.',
  'Referido a trabajo social para evaluación de red de apoyo y orientación laboral.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Maria Santos López (Bipolar I manic + psychosis)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Episodio maníaco con síntomas psicóticos. Insomnio de 4 días, conducta desorganizada y delirios de grandeza.',
  'Mujer de 28 años con TAB I diagnósticado a los 22 años. Presenta episodio maníaco de 10 días de evolución con grandiosidad, disminución de necesidad de sueño, verborrea, gastos excesivos e ideas delirantes de misión religiosa.',
  'Diagnóstico de TAB I a los 22 años. Litio previo descontinuado por nefrotoxicidad. Un episodio maníaco previo y dos episodios depresivos.',
  'Sin alergias conocidas. Nefrotoxicidad previa por litio, función renal actualmente normal.',
  'Madre con trastorno bipolar II. Abuelo materno con esquizofrenia.',
  'Nacida en Quetzaltenango, hija unica. Soltera, vive sola.',
  'Niega consumo de sustancias. Consumo social de alcohol suspendido desde diagnóstico.',
  'Sin antecedentes legales.',
  'Vive sola en apartamento. Trabaja como disenadora gráfica independiente. Red de apoyo limitada, madre vive en otra ciudad.',
  'Desarrollo normal. Buena adaptación escolar.',
  'Licenciatura en diseno gráfico. Trabajo independiente, ingresos variables.',
  'Soltera, relaciones afectivas inestables durante episodios maníacos.',
  'Sin práctica religiosa formal, pero delirios actuales con contenido religioso.',
  'Consciente, desorientada en tiempo. Afecto expansivo e irritable. Habla presionada, fuga de ideas. Delirios de grandeza con contenido mesiánico. Alucinaciones auditivas ocasionales. Sin insight.',
  'PA 135/85, FC 102, FR 20, T 36.8. Agitada, deshidratación leve. Sin focalidad neurológica.',
  'F31.2 Trastorno bipolar, episodio actual maníaco con síntomas psicóticos (CIE-10).',
  'Hospitalización. Quetiapina 300mg BID como antipsicótico y estabilizador. Biperiden 2mg BID profiláctico. Midazolam 15mg IM PRN. Ambiente bajo estímulo. Laboratorios incluyendo perfil tiroideo.',
  'Riesgo de agresion MODERADO por irritabilidad y agitación. Riesgo suicida bajo. Riesgo de conductas impulsivas alto.',
  'Favorable con estabilización farmacológica. Requiere tratamiento de mantenimiento a largo plazo.',
  'Consentimiento firmado por madre como responsable dado el estado clínico de la paciente.',
  'Contactar madre para psicoeducación sobre la enfermedad.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Pedro García Hernández (Alcohol withdrawal)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Síndrome de abstinencia alcohólica severa con riesgo de delirium tremens. Último consumo hace 48 horas.',
  'Hombre de 52 años con consumo de alcohol de 30 años de evolución, pauta de 1 litro de aguardiente diario. Suspendio consumo abruptamente hace 48 horas. Presenta temblor, diaforesis, náuseas, ansiedad severa y alucinaciones visuales incipientes.',
  'Sin hospitalizaciones psiquiátricas previas. Nunca ha recibido tratamiento por dependencia al alcohol.',
  'DM2 en tratamiento con metformina. Hepatopatía alcohólica documentada. Fumador de 20 cigarrillos/día. Alergia: ninguna conocida.',
  'Padre fallecido por cirrosis hepática. Hermano mayor con dependencia al alcohol.',
  'Nacido en area rural de Huehuetenango, migró a la capital a los 18 años.',
  'Alcohol: consumo diario de 1 litro de aguardiente por 30 años. Tabaco: 20 cigarrillos/día por 25 años. Niega otras sustancias.',
  'Un arresto por conducir bajo efectos del alcohol hace 5 años.',
  'Divorciado, vive solo. Relacion distante con dos hijos adultos. Aislamiento social progresivo.',
  'Desarrollo normal. Antecedente de padre violento.',
  'Primaria completa. Trabaja como albanil, empleo irregular.',
  'Divorciado hace 8 años. Sin pareja actual.',
  'Catolico nominal, no practicante.',
  'Consciente, orientado parcialmente. Ansioso, diaforético, temblor grueso distal. Alucinaciones visuales incipientes. CIWA-Ar: 22 puntos (abstinencia severa).',
  'PA 158/98, FC 112, FR 22, T 37.3. Hepatomegalia 3 cm, spider naevi en torso. Temblor generalizado. Glucosa capilar 185 mg/dL.',
  'F10.3 Síndrome de abstinencia alcohólica con delirium (CIE-10). F10.2 Dependencia al alcohol. E11 DM2. K70 Hepatopatía alcohólica.',
  'Hospitalización. Protocolo CIWA-Ar cada 4 horas. Diazepam 10mg IM PRN según escala. Pregabalina 75mg BID. Dieta diabética. Laboratorios completos incluyendo panel hepático y toxicológico.',
  'Riesgo de convulsiones y delirium tremens ALTO. Riesgo suicida bajo. Riesgo médico alto por comorbilidades.',
  'Reservado por múltiples comorbilidades y cronicidad del consumo. Requiere programa de rehabilitación posterior.',
  'Paciente informado sobre riesgos de abstinencia y plan de tratamiento. Consentimiento firmado.',
  'Referir a programa de rehabilitación de adicciones al egreso. Control glicemico estricto.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Ana Martínez Ruiz (Paranoid schizophrenia)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Descompensacion psicótica por abandono de tratamiento de 2 meses. Ideas paranoides y alucinaciones auditivas.',
  'Mujer de 38 años con esquizofrenia paranoide diagnósticada a los 25 años. Abandono olanzapina hace 2 meses por aumento de peso. Reactivacíon de síntomas psicóticos con ideas de persecución, alucinaciones auditivas y aislamiento progresivo.',
  'Esquizofrenia paranoide desde los 25 años, 3 hospitalizaciones previas. Mejor respuesta a olanzapina. Alergia a haloperidol (distonía aguda). Intentos previos con risperidona con respuesta parcial.',
  'HTA controlada con enalapril 10mg. Alergia: Haloperidol (distonía aguda severa).',
  'Sin antecedentes psiquiátricos familiares conocidos.',
  'Nacida en Guatemala City, segunda de tres hermanos. Soltera, vive con hijo de 15 años y madre.',
  'Niega consumo de sustancias.',
  'Sin antecedentes legales.',
  'Vive con madre e hijo. Red de apoyo familiar limitada. Madre es cuidadora principal.',
  'Desarrollo normal. Rendimiento escolar adecuado hasta secundaria.',
  'Secundaria completa. Sin empleo formal por enfermedad. Recibe pensión por discapacidad.',
  'Un hijo de 15 años, padre ausente. Sin pareja actual.',
  'Evangelica, asiste ocasionalmente a iglesia.',
  'Consciente, orientada parcialmente. Suspicaz, evita contacto visual. Ideas delirantes paranoides sistematizadas (vecinos la espian). Alucinaciones auditivas imperativas. Afecto inapropiado. Sin insight.',
  'PA 148/92, FC 76, FR 16, T 36.4. IMC 32. Sin signos neurológicos focales.',
  'F20.0 Esquizofrenia paranoide (CIE-10). Descompensacion por abandono terapéutico. I10 HTA.',
  'Hospitalización. Olanzapina 5mg BID (evitar haloperidol). Biperiden 2mg BID profiláctico. Clonazepam 2mg PRN. Control de PA cada 8 horas. Psicoeducación familiar.',
  'Riesgo de heteroagresion bajo-moderado (voces imperativas). Riesgo suicida bajo. Riesgo de abandono terapéutico alto.',
  'Favorable con reintroducción de olanzapina. Fundamental asegurar adherencia al egreso.',
  'Madre firma consentimiento como responsable legal.',
  'Coordinar con centro de salud para seguimiento ambulatorio y aplicación de antipsicótico de depósito si no hay adherencia oral.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Luis Morales Castro (Polysubstance + psychosis)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Psicosis inducida por sustancias con agitación psicomotora. Traido por policia tras conducta desorganizada en via pública.',
  'Hombre de 22 años sin antecedentes psiquiátricos previos. Consumo de cocaína, cannabis y benzodiacepinas en últimos 3 días en contexto de fiesta. Presenta ideas paranoides, agitación y conducta desorganizada.',
  'Sin antecedentes psiquiátricos previos. Primera presentación psicótica.',
  'Sin antecedentes médicos relevantes. Sin alergias conocidas.',
  'Padre con trastorno por uso de alcohol. Sin otros antecedentes psiquiátricos familiares.',
  'Nacido en zona 18, Guatemala City. Hijo único de padres separados.',
  'Cannabis desde los 16 años (diario). Cocaina desde los 19 (fines de semana). Benzodiacepinas recreativas. Alcohol social. Niega drogas inyectables.',
  'Sin antecedentes legales formales.',
  'Vive con padre. Circulo social asociado a consumo de sustancias. Red de apoyo limitada.',
  'Desarrollo normal. Problemas de conducta en adolescencia.',
  'Bachillerato incompleto. Trabaja esporádicamente como repartidor.',
  'Soltero, sin hijos. Relaciones afectivas inestables.',
  'Sin práctica religiosa.',
  'Consciente, desorientado en tiempo y espacio. Agitado, suspicaz. Ideas paranoides no sistematizadas. Alucinaciones visuales y auditivas. Habla incoherente. Sin insight. Juicio severamente alterado.',
  'PA 138/88, FC 96, FR 20, T 36.9. Pupilas midriáticas. Diaforetico. Sin signos meningeos.',
  'F19.5 Trastorno psicótico inducido por uso de múltiples sustancias (CIE-10). F19.2 Dependencia a múltiples sustancias.',
  'Hospitalización. Quetiapina 100mg BID. Biperiden 2mg BID. Monitoreo de abstinencia. Panel toxicológico. Ambiente controlado.',
  'Riesgo de heteroagresion moderado por agitación. Riesgo suicida bajo. Riesgo de fuga moderado.',
  'Favorable para resolución de psicosis (probablemente sustancia-inducida). Pronóstico de adiccion reservado sin tratamiento de rehabilitación.',
  'Padre firma consentimiento informado.',
  'Referir a programa de rehabilitación de adicciones. Evaluar persistencia de síntomas psicóticos tras aclaramiento de sustancias.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Carmen Flores Mejía (PTSD + suicidal ideation)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Ideación suicida activa en contexto de TEPT crónico por violencia intrafamiliar.',
  'Mujer de 34 años con TEPT crónico secundario a 8 años de violencia intrafamiliar. Separada hace 6 meses. Presenta flashbacks diarios, pesadillas, hipervigilancia e ideación suicida activa con plan de ahorcamiento.',
  'Diagnóstico de TEPT hace 2 años. Tratamiento ambulatorio irregular con sertralina. Una hospitalización previa por crisis suicida.',
  'Alergia a penicilina. Sin otros antecedentes médicos.',
  'Madre con depresión. Sin otros antecedentes psiquiátricos familiares.',
  'Nacida en Coban, Alta Verapaz. Casada a los 22 años, separada hace 6 meses tras denuncia de violencia.',
  'Niega consumo de sustancias.',
  'Denuncia vigente contra expareja por violencia intrafamiliar.',
  'Vive con madre y dos hijos (8 y 6 anos). Orden de restricción contra expareja. Red de apoyo familiar presente.',
  'Desarrollo normal. Ambiente familiar de origen funciónal.',
  'Perito contador. Trabajaba en oficina contable, renunció por acoso de expareja en lugar de trabajo.',
  'Refiere que relacion conyugal fue inicialmente adecuada. Violencia inició al tercer año de matrimonio.',
  'Catolica practicante, fuente de apoyo.',
  'Consciente, orientada. Afecto constreñido, llanto facil. Ánimo deprimido moderado. Ideación suicida activa con plan. Flashbacks durante entrevista. Hipervigilancia marcada. Insight presente.',
  'PA 118/74, FC 68, FR 16, T 36.3. Marcas antiguas de agresion en brazos. Sin hallazgos agudos.',
  'F43.1 TEPT crónico (CIE-10). F32.1 Episodio depresivo moderado. Ideación suicida activa.',
  'Hospitalización. Sertralina 50mg QD. Clonazepam 2mg HS. Precauciones suicidas. Psicoterapia de trauma (EMDR/TCC). Evaluación por trabajo social.',
  'Riesgo suicida MODERADO-ALTO. Plan estructurado pero factores protectores presentes (hijos, fe, red familiar).',
  'Favorable con tratamiento integral. Requiere abordaje sostenido del trauma y fortalecimiento de red de apoyo.',
  'Paciente firma consentimiento informado. Capacidad de decision preservada.',
  'Coordinar con trabajo social para proteccion legal y apoyo socioeconomico.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Roberto Díaz Vargas (Bipolar II depressive)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Episodio depresivo mayor en contexto de TAB II con ideación suicida pasiva y deterioro funciónal severo.',
  'Hombre de 58 años con TAB II conocido. Episodio depresivo de 3 meses con anhedonia profunda, hipersomnia, enlentecimiento psicomotor e ideación suicida pasiva. No responde a lamotrigina actual.',
  'TAB II diagnósticado a los 40 años. Múltiples episodios depresivos. Hipomanias leves. Lamotrigina de mantenimiento.',
  'Cardiopatía isquémica estable, stent coronario hace 3 años. Hipotiroidismo en tratamiento con levotiroxina. Sin alergias.',
  'Hermana con trastorno depresivo mayor.',
  'Nacido en Guatemala City, casado, dos hijos adultos.',
  'Exfumador (dejo hace 5 anos). Consumo social mínimo de alcohol.',
  'Sin antecedentes legales.',
  'Vive con esposa. Hijos independientes. Buena red de apoyo familiar. Jubilado.',
  'Desarrollo normal.',
  'Ingeniero civil jubilado. Carrera profesional éxitosa.',
  'Casado 32 años, relacion estable. Refiere disminución de libido por depresión.',
  'Catolico practicante.',
  'Consciente, orientado. Enlentecimiento psicomotor marcado. Afecto deprimido, voz monotona. Ideación suicida pasiva sin plan. Insight presente.',
  'PA 128/78, FC 58, FR 14, T 36.2. Bradicardia relativa (betabloqueador). Sin edema. Tiroides no palpable.',
  'F31.3 TAB tipo II, episodio actual depresivo grave (CIE-10). I25 Cardiopatía isquémica. E03 Hipotiroidismo.',
  'Hospitalización. Quetiapina 300mg HS (aprobado para depresión bipolar). Perfil tiroideo. Dieta baja en sodio. Monitoreo cardiológico.',
  'Riesgo suicida bajo-moderado (ideación pasiva, factores protectores significativos).',
  'Favorable con optimización farmacológica. Buen soporte familiar y insight preservado.',
  'Paciente firma consentimiento informado.',
  'Coordinar con cardiología para compatibilidad farmacológica.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Sofia Ramírez Paz (BPD + self-harm)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Autolesiones recurrentes (cutting en antebrazos) con ideación suicida ambivalente tras ruptura sentimental.',
  'Mujer de 24 años con TLP conocido. Presenta crisis emocional tras ruptura con pareja de 2 años. Múltiples cortes superficiales en antebrazos, llanto incontrolable, amenazas suicidas.',
  'TLP diagnósticado a los 19 años. Tres ingresos previos por crisis con autolesiónes. DBT ambulatoria interrumpida.',
  'Sin antecedentes médicos relevantes. Sin alergias.',
  'Madre con rasgos de personalidad limite. Padre ausente.',
  'Nacida en Antigua Guatemala. Hija única de madre soltera.',
  'Niega consumo de sustancias actualmente. Consumo experimental de alcohol en el pasado.',
  'Sin antecedentes legales.',
  'Vive con madre. Relaciones interpersonales intensas e inestables. Red de apoyo limitada.',
  'Desarrollo normal. Abandono paterno a los 3 años, factor de vulnerabilidad.',
  'Estudiante universitaria de psicologia, 6to semestre. Rendimiento irregular.',
  'Relaciones sentimentales intensas y breves. Ruptura actual como factor precipitante.',
  'Sin práctica religiosa.',
  'Consciente, orientada. Labil emocionalmente, oscila entre llanto y enojo. Ideación suicida ambivalente. Heridas superficiales recientes en antebrazos. Insight fluctuante.',
  'PA 110/68, FC 88, FR 18, T 36.5. Múltiples cicatrices lineales en antebrazos bilaterales. Heridas superficiales recientes suturadas en emergencia.',
  'F60.31 TLP (CIE-10). Autolesiones no suicidas recurrentes. Crisis emocional aguda.',
  'Hospitalización breve para estabilización. Sertralina 50mg QD. Quetiapina 100mg HS. Protocolo de seguridad por autolesiónes. Retomar DBT al egreso.',
  'Riesgo suicida moderado (autolesiónes crónicas, impulsividad). Riesgo de autolesión alto.',
  'Variable a corto plazo. Favorable a largo plazo si mantiene DBT y tratamiento farmacológico.',
  'Paciente firma consentimiento. Madre contactada como apoyo.',
  'Prioridad: retomar terapia dialéctica conductual ambulatoria al egreso.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Miguel Torres Luna (Schizophrenia - DISCHARGED)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Descompensacion psicótica con agitación psicomotora severa y conducta heteroagresiva.',
  'Hombre de 42 años con esquizofrenia paranoide crónica. Múltiples hospitalizaciones. Abandono medicación hace 3 semanas. Agitación progresiva, alucinaciones y amenazas a familiares.',
  'Esquizofrenia paranoide desde los 20 años. Más de 8 hospitalizaciones. Respondedor parcial a antipsicóticos típicos. Mejor respuesta con olanzapina.',
  'Sin patologías médicas relevantes. Sin alergias conocidas.',
  'Tio materno con esquizofrenia.',
  'Nacido en Guatemala City. Casado, un hijo de 18 años.',
  'Niega consumo de sustancias.',
  'Sin antecedentes legales.',
  'Vive con esposa e hijo. Esposa es cuidadora principal. Discapacidad funciónal crónica.',
  'Desarrollo normal hasta los 18 años. Ruptura funciónal con inició de enfermedad.',
  'Bachillerato completo. Sin empleo formal por enfermedad. Pension por discapacidad.',
  'Casado 20 años, relacion estable pero tensa por enfermedad.',
  'Catolico, asiste con esposa.',
  'Agitado, hostil, desorientado parcialmente. Alucinaciones auditivas imperativas. Ideas de persecución contra vecinos. Sin insight.',
  'PA 122/78, FC 74, FR 16, T 36.5. Higiene deficiente. Sin hallazgos médicos agudos.',
  'F20.0 Esquizofrenia paranoide crónica (CIE-10). Descompensacion por abandono terapéutico.',
  'Hospitalización. Olanzapina IM PRN para agitación, luego oral 5mg BID. Biperiden 2mg BID. Monitoreo conductual. Psicoeducación familiar.',
  'Riesgo de heteroagresion moderado en fase aguda. Riesgo suicida bajo.',
  'Estabilizacion esperable con reintroducción de olanzapina. Pronóstico a largo plazo reservado por cronicidad.',
  'Esposa firma consentimiento como responsable.',
  'Evaluar antipsicótico de depósito para prevenir abandono terapéutico.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Elena Sánchez Rivas (GAD + Panic - DISCHARGED)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'Ataques de pánico múltiples diarios incapacitantes con agorafobia secundaria.',
  'Mujer de 29 años sin antecedentes psiquiátricos previos. Presenta 3-4 ataques de pánico diarios con palpitaciones, disnea y miedo a morir, de 2 meses de evolución. Incapacidad laboral y agorafobia progresiva.',
  'Primera presentación psiquiátrica. Sin tratamiento previo.',
  'Sin antecedentes médicos. Sin alergias. EKG y ecocardiograma normales (descartada causa cardiaca).',
  'Madre con TAG. Sin otros antecedentes.',
  'Nacida en Guatemala City. Soltera, vive con padres.',
  'Niega consumo de sustancias. Consumo elevado de cafeína (6 tazas de cafe al dia).',
  'Sin antecedentes legales.',
  'Vive con padres. Red de apoyo familiar solida. Amistades cercanas.',
  'Desarrollo normal. Personalidad premorbida ansiosa.',
  'Licenciatura en mercadeo. Trabaja en empresa de publicidad. Incapacidad laboral de 1 mes.',
  'Soltera, sin pareja actual. Sin hijos.',
  'Evangelica, asiste regularmente.',
  'Consciente, orientada. Ansiosa, temblor fino, tensión muscular. Miedo anticipatorio intenso. Cogniciones catastróficas. Sin ideación suicida. Insight presente.',
  'PA 118/72, FC 86, FR 18, T 36.4. Temblor fino de manos. Sin hallazgos cardiopulmonares.',
  'F41.0 Trastorno de pánico (CIE-10). F41.1 TAG. F40.0 Agorafobia incipiente.',
  'Hospitalización para estabilización. Sertralina 50mg QD. Clonazepam 2mg PRN. Perfil tiroideo. Reduccion de cafeína. TCC para pánico. Técnicas de relajación.',
  'Riesgo suicida bajo. Sin ideación. Riesgo principal: deterioro funciónal progresivo por evitacion.',
  'Muy favorable con tratamiento integral. Buen pronóstico por primer episodio, juventud, insight y red de apoyo.',
  'Paciente firma consentimiento informado.',
  'Suspender cafeína. Referir a TCC ambulatoria al egreso.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Francisco Mendoza Aguilar (MDD geriatric - DISCHARGED)
INSERT INTO clinical_histories (admission_id, reason_for_admission, history_of_present_illness, psychiatric_history, medical_history, family_history, personal_history, substance_use_history, legal_history, social_history, developmental_history, educational_occupational_history, sexual_history, religious_spiritual_history, mental_status_exam, physical_exam, diagnostic_impression, treatment_plan, risk_assessment, prognosis, informed_consent_notes, additional_notes, created_at, updated_at, created_by)
SELECT a.id,
  'TDM recurrente en adulto mayor con síntomas melancólicos severos, pérdida de peso significativa y retraimiento total.',
  'Hombre de 72 años con TDM recurrente. Episodio actual de 2 meses con anhedonia total, despertar temprano a las 3 AM, pérdida de 7 kg, enlentecimiento psicomotor severo y culpa patológica.',
  'TDM recurrente desde los 55 años. 3 episodios previos tratados con antidepresivos con buena respuesta. 3 esquemas fallidos en episodio actual.',
  'Artritis reumatoide en tratamiento con metotrexato. Movilidad limitada. Sin alergias.',
  'Sin antecedentes psiquiátricos familiares conocidos.',
  'Nacido en Escuintla. Viudo desde hace 3 años (factor precipitante del primer episodio tardío). 3 hijos adultos.',
  'Niega consumo de sustancias.',
  'Sin antecedentes legales.',
  'Vive solo desde fallecimiento de esposa. Hijos lo visitan los fines de semana. Aislamiento progresivo.',
  'Desarrollo normal. Sin eventos adversos en infancia.',
  'Maestro jubilado. 35 años de servicio en educación pública.',
  'Viudo desde hace 3 años. Matrimonio de 45 años descrito como satisfactorio.',
  'Catolico devoto, fuente de consuelo.',
  'Consciente, orientado. Enlentecimiento psicomotor severo. Afecto melancólico. Culpa excesiva. Sin ideación suicida activa. Insight presente. MMSE 26/30.',
  'PA 140/82, FC 66, FR 15, T 36.2. IMC 21 (bajo peso). Articulaciones deformadas en manos. Movilidad con andador.',
  'F33.2 TDM recurrente, episodio actual grave con síntomas melancólicos (CIE-10). Resistencia a tratamiento. M06 Artritis reumatoide.',
  'Hospitalización. Sertralina 50mg QD. Quetiapina 100mg HS. Dieta blanda con suplemento nutricional. Terapia ocupacional adaptada. Evaluar TEC si no hay respuesta.',
  'Riesgo suicida bajo (sin ideación activa, factores protectores: religion, hijos). Riesgo nutricional moderado.',
  'Moderadamente favorable. Resistencia a tratamiento es preocupante pero aún hay opciones terapéuticas.',
  'Hijo mayor firma consentimiento como responsable.',
  'Evaluar necesidad de TEC si no hay respuesta a esquema actual en 2-3 semanas. Coordinar atención domiciliaria al egreso.',
  a.admission_date, a.admission_date, a.treating_physician_id
FROM admissions a JOIN patients p ON a.patient_id = p.id
WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- ============================================================================
-- PROGRESS NOTES (~45 records, SOAP format)
-- ============================================================================

-- Juan Pérez González (6 notes: days 0,2,4,7,10,13)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Paciente refiere desesperanza intensa y culpa por intento suicida. No desea hablar.', 'O: Afecto aplanado, contacto visual mínimo, cooperación limitada. Signos vitales estables. Precauciones suicidas activas.', 'A: TDM severo con ideación suicida persistente. Fase aguda post-intento. Riesgo alto.', 'P: Continuar precauciones suicidas estrictas. Iniciar sertralina 50mg. Quetiapina 300mg HS. Psicoterapia de apoyo diaria.', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Refiere insomnio persistente y pesadillas sobre el intento. Acepta que necesita ayuda.', 'O: Leve mejoría en cooperación. Acepta medicación. Sueño fragmentado 3 horas.', 'A: Inicio de alianza terapéutica. Sertralina aún sin efecto terapéutico esperado (dia 2). Insomnio severo.', 'P: Continuar esquema. Aumentar quetiapina si persiste insomnio. Iniciar psicoterapia cognitiva.', (a.admission_date + INTERVAL '2 days'), (a.admission_date + INTERVAL '2 days'), (SELECT id FROM users WHERE username = 'doctor1') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Logró dormir 5 horas. Refiere disminución leve de angustia. Habla sobre situación laboral.', 'O: Contacto visual mejorado. Participa brevemente en actividades grupales. Ideación suicida presente pero menos intensa.', 'A: Respuesta inicial favorable a quetiapina para sueño. Sertralina requiere más tiempo. Factores psicosociales relevantes.', 'P: Mantener esquema actual. Referir a trabajo social para orientación laboral. Continuar psicoterapia.', (a.admission_date + INTERVAL '4 days'), (a.admission_date + INTERVAL '4 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Refiere mejoría del ánimo. Participa en terapia grupal. Ideación suicida disminuida significativamente.', 'O: Afecto reactivo, sonrie ocasionalmente. Sueño 6-7 horas. Apetito mejorando. Se relaciona con otros pacientes.', 'A: Respuesta terapéutica a sertralina emergiendo (semana 1). Disminucion progresiva de riesgo suicida.', 'P: Reclasificar precauciones suicidas a nivel moderado. Continuar esquema. Iniciar planificación de egreso.', (a.admission_date + INTERVAL '7 days'), (a.admission_date + INTERVAL '7 days'), (SELECT id FROM users WHERE username = 'doctor1') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Se siente mejor, con esperanza. Habla sobre planes futuros. Niega ideación suicida.', 'O: Eutimico la mayor parte del día. Participa activamente en actividades terapéuticas. Sueño y apetito normalizados.', 'A: Remisión parcial del episodio depresivo. Riesgo suicida bajo. Buena respuesta a sertralina+quetiapina.', 'P: Preparar plan de egreso. Coordinar seguimiento ambulatorio. Psicoeducación a esposa.', (a.admission_date + INTERVAL '10 days'), (a.admission_date + INTERVAL '10 days'), (SELECT id FROM users WHERE username = 'doctor6') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Listo para egreso. Refiere motivación para continuar tratamiento. Esposa confirma mejoría notable.', 'O: Eutimico, funciónalidad recuperada. Sin ideación suicida. Insight completo.', 'A: Remisión parcial sostenida. Apto para manejo ambulatorio con seguimiento cercano.', 'P: Planificar egreso en proximos días. Receta ambulatoria. Cita control en 1 semana.', (a.admission_date + INTERVAL '13 days'), (a.admission_date + INTERVAL '13 days'), (SELECT id FROM users WHERE username = 'doctor1') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Maria Santos López (5 notes: days 0,2,5,8,11)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Paciente verborreica, refiere ser elegida para misión divina. No percibe estar enferma.', 'O: Agitada, logorreica, fuga de ideas, delirios de grandeza con contenido mesiánico. FC 102. No durmió en 4 días.', 'A: Episodio maníaco severo con síntomas psicóticos. Requiere estabilización urgente.', 'P: Iniciar quetiapina 300mg BID. Biperiden profiláctico. Ambiente bajo estímulo. Midazolam PRN.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Menos agitada pero mantiene ideas grandiosas. Durmio 3 horas con medicación.', 'O: Disminucion de agitación psicomotora. Persisten delirios pero menos intensos. Acepta medicación.', 'A: Respuesta inicial a quetiapina. Mania aún activa pero en descenso.', 'P: Mantener quetiapina BID. Solicitar perfil tiroideo. Monitoreo de signos vitales.', (a.admission_date + INTERVAL '2 days'), (a.admission_date + INTERVAL '2 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Reconoce que estaba enferma. Preocupada por gastos excesivos durante mania.', 'O: Sueño 6 horas. Delirios en remisión. Afecto estabilizandose. FC normalizada.', 'A: Resolución progresiva del episodio maníaco. Emergencia de insight.', 'P: Continuar esquema. Psicoeducación sobre TAB. Contactar madre para planificar egreso.', (a.admission_date + INTERVAL '5 days'), (a.admission_date + INTERVAL '5 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Eutimia relativa. Preocupada por volver al trabajo. Comprometida con tratamiento.', 'O: Sin síntomas psicóticos. Afecto eutimico. Sueño y apetito normalizados.', 'A: Remisión del episodio maníaco. Estable para planificar egreso.', 'P: Iniciar plan de egreso. Receta de mantenimiento. Cita control en 2 semanas.', (a.admission_date + INTERVAL '8 days'), (a.admission_date + INTERVAL '8 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Estable, lista para egreso. Madre presente y comprometida con supervisión.', 'O: Eutimia sostenida. Funciónalidad recuperada. Perfil tiroideo normal.', 'A: Remisión completa del episodio. Apta para manejo ambulatorio.', 'P: Egreso con quetiapina de mantenimiento. Referir a psiquiatra ambulatorio y psicoterapia.', (a.admission_date + INTERVAL '11 days'), (a.admission_date + INTERVAL '11 days'), (SELECT id FROM users WHERE username = 'doctor8') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Pedro García Hernández (5 notes: days 0,1,3,6,9)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Refiere ansiedad severa, náuseas y vision de insectos. Solicita alcohol.', 'O: Temblor grueso, diaforesis, PA 158/98, FC 112, T 37.3. CIWA-Ar 22. Alucinaciones visuales.', 'A: Síndrome de abstinencia alcohólica severa. Riesgo de delirium tremens y convulsiones.', 'P: Protocolo CIWA cada 4 horas. Diazepam 10mg IM. Tiamina. Hidratacion IV. Precauciones contra convulsiones.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Noche agitada, refiere ver sombras. Sudoracion profusa. Solicita más medicamento.', 'O: CIWA-Ar 18. Temblor persistente. PA 150/92, FC 105. Alucinaciones visuales.', 'A: Abstinencia en fase critica. Mejorandose lentamente pero aún riesgo de complicaciones.', 'P: Continuar protocolo CIWA. Ajustar dosis de diazepam según escala. Iniciar pregabalina 75mg BID.', (a.admission_date + INTERVAL '1 day'), (a.admission_date + INTERVAL '1 day'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Mejoría significativa. Sin alucinaciones. Logra dormir 5 horas. Apetito regresando.', 'O: CIWA-Ar 8. PA 132/84, FC 82. Temblor fino residual. Orientado.', 'A: Resolución de fase critica de abstinencia. Estabilizacion progresiva.', 'P: Espaciar CIWA a cada 8 horas. Mantener pregabalina. Dieta diabética. Control glicemico.', (a.admission_date + INTERVAL '3 days'), (a.admission_date + INTERVAL '3 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Se siente mejor. Reflexiona sobre consumo. Motivado para dejar alcohol.', 'O: Signos vitales normalizados. CIWA-Ar 3. Pruebas hepáticas elevadas pero estables.', 'A: Abstinencia resuelta. Hepatopatía alcohólica estable. Momento de intervención motivacional.', 'P: Suspender protocolo CIWA. Psicoeducación sobre alcoholismo. Referir a programa de rehabilitación.', (a.admission_date + INTERVAL '6 days'), (a.admission_date + INTERVAL '6 days'), (SELECT id FROM users WHERE username = 'doctor6') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Estable. Comprometido con no beber. Preocupado por acceso a programa de rehabilitación.', 'O: Eutimico, funciónal. Sin temblor. Glicemia controlada.', 'A: Resolución completa de abstinencia. Apto para continuar tratamiento ambulatorio.', 'P: Planificar egreso. Referir a AA y programa de rehabilitación. Control hepático en 1 mes.', (a.admission_date + INTERVAL '9 days'), (a.admission_date + INTERVAL '9 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Ana Martínez Ruiz (4 notes: days 0,2,5,7)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Paciente suspicaz, refiere que vecinos envian señales a través de la television.', 'O: Hostil, evita contacto visual, ideación paranoide activa, alucinaciones auditivas. PA 148/92.', 'A: Esquizofrenia paranoide descompensada por abandono terapéutico. HTA no controlada.', 'P: Reiniciar olanzapina 5mg BID. Biperiden profiláctico. Control de PA.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Acepta medicación pero mantiene suspicacia. Refiere que voces disminuyeron en volumen.', 'O: Menos hostil. Acepta alimentación. Alucinaciones presentes pero menos perturbadoras. PA 140/88.', 'A: Respuesta inicial a olanzapina. Requiere tiempo para efecto completo.', 'P: Continuar olanzapina. Enalapril para HTA. Ambiente terapéutico no confrontativo.', (a.admission_date + INTERVAL '2 days'), (a.admission_date + INTERVAL '2 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Reconoce parcialmente que estaba enferma. Pregunta por su hijo.', 'O: Delirios menos elaborados. Alucinaciones esporádicas. Cooperativa con cuidados. PA 135/85.', 'A: Mejoría gradual con olanzapina. Emergencia de insight parcial.', 'P: Mantener esquema. Facilitar llamada a hijo. Psicoeducación a madre sobre adherencia.', (a.admission_date + INTERVAL '5 days'), (a.admission_date + INTERVAL '5 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Se siente mejor. Quiere ir a casa con su hijo. Comprende importancia de medicación.', 'O: Delirios residuales mínimos. Sin alucinaciones en últimas 48 horas. PA controlada.', 'A: Remisión parcial satisfactoria. Apta para planificar egreso con supervisión.', 'P: Preparar egreso con madre como supervisora de medicación. Evaluar antipsicótico de depósito.', (a.admission_date + INTERVAL '7 days'), (a.admission_date + INTERVAL '7 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Luis Morales Castro (4 notes: days 0,2,4,6)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Paciente incoherente, refiere que lo persiguen. Agitado, no coopera.', 'O: Desorientado, midriasis, diaforesis. Ideas paranoides no sistematizadas. Panel toxicológico positivo para cocaína, cannabis, benzodiacepinas.', 'A: Psicosis aguda inducida por polisubstancias. Requiere estabilización y observacíon.', 'P: Quetiapina 100mg BID. Biperiden. Ambiente controlado. Monitoreo de abstinencia.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Más orientado. Reconoce consumo de sustancias. Ansioso y con craving.', 'O: Orientado en persona y lugar. Ideas paranoides disminuidas. Sin alucinaciones.', 'A: Resolución progresiva de psicosis conforme sustancias se metabolizan.', 'P: Mantener quetiapina. Iniciar entrevista motivacional. Manejo de craving.', (a.admission_date + INTERVAL '2 days'), (a.admission_date + INTERVAL '2 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Sin síntomas psicóticos. Preocupado por su futuro. Dispuesto a aceptar ayuda.', 'O: Orientado completamente. Afecto ansioso pero cooperativo. Sin síntomas psicóticos.', 'A: Resolución de psicosis inducida por sustancias. Trastorno por uso de sustancias como diagnóstico primario.', 'P: Planificar egreso. Referir a programa de rehabilitación. Evaluar necesidad de quetiapina a largo plazo.', (a.admission_date + INTERVAL '4 days'), (a.admission_date + INTERVAL '4 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Estable. Padre vino a visitarlo. Motivado para ingreso a rehabilitación.', 'O: Eutimico, sin síntomas psicóticos residuales. Funciónalidad recuperada.', 'A: Psicosis resuelta. Apto para egreso a programa de rehabilitación.', 'P: Egreso a programa de rehabilitación de adicciones. Suspender quetiapina gradualmente.', (a.admission_date + INTERVAL '6 days'), (a.admission_date + INTERVAL '6 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Carmen Flores Mejía (3 notes: days 0,2,4)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Llanto intenso, refiere flashbacks diarios y pesadillas. Expresa deseos de morir para que el sufrimiento termine.', 'O: Afecto constreñido, hipervigilancia, sobresalto exagerado. Ideación suicida activa con plan.', 'A: TEPT crónico con episodio depresivo comorbido y riesgo suicida alto.', 'P: Precauciones suicidas. Sertralina 50mg. Clonazepam HS. Psicoterapia de estabilización.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Mejoría leve del sueño con clonazepam. Participó en sesión individual. Flashbacks persisten pero menos intensos.', 'O: Ánimo deprimido pero cooperativa. Ideación suicida pasiva sin plan.', 'A: Estabilizacion inicial. Disminucion de riesgo suicida de alto a moderado.', 'P: Continuar esquema. Referir a trabajo social. Iniciar técnicas de estabilización de trauma.', (a.admission_date + INTERVAL '2 days'), (a.admission_date + INTERVAL '2 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Durmio mejor. Hablo con sus hijos por teléfono, se siente motivada por ellos. Niega ideación suicida activa.', 'O: Afecto más reactivo. Hipervigilancia disminuida. Participa en terapia grupal.', 'A: Mejoría progresiva. Hijos como factor protector significativo. Riesgo suicida bajo.', 'P: Reclasificar riesgo. Continuar tratamiento. Planificar seguimiento ambulatorio con EMDR.', (a.admission_date + INTERVAL '4 days'), (a.admission_date + INTERVAL '4 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Roberto Díaz Vargas (3 notes: days 0,2,3)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Refiere anhedonia total, fatiga extrema. No puede disfrutar nada. Ideas de muerte pasivas.', 'O: Enlentecimiento psicomotor marcado. Voz monotona. FC 58 (betabloqueador). Cooperativo.', 'A: TAB II, episodio depresivo grave. Comorbilidad cardiaca limita opciones farmacológicas.', 'P: Quetiapina 300mg HS. Perfil tiroideo. Coordinacion con cardiología para seguridad farmacológica.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Durmio mejor con quetiapina. Persiste anhedonia pero siente leve mejoría energetica.', 'O: Enlentecimiento persiste pero menos marcado. TSH ligeramente elevada (pendiente resultado completo).', 'A: Inicio de respuesta a quetiapina para sueño. Descartar hipotiroidismo como factor contribuyente.', 'P: Mantener quetiapina. Evaluar resultado tiroideo completo. Dieta baja en sodio.', (a.admission_date + INTERVAL '2 days'), (a.admission_date + INTERVAL '2 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Mejoría gradual. Participó en caminata breve. Esposa nota cambio positivo.', 'O: Menos enlentecido. Afecto levemente reactivo. Perfil tiroideo confirma hipotiroidismo subóptimo.', 'A: Depresion bipolar respondiendo a quetiapina. Ajustar levotiroxina por hipotiroidismo.', 'P: Continuar quetiapina. Aumentar levotiroxina. Continuar hospitalización para optimizar respuesta.', (a.admission_date + INTERVAL '3 days'), (a.admission_date + INTERVAL '3 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Sofia Ramírez Paz (2 notes: days 0,2)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Llorando, refiere que no puede vivir sin su pareja. Muestra heridas en antebrazos.', 'O: Labil, oscila entre llanto y enojo. Heridas superficiales suturadas. Ideación suicida ambivalente.', 'A: Crisis emocional aguda en contexto de TLP. Autolesiones como regulación emocional.', 'P: Protocolo de seguridad. Sertralina 50mg. Quetiapina 100mg HS. Validación emocional, evitar refuerzo de conducta.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Más calmada. Reconoce patrón de crisis tras rupturas. Dispuesta a retomar DBT.', 'O: Afecto más estable. Sin ideación suicida. Heridas en cicatrización. Participa en sesión individual.', 'A: Estabilizacion de crisis. Insight presente sobre patrón conductal.', 'P: Planificar egreso con referencia a DBT ambulatoria. Continuar sertralina y quetiapina.', (a.admission_date + INTERVAL '2 days'), (a.admission_date + INTERVAL '2 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Miguel Torres Luna (DISCHARGED, 5 notes: days 0,3,6,10,13)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Paciente agitado, refiere que vecinos quieren matarlo. No reconoce estar enfermo.', 'O: Hostil, agitación psicomotora severa. Alucinaciones auditivas imperativas. Requirió contención verbal.', 'A: Esquizofrenia paranoide descompensada. Riesgo de heteroagresion.', 'P: Olanzapina 10mg IM. Luego olanzapina 5mg BID oral. Biperiden 2mg BID. Monitoreo conductual.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Menos agitado. Persisten alucinaciones pero acepta medicación oral.', 'O: Disminucion de agitación. Ideación paranoide presente pero menos intensa. Higiene deficiente.', 'A: Respuesta inicial a olanzapina. Paciente con respuesta lenta conocida.', 'P: Continuar olanzapina oral. Asistencia con higiene personal. Psicoeducación a esposa.', (a.admission_date + INTERVAL '3 days'), (a.admission_date + INTERVAL '3 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Alucinaciones disminuidas. Reconoce parcialmente que estaba enfermo.', 'O: Cooperativo. Delirios menos elaborados. Realiza higiene independiente.', 'A: Mejoría progresiva. Emergencia de insight parcial.', 'P: Mantener olanzapina. Incorporar a actividades terapéuticas. Terapia ocupacional.', (a.admission_date + INTERVAL '6 days'), (a.admission_date + INTERVAL '6 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Estable. Alucinaciones residuales mínimas. Participa en actividades.', 'O: Afecto aplanado pero funciónal. Sin conductas agresivas. Adherente a medicación.', 'A: Remisión parcial estable. Síntomas negativos residuales esperados.', 'P: Preparar plan de egreso. Evaluar antipsicótico de depósito.', (a.admission_date + INTERVAL '10 days'), (a.admission_date + INTERVAL '10 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Listo para egreso. Esposa comprometida con supervisión de medicación.', 'O: Estable, sin síntomas psicóticos activos. Funciónalidad basal recuperada.', 'A: Apto para egreso con supervisión familiar.', 'P: Egreso con olanzapina y biperiden. Cita control en 2 semanas. Considerar depósito en siguiente cita.', (a.admission_date + INTERVAL '13 days'), (a.admission_date + INTERVAL '13 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Elena Sánchez Rivas (DISCHARGED, 4 notes: days 0,3,6,10)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Ataques de pánico 3-4 veces al día. Terror a morir. No puede salir de casa.', 'O: Ansiosa, temblor fino, tensión muscular. FC 86. Sin patologia cardiaca.', 'A: Trastorno de pánico con agorafobia. TAG comorbido. Primera presentación.', 'P: Sertralina 50mg QD. Clonazepam PRN. Suspender cafeína. Técnicas de respiración. TCC.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Ataques reducidos a 1 por día. Practica respiración diafragmática. Sueño 5 horas.', 'O: Menos tensión muscular. Cooperativa con TCC. FC normalizada.', 'A: Respuesta favorable a tratamiento combinado. Sertralina aún en período de latencia.', 'P: Mantener esquema. Continuar TCC. Exposición gradual a situaciones evitadas.', (a.admission_date + INTERVAL '3 days'), (a.admission_date + INTERVAL '3 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Sin ataques en últimos 2 días. Logró caminar por el jardin sin ansiedad.', 'O: Ansiedad basal leve. Sueño 7 horas. Perfil tiroideo normal.', 'A: Remisión de ataques de pánico. Sertralina con efecto terapéutico.', 'P: Reducir clonazepam gradualmente. Planificar egreso. Referir a TCC ambulatoria.', (a.admission_date + INTERVAL '6 days'), (a.admission_date + INTERVAL '6 days'), (SELECT id FROM users WHERE username = 'doctor6') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Lista para egreso. Sin ataques de pánico en 4 días. Confiada en herramientas aprendidas.', 'O: Eutimia. Sin ansiedad patológica. Funciónalidad recuperada.', 'A: Remisión completa. Excelente pronóstico por juventud, primer episodio y buena respuesta.', 'P: Egreso con sertralina. Suspender clonazepam. TCC ambulatoria semanal. Control en 2 semanas.', (a.admission_date + INTERVAL '10 days'), (a.admission_date + INTERVAL '10 days'), (SELECT id FROM users WHERE username = 'doctor4') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- Francisco Mendoza Aguilar (DISCHARGED, 4 notes: days 0,3,6,10)
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: No quiere hablar. Refiere que no vale la pena seguir viviendo. Anorexia marcada.', 'O: Enlentecimiento psicomotor severo. Culpa patológica. Pérdida de 7 kg. Movilidad limitada por artritis.', 'A: TDM recurrente, episodio grave con melancólicos. Resistencia a tratamiento. Riesgo nutricional.', 'P: Sertralina 50mg. Quetiapina 100mg HS. Suplemento nutricional. Terapia ocupacional adaptada.', a.admission_date, a.admission_date, (SELECT id FROM users WHERE username = 'doctor6') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Leve mejoría. Come 50% de racion. Durmio 5 horas con quetiapina. Persiste culpa.', 'O: Enlentecimiento persiste. Ingesta alimentaria mejorando con suplemento.', 'A: Respuesta lenta pero presente. Sueño mejorando. Nutricion en recuperación.', 'P: Mantener esquema. Terapia ocupacional diaria. Ejercicios de movilidad pasiva.', (a.admission_date + INTERVAL '3 days'), (a.admission_date + INTERVAL '3 days'), (SELECT id FROM users WHERE username = 'doctor3') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Se siente un poco mejor. Participa en terapia ocupacional. Come 70% de racion.', 'O: Mayor iniciativa verbal. Menos enlentecido. Recuperacion parcial de peso (1 kg).', 'A: Respuesta tardía pero significativa a sertralina+quetiapina. Mejoría funciónal progresiva.', 'P: Continuar esquema. Considerar ajuste de sertralina si plateau. Planificar cuidados al egreso.', (a.admission_date + INTERVAL '6 days'), (a.admission_date + INTERVAL '6 days'), (SELECT id FROM users WHERE username = 'doctor5') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';
INSERT INTO progress_notes (admission_id, subjective_data, objective_data, analysis, action_plans, created_at, updated_at, created_by)
SELECT a.id, 'S: Mejoría notable. Disfruta visitas de hijos. Come completo. Quiere volver a casa.', 'O: Afecto reactivo, sonrie. Funciónalidad mejorada. Deambula con andador. Peso recuperandose.', 'A: Remisión parcial satisfactoria. Apto para manejo ambulatorio con apoyo familiar.', 'P: Egreso con sertralina y quetiapina. Atención domiciliaria para artritis. Control en 2 semanas.', (a.admission_date + INTERVAL '10 days'), (a.admission_date + INTERVAL '10 days'), (SELECT id FROM users WHERE username = 'doctor6') FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.type = 'HOSPITALIZATION' AND a.status = 'DISCHARGED';

-- ============================================================================
-- STEP 19: MEDICAL ORDERS (~70 total)
-- ============================================================================
-- === Active Patients ===
-- Juan Pérez González (MDD + suicide attempt, 7 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Sertralina 50mg', '50mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'ISRS para TDM severo, tomar con alimentos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL DE SEROLUX (SERTRALINA 50 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Quetiapina 300mg', '300mg', 'ORAL', 'Cada noche', '21:00', 'Estabilizador para insomnio y ansiedad severa', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA DE QUTIAPINA (QUETIOXAL 300 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Clonazepam 2mg', '2mg', 'ORAL', 'PRN', 'SOS', 'Para ansiedad severa o agitación, max 2 dosis/dia', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular, 3 comidas principales y 2 meriendas', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Precauciones suicidas: monitoreo cada 15 minutos, sin objetos cortopunzantes, supervisión constante en baño', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'ACTIVIDAD_FISICA', a.admission_date::DATE, 'Actividad supervisada en areas comunes, acompañamiento permanente', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Juan' AND p.last_name = 'Pérez González' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Maria Santos López (Bipolar I manic + psychosis, 8 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Quetiapina 300mg', '300mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Antipsicótico para mania con psicosis', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA DE QUTIAPINA (QUETIOXAL 300 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Biperiden 2mg', '2mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Prevencion de efectos extrapiramidales', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL BIPERIDENO HC (BIPARK 2 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Midazolam 15mg', '15mg', 'IM', 'PRN', 'SOS', 'Para agitación severa, max 2 dosis/dia', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'AMPOLLA MIDAZOLAM (DORMICUM 15MG/3ML)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', (a.admission_date + INTERVAL '1 day')::DATE, 'T3-T4-TSH', 'Perfil tiroideo para descartar causa orgánica', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'T3-T4, TSH'), a.admission_date + INTERVAL '1 day', a.admission_date + INTERVAL '1 day', a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular, 3 comidas y 2 meriendas', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Precauciones por agitación, ambiente bajo estímulo, limitar visitas', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'ORDENES_MEDICAS', a.admission_date::DATE, 'Signos vitales cada 8 horas, reportar FC>120 o PA>160/100', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Maria' AND p.last_name = 'Santos López' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Pedro García Hernández (Alcohol withdrawal, 7 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Diazepam 10mg', '10mg', 'IM', 'PRN', 'SOS', 'Protocolo CIWA-Ar: administrar si puntaje >10', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'AMPOLLA DE DIAZEPAM DORMICUM 10 MG'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Pregabalina 75mg', '75mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Para ansiedad y neuropatía por abstinencia', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA PREGABALINA (MARTESIA 75 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'PANEL DE DROGAS', 'Panel toxicológico en orina', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'PANEL DE DROGAS'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'PRUEBAS HEPÁTICAS TGO-TGP-GGT', 'Evaluar función hepática por hepatopatía alcohólica', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'PRUEBAS HEPÁTICAS TGO-TGP-GGT'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta diabética, 3 comidas y 2 meriendas. Control de glucosa capilar antes de cada comida.', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Protocolo CIWA-Ar cada 4 horas, precauciones contra convulsiones, barandales elevados', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Pedro' AND p.last_name = 'García Hernández' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Ana Martínez Ruiz (Schizophrenia, 7 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Olanzapina 5mg', '5mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Antipsicótico atípico para esquizofrenia paranoide', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL OLANZAPINA (ZYPREXA 5 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Biperiden 2mg', '2mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Prevencion de efectos extrapiramidales', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL BIPERIDENO HC (BIPARK 2 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Clonazepam 2mg', '2mg', 'ORAL', 'PRN', 'SOS', 'Para ansiedad o agitación, max 2 dosis/dia', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG) II'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular, 3 comidas y 2 meriendas', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Monitoreo de conducta paranoide, sin restricción de movilidad', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'ORDENES_MEDICAS', a.admission_date::DATE, 'Control de PA cada 8 horas (paciente hipertensa). Reportar PA >150/95', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Ana' AND p.last_name = 'Martínez Ruiz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Luis Morales Castro (Polysubstance + psychosis, 6 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Quetiapina 100mg', '100mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Antipsicótico para psicosis inducida por sustancias', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA DE SEROQUEL (QUETIAPINA 100 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Biperiden 2mg', '2mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Prevencion de efectos extrapiramidales', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL BIPERIDENO HC (BIPARK 2 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'PANEL DE DROGAS', 'Panel toxicológico en orina', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'PANEL DE DROGAS'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular, 3 comidas y 2 meriendas', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Monitoreo de abstinencia, ambiente controlado, reportar craving o conducta desorganizada', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Luis' AND p.last_name = 'Morales Castro' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Carmen Flores Mejía (PTSD + suicidal ideation, 5 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Sertralina 50mg', '50mg', 'ORAL', 'Una vez al dia', '08:00', 'ISRS para TEPT', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL ALTRULINE (SERTRALINA) 50 MG'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Clonazepam 2mg', '2mg', 'ORAL', 'Cada noche', '21:00', 'Para insomnio y pesadillas asociadas a TEPT', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular, 3 comidas y 2 meriendas', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Precauciones suicidas nivel moderado: monitoreo cada 30 minutos, supervisión en baño', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Carmen' AND p.last_name = 'Flores Mejía' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Roberto Díaz Vargas (Bipolar II depressive, 4 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Quetiapina 300mg', '300mg', 'ORAL', 'Cada noche', '21:00', 'Estabilizador para episodio depresivo bipolar', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA DE QUTIAPINA (QUETIOXAL 300 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', (a.admission_date + INTERVAL '1 day')::DATE, 'T3-T4-TSH', 'Perfil tiroideo, descartar hipotiroidismo', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'T3-T4, TSH'), a.admission_date + INTERVAL '1 day', a.admission_date + INTERVAL '1 day', a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular baja en sodio (paciente con condición cardiaca)', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Roberto' AND p.last_name = 'Díaz Vargas' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- Sofia Ramírez Paz (BPD + self-harm, 5 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Sertralina 50mg', '50mg', 'ORAL', 'Una vez al dia', '08:00', 'ISRS para desregulación emocional del TLP', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL DE SEROLUX (SERTRALINA 50 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Quetiapina 100mg', '100mg', 'ORAL', 'Cada noche', '21:00', 'Para insomnio y estabilización emocional', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'TABLETA DE SEROQUEL (QUETIAPINA 100 MG)'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso completos', 'ACTIVE', (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular, 3 comidas y 2 meriendas', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Protocolo de seguridad por autolesiónes: sin objetos cortopunzantes, revisión de pertenencias, supervisión en baño', 'ACTIVE', a.admission_date, a.admission_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Sofia' AND p.last_name = 'Ramírez Paz' AND a.type = 'HOSPITALIZATION' AND a.status = 'ACTIVE';

-- === Discharged Patients (ALL orders DISCONTINUED) ===
-- Miguel Torres Luna (Schizophrenia, 6 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Olanzapina 10mg', '10mg', 'IM', 'PRN', 'SOS', 'Para agitación psicomotora severa', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'AMPOLLA DE OLANZAPINA (ZYPREXA IM 10 MG)'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Olanzapina 5mg', '5mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Antipsicótico de mantenimiento', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL OLANZAPINA (ZYPREXA 5 MG)'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Biperiden 2mg', '2mg', 'ORAL', 'Cada 12 horas', '08:00, 20:00', 'Prevencion de extrapiramidales', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL BIPERIDENO HC (BIPARK 2 MG)'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, discontinued_at, discontinued_by, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, discontinued_at, discontinued_by, created_at, updated_at, created_by)
SELECT a.id, 'CUIDADOS_ESPECIALES', a.admission_date::DATE, 'Monitoreo de conducta psicótica', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Miguel' AND p.last_name = 'Torres Luna' AND a.status = 'DISCHARGED';

-- Elena Sánchez Rivas (GAD + Panic, 5 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Sertralina 50mg', '50mg', 'ORAL', 'Una vez al dia', '08:00', 'ISRS para TAG y pánico', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL ALTRULINE (SERTRALINA) 50 MG'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Clonazepam 2mg', '2mg', 'ORAL', 'PRN', 'SOS', 'Para crisis de pánico', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL CLONAZEPAM (RIVOTRIL 2 MG)'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', (a.admission_date + INTERVAL '1 day')::DATE, 'T3-T4-TSH', 'Perfil tiroideo', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'T3-T4, TSH'), a.admission_date + INTERVAL '1 day', a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, discontinued_at, discontinued_by, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular, evitar cafeína', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Elena' AND p.last_name = 'Sánchez Rivas' AND a.status = 'DISCHARGED';

-- Francisco Mendoza Aguilar (MDD geriatric, 4 orders)
INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Sertralina 50mg', '50mg', 'ORAL', 'Una vez al dia', '08:00', 'ISRS para depresión en adulto mayor', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'TABLETA INDIVIDUAL DE SEROLUX (SERTRALINA 50 MG)'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, dosage, route, frequency, schedule, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'MEDICAMENTOS', a.admission_date::DATE, 'Quetiapina 100mg', '100mg', 'ORAL', 'Cada noche', '21:00', 'Coadyuvante para insomnio', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'TABLETA DE SEROQUEL (QUETIAPINA 100 MG)'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, medication, observations, status, discontinued_at, discontinued_by, inventory_item_id, created_at, updated_at, created_by)
SELECT a.id, 'LABORATORIOS', a.admission_date::DATE, 'KIT DE INGRESO', 'Laboratorios de ingreso', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, (SELECT id FROM inventory_items WHERE name = 'KIT DE INGRESO'), a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED';

INSERT INTO medical_orders (admission_id, category, start_date, observations, status, discontinued_at, discontinued_by, created_at, updated_at, created_by)
SELECT a.id, 'DIETA', a.admission_date::DATE, 'Dieta regular blanda, suplemento nutricional diario', 'DISCONTINUED', a.discharge_date, a.treating_physician_id, a.admission_date, a.discharge_date, a.treating_physician_id FROM admissions a JOIN patients p ON a.patient_id = p.id WHERE p.first_name = 'Francisco' AND p.last_name = 'Mendoza Aguilar' AND a.status = 'DISCHARGED';


SET session_replication_role = DEFAULT;
