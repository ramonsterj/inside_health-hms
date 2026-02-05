package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.MedicalOrderCategory

data class GroupedMedicalOrdersResponse(val orders: Map<MedicalOrderCategory, List<MedicalOrderResponse>>)
