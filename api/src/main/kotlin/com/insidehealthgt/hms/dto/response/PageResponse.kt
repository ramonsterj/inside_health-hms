package com.insidehealthgt.hms.dto.response

import org.springframework.data.domain.Page

data class PageResponse<T : Any>(val content: List<T>, val page: PageInfo) {
    companion object {
        fun <T : Any> from(page: Page<T>): PageResponse<T> = PageResponse(
            content = page.content,
            page = PageInfo(
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                size = page.size,
                number = page.number,
            ),
        )
    }
}

data class PageInfo(val totalElements: Long, val totalPages: Int, val size: Int, val number: Int)
