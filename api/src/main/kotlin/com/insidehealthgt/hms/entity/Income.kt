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
@Table(name = "income_records")
@SQLRestriction("deleted_at IS NULL")
class Income(

    @Column(nullable = false, length = 255)
    var description: String,

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var category: IncomeCategory,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    @Column(name = "income_date", nullable = false)
    var incomeDate: LocalDate,

    @Column(length = 100)
    var reference: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    var bankAccount: BankAccount,

    @Column(name = "invoice_id")
    var invoiceId: Long? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
