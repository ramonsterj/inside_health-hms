package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "bank_account_column_mappings")
@SQLRestriction("deleted_at IS NULL")
class BankAccountColumnMapping(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    var bankAccount: BankAccount,

    @Column(name = "file_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var fileType: StatementFileType = StatementFileType.XLSX,

    @Column(name = "has_header", nullable = false)
    var hasHeader: Boolean = true,

    @Column(name = "date_column", nullable = false, length = 50)
    var dateColumn: String,

    @Column(name = "description_column", length = 50)
    var descriptionColumn: String? = null,

    @Column(name = "reference_column", length = 50)
    var referenceColumn: String? = null,

    @Column(name = "debit_column", nullable = false, length = 50)
    var debitColumn: String,

    @Column(name = "credit_column", nullable = false, length = 50)
    var creditColumn: String,

    @Column(name = "balance_column", length = 50)
    var balanceColumn: String? = null,

    @Column(name = "date_format", nullable = false, length = 30)
    var dateFormat: String = "dd/MM/yyyy",

    @Column(name = "skip_rows", nullable = false)
    var skipRows: Int = 0,

) : BaseEntity()
