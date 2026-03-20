package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.BankStatement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BankStatementRepository : JpaRepository<BankStatement, Long> {

    fun findAllByBankAccountIdOrderByStatementDateDesc(bankAccountId: Long): List<BankStatement>

    fun findByIdAndBankAccountId(id: Long, bankAccountId: Long): BankStatement?

    fun findTopByBankAccountIdOrderByStatementDateDesc(bankAccountId: Long): BankStatement?
}
