package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Room
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
            WHERE a.room = r AND a.status = 'ACTIVE' AND a.deletedAt IS NULL
        )
        FROM Room r
        WHERE r.capacity > (
            SELECT COUNT(a) FROM Admission a
            WHERE a.room = r AND a.status = 'ACTIVE' AND a.deletedAt IS NULL
        )
        ORDER BY r.number
        """,
    )
    fun findRoomsWithAvailabilityAndCount(): List<Array<Any>>

    @Query(
        """
        SELECT COUNT(a) FROM Admission a
        WHERE a.room.id = :roomId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL
        """,
    )
    fun countActiveAdmissionsByRoomId(@Param("roomId") roomId: Long): Long

    /**
     * Delete all rooms including soft-deleted ones (for test cleanup).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM rooms", nativeQuery = true)
    fun deleteAllHard()
}
