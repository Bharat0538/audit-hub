# AuditHub — Complete System Design

> **Version:** 1.0  
> **Stack:** Java 21 · Spring Boot 3.4.x · Kafka · Cassandra · PostgreSQL · Redis · React 18  
> **Target Market:** Indian Enterprise (Banking, Insurance, Healthcare, SaaS)  
> **Compliance:** DPDP Act 2023 · RBI IT Framework · SEBI Audit Trail Circular 2021  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Business Context & Revenue Model](#2-business-context--revenue-model)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Domain Model & Bounded Contexts](#4-domain-model--bounded-contexts)
5. [Database Schema — PostgreSQL (Control Plane)](#5-database-schema--postgresql-control-plane)
6. [Database Schema — Cassandra (Audit Data Plane)](#6-database-schema--cassandra-audit-data-plane)
7. [OpenAPI Specification](#7-openapi-specification)
8. [Backend Microservices Design](#8-backend-microservices-design)
9. [Kafka Topics & Event Schema](#9-kafka-topics--event-schema)
10. [Redis Caching Strategy](#10-redis-caching-strategy)
11. [Frontend React Application Design](#11-frontend-react-application-design)
12. [Security Architecture](#12-security-architecture)
13. [Deployment Architecture (AWS)](#13-deployment-architecture-aws)
14. [Compliance & Data Retention](#14-compliance--data-retention)
15. [SDK Design (Java Client)](#15-sdk-design-java-client)

---

## 1. Executive Summary

**AuditHub** is a multi-tenant, enterprise-grade Audit Trail Platform that captures, stores, searches, and reports on every change event across an organization's systems. It solves the compliance audit problem that every bank, insurance company, healthcare provider, and SaaS product faces — *who changed what, when, from where, and why*.

### Core Capabilities

| Capability | Description |
|---|---|
| Event Ingestion | REST API, Kafka consumer, Database CDC (Debezium) |
| Storage | Cassandra time-series with configurable retention |
| Search | By user, entity, date range, action type, tenant |
| Compliance Reports | PDF/CSV export, RBI/SEBI templates |
| Event Replay | Re-publish audit events to Kafka for downstream systems |
| Multi-Tenancy | Org isolation, RBAC (Admin / Auditor / Viewer) |
| Dashboard | Real-time trends, anomaly alerts, user activity heatmaps |
| SDK | Java, Spring Boot auto-configuration, annotation-driven |

### Why Cassandra for Audit Storage?
- Write-heavy workload (millions of events/day per large tenant)
- Time-series access pattern (query by entity + time range)
- Linear horizontal scale
- TTL-based automatic retention expiry
- No single point of failure — perfect for compliance SLAs

---

## 2. Business Context & Revenue Model

### Target Personas

| Persona | Pain Point | Willingness to Pay |
|---|---|---|
| BFSI CTO | RBI mandates 7-year audit trails | High (₹5L+/year) |
| SaaS Founder | Customers ask "who changed my data?" | Medium (₹999–₹4999/month) |
| Healthcare CIO | DPDP Act + patient data audit | High (₹2L+/year) |
| Government IT Head | RTI compliance, accountability | Very High (Enterprise) |

### Pricing Tiers

```
FREE TIER
├── 10,000 events/month
├── 7-day retention
├── REST API ingestion only
├── Basic search
└── 1 application

STARTER — ₹999/month
├── 1,000,000 events/month
├── 90-day retention
├── REST + Kafka ingestion
├── Full search
├── CSV export
└── 5 applications

PROFESSIONAL — ₹4,999/month
├── 10,000,000 events/month
├── 1-year retention
├── REST + Kafka + CDC ingestion
├── Full search + filters
├── CSV + PDF export
├── Event replay
├── 20 applications
└── Priority support

ENTERPRISE — ₹50,000–₹5,00,000/year
├── Unlimited events
├── Configurable retention (up to 10 years)
├── All ingestion modes
├── Compliance report templates (RBI, SEBI, DPDP)
├── Dedicated Kafka cluster
├── SAML/SSO
├── Self-hosted option
├── SLA 99.99%
└── Dedicated CSM
```

---

## 3. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT SYSTEMS                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ Java SDK     │  │ REST API     │  │ Kafka Topic  │  │ Debezium CDC  │  │
│  │ (annotation) │  │ (direct HTTP)│  │ (async push) │  │ (DB change)   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘  │
└─────────┼─────────────────┼─────────────────┼──────────────────┼───────────┘
          │                 │                 │                  │
          ▼                 ▼                 │                  │
┌─────────────────────────────────────────┐  │                  │
│          API GATEWAY (Kong/Spring)       │  │                  │
│  Rate Limiting · Auth · Tenant Routing  │  │                  │
└─────────────────┬───────────────────────┘  │                  │
                  │                           │                  │
    ┌─────────────▼─────────────┐            │                  │
    │    INGESTION SERVICE       │◄───────────┘◄─────────────────┘
    │  (Spring Boot 3.4.x)       │
    │  - Validate event schema   │
    │  - Enrich (IP, geoIP)      │
    │  - Tenant quota check      │
    │  - Publish to Kafka        │
    └─────────────┬──────────────┘
                  │
          ┌───────▼────────────────────────────────────┐
          │              KAFKA CLUSTER                  │
          │  audit.events.raw      (partitioned by org) │
          │  audit.events.enriched (partitioned by org) │
          │  audit.dlq             (dead letter queue)  │
          │  audit.replay          (replay channel)     │
          └───────┬────────────────────────────────────┘
                  │
    ┌─────────────▼─────────────┐    ┌─────────────────────────┐
    │    STORAGE SERVICE         │    │    QUERY SERVICE          │
    │  (Spring Boot 3.4.x)       │    │  (Spring Boot 3.4.x)     │
    │  - Consume enriched events │    │  - Search audit events   │
    │  - Write to Cassandra      │    │  - Filter / paginate     │
    │  - Apply retention TTL     │    │  - Read from Cassandra   │
    │  - Update stats in Redis   │    │  - Redis cache L1        │
    └───────────────────────────┘    └─────────────────────────┘
                                               │
    ┌──────────────────────────────────────────▼────────────────┐
    │                    REPORT SERVICE                          │
    │  - Generate CSV / PDF compliance reports                   │
    │  - Schedule periodic reports (cron)                        │
    │  - RBI Audit Trail Report template                         │
    │  - Store reports in S3 / generate presigned URLs           │
    └────────────────────────────────────────────────────────────┘

    ┌──────────────────────────────────────────────────────────┐
    │                  CONTROL PLANE SERVICES                   │
    │  ┌────────────────┐  ┌────────────────┐  ┌───────────┐  │
    │  │ Auth Service   │  │ Tenant Service │  │ Billing   │  │
    │  │ JWT/SAML/OIDC  │  │ Org/App/User   │  │ Razorpay  │  │
    │  └────────────────┘  └────────────────┘  └───────────┘  │
    └──────────────────────────────────────────────────────────┘

    ┌────────────────────────────────────────────────────────┐
    │                    DATA STORES                          │
    │  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐  │
    │  │ Cassandra   │  │ PostgreSQL  │  │ Redis Cluster │  │
    │  │ (audit data)│  │ (control)   │  │ (cache/quota) │  │
    │  └─────────────┘  └─────────────┘  └───────────────┘  │
    └────────────────────────────────────────────────────────┘

    ┌────────────────────────────────────────────────────────┐
    │                   REACT FRONTEND                        │
    │  Dashboard · Search · Reports · Settings · Admin        │
    └────────────────────────────────────────────────────────┘
```

### Service Port Map

| Service | Port | Description |
|---|---|---|
| API Gateway | 8080 | Single entry point |
| Ingestion Service | 8081 | Event intake |
| Query Service | 8082 | Search & retrieval |
| Storage Service | 8083 | Internal only |
| Report Service | 8084 | Report generation |
| Auth Service | 8085 | JWT/SAML |
| Tenant Service | 8086 | Org management |
| Billing Service | 8087 | Subscription/usage |

---

## 4. Domain Model & Bounded Contexts

### Audit Event — Core Domain Object

```
AuditEvent
├── eventId          UUID (globally unique)
├── organizationId   UUID (tenant)
├── applicationId    UUID (source app)
├── eventTime        Instant (UTC)
├── actor            Actor
│   ├── userId       String
│   ├── userEmail    String
│   ├── userName     String
│   ├── ipAddress    String
│   ├── userAgent    String
│   └── sessionId    String
├── action           Action
│   ├── type         Enum (CREATE/UPDATE/DELETE/READ/LOGIN/LOGOUT/EXPORT/APPROVE/REJECT/CUSTOM)
│   ├── name         String ("user.password.changed")
│   └── description  String (human-readable)
├── resource         Resource
│   ├── type         String ("User", "Account", "Transaction", "Policy")
│   ├── id           String (entity ID)
│   ├── name         String (entity display name)
│   └── path         String ("/api/v1/users/123")
├── changes          List<FieldChange>
│   ├── fieldName    String
│   ├── oldValue     JsonNode (nullable)
│   └── newValue     JsonNode (nullable)
├── outcome          Enum (SUCCESS / FAILURE / PARTIAL)
├── severity         Enum (LOW / MEDIUM / HIGH / CRITICAL)
├── metadata         Map<String, String> (custom key-value)
├── correlationId    String (request tracing)
└── tags             List<String> (searchable labels)
```

### Bounded Contexts

```
┌─────────────────────────────────────────────────────┐
│  AUDIT CONTEXT                                       │
│  AuditEvent, FieldChange, Actor, Resource, Action   │
│  — Core domain, immutable after write               │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  TENANT CONTEXT                                      │
│  Organization, Application, ApiKey, RetentionPolicy │
│  — Control plane, mutable                           │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  IAM CONTEXT                                         │
│  User, Role, Permission, Session                    │
│  — Auth and RBAC                                    │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  BILLING CONTEXT                                     │
│  Subscription, UsageQuota, Invoice, Plan            │
│  — Revenue tracking                                 │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  REPORT CONTEXT                                      │
│  ReportTemplate, GeneratedReport, Schedule          │
│  — Compliance exports                               │
└─────────────────────────────────────────────────────┘
```

---

## 5. Database Schema — PostgreSQL (Control Plane)

```sql
-- ─────────────────────────────────────────────────
-- TENANT CONTEXT
-- ─────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE organizations (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                VARCHAR(255) NOT NULL,
    slug                VARCHAR(100) NOT NULL UNIQUE,  -- used in API URLs
    display_name        VARCHAR(255) NOT NULL,
    plan                VARCHAR(50) NOT NULL DEFAULT 'FREE',  -- FREE/STARTER/PROFESSIONAL/ENTERPRISE
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/SUSPENDED/DELETED
    country             VARCHAR(10) NOT NULL DEFAULT 'IN',
    gst_number          VARCHAR(20),
    pan_number          VARCHAR(20),
    contact_email       VARCHAR(255) NOT NULL,
    contact_phone       VARCHAR(20),
    max_events_per_month BIGINT NOT NULL DEFAULT 10000,
    max_applications    INT NOT NULL DEFAULT 1,
    retention_days      INT NOT NULL DEFAULT 7,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_plan ON organizations(plan);
CREATE INDEX idx_organizations_status ON organizations(status);

-- ─────────────────────────────────────────────────

CREATE TABLE applications (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    slug                VARCHAR(100) NOT NULL,
    description         TEXT,
    environment         VARCHAR(50) NOT NULL DEFAULT 'PRODUCTION',  -- PRODUCTION/STAGING/DEVELOPMENT
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    webhook_url         VARCHAR(500),  -- alert callbacks
    webhook_secret      VARCHAR(255),
    created_by          UUID NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, slug)
);

CREATE INDEX idx_applications_org_id ON applications(organization_id);
CREATE INDEX idx_applications_status ON applications(status);

-- ─────────────────────────────────────────────────

CREATE TABLE api_keys (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    application_id      UUID REFERENCES applications(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    key_prefix          VARCHAR(10) NOT NULL,   -- first 8 chars visible: "ah_live_"
    key_hash            VARCHAR(255) NOT NULL,  -- bcrypt hash of full key
    key_type            VARCHAR(50) NOT NULL DEFAULT 'WRITE',  -- WRITE / READ / ADMIN
    scopes              TEXT[],                 -- ["audit:write", "audit:read", "reports:read"]
    last_used_at        TIMESTAMP WITH TIME ZONE,
    expires_at          TIMESTAMP WITH TIME ZONE,
    created_by          UUID NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_org_id ON api_keys(organization_id);
CREATE INDEX idx_api_keys_key_prefix ON api_keys(key_prefix);

-- ─────────────────────────────────────────────────
-- IAM CONTEXT
-- ─────────────────────────────────────────────────

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email               VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255),  -- null for SSO users
    avatar_url          VARCHAR(500),
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/INVITED/SUSPENDED
    auth_provider       VARCHAR(50) NOT NULL DEFAULT 'LOCAL',   -- LOCAL/GOOGLE/SAML
    auth_provider_id    VARCHAR(255),  -- external SSO user id
    mfa_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret          VARCHAR(255),  -- encrypted TOTP secret
    last_login_at       TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP WITH TIME ZONE,
    UNIQUE(organization_id, email)
);

CREATE INDEX idx_users_org_id ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- ─────────────────────────────────────────────────

CREATE TYPE role_name AS ENUM ('OWNER', 'ADMIN', 'AUDITOR', 'VIEWER', 'DEVELOPER');

CREATE TABLE user_roles (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role                role_name NOT NULL,
    application_id      UUID REFERENCES applications(id) ON DELETE CASCADE,  -- null = org-wide
    granted_by          UUID REFERENCES users(id),
    granted_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, organization_id, role, application_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_org_id ON user_roles(organization_id);

-- ─────────────────────────────────────────────────

CREATE TABLE sessions (
    id                  VARCHAR(128) PRIMARY KEY,  -- JWT jti
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id     UUID NOT NULL,
    ip_address          VARCHAR(45),
    user_agent          TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at          TIMESTAMP WITH TIME ZONE,
    revoke_reason       VARCHAR(255)
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at);

-- ─────────────────────────────────────────────────
-- BILLING CONTEXT
-- ─────────────────────────────────────────────────

CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL UNIQUE REFERENCES organizations(id),
    plan                VARCHAR(50) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/PAST_DUE/CANCELLED/TRIALING
    razorpay_customer_id    VARCHAR(100),
    razorpay_subscription_id VARCHAR(100),
    current_period_start    TIMESTAMP WITH TIME ZONE,
    current_period_end      TIMESTAMP WITH TIME ZONE,
    trial_end           TIMESTAMP WITH TIME ZONE,
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_razorpay ON subscriptions(razorpay_subscription_id);

-- ─────────────────────────────────────────────────

CREATE TABLE usage_snapshots (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    application_id      UUID REFERENCES applications(id),
    period_start        TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end          TIMESTAMP WITH TIME ZONE NOT NULL,
    event_count         BIGINT NOT NULL DEFAULT 0,
    storage_bytes       BIGINT NOT NULL DEFAULT 0,
    api_calls           BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usage_snapshots_org_period ON usage_snapshots(organization_id, period_start);

-- ─────────────────────────────────────────────────
-- REPORT CONTEXT
-- ─────────────────────────────────────────────────

CREATE TABLE report_templates (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID REFERENCES organizations(id),  -- null = system template
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    template_type       VARCHAR(100) NOT NULL,  -- RBI_AUDIT_TRAIL/SEBI_COMPLIANCE/DPDP_DATA_ACCESS/CUSTOM
    format              VARCHAR(20) NOT NULL,   -- PDF/CSV/XLSX
    config              JSONB NOT NULL DEFAULT '{}',  -- filters, columns, grouping
    is_system           BOOLEAN NOT NULL DEFAULT FALSE,
    created_by          UUID,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────

CREATE TABLE generated_reports (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    template_id         UUID REFERENCES report_templates(id),
    name                VARCHAR(500) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- PENDING/PROCESSING/COMPLETED/FAILED
    filters             JSONB NOT NULL DEFAULT '{}',
    format              VARCHAR(20) NOT NULL,
    s3_key              VARCHAR(1000),
    presigned_url       VARCHAR(2000),
    presigned_url_expires_at TIMESTAMP WITH TIME ZONE,
    error_message       TEXT,
    row_count           BIGINT,
    file_size_bytes     BIGINT,
    requested_by        UUID NOT NULL REFERENCES users(id),
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_generated_reports_org ON generated_reports(organization_id, created_at DESC);
CREATE INDEX idx_generated_reports_status ON generated_reports(status);

-- ─────────────────────────────────────────────────

CREATE TABLE report_schedules (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    template_id         UUID NOT NULL REFERENCES report_templates(id),
    name                VARCHAR(255) NOT NULL,
    cron_expression     VARCHAR(100) NOT NULL,  -- "0 0 1 * *" = 1st of month
    timezone            VARCHAR(100) NOT NULL DEFAULT 'Asia/Kolkata',
    recipients          TEXT[],                 -- email addresses
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at         TIMESTAMP WITH TIME ZONE,
    next_run_at         TIMESTAMP WITH TIME ZONE,
    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────
-- ALERT / NOTIFICATION CONFIG
-- ─────────────────────────────────────────────────

CREATE TABLE alert_rules (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    application_id      UUID REFERENCES applications(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    condition_type      VARCHAR(100) NOT NULL,  -- THRESHOLD/PATTERN/ANOMALY
    condition_config    JSONB NOT NULL,
    -- e.g. {"action_type": "DELETE", "resource_type": "Account", "threshold": 10, "window_minutes": 5}
    severity            VARCHAR(50) NOT NULL DEFAULT 'HIGH',
    notification_channels TEXT[],              -- ["EMAIL", "WEBHOOK", "SLACK"]
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

---

## 6. Database Schema — Cassandra (Audit Data Plane)

```cql
-- ─────────────────────────────────────────────────────────
-- KEYSPACE
-- ─────────────────────────────────────────────────────────
-- NetworkTopologyStrategy for multi-region enterprise deployments

CREATE KEYSPACE audithub
    WITH replication = {
        'class': 'NetworkTopologyStrategy',
        'ap-south-1': 3
    }
    AND durable_writes = true;

USE audithub;

-- ─────────────────────────────────────────────────────────
-- PRIMARY AUDIT EVENT TABLE
-- Partition key: (organization_id, application_id, month_bucket)
-- Clustering: event_time DESC (most recent first), event_id
-- Supports: "show me all events for this org/app in a time range"
-- ─────────────────────────────────────────────────────────

CREATE TABLE audit_events (
    organization_id     UUID,
    application_id      UUID,
    month_bucket        TEXT,            -- '2025-06' — limits partition size
    event_time          TIMESTAMP,
    event_id            UUID,
    actor_user_id       TEXT,
    actor_user_email    TEXT,
    actor_user_name     TEXT,
    actor_ip_address    TEXT,
    actor_user_agent    TEXT,
    actor_session_id    TEXT,
    action_type         TEXT,            -- CREATE/UPDATE/DELETE/READ etc.
    action_name         TEXT,            -- 'user.password.changed'
    action_description  TEXT,
    resource_type       TEXT,            -- 'User','Account','Transaction'
    resource_id         TEXT,
    resource_name       TEXT,
    resource_path       TEXT,
    changes             TEXT,            -- JSON serialized List<FieldChange>
    outcome             TEXT,            -- SUCCESS/FAILURE/PARTIAL
    severity            TEXT,            -- LOW/MEDIUM/HIGH/CRITICAL
    metadata            MAP<TEXT, TEXT>,
    correlation_id      TEXT,
    tags                LIST<TEXT>,
    raw_payload         TEXT,            -- original event JSON (for replay)
    PRIMARY KEY (
        (organization_id, application_id, month_bucket),
        event_time,
        event_id
    )
) WITH CLUSTERING ORDER BY (event_time DESC, event_id ASC)
  AND default_time_to_live = 7776000     -- 90 days default; overridden per tenant via TTL on write
  AND compaction = {
      'class': 'TimeWindowCompactionStrategy',
      'compaction_window_unit': 'DAYS',
      'compaction_window_size': 1
  }
  AND compression = {
      'class': 'LZ4Compressor'
  }
  AND gc_grace_seconds = 86400;

-- ─────────────────────────────────────────────────────────
-- BY USER TABLE
-- "Show me everything this user did across all apps"
-- Partition key: (organization_id, actor_user_id, month_bucket)
-- ─────────────────────────────────────────────────────────

CREATE TABLE audit_events_by_user (
    organization_id     UUID,
    actor_user_id       TEXT,
    month_bucket        TEXT,
    event_time          TIMESTAMP,
    event_id            UUID,
    application_id      UUID,
    action_type         TEXT,
    action_name         TEXT,
    resource_type       TEXT,
    resource_id         TEXT,
    resource_name       TEXT,
    outcome             TEXT,
    severity            TEXT,
    actor_ip_address    TEXT,
    PRIMARY KEY (
        (organization_id, actor_user_id, month_bucket),
        event_time,
        event_id
    )
) WITH CLUSTERING ORDER BY (event_time DESC, event_id ASC)
  AND default_time_to_live = 7776000
  AND compaction = {
      'class': 'TimeWindowCompactionStrategy',
      'compaction_window_unit': 'DAYS',
      'compaction_window_size': 1
  };

-- ─────────────────────────────────────────────────────────
-- BY RESOURCE TABLE
-- "Show me all changes to Account #HDFC-ACC-001"
-- Partition key: (organization_id, resource_type, resource_id)
-- ─────────────────────────────────────────────────────────

CREATE TABLE audit_events_by_resource (
    organization_id     UUID,
    resource_type       TEXT,
    resource_id         TEXT,
    event_time          TIMESTAMP,
    event_id            UUID,
    application_id      UUID,
    actor_user_id       TEXT,
    actor_user_email    TEXT,
    action_type         TEXT,
    action_name         TEXT,
    changes             TEXT,
    outcome             TEXT,
    severity            TEXT,
    PRIMARY KEY (
        (organization_id, resource_type, resource_id),
        event_time,
        event_id
    )
) WITH CLUSTERING ORDER BY (event_time DESC, event_id ASC)
  AND default_time_to_live = 7776000
  AND compaction = {
      'class': 'TimeWindowCompactionStrategy',
      'compaction_window_unit': 'DAYS',
      'compaction_window_size': 1
  };

-- ─────────────────────────────────────────────────────────
-- BY SEVERITY TABLE
-- "Show me all CRITICAL events this month" — for alert dashboard
-- ─────────────────────────────────────────────────────────

CREATE TABLE audit_events_by_severity (
    organization_id     UUID,
    severity            TEXT,
    month_bucket        TEXT,
    event_time          TIMESTAMP,
    event_id            UUID,
    application_id      UUID,
    actor_user_id       TEXT,
    action_type         TEXT,
    action_name         TEXT,
    resource_type       TEXT,
    resource_id         TEXT,
    outcome             TEXT,
    PRIMARY KEY (
        (organization_id, severity, month_bucket),
        event_time,
        event_id
    )
) WITH CLUSTERING ORDER BY (event_time DESC, event_id ASC)
  AND default_time_to_live = 7776000;

-- ─────────────────────────────────────────────────────────
-- DAILY STATS COUNTER TABLE
-- Pre-aggregated counts for dashboard widgets
-- Updated by Storage Service via counter increments
-- ─────────────────────────────────────────────────────────

CREATE TABLE daily_stats (
    organization_id     UUID,
    application_id      UUID,
    stat_date           DATE,
    action_type         TEXT,
    total_events        COUNTER,
    success_count       COUNTER,
    failure_count       COUNTER,
    critical_count      COUNTER,
    unique_users        COUNTER,         -- approximate (not exact)
    PRIMARY KEY (
        (organization_id, application_id),
        stat_date,
        action_type
    )
) WITH CLUSTERING ORDER BY (stat_date DESC, action_type ASC);

-- ─────────────────────────────────────────────────────────
-- ENTITY TIMELINE MATERIALIZED VIEW HELPER
-- Used to build entity history timeline in UI
-- ─────────────────────────────────────────────────────────

CREATE TABLE entity_timeline (
    organization_id     UUID,
    resource_type       TEXT,
    resource_id         TEXT,
    event_time          TIMESTAMP,
    event_id            UUID,
    actor_user_id       TEXT,
    actor_user_name     TEXT,
    action_type         TEXT,
    action_name         TEXT,
    action_description  TEXT,
    changes             TEXT,
    outcome             TEXT,
    severity            TEXT,
    PRIMARY KEY (
        (organization_id, resource_type, resource_id),
        event_time,
        event_id
    )
) WITH CLUSTERING ORDER BY (event_time DESC, event_id ASC)
  AND default_time_to_live = 7776000;
```
