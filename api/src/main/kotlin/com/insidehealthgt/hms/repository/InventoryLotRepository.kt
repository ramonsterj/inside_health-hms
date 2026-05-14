package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.InventoryLot
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
@Suppress("TooManyFunctions") // Repository naturally has many fine-grained query methods.
interface InventoryLotRepository : JpaRepository<InventoryLot, Long> {

    /**
     * FEFO selection (write path): pessimistic-lock the soonest-to-expire
     * non-recalled lot with enough stock to cover [minQty]. Used only inside
     * the dispense transaction. UI previews must use [peekFefoCandidates].
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT l FROM InventoryLot l " +
            "WHERE l.item.id = :itemId " +
            "AND l.recalled = false " +
            "AND l.quantityOnHand >= :minQty " +
            "ORDER BY l.expirationDate ASC",
    )
    fun findFefoCandidates(@Param("itemId") itemId: Long, @Param("minQty") minQty: Int): List<InventoryLot>

    /**
     * Non-locking FEFO peek. Returns the same ordering as [findFefoCandidates]
     * but does NOT acquire row locks, so concurrent UI previews cannot block
     * each other (or block a dispense).
     */
    @Query(
        "SELECT l FROM InventoryLot l " +
            "WHERE l.item.id = :itemId " +
            "AND l.recalled = false " +
            "AND l.quantityOnHand >= :minQty " +
            "ORDER BY l.expirationDate ASC",
    )
    fun peekFefoCandidates(@Param("itemId") itemId: Long, @Param("minQty") minQty: Int): List<InventoryLot>

    /**
     * Lock a specific lot for update (manual admin override on EXIT). Returns
     * empty if the lot was soft-deleted or does not exist.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InventoryLot l WHERE l.id = :lotId")
    fun findByIdForUpdate(@Param("lotId") lotId: Long): InventoryLot?

    @Query(
        value = "SELECT COALESCE(SUM(quantity_on_hand), 0) FROM inventory_lots " +
            "WHERE item_id = :itemId AND deleted_at IS NULL AND recalled = FALSE",
        nativeQuery = true,
    )
    fun sumQuantityOnHand(@Param("itemId") itemId: Long): Int

    @Query(
        "SELECT l FROM InventoryLot l " +
            "WHERE l.item.id = :itemId " +
            "AND (:lotNumber IS NULL AND l.lotNumber IS NULL OR l.lotNumber = :lotNumber) " +
            "AND l.expirationDate = :expirationDate",
    )
    fun findByItemIdAndLotNumberAndExpirationDate(
        @Param("itemId") itemId: Long,
        @Param("lotNumber") lotNumber: String?,
        @Param("expirationDate") expirationDate: LocalDate,
    ): InventoryLot?

    @Query(
        "SELECT l FROM InventoryLot l JOIN FETCH l.item " +
            "WHERE l.item.id = :itemId ORDER BY l.expirationDate ASC",
    )
    fun findAllByItemIdOrderByExpirationDate(@Param("itemId") itemId: Long): List<InventoryLot>

    @Query(
        value = "SELECT EXISTS(" +
            "SELECT 1 FROM inventory_movements " +
            "WHERE lot_id = :lotId AND deleted_at IS NULL)",
        nativeQuery = true,
    )
    fun existsMovementsByLotId(@Param("lotId") lotId: Long): Boolean

    @Query(
        value = "SELECT EXISTS(" +
            "SELECT 1 FROM medication_administrations " +
            "WHERE lot_id = :lotId AND deleted_at IS NULL)",
        nativeQuery = true,
    )
    fun existsAdministrationsByLotId(@Param("lotId") lotId: Long): Boolean

    fun countByItemIdAndDeletedAtIsNull(itemId: Long): Long

    @Query(
        "SELECT l FROM InventoryLot l " +
            "WHERE l.item.id = :itemId AND l.syntheticLegacy = true",
    )
    fun findSyntheticLegacyForItem(@Param("itemId") itemId: Long): List<InventoryLot>

    @Query(
        "SELECT l FROM InventoryLot l JOIN FETCH l.item i LEFT JOIN FETCH i.category " +
            "WHERE l.recalled = false " +
            "ORDER BY l.expirationDate ASC",
    )
    fun findAllActiveWithItem(): List<InventoryLot>

    /**
     * Soonest-to-expire active lot per item, for a batch of items. Used by the
     * Kardex aggregator to render an expiry chip next to each medication
     * without an N+1 fefo call.
     */
    @Query(
        value = "SELECT item_id AS itemId, MIN(expiration_date) AS expirationDate " +
            "FROM inventory_lots " +
            "WHERE item_id IN (:itemIds) " +
            "  AND deleted_at IS NULL " +
            "  AND recalled = FALSE " +
            "  AND quantity_on_hand > 0 " +
            "GROUP BY item_id",
        nativeQuery = true,
    )
    fun findNextExpirationByItemIds(@Param("itemIds") itemIds: List<Long>): List<NextExpirationRow>

    interface NextExpirationRow {
        val itemId: Long
        val expirationDate: LocalDate
    }
}
