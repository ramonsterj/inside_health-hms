package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.MedicationDetails
import com.insidehealthgt.hms.entity.MedicationReviewStatus
import com.insidehealthgt.hms.entity.MedicationSection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MedicationDetailsRepository : JpaRepository<MedicationDetails, Long> {

    fun findByItemId(itemId: Long): MedicationDetails?

    fun findAllByReviewStatus(reviewStatus: MedicationReviewStatus): List<MedicationDetails>

    @Query(
        value = "SELECT m FROM MedicationDetails m JOIN FETCH m.item " +
            "WHERE (:section IS NULL OR m.section = :section) " +
            "AND (:controlled IS NULL OR m.controlled = :controlled) " +
            "AND (:search = '' " +
            "     OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(m.commercialName, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(m.item.sku, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT COUNT(m) FROM MedicationDetails m " +
            "WHERE (:section IS NULL OR m.section = :section) " +
            "AND (:controlled IS NULL OR m.controlled = :controlled) " +
            "AND (:search = '' " +
            "     OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(m.commercialName, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(m.item.sku, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
    )
    fun findAllWithFilters(
        section: MedicationSection?,
        controlled: Boolean?,
        search: String,
        pageable: Pageable,
    ): Page<MedicationDetails>
}
