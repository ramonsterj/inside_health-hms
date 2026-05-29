package com.insidehealthgt.hms.entity

/**
 * Transfer lifecycle. v1 ships one-step transfers only — the create endpoint
 * immediately marks them [COMPLETED]. [PENDING] is reserved for the Phase 2
 * approval workflow (schema already carries it so no migration is needed later).
 */
enum class TransferStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
}
