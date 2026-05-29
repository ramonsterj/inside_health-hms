package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.WarehouseCharge
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WarehouseChargeRepository : JpaRepository<WarehouseCharge, Long> {

    @Query(
        value = "SELECT wc FROM WarehouseCharge wc " +
            "JOIN FETCH wc.warehouse JOIN FETCH wc.item JOIN FETCH wc.admission a JOIN FETCH a.patient " +
            "WHERE (:warehouseId IS NULL OR wc.warehouse.id = :warehouseId) " +
            "AND (:admissionId IS NULL OR wc.admission.id = :admissionId)",
        countQuery = "SELECT COUNT(wc) FROM WarehouseCharge wc " +
            "WHERE (:warehouseId IS NULL OR wc.warehouse.id = :warehouseId) " +
            "AND (:admissionId IS NULL OR wc.admission.id = :admissionId)",
    )
    fun findHistory(
        @Param("warehouseId") warehouseId: Long?,
        @Param("admissionId") admissionId: Long?,
        pageable: Pageable,
    ): Page<WarehouseCharge>
}
