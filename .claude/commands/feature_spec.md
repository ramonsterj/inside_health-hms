# Create Feature Specification

Guide the user through creating a complete feature specification document following the project template.

## Overview

This command interactively gathers information to create a feature spec at `docs/features/FEATURE_NAME.md` using the template at `docs/templates/FEATURE_TEMPLATE.md`.

## Process

### Phase 1: Core Understanding (Ask the User)

Ask these questions ONE AT A TIME, waiting for the user's response before proceeding:

1. **Feature Name**: "What should we call this feature? (e.g., 'User Profile Management', 'Export Reports')"

2. **Overview**: "In 1-3 sentences, what does this feature do and why do we need it?"

3. **Primary Users**: "Who will use this feature? (e.g., Admin, Regular User, API Consumer, multiple roles)"

4. **Functional Requirements**: "What are the must-have requirements? List the specific things this feature must do."

5. **Non-Functional Requirements** (optional): "Any performance, security, or reliability requirements? (Press Enter to skip)"

6. **Related Items** (optional): "Any related GitHub issues, existing features, or documentation to link? (Press Enter to skip)"

### Phase 2: User Stories (Claude Proposes)

Based on the information gathered, PROPOSE 1-3 user stories:

- Analyze the feature overview, primary users, and requirements
- Draft user stories in the format: "As a [role], I want to [action] so that [benefit]."
- Consider different user perspectives if multiple roles are involved
- Present the proposed user stories and ask: "Here are the user stories I've drafted. Do these look correct, or would you like to modify any?"

Wait for user confirmation or modifications before proceeding.

### Phase 3: Authorization (Ask the User)

7. **Roles & Permissions**: "Which roles should have access to this feature? What actions can each role perform?"
   - Prompt for: View, Create, Update, Delete permissions
   - Ask about any special permission requirements

### Phase 4: Acceptance Criteria (Claude Proposes)

Based on ALL information gathered so far, PROPOSE acceptance criteria:

- Draft 4-8 concrete test scenarios
- Include happy path scenarios
- Include edge cases worth testing:
  - Invalid input handling
  - Permission/authorization failures
  - Boundary conditions
  - Error states
  - Concurrent access (if applicable)
- Present the proposed criteria and ask: "Here are the acceptance criteria I've drafted, including some edge cases. Do these cover everything, or would you like to add/modify any?"

Wait for user confirmation or modifications before proceeding.

### Phase 5: Technical Design (Claude Proposes Full Document)

Based on everything discussed, CREATE THE COMPLETE FEATURE SPEC including:

- API Contract (endpoints, DTOs, request/response examples)
- Database Changes (entities, migrations, indexes)
- Frontend Changes (components, stores, routes, validation schemas)
- Implementation Notes (patterns to follow, considerations)
- QA Checklist (pre-filled based on feature scope)
- Documentation Updates Required

Then present a summary: "I've created the complete feature specification at `docs/features/{name}.md`. Please review it and let me know if you'd like any changes."

The user may request modifications - iterate until approved.

## Output

After Phase 5:

1. Read the template from `docs/templates/FEATURE_TEMPLATE.md`
2. Create the feature spec file at `docs/features/{feature-name-kebab-case}.md`
3. Fill in ALL sections from the template:
   - User-provided info (overview, requirements, permissions)
   - Confirmed user stories
   - Confirmed acceptance criteria
   - Claude-proposed technical design (API, DB, Frontend)
4. Set the Revision History to version 1.0 with today's date
5. Pre-fill QA Checklist based on feature scope (backend-only, frontend-only, or full-stack)
6. Ask user to review the complete document and request any changes
7. Iterate on changes until the user approves

## Guidelines

- Ask questions conversationally, one at a time
- If the user's answer is unclear, ask a follow-up question
- Suggest examples when helpful
- When proposing user stories, think from each user role's perspective
- When proposing acceptance criteria, be thorough about edge cases:
  - Invalid/malformed input
  - Missing required fields
  - Authorization failures (wrong role, wrong user)
  - Business rule violations
  - Boundary conditions (empty lists, max lengths, etc.)
- For technical sections (API, Database, Frontend), design based on project patterns
- Use the project's conventions:
  - Entities extend `BaseEntity`
  - All tables need `deleted_at` index for soft deletes
  - DTOs for API contracts
  - Pinia stores for frontend state
  - VeeValidate + Zod for validation
- If the feature is backend-only or frontend-only, mark irrelevant sections as "N/A - [reason]"
- Create the `docs/features/` directory if it doesn't exist

## Example Interaction

```
User: /feature_spec

Claude: Let's create a feature specification! I'll ask you some questions, then propose user stories and acceptance criteria for your review.

**Question 1/6**: What should we call this feature?

User: Password Change

Claude: **Question 2/6**: In 1-3 sentences, what does this feature do and why do we need it?

User: Allow users to change their password from their profile settings. Required for basic account security.

Claude: **Question 3/6**: Who will use this feature?

User: Any authenticated user

[...questions 4-6...]

Claude: Based on what you've told me, here are the **user stories** I propose:

1. As an authenticated user, I want to change my password so that I can maintain my account security.
2. As an authenticated user, I want to receive confirmation when my password is changed so that I know the change was successful.

Do these look correct, or would you like to modify any?

User: Add one about being notified by email

Claude: Updated user stories:
1. As an authenticated user, I want to change my password so that I can maintain my account security.
2. As an authenticated user, I want to receive confirmation when my password is changed so that I know the change was successful.
3. As an authenticated user, I want to receive an email notification when my password is changed so that I'm alerted to any unauthorized changes.

**Question 7/7**: Which roles can perform this action? Any special permissions needed?

User: Any authenticated user can change their own password only

Claude: Now here are the **acceptance criteria** I propose, including edge cases:

**Happy Path:**
- When a user submits valid current password and valid new password, the password is updated and 200 OK is returned.
- When password is changed, user receives email notification.

**Edge Cases:**
- When current password is incorrect, return 400 Bad Request with error message.
- When new password doesn't meet complexity requirements, return 400 with validation errors.
- When new password matches current password, return 400 with appropriate message.
- When user is not authenticated, return 401 Unauthorized.
- When user tries to change another user's password, return 403 Forbidden.

Do these cover everything, or would you like to add/modify any?

User: Looks good

Claude: I've created the complete feature specification at `docs/features/password-change.md`.

**Summary:**
- 1 API endpoint: PUT /api/v1/users/me/password
- No new database tables (uses existing User entity)
- Frontend: PasswordChangeForm component, update to profile page
- Email notification on success

Please review the document and let me know if you'd like any changes.
```

## Notes

- Do NOT skip questions without user input (except optional ones)
- Do NOT assume answers for user questions - always ask
- DO propose user stories and acceptance criteria - don't ask the user to write them
- DO think through edge cases thoroughly when proposing acceptance criteria
- DO wait for user confirmation on proposals before proceeding
- DO iterate if user wants modifications to proposals
- DO create the docs/features/ directory if it doesn't exist
- DO use today's date in the revision history
- DO fill in the Author field with a placeholder like "@author" for the user to update
- DO ask user to review the final document and iterate until approved
