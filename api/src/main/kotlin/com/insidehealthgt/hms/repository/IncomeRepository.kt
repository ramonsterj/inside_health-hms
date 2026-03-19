package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Income
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface IncomeRepository :
    JpaRepository<Income, Long>,
    JpaSpecificationExecutor<Income> {

    fun findAllByBankAccountIdAndIncomeDateBetween(
        bankAccountId: Long,
        from: java.time.LocalDate,
        to: java.time.LocalDate,
    ): List<Income>
}
