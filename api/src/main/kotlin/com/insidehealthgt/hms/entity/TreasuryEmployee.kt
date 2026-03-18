package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "treasury_employees")
@SQLRestriction("deleted_at IS NULL")
class TreasuryEmployee(

    @Column(name = "full_name", nullable = false, length = 255)
    var fullName: String,

    @Column(name = "employee_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var employeeType: EmployeeType,

    @Column(name = "tax_id", length = 50)
    var taxId: String? = null,

    @Column(length = 100)
    var position: String? = null,

    @Column(name = "base_salary", precision = 12, scale = 2)
    var baseSalary: BigDecimal? = null,

    @Column(name = "contracted_rate", precision = 12, scale = 2)
    var contractedRate: BigDecimal? = null,

    @Column(name = "doctor_fee_arrangement", length = 20)
    @Enumerated(EnumType.STRING)
    var doctorFeeArrangement: DoctorFeeArrangement? = null,

    @Column(name = "hospital_commission_pct", precision = 5, scale = 2)
    var hospitalCommissionPct: BigDecimal = BigDecimal.ZERO,

    @Column(name = "hire_date")
    var hireDate: LocalDate? = null,

    @Column(name = "termination_date")
    var terminationDate: LocalDate? = null,

    @Column(name = "termination_reason", length = 255)
    var terminationReason: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
