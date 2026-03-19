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
import java.time.LocalDate

@Entity
@Table(name = "bank_statements")
@SQLRestriction("deleted_at IS NULL")
class BankStatement(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    var bankAccount: BankAccount,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "file_path", nullable = false, length = 500)
    var filePath: String,

    @Column(name = "statement_date", nullable = false)
    var statementDate: LocalDate,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: BankStatementStatus = BankStatementStatus.IN_PROGRESS,

    @Column(name = "total_rows", nullable = false)
    var totalRows: Int = 0,

    @Column(name = "matched_count", nullable = false)
    var matchedCount: Int = 0,

    @Column(name = "unmatched_count", nullable = false)
    var unmatchedCount: Int = 0,

    @Column(name = "acknowledged_count", nullable = false)
    var acknowledgedCount: Int = 0,

    @Column(name = "suggested_count", nullable = false)
    var suggestedCount: Int = 0,

) : BaseEntity()
