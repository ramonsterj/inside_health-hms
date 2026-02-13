package com.insidehealthgt.hms.scheduler

import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.service.BillingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Scheduled task that generates daily room and diet charges for all active admissions.
 *
 * Runs on cron schedule `0 0 1 * * *` (daily at 1:00 AM) and creates charges
 * for the previous day. Idempotency is enforced via dual layers:
 * 1. Application-level `existsBy…` check in [BillingService]
 * 2. Database unique partial indexes
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

        var roomCreated = 0
        var dietCreated = 0
        var skipped = 0

        for (admission in activeAdmissions) {
            // Room charges
            try {
                val room = admission.room
                if (room == null) {
                    log.warn("Admission {} has no room assigned, skipping room charge", admission.id)
                    skipped++
                } else if (room.price == null) {
                    log.warn(
                        "Room {} has no price configured, skipping room charge for admission {}",
                        room.number,
                        admission.id,
                    )
                    skipped++
                } else {
                    billingService.createRoomCharge(admission.id!!, room, chargeDate)
                    roomCreated++
                }
            } catch (e: Exception) {
                log.error("Failed to create room charge for admission {}", admission.id, e)
            }

            // Diet charges (only for HOSPITALIZATION admissions)
            if (admission.type == AdmissionType.HOSPITALIZATION) {
                try {
                    billingService.createDietCharge(admission.id!!, chargeDate)
                    dietCreated++
                } catch (e: Exception) {
                    log.error("Failed to create diet charge for admission {}", admission.id, e)
                }
            }
        }

        log.info(
            "Daily charge generation complete: {} room charges, {} diet charges, {} skipped",
            roomCreated,
            dietCreated,
            skipped,
        )
    }
}
