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

    class Salutation {
        <<enumeration>>
        SR
        SRA
        SRTA
        DR
        DRA
        MR
        MRS
        MISS
    }

    class PhoneType {
        <<enumeration>>
        MOBILE
        PRACTICE
        HOME
        WORK
        OTHER
    }

    class AuditAction {
        <<enumeration>>
        CREATE
        UPDATE
        DELETE
    }

    class Sex {
        <<enumeration>>
        MALE
        FEMALE
    }

    class MaritalStatus {
        <<enumeration>>
        SINGLE
        MARRIED
        DIVORCED
        WIDOWED
        SEPARATED
        OTHER
    }

    class EducationLevel {
        <<enumeration>>
        NONE
        PRIMARY
        SECONDARY
        TECHNICAL
        UNIVERSITY
        POSTGRADUATE
    }

    class AdmissionStatus {
        <<enumeration>>
        ACTIVE
        DISCHARGED
    }

    class RoomType {
        <<enumeration>>
        PRIVATE
        SHARED
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
        +Salutation? salutation
        +Boolean mustChangePassword
        +getAllPermissions() Set~Permission~
        +hasPermission(String) Boolean
        +hasRole(String) Boolean
        +addPhoneNumber(UserPhoneNumber) void
        +removePhoneNumber(UserPhoneNumber) void
    }

    %% UserPhoneNumber Entity
    class UserPhoneNumber {
        +String phoneNumber
        +PhoneType phoneType
        +Boolean isPrimary
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

    %% Patient Entity
    class Patient {
        +String firstName
        +String lastName
        +Integer age
        +Sex sex
        +String gender
        +MaritalStatus maritalStatus
        +String religion
        +EducationLevel educationLevel
        +String occupation
        +String address
        +String email
        +String? idDocumentNumber
        +String? notes
        +hasIdDocument() Boolean
        +addEmergencyContact(EmergencyContact) void
        +removeEmergencyContact(EmergencyContact) void
    }

    %% EmergencyContact Entity
    class EmergencyContact {
        +String name
        +String relationship
        +String phone
    }

    %% PatientIdDocument Entity
    class PatientIdDocument {
        +String fileName
        +String contentType
        +Long fileSize
        +byte[] fileData
    }

    %% TriageCode Entity
    class TriageCode {
        +String code
        +String color
        +String? description
        +Integer displayOrder
    }

    %% Room Entity
    class Room {
        +String number
        +RoomType type
        +Integer capacity
    }

    %% Admission Entity
    class Admission {
        +LocalDateTime admissionDate
        +LocalDateTime? dischargeDate
        +AdmissionStatus status
        +String? inventory
        +hasConsentDocument() Boolean
        +isActive() Boolean
        +isDischarged() Boolean
    }

    %% AdmissionConsentDocument Entity
    class AdmissionConsentDocument {
        +String fileName
        +String contentType
        +Long fileSize
        +byte[] fileData
    }

    %% AdmissionConsultingPhysician Entity
    class AdmissionConsultingPhysician {
        +String? reason
        +LocalDate? requestedDate
    }

    %% Inheritance Relationships
    BaseEntity <|-- User : extends
    BaseEntity <|-- UserPhoneNumber : extends
    BaseEntity <|-- Role : extends
    BaseEntity <|-- Permission : extends
    BaseEntity <|-- RefreshToken : extends
    BaseEntity <|-- PasswordResetToken : extends
    BaseEntity <|-- Patient : extends
    BaseEntity <|-- EmergencyContact : extends
    BaseEntity <|-- PatientIdDocument : extends
    BaseEntity <|-- TriageCode : extends
    BaseEntity <|-- Room : extends
    BaseEntity <|-- Admission : extends
    BaseEntity <|-- AdmissionConsentDocument : extends
    BaseEntity <|-- AdmissionConsultingPhysician : extends

    %% Associations
    User "1" -- "*" RefreshToken : has
    User "1" -- "*" PasswordResetToken : has
    User "1" -- "*" UserPhoneNumber : has
    User "*" -- "*" Role : user_roles
    Role "*" -- "*" Permission : role_permissions
    Patient "1" -- "*" EmergencyContact : has
    Patient "1" -- "0..1" PatientIdDocument : has
    Patient "1" -- "*" Admission : has
    Admission "*" -- "1" TriageCode : uses
    Admission "*" -- "1" Room : assigned to
    Admission "*" -- "1" User : treating physician
    Admission "1" -- "0..1" AdmissionConsentDocument : has
    Admission "1" -- "*" AdmissionConsultingPhysician : has
    AdmissionConsultingPhysician "*" -- "1" User : physician

    %% Enum Usage
    User ..> UserStatus : uses
    User ..> Salutation : uses
    UserPhoneNumber ..> PhoneType : uses
    AuditLog ..> AuditAction : uses
    Patient ..> Sex : uses
    Patient ..> MaritalStatus : uses
    Patient ..> EducationLevel : uses
    Admission ..> AdmissionStatus : uses
    Room ..> RoomType : uses
```

## Entity Relationships

| Relationship | Type | Join Table | Description |
|-------------|------|------------|-------------|
| User ↔ Role | ManyToMany | `user_roles` | Users can have multiple roles |
| Role ↔ Permission | ManyToMany | `role_permissions` | Roles can have multiple permissions |
| User → RefreshToken | OneToMany | - | Users can have multiple active refresh tokens |
| User → PasswordResetToken | OneToMany | - | Users can have multiple password reset tokens |
| User → UserPhoneNumber | OneToMany | - | Users can have multiple phone numbers |
| Patient → EmergencyContact | OneToMany | - | Patients can have multiple emergency contacts |
| Patient → PatientIdDocument | OneToOne | - | Patient can have one ID document (optional) |
| Patient → Admission | OneToMany | - | Patients can have multiple admissions |
| Admission → TriageCode | ManyToOne | - | Each admission has a triage code |
| Admission → Room | ManyToOne | - | Each admission is assigned to a room |
| Admission → User (treating) | ManyToOne | - | Each admission has a treating physician |
| Admission → AdmissionConsentDocument | OneToOne | - | Admission can have one consent document (optional) |
| Admission → AdmissionConsultingPhysician | OneToMany | - | Admissions can have multiple consulting physicians |
| AdmissionConsultingPhysician → User | ManyToOne | - | Each consulting record references a physician |

## Notes

- **BaseEntity**: Abstract mapped superclass providing common audit fields and soft delete support
- **AuditLog**: Standalone entity (does not extend BaseEntity) - audit logs are immutable records
- **Soft Deletes**: All entities except AuditLog use `@SQLRestriction("deleted_at IS NULL")`
- **Audit Trail**: All entities extending BaseEntity track who created/updated them via createdBy/updatedBy fields (reference User)
