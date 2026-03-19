package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.BankAccountColumnMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BankAccountColumnMappingRepository : JpaRepository<BankAccountColumnMapping, Long> {

    fun findByBankAccountId(bankAccountId: Long): BankAccountColumnMapping?
}
