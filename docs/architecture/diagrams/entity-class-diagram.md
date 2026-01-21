# Entity Class Diagram

```mermaid
classDiagram
    direction TB

    %% Base Entity (MappedSuperclass)
    class BaseEntity {
        <<abstract>>
        +Long? id
        +LocalDateTime? createdAt
        +LocalDateTime? updatedAt
        +Long? createdBy
        +Long? updatedBy
        +LocalDateTime? deletedAt
    }

    %% Enums
    class UserStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        SUSPENDED
        DELETED
    }

    class AuditAction {
        <<enumeration>>
        CREATE
        UPDATE
        DELETE
    }

    %% User Entity
    class User {
        +String username
        +String email
        +String passwordHash
        +String? firstName
        +String? lastName
        +UserStatus status
        +Boolean emailVerified
        +String? localePreference
        +getAllPermissions() Set~Permission~
        +hasPermission(String) Boolean
        +hasRole(String) Boolean
    }

    %% Role Entity
    class Role {
        +String code
        +String name
        +String? description
        +Boolean isSystem
    }

    %% Permission Entity
    class Permission {
        +String code
        +String name
        +String? description
        +String resource
        +String action
    }

    %% RefreshToken Entity
    class RefreshToken {
        +String token
        +LocalDateTime expiresAt
    }

    %% PasswordResetToken Entity
    class PasswordResetToken {
        +String token
        +LocalDateTime expiresAt
    }

    %% AuditLog Entity (standalone, no BaseEntity)
    class AuditLog {
        +Long? id
        +Long? userId
        +String? username
        +AuditAction action
        +String entityType
        +Long entityId
        +String? oldValues
        +String? newValues
        +String? ipAddress
        +String? changedFields
        +LocalDateTime timestamp
        +LocalDateTime createdAt
    }

    %% Inheritance Relationships
    BaseEntity <|-- User : extends
    BaseEntity <|-- Role : extends
    BaseEntity <|-- Permission : extends
    BaseEntity <|-- RefreshToken : extends
    BaseEntity <|-- PasswordResetToken : extends

    %% Associations
    User "1" -- "*" RefreshToken : has
    User "1" -- "*" PasswordResetToken : has
    User "*" -- "*" Role : user_roles
    Role "*" -- "*" Permission : role_permissions

    %% Enum Usage
    User ..> UserStatus : uses
    AuditLog ..> AuditAction : uses
```

## Entity Relationships

| Relationship | Type | Join Table | Description |
|-------------|------|------------|-------------|
| User ↔ Role | ManyToMany | `user_roles` | Users can have multiple roles |
| Role ↔ Permission | ManyToMany | `role_permissions` | Roles can have multiple permissions |
| User → RefreshToken | OneToMany | - | Users can have multiple active refresh tokens |
| User → PasswordResetToken | OneToMany | - | Users can have multiple password reset tokens |

## Notes

- **BaseEntity**: Abstract mapped superclass providing common audit fields and soft delete support
- **AuditLog**: Standalone entity (does not extend BaseEntity) - audit logs are immutable records
- **Soft Deletes**: All entities except AuditLog use `@SQLRestriction("deleted_at IS NULL")`
