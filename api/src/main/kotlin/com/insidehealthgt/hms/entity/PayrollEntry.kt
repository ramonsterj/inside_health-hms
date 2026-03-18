package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "payroll_entries")
@SQLRestriction("deleted_at IS NULL")
class PayrollEntry(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treasury_employee_id", nullable = false)
    var employee: TreasuryEmployee,

    @Column(nullable = false)
    var year: Int,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var period: PayrollPeriod,

    @Column(name = "period_label", nullable = false, length = 50)
    var periodLabel: String,

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    var baseSalary: BigDecimal,

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    var grossAmount: BigDecimal,

    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: PayrollStatus = PayrollStatus.PENDING,

    @Column(name = "paid_date")
    var paidDate: LocalDate? = null,

    @Column(name = "expense_id")
    var expenseId: Long? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
