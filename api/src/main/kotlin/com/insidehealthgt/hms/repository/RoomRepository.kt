package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.repository.projection.RoomOccupancyRow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface RoomRepository : JpaRepository<Room, Long> {

    fun existsByNumber(number: String): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Room r WHERE r.number = :number AND r.id != :excludeId",
    )
    fun existsByNumberExcludingId(number: String, excludeId: Long): Boolean

    @Query(
        """
        SELECT r, (
            SELECT COUNT(a) FROM Admission a
            WHERE a.room = r
              AND a.status = com.insidehealthgt.hms.entity.AdmissionStatus.ACTIVE
              AND a.type = com.insidehealthgt.hms.entity.AdmissionType.HOSPITALIZATION
              AND a.deletedAt IS NULL
        )
        FROM Room r
        WHERE r.capacity > (
            SELECT COUNT(a) FROM Admission a
            WHERE a.room = r
              AND a.status = com.insidehealthgt.hms.entity.AdmissionStatus.ACTIVE
              AND a.type = com.insidehealthgt.hms.entity.AdmissionType.HOSPITALIZATION
              AND a.deletedAt IS NULL
        )
        ORDER BY r.number
        """,
    )
    fun findRoomsWithAvailabilityAndCount(): List<Array<Any>>

    @Query(
        """
        SELECT COUNT(a) FROM Admission a
        WHERE a.room.id = :roomId
          AND a.status = com.insidehealthgt.hms.entity.AdmissionStatus.ACTIVE
          AND a.type = com.insidehealthgt.hms.entity.AdmissionType.HOSPITALIZATION
          AND a.deletedAt IS NULL
        """,
    )
    fun countActiveAdmissionsByRoomId(@Param("roomId") roomId: Long): Long

    /**
     * Returns one row per (room, active occupant). Rooms without occupants appear once
     * with null occupant fields. Only counts ACTIVE, non-deleted, HOSPITALIZATION admissions
     * (the only type that requires a room — see [com.insidehealthgt.hms.entity.AdmissionType.requiresRoom]).
     * Soft-deleted rooms are excluded automatically via the `@SQLRestriction` on [Room].
     */
    @Query(
        """
        SELECT new com.insidehealthgt.hms.repository.projection.RoomOccupancyRow(
            r.id, r.number, r.type, r.gender, r.capacity,
            a.id, p.id, p.firstName, p.lastName, a.admissionDate
        )
        FROM Room r
        LEFT JOIN Admission a ON a.room = r
            AND a.status = com.insidehealthgt.hms.entity.AdmissionStatus.ACTIVE
            AND a.type = com.insidehealthgt.hms.entity.AdmissionType.HOSPITALIZATION
            AND a.deletedAt IS NULL
        LEFT JOIN a.patient p
        ORDER BY r.number, a.admissionDate
        """,
    )
    fun findRoomsWithActiveAdmissions(): List<RoomOccupancyRow>

    /**
     * Delete all rooms including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM rooms", nativeQuery = true)
    fun deleteAllHard()
}
