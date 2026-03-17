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
@Table(name = "expense_payments")
@SQLRestriction("deleted_at IS NULL")
class ExpensePayment(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    var expense: Expense,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    @Column(name = "payment_date", nullable = false)
    var paymentDate: LocalDate,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    var bankAccount: BankAccount,

    @Column(length = 255)
    var reference: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
