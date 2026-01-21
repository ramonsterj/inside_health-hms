package com.insidehealthgt.hms.audit

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.entity.AuditLog
import com.insidehealthgt.hms.entity.BaseEntity
import com.insidehealthgt.hms.security.CustomUserDetails
import jakarta.persistence.EntityManager
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.PreUpdate
import org.hibernate.engine.spi.SessionImplementor
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

/**
 * JPA Entity Listener that automatically logs CREATE, UPDATE, and DELETE operations
 * on entities that extend BaseEntity.
 *
 * Note: JPA instantiates entity listeners directly, so we use SpringContext
 * to access Spring beans. Events are published and handled by AuditEventHandler
 * after the transaction commits to avoid Hibernate 6's flush callback restrictions.
 */
class AuditEntityListener {
    private val log = LoggerFactory.getLogger(AuditEntityListener::class.java)

    private val eventPublisher: ApplicationEventPublisher?
        get() = SpringContext.getBeanOrNull(ApplicationEventPublisher::class.java)

    private val entityManager: EntityManager?
        get() = SpringContext.getBeanOrNull(EntityManager::class.java)

    /**
     * Holds the pre-update snapshot: old values JSON and list of changed field names.
     */
    private data class PreUpdateSnapshot(val oldValues: String, val changedFields: List<String>)

    companion object {
        // ThreadLocal to store entity state before update for comparison
        private val preUpdateState = ThreadLocal<MutableMap<Long, PreUpdateSnapshot>>()

        // Fields to exclude from audit logging (sensitive data)
        private val EXCLUDED_FIELDS = setOf(
            "passwordHash",
            "password",
            "token",
            "refreshToken",
            "secret",
        )

        // ObjectMapper for serializing entities to JSON
        private val objectMapper: ObjectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        }
    }

    @PreUpdate
    fun preUpdate(entity: Any) {
        if (entity is AuditLog || entity !is BaseEntity) return
        val id = entity.id ?: return

        try {
            // Get the original values and changed fields from Hibernate's persistence context
            val snapshot = getPreUpdateSnapshot(entity)
            if (snapshot != null) {
                val map = preUpdateState.get()
                    ?: mutableMapOf<Long, PreUpdateSnapshot>().also {
                        preUpdateState.set(it)
                    }
                map[id] = snapshot
            }
        } catch (e: IllegalStateException) {
            log.warn("Failed to get pre-update state for entity: ${entity::class.simpleName}", e)
        } catch (e: IllegalArgumentException) {
            log.warn("Failed to get pre-update state for entity: ${entity::class.simpleName}", e)
        }
    }

    /**
     * Gets the original state of an entity from Hibernate's persistence context and computes
     * which fields have changed (including sensitive fields - we record the field name but not the value).
     */
    private fun getPreUpdateSnapshot(entity: BaseEntity): PreUpdateSnapshot? {
        val (entityEntry, loadedState) = getEntityEntryWithLoadedState(entity) ?: return null

        val persister = entityEntry.persister
        val propertyNames = persister.propertyNames
        val originalMap = mutableMapOf<String, Any?>()
        val changedFields = mutableListOf<String>()
        originalMap["id"] = entity.id

        for (i in propertyNames.indices) {
            val propertyName = propertyNames[i]
            val oldValue = loadedState[i]
            val newValue = persister.getValue(entity, i)

            if (!valuesEqual(oldValue, newValue)) {
                changedFields.add(propertyName)
            }

            val shouldSkip = propertyName in EXCLUDED_FIELDS || oldValue is Collection<*>
            if (!shouldSkip) {
                addToOriginalMap(originalMap, propertyName, oldValue)
            }
        }

        return PreUpdateSnapshot(
            oldValues = objectMapper.writeValueAsString(originalMap),
            changedFields = changedFields,
        )
    }

    private fun getEntityEntryWithLoadedState(
        entity: BaseEntity,
    ): Pair<org.hibernate.engine.spi.EntityEntry, Array<Any?>>? {
        val entityEntry = entityManager
            ?.unwrap(SessionImplementor::class.java)
            ?.persistenceContext
            ?.getEntry(entity)
        val loadedState = entityEntry?.loadedState
        return if (entityEntry != null && loadedState != null) Pair(entityEntry, loadedState) else null
    }

    private fun addToOriginalMap(map: MutableMap<String, Any?>, propertyName: String, value: Any?) {
        when (value) {
            null -> map[propertyName] = null
            is BaseEntity -> map[propertyName + "Id"] = value.id
            else -> map[propertyName] = value
        }
    }

    /**
     * Compares two values for equality, handling entity references by ID.
     */
    private fun valuesEqual(oldValue: Any?, newValue: Any?): Boolean = when {
        oldValue == newValue -> true
        oldValue == null || newValue == null -> false
        oldValue is BaseEntity && newValue is BaseEntity -> oldValue.id == newValue.id
        else -> oldValue == newValue
    }

    @PostPersist
    fun postPersist(entity: Any) {
        if (entity is AuditLog || entity !is BaseEntity) return

        try {
            publishAuditEvent(
                action = AuditAction.CREATE,
                entity = entity,
                oldValues = null,
                newValues = serializeEntity(entity),
                changedFields = null, // All fields are "new" on create
            )
        } catch (e: JsonProcessingException) {
            log.error("Failed to serialize entity for CREATE audit log", e)
        }
    }

    @PostUpdate
    fun postUpdate(entity: Any) {
        if (entity is AuditLog || entity !is BaseEntity) return
        val id = entity.id ?: return

        try {
            val snapshot = preUpdateState.get()?.remove(id)

            publishAuditEvent(
                action = AuditAction.UPDATE,
                entity = entity,
                oldValues = snapshot?.oldValues,
                newValues = serializeEntity(entity),
                changedFields = snapshot?.changedFields,
            )

            // Clean up ThreadLocal if empty
            if (preUpdateState.get()?.isEmpty() == true) {
                preUpdateState.remove()
            }
        } catch (e: JsonProcessingException) {
            log.error("Failed to serialize entity for UPDATE audit log", e)
        }
    }

    @PostRemove
    fun postRemove(entity: Any) {
        if (entity is AuditLog || entity !is BaseEntity) return

        try {
            publishAuditEvent(
                action = AuditAction.DELETE,
                entity = entity,
                oldValues = serializeEntity(entity),
                newValues = null,
                changedFields = null, // All fields are being removed
            )
        } catch (e: JsonProcessingException) {
            log.error("Failed to serialize entity for DELETE audit log", e)
        }
    }

    private fun publishAuditEvent(
        action: AuditAction,
        entity: BaseEntity,
        oldValues: String?,
        newValues: String?,
        changedFields: List<String>?,
    ) {
        val publisher = eventPublisher
        if (publisher == null) {
            log.warn("ApplicationEventPublisher not available, skipping audit log")
            return
        }

        val (userId, username) = getCurrentUser()
        val ipAddress = AuditContext.getIpAddress()

        val event = AuditEvent(
            userId = userId,
            username = username,
            action = action,
            entityType = entity::class.simpleName ?: "Unknown",
            entityId = entity.id ?: 0L,
            oldValues = oldValues,
            newValues = newValues,
            changedFields = changedFields,
            ipAddress = ipAddress,
            timestamp = LocalDateTime.now(),
        )

        publisher.publishEvent(event)
        log.info(
            "Audit event published: {} {} {}",
            action,
            entity::class.simpleName,
            entity.id,
        )
    }

    private fun getCurrentUser(): Pair<Long?, String?> {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal
        val isValidAuth = authentication != null &&
            authentication.isAuthenticated &&
            principal != "anonymousUser"
        return if (isValidAuth && principal is CustomUserDetails) {
            Pair(principal.id, principal.username)
        } else {
            Pair(null, null)
        }
    }

    private fun serializeEntity(entity: Any): String {
        // Create a map of non-sensitive fields
        val entityMap = mutableMapOf<String, Any?>()

        // Get all fields including inherited ones
        var currentClass: Class<*>? = entity::class.java
        while (currentClass != null && currentClass != Any::class.java) {
            currentClass.declaredFields.forEach { field ->
                // Skip fields marked with @JsonIgnore
                if (field.isAnnotationPresent(JsonIgnore::class.java)) return@forEach

                // Skip excluded sensitive fields
                if (field.name in EXCLUDED_FIELDS) return@forEach

                // Skip synthetic fields
                if (field.name.contains("$")) return@forEach

                field.isAccessible = true
                try {
                    val value = field.get(entity)
                    // Skip all collections to avoid circular references
                    if (value is Collection<*>) {
                        return@forEach
                    }
                    // For entity references, just store the ID
                    if (value is BaseEntity) {
                        entityMap[field.name + "Id"] = value.id
                        return@forEach
                    }
                    entityMap[field.name] = value
                } catch (ignored: ReflectiveOperationException) {
                    // Skip fields that can't be accessed
                }
            }
            currentClass = currentClass.superclass
        }

        return objectMapper.writeValueAsString(entityMap)
    }
}
