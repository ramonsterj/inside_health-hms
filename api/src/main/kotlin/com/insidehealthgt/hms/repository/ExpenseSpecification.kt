package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpenseStatus
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate

object ExpenseSpecification {

    fun withFilters(
        status: ExpenseStatus?,
        category: ExpenseCategory?,
        from: LocalDate?,
        to: LocalDate?,
        search: String?,
    ): Specification<Expense> {
        var spec: Specification<Expense> = Specification { _, _, _ -> null }

        if (status != null) {
            spec = spec.and(
                Specification { root: Root<Expense>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.equal(root.get<ExpenseStatus>("status"), status)
                },
            )
        }
        if (category != null) {
            spec = spec.and(
                Specification { root: Root<Expense>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.equal(root.get<ExpenseCategory>("category"), category)
                },
            )
        }
        if (from != null) {
            spec = spec.and(
                Specification { root: Root<Expense>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.greaterThanOrEqualTo(root.get("expenseDate"), from)
                },
            )
        }
        if (to != null) {
            spec = spec.and(
                Specification { root: Root<Expense>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.lessThanOrEqualTo(root.get("expenseDate"), to)
                },
            )
        }
        if (!search.isNullOrBlank()) {
            spec = spec.and(
                Specification { root: Root<Expense>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    val pattern = "%${search.lowercase()}%"
                    cb.or(
                        cb.like(cb.lower(root.get("supplierName")), pattern),
                        cb.like(cb.lower(root.get("invoiceNumber")), pattern),
                    )
                },
            )
        }

        return spec
    }
}
