package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "salary_history")
@SQLRestriction("deleted_at IS NULL")
class SalaryHistory(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treasury_employee_id", nullable = false)
    var employee: TreasuryEmployee,

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    var baseSalary: BigDecimal,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate,

    @Column(name = "effective_to")
    var effectiveTo: LocalDate? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
