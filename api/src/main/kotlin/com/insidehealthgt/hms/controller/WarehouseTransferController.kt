package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateTransferRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.dto.response.TransferResponse
import com.insidehealthgt.hms.service.WarehouseTransferService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/warehouse-transfers")
class WarehouseTransferController(private val transferService: WarehouseTransferService) {

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse-transfer:create')")
    fun create(@Valid @RequestBody request: CreateTransferRequest): ResponseEntity<ApiResponse<TransferResponse>> {
        val created = transferService.createTransfer(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse-transfer:read')")
    fun list(
        @RequestParam(required = false) warehouseId: Long?,
        @RequestParam(required = false) itemId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<TransferResponse>>> {
        val page = transferService.listTransfers(warehouseId, itemId, pageable)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)))
    }
}
