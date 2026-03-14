package com.insidehealthgt.hms.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(val cors: CorsProperties = CorsProperties(), val csp: CspProperties = CspProperties()) {
    data class CorsProperties(val additionalOrigins: List<String> = emptyList())

    data class CspProperties(val connectSrcExtra: List<String> = emptyList())
}
