package com.insidehealthgt.hms.dto.request

/**
 * Update request uses the same structure as create request.
 *
 * Required fields: category, startDate
 * Optional fields: endDate, medication, dosage, route, frequency, schedule, observations
 *
 * Note: Partial updates are not supported. All required fields must be provided.
 * Updates to discontinued orders are not allowed.
 */
typealias UpdateMedicalOrderRequest = CreateMedicalOrderRequest
