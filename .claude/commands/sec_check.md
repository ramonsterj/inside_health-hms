# Security Check

Run all security scanning tools and attempt to fix any issues found.

## Tools to Run

Execute these security checks in order:

### 1. Gitleaks (Secret Scanning)
```bash
gitleaks detect --source . --no-git --verbose
```
- Scans for hardcoded secrets, API keys, passwords, tokens
- If secrets found:
  - Remove the hardcoded secret from the code
  - Replace with environment variable reference (e.g., `System.getenv("API_KEY")` or `process.env.API_KEY`)
  - Warn user that any exposed secrets should be rotated immediately

### 2. Detekt (Kotlin Static Analysis)
```bash
cd api && ./gradlew detekt
```
- Scans for code quality issues including security anti-patterns
- Run `./gradlew detekt --auto-correct` first for style issues
- For remaining issues: edit the code to fix security anti-patterns (hardcoded credentials, unsafe deserialization, etc.)

### 3. ESLint Security (Frontend)
```bash
cd web && npm run lint
```
- Includes eslint-plugin-security rules
- Run `npm run lint:fix` first for auto-fixable issues
- For remaining security issues: edit the code to fix them (e.g., replace object bracket notation with safer patterns, sanitize user input)

### 4. npm audit (Node.js Dependencies)
```bash
cd web && npm audit --audit-level=high
```
- Checks for known vulnerabilities in npm dependencies
- Run `npm audit fix` first for compatible updates
- If vulnerabilities remain: update package.json with fixed versions manually
- For transitive dependencies: check if parent package has an update, or add overrides in package.json

### 5. OWASP Dependency Check (JVM Dependencies)
```bash
cd api && ./gradlew dependencyCheckAnalyze
```
- Scans Java/Kotlin dependencies against NVD vulnerability database
- **Runtime**: 2-5+ minutes (downloads/updates vulnerability DB)
- Report location: `api/build/reports/dependency-check-report.html`
- For vulnerabilities found: update dependency versions in build.gradle.kts
- For transitive dependencies: add explicit version constraints or exclusions

## Fixing Strategy

For each issue found:

1. **Auto-fixable via tool**: Run the tool's auto-fix command (e.g., `npm audit fix`, `./gradlew detekt --auto-correct`)
2. **Requires code changes**: Fix the issue directly by editing the affected files:
   - Update vulnerable dependency versions in build.gradle.kts or package.json
   - Refactor insecure code patterns (e.g., replace eval(), fix SQL injection, remove hardcoded secrets)
   - Add input validation or sanitization where needed
3. **Truly unfixable**: Only mark as unfixable if ALL of these apply:
   - No newer version of the dependency exists that fixes the issue
   - No code workaround is possible
   - The vulnerability is in a transitive dependency with no available update

   In this case, explain:
   - Why it cannot be fixed
   - The risk level and whether the vulnerable code path is actually used
   - Recommend adding to suppression file if it's a false positive or accepted risk

## Output Summary

After running all tools and applying fixes, provide:
1. Total issues found per tool
2. Issues fixed (automatically or via code edits)
3. Issues that could not be fixed (with clear explanations why)
