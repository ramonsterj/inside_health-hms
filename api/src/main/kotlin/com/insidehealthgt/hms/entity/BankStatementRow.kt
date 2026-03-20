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
@Table(name = "bank_statement_rows")
@SQLRestriction("deleted_at IS NULL")
class BankStatementRow(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_statement_id", nullable = false)
    var bankStatement: BankStatement,

    @Column(name = "row_number", nullable = false)
    var rowNumber: Int,

    @Column(name = "transaction_date", nullable = false)
    var transactionDate: LocalDate,

    @Column(length = 500)
    var description: String? = null,

    @Column(length = 255)
    var reference: String? = null,

    @Column(name = "debit_amount", precision = 12, scale = 2)
    var debitAmount: BigDecimal? = null,

    @Column(name = "credit_amount", precision = 12, scale = 2)
    var creditAmount: BigDecimal? = null,

    @Column(precision = 12, scale = 2)
    var balance: BigDecimal? = null,

    @Column(name = "match_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var matchStatus: MatchStatus = MatchStatus.UNMATCHED,

    @Column(name = "matched_entity_type", length = 20)
    @Enumerated(EnumType.STRING)
    var matchedEntityType: MatchedEntityType? = null,

    @Column(name = "matched_entity_id")
    var matchedEntityId: Long? = null,

    @Column(name = "acknowledged_reason", length = 255)
    var acknowledgedReason: String? = null,

    @Column(name = "non_ledger", nullable = false)
    var nonLedger: Boolean = false,

) : BaseEntity()
