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
@Table(name = "expenses")
@SQLRestriction("deleted_at IS NULL")
class Expense(

    @Column(name = "supplier_name", nullable = false, length = 255)
    var supplierName: String,

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var category: ExpenseCategory,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    @Column(name = "expense_date", nullable = false)
    var expenseDate: LocalDate,

    @Column(name = "invoice_number", nullable = false, length = 100)
    var invoiceNumber: String,

    @Column(name = "invoice_document_path", length = 500)
    var invoiceDocumentPath: String? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: ExpenseStatus = ExpenseStatus.PENDING,

    @Column(name = "due_date")
    var dueDate: LocalDate? = null,

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    var paidAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "treasury_employee_id")
    var treasuryEmployeeId: Long? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
