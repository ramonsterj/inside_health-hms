# HMS Production Deployment Plan

**Platform**: Uncloud (Docker Compose + WireGuard + Caddy)
**Target**: Per-client isolated deployments on Linux VPS
**Last Updated**: 2026-03-13

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Phase 1: Create Deployment Artifacts](#phase-1-create-deployment-artifacts)
4. [Phase 2: Server Provisioning](#phase-2-server-provisioning)
5. [Phase 3: Deploy with Uncloud](#phase-3-deploy-with-uncloud)
6. [Phase 4: Post-Deployment Verification](#phase-4-post-deployment-verification)
7. [Phase 5: CI/CD Setup](#phase-5-cicd-setup)
8. [Backup and Recovery](#backup-and-recovery)
9. [Scaling](#scaling)
10. [Monitoring](#monitoring)
11. [Security Hardening](#security-hardening)
12. [Deployment Checklist](#deployment-checklist)

---

## Overview

Each HMS client gets an **isolated deployment** consisting of:

- Separate Uncloud cluster (or separate compose stack)
- Dedicated PostgreSQL database
- Isolated application containers (API + frontend + database)
- Automatic HTTPS via Caddy + Let's Encrypt
- Free `*.uncld.dev` subdomain or custom domain

This model provides complete data isolation, independent scaling, and zero multi-tenancy complexity.

### Architecture Diagram

```
                    Internet
                       |
              [ Caddy Reverse Proxy ]
              (auto HTTPS/TLS certs)
                   /         \
         app.domain.com   api.domain.com
              |                  |
     [ Frontend (nginx) ]  [ Backend API ]  x2 replicas
         static SPA         Spring Boot 4
                                 |
                          [ PostgreSQL 17 ]
                          (persistent volume)
```

All containers communicate over a **WireGuard mesh network** — encrypted and private.

---

## Prerequisites

### Local Machine

| Requirement | Version | Purpose |
|---|---|---|
| Uncloud CLI (`uc`) | Latest | Deployment tool |
| Docker | 24+ | Build images locally (optional) |
| SSH key pair | - | Server access |

### Server (per client)

| Requirement | Minimum | Recommended |
|---|---|---|
| OS | Ubuntu 22.04 / Debian 11 | Ubuntu 24.04 LTS |
| RAM | 2 GB | 4 GB |
| CPU | 2 vCPU | 4 vCPU |
| Disk | 20 GB SSD | 50 GB SSD |
| Firewall ports | 22, 80, 443 | + 51820 (WireGuard) |

### Artifacts to Create (not yet in repo)

- [ ] `api/Dockerfile`
- [ ] `web/Dockerfile`
- [ ] `compose.prod.yml` (production Docker Compose with Uncloud extensions)
- [ ] `.env.prod` (production environment variables)

---

## Phase 1: Create Deployment Artifacts

### 1.1 API Dockerfile (`api/Dockerfile`)

Multi-stage build: Gradle build → JRE 21 runtime.

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

RUN groupadd -r hms && useradd -r -g hms hms
RUN mkdir -p /var/data/hms/files && chown -R hms:hms /var/data/hms
USER hms

EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Key decisions**:
- Non-root user (`hms`) for security
- Dependencies cached in separate layer for faster rebuilds
- Tests skipped in Docker build (run in CI before build)
- File storage directory pre-created at `/var/data/hms/files`

### 1.2 Frontend Dockerfile (`web/Dockerfile`)

Multi-stage build: Node build → nginx static server.

```dockerfile
# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# Stage 2: Serve
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

Requires a `web/nginx.conf` for SPA routing:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

**Key decisions**:
- `VITE_API_URL` injected at build time (baked into the SPA bundle)
- SPA fallback to `index.html` for client-side routing
- Static assets cached aggressively

### 1.3 Production Compose File (`compose.prod.yml`)

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: ${DATABASE_NAME}
      POSTGRES_USER: ${DATABASE_USERNAME}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    deploy:
      replicas: 1
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DATABASE_USERNAME} -d ${DATABASE_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./api
      dockerfile: Dockerfile
    environment:
      DATABASE_HOST: postgres
      DATABASE_PORT: 5432
      DATABASE_NAME: ${DATABASE_NAME}
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SERVER_PORT: 8081
      FILE_STORAGE_PATH: /var/data/hms/files
    volumes:
      - file_storage:/var/data/hms/files
    depends_on:
      postgres:
        condition: service_healthy
    x-ports:
      - api.${DOMAIN}:8081/https
    deploy:
      replicas: 2
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  frontend:
    build:
      context: ./web
      dockerfile: Dockerfile
      args:
        VITE_API_URL: https://api.${DOMAIN}
    depends_on:
      - backend
    x-ports:
      - ${DOMAIN}:80/https
    deploy:
      replicas: 2

volumes:
  postgres_data:
  file_storage:
```

**Uncloud-specific extensions**:
- `x-ports` — exposes services through Caddy with automatic HTTPS
- `deploy.replicas` — load balanced across cluster machines
- No external port mappings needed (Caddy handles routing)

### 1.4 Environment Variables (`.env.prod`)

```bash
# Database
DATABASE_NAME=hms_prod
DATABASE_USERNAME=hms_admin
DATABASE_PASSWORD=<generate-with: openssl rand -base64 32>

# Authentication
JWT_SECRET=<generate-with: openssl rand -base64 64>

# Domain
DOMAIN=clientname.uncld.dev
# Or custom: DOMAIN=hms.clientname.com
```

**Generate secure values**:
```bash
# Database password
openssl rand -base64 32

# JWT secret (minimum 256 bits)
openssl rand -base64 64
```

> `.env.prod` must NEVER be committed to git. Add it to `.gitignore`.

---

## Phase 2: Server Provisioning

### 2.1 Provision a VPS

Recommended providers: **Hetzner** (cost-effective), **DigitalOcean**, **AWS Lightsail**, **Vultr**.

For a single-client HMS deployment:
- **Hetzner CX22**: 2 vCPU, 4 GB RAM, 40 GB SSD (~$4.50/mo)
- **DigitalOcean Basic**: 2 vCPU, 4 GB RAM, 80 GB SSD (~$24/mo)

### 2.2 Configure SSH Access

```bash
# Copy your SSH public key to the server
ssh-copy-id root@your-server-ip

# Verify key-based access works
ssh root@your-server-ip
```

### 2.3 Configure Firewall

```bash
# On the server
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP (redirect to HTTPS)
ufw allow 443/tcp   # HTTPS
ufw allow 51820/udp # WireGuard (for multi-machine clusters)
ufw enable
```

---

## Phase 3: Deploy with Uncloud

### 3.1 Install Uncloud CLI

```bash
# macOS/Linux via Homebrew
brew install psviderski/tap/uncloud

# Or via install script
curl -fsS https://get.uncloud.run/install.sh | sh
```

### 3.2 Initialize the Cluster

```bash
# Initialize your server (installs Docker, WireGuard, Caddy)
uc machine init root@your-server-ip

# This will:
#   - Install Docker on the server
#   - Install the Uncloud daemon
#   - Set up WireGuard mesh network
#   - Deploy Caddy reverse proxy
#   - Reserve a free *.uncld.dev subdomain
```

### 3.3 Deploy the Application

```bash
# From the project root directory
uc deploy -f compose.prod.yml --env-file .env.prod

# This will:
#   - Build Docker images on the cluster machine
#   - Start all containers with zero-downtime rolling updates
#   - Configure Caddy for automatic HTTPS
#   - Update DNS (if using *.uncld.dev)
```

### 3.4 Verify Deployment

```bash
# List all services and their public URLs
uc ls

# Check service status
uc ps backend
uc ps frontend
uc ps postgres

# Watch logs
uc logs backend -f
uc logs frontend -f
```

### 3.5 Run Flyway Migrations

Flyway runs automatically on Spring Boot startup (`spring.flyway.enabled=true`), so migrations execute when the backend container starts. No manual step needed.

Verify migrations ran:
```bash
uc logs backend | grep -i flyway
```

---

## Phase 4: Post-Deployment Verification

### 4.1 Functional Checks

```bash
# Health endpoint
curl https://api.${DOMAIN}/actuator/health

# Login with default admin (then CHANGE THE PASSWORD)
curl -X POST https://api.${DOMAIN}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# Verify frontend loads
curl -I https://${DOMAIN}
```

### 4.2 Security Checks

- [ ] HTTPS active with valid certificate (check browser padlock)
- [ ] HTTP redirects to HTTPS
- [ ] Swagger UI disabled (`/swagger-ui.html` returns 404)
- [ ] Actuator only exposes `/actuator/health`
- [ ] Default admin password changed
- [ ] Database not accessible from outside the cluster

### 4.3 Performance Baseline

- [ ] Frontend loads in < 3 seconds
- [ ] API health endpoint responds in < 200ms
- [ ] Login flow completes in < 1 second

---

## Phase 5: CI/CD Setup (Optional)

### GitHub Actions Workflow

Create `.github/workflows/deploy.yml`:

```yaml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run backend tests
        run: cd api && ./gradlew test

      - name: Set up Node.js 22
        uses: actions/setup-node@v4
        with:
          node-version: '22'

      - name: Run frontend tests
        run: cd web && npm ci && npm run test:run

  deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Install Uncloud CLI
        run: curl -fsS https://get.uncloud.run/install.sh | sh

      - name: Configure SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ secrets.SERVER_IP }} >> ~/.ssh/known_hosts

      - name: Initialize Uncloud context
        run: uc machine init ${{ secrets.SSH_USER }}@${{ secrets.SERVER_IP }}

      - name: Deploy
        run: |
          cat > .env.prod << EOF
          DATABASE_NAME=${{ secrets.DATABASE_NAME }}
          DATABASE_USERNAME=${{ secrets.DATABASE_USERNAME }}
          DATABASE_PASSWORD=${{ secrets.DATABASE_PASSWORD }}
          JWT_SECRET=${{ secrets.JWT_SECRET }}
          DOMAIN=${{ secrets.DOMAIN }}
          EOF
          uc deploy -f compose.prod.yml --env-file .env.prod
```

### Required GitHub Secrets

| Secret | Description |
|---|---|
| `SSH_PRIVATE_KEY` | Private key for server SSH access |
| `SSH_USER` | SSH username (e.g., `root`) |
| `SERVER_IP` | Server IP address |
| `DATABASE_NAME` | Production database name |
| `DATABASE_USERNAME` | Production database user |
| `DATABASE_PASSWORD` | Production database password |
| `JWT_SECRET` | JWT signing secret (min 256 bits) |
| `DOMAIN` | Production domain (e.g., `hms.uncld.dev`) |

---

## Backup and Recovery

### Automated Database Backups

Set up a cron job on the server:

```bash
# SSH into the server
ssh root@your-server-ip

# Create backup directory
mkdir -p /backups/hms

# Add cron job (daily at 2 AM)
crontab -e
```

```cron
0 2 * * * docker exec $(docker ps -qf name=postgres) pg_dump -U hms_admin hms_prod | gzip > /backups/hms/db-$(date +\%Y\%m\%d-\%H\%M).sql.gz
0 3 * * * find /backups/hms -name "*.sql.gz" -mtime +30 -delete
```

### Manual Backup

```bash
uc exec postgres pg_dump -U hms_admin hms_prod > backup-$(date +%Y%m%d).sql
```

### Restore from Backup

```bash
# Stop backend to prevent writes
uc scale backend=0

# Restore
gunzip -c backup-20260313.sql.gz | uc exec -i postgres psql -U hms_admin hms_prod

# Restart backend
uc scale backend=2
```

### File Storage Backup

Patient documents are stored in the `file_storage` Docker volume. Back up periodically:

```bash
ssh root@your-server-ip
docker run --rm -v file_storage:/data -v /backups/hms:/backup alpine \
  tar czf /backup/files-$(date +%Y%m%d).tar.gz -C /data .
```

---

## Scaling

### Vertical (single machine)

Upgrade the VPS to more CPU/RAM. No application changes needed.

### Horizontal (multiple machines)

```bash
# Add a second machine to the cluster
uc machine init root@second-server-ip

# Increase replicas (they auto-distribute across machines)
# Edit compose.prod.yml:
#   backend: deploy.replicas: 4
#   frontend: deploy.replicas: 4

uc deploy -f compose.prod.yml --env-file .env.prod
```

### Database Scaling

For larger deployments, switch to managed PostgreSQL:

1. Provision managed PostgreSQL (e.g., DigitalOcean Managed DB, AWS RDS)
2. Update `.env.prod` with the managed DB connection details
3. Remove the `postgres` service from `compose.prod.yml`
4. Redeploy

---

## Monitoring

### Built-in Commands

```bash
uc ls              # Service overview
uc ps backend      # Service details
uc logs backend -f # Stream logs
uc exec backend top # Resource usage
```

### Recommended External Tools

| Tool | Purpose | Free Tier |
|---|---|---|
| UptimeRobot | Uptime monitoring | 50 monitors |
| Sentry | Error tracking | 5K events/mo |
| Papertrail | Log aggregation | 50 MB/mo |

### Health Check Endpoints

| Endpoint | Expected Response |
|---|---|
| `GET /actuator/health` | `{"status": "UP"}` |
| `GET /` (frontend) | 200 OK with HTML |

---

## Security Hardening

### Before Go-Live

1. **Change default admin password** — `admin123` must be replaced immediately
2. **Generate strong secrets** — use `openssl rand -base64 64` for JWT_SECRET
3. **Disable SSH password auth** — use key-based only
4. **Enable automatic security updates** on the server:
   ```bash
   apt install unattended-upgrades
   dpkg-reconfigure -plow unattended-upgrades
   ```
5. **CORS configuration** — update allowed origins for the production domain
6. **Review firewall rules** — only ports 22, 80, 443 should be open

### Ongoing

- Keep Docker images updated (rebuild and redeploy monthly)
- Run `./gradlew dependencyCheckAnalyze` before each release
- Monitor Uncloud and Caddy for security patches
- Rotate JWT_SECRET periodically (requires all users to re-login)

---

## Deployment Checklist

### Pre-Deployment

- [ ] All backend tests passing (`./gradlew test`)
- [ ] All frontend tests passing (`npm run test:run`)
- [ ] `api/Dockerfile` created and builds successfully
- [ ] `web/Dockerfile` + `web/nginx.conf` created and builds successfully
- [ ] `compose.prod.yml` created with Uncloud extensions
- [ ] `.env.prod` created with secure values (NOT committed to git)
- [ ] `.env.prod` added to `.gitignore`
- [ ] VPS provisioned with SSH key access
- [ ] Firewall configured (ports 22, 80, 443)
- [ ] Uncloud CLI installed locally
- [ ] Domain DNS configured (if using custom domain)

### Deployment

- [ ] `uc machine init` completed successfully
- [ ] `uc deploy` completed without errors
- [ ] All services show as running (`uc ls`)
- [ ] HTTPS certificates active
- [ ] Frontend loads correctly
- [ ] API health check returns UP
- [ ] Login works with admin credentials
- [ ] Default admin password changed

### Post-Deployment

- [ ] Database backup cron job configured
- [ ] File storage backup configured
- [ ] Uptime monitoring configured
- [ ] Error tracking configured (Sentry recommended)
- [ ] CI/CD pipeline configured (if using GitHub Actions)
- [ ] Security hardening steps completed
- [ ] Smoke test all major features:
  - [ ] Patient registration
  - [ ] Admissions
  - [ ] Medical records
  - [ ] Nursing notes & vital signs
  - [ ] Inventory management
  - [ ] Billing & invoicing
