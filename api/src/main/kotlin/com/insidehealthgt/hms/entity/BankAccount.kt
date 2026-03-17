package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal

@Entity
@Table(name = "bank_accounts")
@SQLRestriction("deleted_at IS NULL")
class BankAccount(

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "bank_name", length = 100)
    var bankName: String? = null,

    @Column(name = "account_number", length = 50)
    var accountNumber: String? = null,

    @Column(name = "account_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var accountType: BankAccountType = BankAccountType.CHECKING,

    @Column(nullable = false, length = 3)
    var currency: String = "GTQ",

    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    var openingBalance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "is_petty_cash", nullable = false)
    var isPettyCash: Boolean = false,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

) : BaseEntity()
