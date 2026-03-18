package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Income
import com.insidehealthgt.hms.entity.IncomeCategory
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate

object IncomeSpecification {

    fun withFilters(
        category: IncomeCategory?,
        bankAccountId: Long?,
        from: LocalDate?,
        to: LocalDate?,
        search: String?,
    ): Specification<Income> {
        var spec: Specification<Income> = Specification { _, _, _ -> null }

        if (category != null) {
            spec = spec.and(
                Specification { root: Root<Income>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.equal(root.get<IncomeCategory>("category"), category)
                },
            )
        }
        if (bankAccountId != null) {
            spec = spec.and(
                Specification { root: Root<Income>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.equal(root.get<Any>("bankAccount").get<Long>("id"), bankAccountId)
                },
            )
        }
        if (from != null) {
            spec = spec.and(
                Specification { root: Root<Income>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.greaterThanOrEqualTo(root.get("incomeDate"), from)
                },
            )
        }
        if (to != null) {
            spec = spec.and(
                Specification { root: Root<Income>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    cb.lessThanOrEqualTo(root.get("incomeDate"), to)
                },
            )
        }
        if (!search.isNullOrBlank()) {
            spec = spec.and(
                Specification { root: Root<Income>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
                    val pattern = "%${search.lowercase()}%"
                    cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("reference")), pattern),
                    )
                },
            )
        }

        return spec
    }
}
