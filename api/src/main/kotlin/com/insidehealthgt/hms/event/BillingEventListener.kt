package com.insidehealthgt.hms.event

import com.insidehealthgt.hms.service.BillingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Listens for domain events that require billing actions.
 *
 * Handlers use [TransactionPhase.AFTER_COMMIT] so failures do not roll back the
 * originating transaction.
 */
@Component
class BillingEventListener(private val billingService: BillingService) {

    private val log = LoggerFactory.getLogger(BillingEventListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Suppress("TooGenericExceptionCaught")
    fun handleInventoryDispensed(event: InventoryDispensedEvent) {
        log.info(
            "Received InventoryDispensedEvent for admission {} item {}",
            event.admissionId,
            event.inventoryItemId,
        )
        try {
            billingService.createChargeFromInventoryDispensed(event)
        } catch (e: Exception) {
            log.error(
                "Failed to create charge from inventory dispensed event for admission {}",
                event.admissionId,
                e,
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePatientDischarged(event: PatientDischargedEvent) {
        log.info(
            "Patient discharged from admission {}. Invoice generation is a manual admin action.",
            event.admissionId,
        )
    }
}
