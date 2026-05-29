package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.RoleDefaultWarehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RoleDefaultWarehouseRepository : JpaRepository<RoleDefaultWarehouse, Long> {

    @Query(
        "SELECT rdw FROM RoleDefaultWarehouse rdw JOIN FETCH rdw.warehouse " +
            "WHERE rdw.role.code IN (:roleCodes)",
    )
    fun findByRoleCodes(@Param("roleCodes") roleCodes: Collection<String>): List<RoleDefaultWarehouse>
}
