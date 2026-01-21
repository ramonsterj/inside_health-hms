#!/bin/bash
#
# Template Project Setup Script
# Automates package renaming and configuration updates for new projects
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         Kotlin Spring Template - Project Setup            ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# =============================================================================
# Input Collection
# =============================================================================

# Company name (e.g., "acme" for com.acme.* or "dropsmethod" for com.dropsmethod)
read -p "Enter your company/organization name (lowercase, no spaces) [yourcompany]: " COMPANY_NAME
COMPANY_NAME=${COMPANY_NAME:-yourcompany}
COMPANY_NAME=$(echo "$COMPANY_NAME" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')

# Project name (optional - leave empty if company IS the project)
echo ""
echo -e "${YELLOW}Project name is optional.${NC}"
echo "  - For 'com.acme.inventory' enter: inventory"
echo "  - For 'com.dropsmethod' (company IS the app): press Enter to skip"
echo ""
read -p "Enter project name (leave empty if company IS the project): " PROJECT_NAME
PROJECT_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')

# Application display name (for configs and UI)
# Note: Using portable capitalization (works on Bash 3.x / macOS)
capitalize_first() {
    echo "$1" | awk '{print toupper(substr($0,1,1)) substr($0,2)}'
}

if [ -z "$PROJECT_NAME" ]; then
    DEFAULT_DISPLAY_NAME=$(capitalize_first "$COMPANY_NAME")
else
    DEFAULT_DISPLAY_NAME=$(capitalize_first "$PROJECT_NAME")
fi
read -p "Enter application display name [$DEFAULT_DISPLAY_NAME]: " APP_DISPLAY_NAME
APP_DISPLAY_NAME=${APP_DISPLAY_NAME:-$DEFAULT_DISPLAY_NAME}

# Database name
if [ -z "$PROJECT_NAME" ]; then
    DEFAULT_DB_NAME="${COMPANY_NAME}_db"
else
    DEFAULT_DB_NAME="${PROJECT_NAME}_db"
fi
read -p "Enter database name [$DEFAULT_DB_NAME]: " DATABASE_NAME
DATABASE_NAME=${DATABASE_NAME:-$DEFAULT_DB_NAME}

# =============================================================================
# Derived Variables
# =============================================================================

OLD_PACKAGE="com.yourcompany.template"
OLD_PACKAGE_PATH="com/yourcompany/template"

# Convert to PascalCase (portable - works on macOS/BSD)
# Handles: "my-app" -> "MyApp", "inventory" -> "Inventory"
to_pascal_case() {
    echo "$1" | awk -F'-' '{
        result = ""
        for (i = 1; i <= NF; i++) {
            result = result toupper(substr($i, 1, 1)) substr($i, 2)
        }
        print result
    }'
}

# Determine package structure based on whether project name is provided
if [ -z "$PROJECT_NAME" ]; then
    # Company IS the project (e.g., com.dropsmethod)
    NEW_PACKAGE="com.${COMPANY_NAME}"
    NEW_PACKAGE_PATH="com/${COMPANY_NAME}"
    PROJECT_IDENTIFIER="$COMPANY_NAME"
    # Application class name from company (PascalCase)
    APP_CLASS_NAME="$(to_pascal_case "$COMPANY_NAME")Application"
else
    # Separate company and project (e.g., com.acme.inventory)
    NEW_PACKAGE="com.${COMPANY_NAME}.${PROJECT_NAME}"
    NEW_PACKAGE_PATH="com/${COMPANY_NAME}/${PROJECT_NAME}"
    PROJECT_IDENTIFIER="$PROJECT_NAME"
    # Application class name from project (PascalCase)
    APP_CLASS_NAME="$(to_pascal_case "$PROJECT_NAME")Application"
fi

# Confirm before proceeding
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}Configuration Summary:${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "  Package:          ${GREEN}${NEW_PACKAGE}${NC}"
echo -e "  Project name:     ${GREEN}${PROJECT_IDENTIFIER}-api${NC} (backend) / ${GREEN}${PROJECT_IDENTIFIER}-web${NC} (frontend)"
echo -e "  Display name:     ${GREEN}${APP_DISPLAY_NAME}${NC}"
echo -e "  Database:         ${GREEN}${DATABASE_NAME}${NC}"
echo -e "  Docker container: ${GREEN}${PROJECT_IDENTIFIER}-postgres${NC}"
echo ""
read -p "Proceed with these settings? (y/N): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo -e "${RED}Setup cancelled.${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Starting setup...${NC}"
echo ""

# =============================================================================
# Step 1: Rename Kotlin packages (main source)
# =============================================================================

echo -e "${BLUE}[1/7]${NC} Renaming Kotlin packages (main source)..."

MAIN_SRC="$SCRIPT_DIR/api/src/main/kotlin"
OLD_MAIN_DIR="$MAIN_SRC/$OLD_PACKAGE_PATH"
NEW_MAIN_DIR="$MAIN_SRC/$NEW_PACKAGE_PATH"

if [ -d "$OLD_MAIN_DIR" ]; then
    # Create new package directory structure
    mkdir -p "$NEW_MAIN_DIR"

    # Move all contents
    if [ "$(ls -A "$OLD_MAIN_DIR")" ]; then
        cp -r "$OLD_MAIN_DIR"/* "$NEW_MAIN_DIR/"
    fi

    # Update package declarations in all Kotlin files
    find "$NEW_MAIN_DIR" -name "*.kt" -type f -exec sed -i.bak "s|$OLD_PACKAGE|$NEW_PACKAGE|g" {} \;
    find "$NEW_MAIN_DIR" -name "*.kt.bak" -type f -delete

    # Rename TemplateApplication.kt
    if [ -f "$NEW_MAIN_DIR/TemplateApplication.kt" ]; then
        sed -i.bak "s|TemplateApplication|${APP_CLASS_NAME}|g" "$NEW_MAIN_DIR/TemplateApplication.kt"
        rm -f "$NEW_MAIN_DIR/TemplateApplication.kt.bak"
        mv "$NEW_MAIN_DIR/TemplateApplication.kt" "$NEW_MAIN_DIR/${APP_CLASS_NAME}.kt"
    fi

    # Remove old directory structure
    rm -rf "$MAIN_SRC/com/yourcompany"

    echo -e "  ${GREEN}✓${NC} Main source packages renamed"
else
    echo -e "  ${YELLOW}⚠${NC} Main source directory not found, skipping"
fi

# =============================================================================
# Step 2: Rename Kotlin packages (test source)
# =============================================================================

echo -e "${BLUE}[2/7]${NC} Renaming Kotlin packages (test source)..."

TEST_SRC="$SCRIPT_DIR/api/src/test/kotlin"
OLD_TEST_DIR="$TEST_SRC/$OLD_PACKAGE_PATH"
NEW_TEST_DIR="$TEST_SRC/$NEW_PACKAGE_PATH"

if [ -d "$OLD_TEST_DIR" ]; then
    # Create new package directory structure
    mkdir -p "$NEW_TEST_DIR"

    # Move all contents
    if [ "$(ls -A "$OLD_TEST_DIR")" ]; then
        cp -r "$OLD_TEST_DIR"/* "$NEW_TEST_DIR/"
    fi

    # Update package declarations and imports in all Kotlin files
    find "$NEW_TEST_DIR" -name "*.kt" -type f -exec sed -i.bak "s|$OLD_PACKAGE|$NEW_PACKAGE|g" {} \;
    find "$NEW_TEST_DIR" -name "*.kt" -type f -exec sed -i.bak "s|TemplateApplication|${APP_CLASS_NAME}|g" {} \;
    find "$NEW_TEST_DIR" -name "*.kt.bak" -type f -delete

    # Rename test file to match new application class
    if [ -f "$NEW_TEST_DIR/TemplateApplicationTest.kt" ]; then
        mv "$NEW_TEST_DIR/TemplateApplicationTest.kt" "$NEW_TEST_DIR/${APP_CLASS_NAME}Test.kt"
    fi

    # Remove old directory structure
    rm -rf "$TEST_SRC/com/yourcompany"

    echo -e "  ${GREEN}✓${NC} Test source packages renamed"
else
    echo -e "  ${YELLOW}⚠${NC} Test source directory not found, skipping"
fi

# =============================================================================
# Step 3: Update Gradle configuration
# =============================================================================

echo -e "${BLUE}[3/7]${NC} Updating Gradle configuration..."

# settings.gradle.kts
SETTINGS_FILE="$SCRIPT_DIR/api/settings.gradle.kts"
if [ -f "$SETTINGS_FILE" ]; then
    sed -i.bak "s|rootProject.name = \"template-api\"|rootProject.name = \"${PROJECT_IDENTIFIER}-api\"|g" "$SETTINGS_FILE"
    rm -f "$SETTINGS_FILE.bak"
    echo -e "  ${GREEN}✓${NC} settings.gradle.kts updated"
fi

# build.gradle.kts
BUILD_FILE="$SCRIPT_DIR/api/build.gradle.kts"
if [ -f "$BUILD_FILE" ]; then
    sed -i.bak "s|group = \"com.yourcompany\"|group = \"com.${COMPANY_NAME}\"|g" "$BUILD_FILE"
    rm -f "$BUILD_FILE.bak"
    echo -e "  ${GREEN}✓${NC} build.gradle.kts updated"
fi

# =============================================================================
# Step 4: Update application configuration files
# =============================================================================

echo -e "${BLUE}[4/7]${NC} Updating application configuration..."

# application.yml
APP_YML="$SCRIPT_DIR/api/src/main/resources/application.yml"
if [ -f "$APP_YML" ]; then
    sed -i.bak "s|name: template-api|name: ${PROJECT_IDENTIFIER}-api|g" "$APP_YML"
    sed -i.bak "s|template_db|${DATABASE_NAME}|g" "$APP_YML"
    sed -i.bak "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$APP_YML"
    rm -f "$APP_YML.bak"
    echo -e "  ${GREEN}✓${NC} application.yml updated"
fi

# application-dev.yml
APP_DEV_YML="$SCRIPT_DIR/api/src/main/resources/application-dev.yml"
if [ -f "$APP_DEV_YML" ]; then
    sed -i.bak "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$APP_DEV_YML"
    rm -f "$APP_DEV_YML.bak"
    echo -e "  ${GREEN}✓${NC} application-dev.yml updated"
fi

# application-prod.yml
APP_PROD_YML="$SCRIPT_DIR/api/src/main/resources/application-prod.yml"
if [ -f "$APP_PROD_YML" ]; then
    sed -i.bak "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$APP_PROD_YML"
    rm -f "$APP_PROD_YML.bak"
    echo -e "  ${GREEN}✓${NC} application-prod.yml updated"
fi

# =============================================================================
# Step 5: Update Docker configuration
# =============================================================================

echo -e "${BLUE}[5/7]${NC} Updating Docker configuration..."

DOCKER_COMPOSE="$SCRIPT_DIR/docker-compose.yml"
if [ -f "$DOCKER_COMPOSE" ]; then
    sed -i.bak "s|container_name: template-postgres|container_name: ${PROJECT_IDENTIFIER}-postgres|g" "$DOCKER_COMPOSE"
    sed -i.bak "s|template_db|${DATABASE_NAME}|g" "$DOCKER_COMPOSE"
    rm -f "$DOCKER_COMPOSE.bak"
    echo -e "  ${GREEN}✓${NC} docker-compose.yml updated"
fi

# =============================================================================
# Step 6: Update Frontend configuration
# =============================================================================

echo -e "${BLUE}[6/7]${NC} Updating frontend configuration..."

# package.json
PACKAGE_JSON="$SCRIPT_DIR/web/package.json"
if [ -f "$PACKAGE_JSON" ]; then
    sed -i.bak "s|\"name\": \"web\"|\"name\": \"${PROJECT_IDENTIFIER}-web\"|g" "$PACKAGE_JSON"
    rm -f "$PACKAGE_JSON.bak"
    echo -e "  ${GREEN}✓${NC} package.json updated"
fi

# i18n locale files
EN_LOCALE="$SCRIPT_DIR/web/src/i18n/locales/en.json"
if [ -f "$EN_LOCALE" ]; then
    sed -i.bak "s|\"appName\": \"Template App\"|\"appName\": \"${APP_DISPLAY_NAME}\"|g" "$EN_LOCALE"
    rm -f "$EN_LOCALE.bak"
    echo -e "  ${GREEN}✓${NC} en.json locale updated"
fi

ES_LOCALE="$SCRIPT_DIR/web/src/i18n/locales/es.json"
if [ -f "$ES_LOCALE" ]; then
    sed -i.bak "s|\"appName\": \"Aplicacion Plantilla\"|\"appName\": \"${APP_DISPLAY_NAME}\"|g" "$ES_LOCALE"
    rm -f "$ES_LOCALE.bak"
    echo -e "  ${GREEN}✓${NC} es.json locale updated"
fi

# =============================================================================
# Step 7: Update documentation
# =============================================================================

echo -e "${BLUE}[7/7]${NC} Updating documentation references..."

# lefthook.yml
LEFTHOOK="$SCRIPT_DIR/lefthook.yml"
if [ -f "$LEFTHOOK" ]; then
    sed -i.bak "s|kotlin-spring-template|${PROJECT_IDENTIFIER}|g" "$LEFTHOOK"
    rm -f "$LEFTHOOK.bak"
    echo -e "  ${GREEN}✓${NC} lefthook.yml updated"
fi

# CLAUDE.md - update package references
CLAUDE_MD="$SCRIPT_DIR/CLAUDE.md"
if [ -f "$CLAUDE_MD" ]; then
    sed -i.bak "s|com.yourcompany.template|$NEW_PACKAGE|g" "$CLAUDE_MD"
    sed -i.bak "s|com.yourcompany|com.${COMPANY_NAME}|g" "$CLAUDE_MD"
    rm -f "$CLAUDE_MD.bak"
    echo -e "  ${GREEN}✓${NC} CLAUDE.md updated"
fi

# ARCHITECTURE.md - update package references
ARCH_MD="$SCRIPT_DIR/docs/architecture/ARCHITECTURE.md"
if [ -f "$ARCH_MD" ]; then
    sed -i.bak "s|com.yourcompany.template|$NEW_PACKAGE|g" "$ARCH_MD"
    sed -i.bak "s|com.yourcompany|com.${COMPANY_NAME}|g" "$ARCH_MD"
    rm -f "$ARCH_MD.bak"
    echo -e "  ${GREEN}✓${NC} ARCHITECTURE.md updated"
fi

# =============================================================================
# Git Repository Setup
# =============================================================================

echo ""
read -p "Would you like to reinitialize git (removes template history)? (y/N): " INIT_GIT

if [[ "$INIT_GIT" =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}Reinitializing git repository...${NC}"
    cd "$SCRIPT_DIR"
    rm -rf .git
    git init
    git add .
    git commit -m "Initial commit: ${APP_DISPLAY_NAME} (from kotlin-spring-template)"
    echo -e "  ${GREEN}✓${NC} Git repository initialized"
fi

# =============================================================================
# Completion Summary
# =============================================================================

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                    Setup Complete!                         ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo ""
echo "1. Start the database:"
echo -e "   ${BLUE}docker-compose up -d${NC}"
echo ""
echo "2. Run the backend:"
echo -e "   ${BLUE}cd api && ./gradlew bootRun${NC}"
echo ""
echo "3. Run the frontend (in another terminal):"
echo -e "   ${BLUE}cd web && npm install && npm run dev${NC}"
echo ""
echo -e "${RED}⚠️  SECURITY REMINDERS:${NC}"
echo "   • Change the default admin password (admin123) after first login"
echo "   • Set a secure JWT_SECRET in production (min 256 bits)"
echo "   • Update database credentials for production"
echo ""
echo -e "Package structure: ${GREEN}$NEW_PACKAGE${NC}"
echo -e "Application class: ${GREEN}${APP_CLASS_NAME}${NC}"
echo ""
