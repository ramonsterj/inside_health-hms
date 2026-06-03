package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.LabPanel

data class LabPanelItemResponse(val labTestId: Long, val labTestName: String)

data class LabPanelResponse(
    val id: Long,
    val name: String,
    val active: Boolean,
    val items: List<LabPanelItemResponse>,
) {
    companion object {
        fun from(panel: LabPanel) = LabPanelResponse(
            id = panel.id!!,
            name = panel.name,
            active = panel.active,
            items = panel.items
                .filter { it.deletedAt == null }
                .map {
                    LabPanelItemResponse(labTestId = it.labTest.id!!, labTestName = it.labTest.name)
                },
        )
    }
}
