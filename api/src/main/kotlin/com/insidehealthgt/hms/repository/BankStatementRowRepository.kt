package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.BankStatementRow
import com.insidehealthgt.hms.entity.MatchStatus
import com.insidehealthgt.hms.entity.MatchedEntityType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BankStatementRowRepository : JpaRepository<BankStatementRow, Long> {

    fun findAllByBankStatementIdOrderByRowNumberAsc(bankStatementId: Long): List<BankStatementRow>

    fun countByBankStatementIdAndMatchStatus(bankStatementId: Long, matchStatus: MatchStatus): Int

    fun existsByMatchedEntityTypeAndMatchedEntityIdAndMatchStatusAndIdNot(
        matchedEntityType: MatchedEntityType,
        matchedEntityId: Long,
        matchStatus: MatchStatus,
        excludeRowId: Long,
    ): Boolean

    @Query(
        "SELECT r.matchStatus, COUNT(r) FROM BankStatementRow r " +
            "WHERE r.bankStatement.id = :bankStatementId GROUP BY r.matchStatus",
    )
    fun countGroupedByMatchStatus(bankStatementId: Long): List<Array<Any>>
}
