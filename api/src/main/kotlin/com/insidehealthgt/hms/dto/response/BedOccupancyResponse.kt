package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import java.time.LocalDate

/**
 * Aggregated bed-occupancy snapshot used to render the bed occupancy screen in a single call.
 * Invariant: for every [RoomOccupancyItem], `occupants.size == occupiedBeds`. The frontend
 * derives free slots as `capacity - occupiedBeds`.
 */
data class BedOccupancyResponse(val summary: OccupancySummary, val rooms: List<RoomOccupancyItem>)

data class OccupancySummary(val totalBeds: Int, val occupiedBeds: Int, val freeBeds: Int, val occupancyPercent: Double)

data class RoomOccupancyItem(
    val id: Long,
    val number: String,
    val type: RoomType,
    val gender: RoomGender,
    val capacity: Int,
    val occupiedBeds: Int,
    val availableBeds: Int,
    val occupants: List<BedOccupant>,
)

data class BedOccupant(
    val admissionId: Long,
    val patientId: Long,
    val patientName: String,
    val admissionDate: LocalDate,
)
