package com.insidehealthgt.hms.event

import com.insidehealthgt.hms.service.BillingService
import com.insidehealthgt.hms.service.InvoiceService
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
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class BillingEventListener(private val billingService: BillingService, private val invoiceService: InvoiceService) {

    private val log = LoggerFactory.getLogger(BillingEventListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handlePsychotherapyActivityCreated(event: PsychotherapyActivityCreatedEvent) {
        log.info(
            "Received PsychotherapyActivityCreatedEvent for admission {} category '{}'",
            event.admissionId,
            event.categoryName,
        )
        try {
            billingService.createChargeFromPsychotherapyActivity(event)
        } catch (e: Exception) {
            log.error(
                "Failed to create charge from psychotherapy activity for admission {}",
                event.admissionId,
                e,
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleMedicalOrderCreated(event: MedicalOrderCreatedEvent) {
        log.info(
            "Received MedicalOrderCreatedEvent for admission {} category {}",
            event.admissionId,
            event.category,
        )
        try {
            billingService.createChargeFromMedicalOrder(event)
        } catch (e: Exception) {
            log.error(
                "Failed to create charge from medical order for admission {}",
                event.admissionId,
                e,
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleAdmissionCreated(event: AdmissionCreatedEvent) {
        log.info(
            "Received AdmissionCreatedEvent for admission {} type {}",
            event.admissionId,
            event.admissionType,
        )
        try {
            billingService.createChargeFromAdmission(event)
        } catch (e: Exception) {
            log.error(
                "Failed to create charge from admission event for admission {}",
                event.admissionId,
                e,
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handlePatientDischargedCharges(event: PatientDischargedEvent) {
        log.info("Patient discharged from admission {}. Creating final charges.", event.admissionId)
        try {
            billingService.createFinalDayCharges(event.admissionId)
        } catch (e: Exception) {
            log.error("Failed to create final day charges for admission {}", event.admissionId, e)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handlePatientDischargedInvoice(event: PatientDischargedEvent) {
        log.info("Patient discharged from admission {}. Generating invoice.", event.admissionId)
        try {
            invoiceService.generateInvoice(event.admissionId)
            log.info("Auto-generated invoice for admission {}", event.admissionId)
        } catch (e: Exception) {
            log.error("Failed to auto-generate invoice for admission {}", event.admissionId, e)
        }
    }
}
