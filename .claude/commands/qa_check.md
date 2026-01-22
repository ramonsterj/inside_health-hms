# QA Check

Review the quality of a feature implementation, refactoring, bug fix, or any other development task against its feature specification.

## Usage

```
/qa_check <feature_doc_path>
```

Example: `/qa_check docs/features/user-profile.md`

## Steps

### Phase 1: Preparation

1. **Verify feature doc exists** at the provided path and review acceptance criteria
2. Run `git status` to see all changes
3. **Code/Documentation alignment check**:
   - Compare the feature doc's requirements against the actual code changes
   - Verify that modified files align with what the feature describes
   - Check for obvious mismatches: missing implementations, extra functionality not in spec, wrong modules modified
   - **STOP and report conflicts** if there are fundamental gaps or misalignments before proceeding
   - Only continue to detailed QA if code and documentation are conceptually aligned

### Phase 2: Backend Quality (Kotlin/Spring Boot)

4. Identify all modified Kotlin files (`.kt`) in the `api/` directory
5. For each modified file (implementation and tests):
   - Run `cd api && ./gradlew compileKotlin compileTestKotlin` and fix all compilation errors
   - Run `cd api && ./gradlew detekt` and fix all linting issues
   - Run `cd api && ./gradlew ktlintCheck` if available, or ensure code follows Kotlin conventions

6. **Backend code quality review** for each modified file:
   - **Entity checks**:
     - Entities MUST extend `BaseEntity` (not define their own id)
     - Entities MUST have `@SQLRestriction("deleted_at IS NULL")` for soft deletes
     - Entities MUST NOT be `data class`
     - All `@Column` annotations specify `length` for strings
     - Enums use `@Enumerated(EnumType.STRING)`
   - **Architecture checks**:
     - Controllers return DTOs, never entities directly
     - Services handle business logic, controllers are thin
     - Repositories extend `JpaRepository` or custom base
     - Password handling uses `PasswordEncoder`, never plain text
   - **General quality**:
     - No code duplication (DRY principle)
     - Single responsibility per class/function
     - Proper error handling with custom exceptions
     - No hardcoded secrets or credentials
   - **Documentation**:
     - Public API methods have KDoc comments
     - Complex logic has inline comments explaining "why"

### Phase 3: Frontend Quality (Vue.js/TypeScript)

7. Identify all modified frontend files (`.ts`, `.vue`) in the `web/` directory
8. For each modified file:
   - Run `cd web && npm run type-check` (or `vue-tsc --noEmit`) and fix all TypeScript errors
   - Run `cd web && npm run lint` and fix all linting issues
   - Run `cd web && npm run format:check` (or `prettier --check`) and fix formatting

9. **Frontend code quality review** for each modified file:
   - **Component checks**:
     - Components use Composition API with `<script setup lang="ts">`
     - Props and emits are properly typed
     - Reactive state uses `ref()` or `reactive()` appropriately
   - **State management**:
     - Pinia stores follow project conventions
     - API calls go through stores or composables, not directly in components
   - **Validation**:
     - Forms use VeeValidate with Zod schemas
     - Error messages are user-friendly and i18n-ready
   - **General quality**:
     - No code duplication
     - Proper TypeScript types (avoid `any`)
     - Async operations have proper error handling
     - No console.log statements left in production code

### Phase 4: Database & Migrations

10. **Migration checks** (if `db/migration/` files were modified):
    - Migration naming follows `V{version}__{description}.sql` pattern
    - New tables include ALL BaseEntity columns: `id`, `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`
    - Indexes exist on: foreign keys, `deleted_at`, frequently queried columns
    - No destructive operations without rollback plan documented

### Phase 5: Dependency Check

11. **Backend dependencies** (if `build.gradle.kts` was modified):
    - All new dependencies are justified and necessary
    - Run `cd api && ./gradlew dependencies` to verify resolution
    - Check for version conflicts or redundant dependencies

12. **Frontend dependencies** (if `package.json` was modified):
    - All new dependencies are justified and necessary
    - Run `cd web && npm ls` to verify installation
    - No duplicate packages with different versions

### Phase 6: Test Coverage

13. **Backend tests**:
    - Verify all new/modified implementation code has corresponding tests
    - Run `cd api && ./gradlew test` and ensure 100% pass rate
    - Check that tests use Testcontainers for database tests
    - Integration tests cover API endpoints
    - Unit tests cover service logic

14. **Frontend tests**:
    - Run `cd web && npm run test:run` for unit tests
    - Component tests exist for new components
    - Store tests exist for new stores

15. **Frontend E2E tests** (MANDATORY for frontend features):
    - Identify E2E test files related to the feature (check `web/e2e/` directory)
    - Run `cd web && npm run test:e2e` to execute all E2E tests
    - If feature-specific E2E tests exist, ensure they pass
    - Verify E2E tests cover the main user flows from the acceptance criteria

16. **Fix all failures**: If tests fail, fix them and re-run until all pass

### Phase 7: Final Validation

17. Run all checks one final time:
    ```bash
    # Backend
    cd api && ./gradlew clean build -x test  # Compile check
    cd api && ./gradlew detekt               # Linting
    cd api && ./gradlew test                 # Tests

    # Frontend
    cd web && npm run type-check             # TypeScript
    cd web && npm run lint                   # ESLint
    cd web && npm run build                  # Build check
    cd web && npm run test:run               # Unit tests
    cd web && npm run test:e2e               # E2E tests
    ```

### Phase 8: Feature Acceptance

18. **Acceptance criteria validation**:
    - Review each acceptance criterion from the feature doc
    - Verify each criterion is met by the implementation
    - Check all items in the feature doc's QA Checklist

19. **Project convention compliance**:
    - [ ] Entities extend `BaseEntity`
    - [ ] Soft deletes use `@SQLRestriction`
    - [ ] Controllers use DTOs (request/response)
    - [ ] JPA auditing fields work (`createdBy`, `updatedBy`)
    - [ ] i18n messages added for user-facing text
    - [ ] API follows RESTful conventions
    - [ ] Frontend uses Pinia for state management
    - [ ] Forms use VeeValidate + Zod

### Phase 9: Summary

20. Provide a comprehensive summary:
    - **Files reviewed**: List of all modified files checked
    - **Issues found**: Categorized by severity (critical/warning/info)
    - **Issues fixed**: What was corrected during the review
    - **Acceptance criteria**: Status of each criterion (✅ met / ❌ not met)
    - **Remaining concerns**: Any items that need user attention
    - **Overall status**: PASS / FAIL with reasoning

---

## Quick Reference: Project Conventions

### Entity Pattern
```kotlin
@Entity
@Table(name = "your_table")
@SQLRestriction("deleted_at IS NULL")
class YourEntity(
    @Column(nullable = false, length = 255)
    var name: String
) : BaseEntity()
```

### Controller Pattern
```kotlin
@RestController
@RequestMapping("/api/v1/resource")
class ResourceController(private val service: ResourceService) {
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<ResourceResponse> {
        return ResponseEntity.ok(service.findById(id))
    }
}
```

### Service Pattern
```kotlin
@Service
@Transactional
class ResourceService(private val repository: ResourceRepository) {
    fun findById(id: Long): ResourceResponse {
        val entity = repository.findById(id)
            .orElseThrow { EntityNotFoundException("Resource not found: $id") }
        return ResourceResponse.from(entity)
    }
}
```

---

## Notes

- This command validates both code quality AND feature completeness
- Tests are first-class code and receive the same quality scrutiny as implementation
- All issues found should be fixed before considering the implementation complete
- The feature doc path is mandatory to ensure traceability between specs and implementation
- If the feature is backend-only or frontend-only, skip the irrelevant phases
