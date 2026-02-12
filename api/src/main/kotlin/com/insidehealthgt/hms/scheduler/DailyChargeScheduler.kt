package com.insidehealthgt.hms.scheduler

import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.service.BillingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Scheduled task that generates daily room charges for all active admissions.
 *
 * Runs on cron schedule `0 0 1 * * *` (daily at 1:00 AM) and creates a room
 * charge for the previous day. Idempotency is enforced via dual layers:
 * 1. Application-level `existsBy…` check in [BillingService.createRoomCharge]
 * 2. Database unique partial index `idx_patient_charges_daily_unique`
 */
@Component
class DailyChargeScheduler(
    private val admissionRepository: AdmissionRepository,
    private val billingService: BillingService,
) {

    private val log = LoggerFactory.getLogger(DailyChargeScheduler::class.java)

    @Scheduled(cron = "0 0 1 * * *")
    @Suppress("TooGenericExceptionCaught", "LoopWithTooManyJumpStatements")
    fun generateDailyCharges() {
        val chargeDate = LocalDate.now().minusDays(1)
        log.info("Starting daily charge generation for date: {}", chargeDate)

        val activeAdmissions = admissionRepository.findAllByStatusWithRoom(AdmissionStatus.ACTIVE)
        log.info("Found {} active admissions", activeAdmissions.size)

        var created = 0
        var skipped = 0

        for (admission in activeAdmissions) {
            try {
                val room = admission.room
                if (room == null) {
                    log.warn("Admission {} has no room assigned, skipping room charge", admission.id)
                    skipped++
                    continue
                }

                if (room.price == null) {
                    log.warn(
                        "Room {} has no price configured, skipping room charge for admission {}",
                        room.number,
                        admission.id,
                    )
                    skipped++
                    continue
                }

                billingService.createRoomCharge(admission.id!!, room, chargeDate)
                created++
            } catch (e: Exception) {
                log.error("Failed to create daily charge for admission {}", admission.id, e)
            }
        }

        log.info("Daily charge generation complete: {} created, {} skipped", created, skipped)
    }
}
