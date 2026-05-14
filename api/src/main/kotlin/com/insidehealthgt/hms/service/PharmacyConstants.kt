package com.insidehealthgt.hms.service

import java.time.LocalDate

object PharmacyConstants {
    /**
     * Sentinel expiration date used by V106 backfill for synthetic legacy lots
     * (rows whose real expiration is unknown). Surfaces as `NO_EXPIRY` in the
     * expiry dashboard. Must match the DATE literal in V106 and any future
     * migrations that touch synthetic_legacy rows.
     */
    @Suppress("MagicNumber")
    val LEGACY_LOT_EXPIRATION: LocalDate = LocalDate.of(9999, 12, 31)
}
