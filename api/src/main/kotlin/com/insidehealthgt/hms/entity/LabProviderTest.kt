package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal

/**
 * A provider's named and priced offering of a canonical [LabTest]. The same canonical test
 * may have a different display name and price per provider. Unique per (provider, lab_test)
 * among non-deleted rows.
 */
@Entity
@Table(name = "lab_provider_tests")
@SQLRestriction("deleted_at IS NULL")
class LabProviderTest(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: LabProvider,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id", nullable = false)
    var labTest: LabTest,

    @Column(name = "display_name", nullable = false, length = 200)
    var displayName: String,

    @Column(nullable = false, precision = 12, scale = 2)
    var cost: BigDecimal,

    @Column(name = "sales_price", nullable = false, precision = 12, scale = 2)
    var salesPrice: BigDecimal,

    @Column(nullable = false)
    var active: Boolean = true,

) : BaseEntity()
