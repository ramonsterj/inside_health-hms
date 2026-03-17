package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository :
    JpaRepository<Expense, Long>,
    JpaSpecificationExecutor<Expense>
