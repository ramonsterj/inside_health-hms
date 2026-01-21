# Clean Code Review

Review the implementation of a feature for clean code violations and code smells, then fix them.

## Usage

```
/clean_code <feature_doc_path>
```

Example: `/clean_code docs/features/user-authentication.md`

## Steps

### Phase 1: Context Gathering

1. **Read the feature document** at the provided path to understand:
   - What the feature does
   - Which files/modules are involved
   - The acceptance criteria and requirements

2. **Identify implementation files** by:
   - Running `git log --name-only --oneline -20` to find recently changed files
   - Cross-referencing with the feature document's scope
   - Including both implementation AND test files

3. **Read all identified files** thoroughly before making any judgments

### Phase 2: Clean Code Analysis

For each file, systematically check for the following violations. **IMPORTANT**: Before flagging an issue, re-read the surrounding context and consider whether the pattern is intentional or justified.

#### SOLID Principle Violations

- **Single Responsibility Principle (SRP)**
  - Classes doing more than one thing (e.g., a service that handles both business logic AND email sending)
  - Methods with multiple unrelated responsibilities
  - *False positive check*: Is this a facade or orchestrator class that legitimately coordinates multiple operations?

- **Open/Closed Principle (OCP)**
  - Code that requires modification to extend behavior (lots of if/else or when/switch on types)
  - *False positive check*: Is the switch exhaustive and unlikely to change? Is it a simple mapping?

- **Liskov Substitution Principle (LSP)**
  - Subclasses that break parent class contracts
  - Overridden methods that throw unexpected exceptions or change behavior
  - *False positive check*: Is the override legitimate and documented?

- **Interface Segregation Principle (ISP)**
  - Large interfaces forcing implementers to define unused methods
  - *False positive check*: Is this a framework-required interface (e.g., Spring's interfaces)?

- **Dependency Inversion Principle (DIP)**
  - High-level modules depending on concrete implementations instead of abstractions
  - Direct instantiation of dependencies instead of injection
  - *False positive check*: Is this a simple utility class or value object where DI is overkill?

#### Code Smells

- **Long Method** (>20-30 lines typically)
  - *False positive check*: Is it a straightforward sequential process that would be harder to follow if split?

- **Large Class** (>300-500 lines typically)
  - *False positive check*: Is this an entity with many fields that legitimately belong together?

- **Feature Envy**
  - Methods that use more features of another class than its own
  - *False positive check*: Is this a legitimate transformer/mapper?

- **Data Clumps**
  - Same group of parameters appearing in multiple methods
  - *False positive check*: Are these parameters genuinely independent, or is there domain concept missing?

- **Primitive Obsession**
  - Using primitives instead of small objects for domain concepts (e.g., `String email` vs `Email` value object)
  - *False positive check*: Is the value truly simple with no validation or behavior?

- **Divergent Change**
  - One class changed for multiple different reasons
  - *False positive check*: Are the changes actually related through a single responsibility?

- **Shotgun Surgery**
  - One change requiring edits to many classes
  - *False positive check*: Is this a legitimate cross-cutting concern (e.g., renaming a domain concept)?

- **Parallel Inheritance Hierarchies**
  - Adding a subclass in one hierarchy requires adding one in another
  - *False positive check*: Is this a deliberate pattern like visitor or strategy?

- **Comments Explaining Bad Code**
  - Comments that explain "what" instead of "why"
  - Comments that could be eliminated by better naming
  - *False positive check*: Is this documenting a non-obvious business rule or workaround?

- **Dead Code**
  - Unused variables, methods, classes, or imports
  - Commented-out code
  - *False positive check*: Is this an interface method required by a contract?

- **Speculative Generality**
  - Abstract classes, interfaces, or parameters that exist "just in case"
  - *False positive check*: Is this a framework extension point or documented future requirement?

#### Naming & Readability

- **Poor Naming**
  - Single-letter variables (except loop indices or lambdas)
  - Misleading names that don't match behavior
  - Inconsistent naming conventions within the codebase
  - *False positive check*: Is this a well-known abbreviation in the domain?

- **Magic Numbers/Strings**
  - Hardcoded values without explanation
  - *False positive check*: Is the meaning obvious from context (e.g., `0`, `1`, `""`)?

- **Inconsistent Abstraction Levels**
  - Methods mixing high-level operations with low-level details
  - *False positive check*: Is the detail genuinely necessary inline for understanding?

#### Complexity Issues

- **Deeply Nested Conditionals** (>3 levels)
  - *False positive check*: Would guard clauses or extraction actually improve readability?

- **Complex Boolean Expressions**
  - Multiple conditions that are hard to understand
  - *False positive check*: Would extracting to a named method actually help, or add indirection?

- **God Object/Class**
  - Class that knows too much or does too much
  - *False positive check*: Is this a legitimate aggregate root or facade?

#### Duplication

- **Copy-Paste Code**
  - Nearly identical code blocks in multiple places
  - *False positive check*: Is the similarity coincidental rather than conceptual? Would extracting create coupling?

- **Duplicated Logic in Tests**
  - Same setup or assertion patterns repeated
  - *False positive check*: Would test helpers actually improve readability, or hide important test context?

### Phase 3: Verification & False Positive Elimination

For EACH potential issue identified:

1. **Re-read the code block** with fresh eyes
2. **Consider the context**: framework requirements, team conventions, intentional design decisions
3. **Ask**: "If I suggested this change to an experienced developer, would they agree it's an improvement?"
4. **Check for comments** explaining why the pattern exists
5. **Verify it's not a project convention** established in CLAUDE.md or ARCHITECTURE.md

**Remove any issue from your list if you have reasonable doubt about whether it's a genuine problem.**

### Phase 4: Severity Classification

Classify each confirmed issue:

- **Critical**: Actively harmful, causes bugs, security issues, or major maintenance burden
- **Warning**: Should be fixed for long-term maintainability
- **Info**: Nice-to-have improvement, low priority

### Phase 5: Write Plan Document

Create a plan document at `docs/reviews/clean-code-{feature-name}.md` with all findings:

```markdown
# Clean Code Review: [Feature Name]

**Feature Document**: [path to feature doc]
**Review Date**: [date]
**Status**: IN PROGRESS

## Summary
- **Files Reviewed**: X
- **Issues Found**: X (Critical: X, Warning: X, Info: X)
- **False Positives Eliminated**: X

## Issues to Fix

### Critical Issues

#### 1. [ ] [Issue Title]
- **File**: [path:line]
- **Type**: [Violation Category]
- **Problem**: [Description of the current state]
- **Impact**: [Why this hurts maintainability]
- **Not a False Positive Because**: [1 sentence justification]
- **Fix**: [Concrete steps to resolve]

[Repeat for each critical issue]

### Warnings

[Same format with [ ] checkbox]

### Info (Nice-to-Have)

[Same format with [ ] checkbox]

## Fix Order

1. **Quick Wins**: [list issue numbers]
2. **Isolated Changes**: [list issue numbers]
3. **Related Changes** (do together): [list grouped issue numbers]
4. **Larger Refactors**: [list issue numbers]
```

### Phase 6: Fix Issues One by One

Work through the plan document, fixing each issue in order:

#### For Quick Wins:
- **Dead code**: Delete unused variables, methods, imports, commented-out code
- **Magic numbers/strings**: Extract to named constants
- **Poor naming**: Rename to be descriptive
- **Redundant comments**: Remove comments that restate the code

#### For Medium Complexity:
- **Long methods**: Extract logical sections into well-named private methods
- **Complex conditionals**: Simplify with guard clauses, extract to named predicates
- **Data clumps**: Create parameter objects or value objects
- **Feature envy**: Move methods to the class whose data they use most

#### For Larger Refactors:
- **SRP violations**: Extract new classes for separate responsibilities
- **Large classes**: Split into focused, cohesive classes
- **Duplicated code**: Extract shared logic to common methods/classes

**After fixing each issue**:
1. Update the plan document: change `[ ]` to `[x]` for that issue
2. Add a brief note of what was done if different from planned
3. Continue to next issue

### Phase 7: Verification

After all fixes are applied:

1. **Run compilation checks**:
   ```bash
   # Backend
   cd api && ./gradlew compileKotlin compileTestKotlin

   # Frontend
   cd web && npm run type-check
   ```

2. **Run linting**:
   ```bash
   # Backend
   cd api && ./gradlew detekt

   # Frontend
   cd web && npm run lint
   ```

3. **Run tests**:
   ```bash
   # Backend
   cd api && ./gradlew test

   # Frontend
   cd web && npm run test
   ```

4. **Fix any failures** introduced by the refactoring

### Phase 8: Summarize and Clean Up

1. **Provide a summary to the user**:

```
# Clean Code Review Complete: [Feature Name]

## Summary
- **Files Reviewed**: X
- **Issues Found**: X (Critical: X, Warning: X, Info: X)
- **Issues Fixed**: X
- **False Positives Avoided**: X

## Verification Results
- Compilation: PASS/FAIL
- Linting: PASS/FAIL
- Tests: PASS/FAIL

## Changes Made
- [file1.kt]: [brief description of changes]
- [file2.ts]: [brief description of changes]

## Notes
[Any remaining concerns or observations]
```

2. **Delete the plan document** - it was a working artifact and is no longer needed

---

## Clean Code Principles Reference

### The Boy Scout Rule
"Leave the code cleaner than you found it" - but only for code you're already modifying.

### YAGNI (You Aren't Gonna Need It)
Don't add functionality until it's actually needed.

### KISS (Keep It Simple, Stupid)
The simplest solution that works is usually the best.

### DRY (Don't Repeat Yourself)
Every piece of knowledge should have a single, unambiguous representation.

### Principle of Least Astonishment
Code should behave as readers would expect.

---

## Notes

- This command analyzes AND fixes clean code issues
- All fixes are verified with compilation and tests before completing
- Some "violations" may be intentional design decisions - rigorously check for false positives
- Focus on issues that genuinely impact maintainability, not stylistic preferences
- Test code is included because tests are first-class code
- If a fix would break functionality or is too risky, document it instead of applying it
