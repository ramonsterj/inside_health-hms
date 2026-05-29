package com.insidehealthgt.hms.entity

/**
 * Stable codes for the day-1 seeded warehouses (see migration V118). Centralized
 * so the "receiving warehouse is ADMINISTRACION" / "nursing dispenses from
 * ENFERMERIA" knowledge lives in one place instead of being duplicated as
 * per-service string constants.
 */
object WarehouseCodes {
    /** Master / receiving bodega — deliveries and new lots land here. */
    const val RECEIVING = "ADMINISTRACION"

    /** Nursing bodega — medication administration debits here by default. */
    const val NURSING = "ENFERMERIA"
}
