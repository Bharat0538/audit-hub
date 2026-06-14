# AuditHub — Functional & Technical Flow

> **Architecture:** Modular Monolith (microservice-ready package structure)  
> **Stack:** Java 21 · Spring Boot 3.4.x · Kafka · Cassandra · PostgreSQL · Redis · React 18  
> **Reading order:** Top to bottom — each section builds on the previous.

---

## Table of Contents

1. [Monolith Module Structure](#1-monolith-module-structure)
2. [Flow 1 — Organization Onboarding (Sign Up)](#2-flow-1--organization-onboarding-sign-up)
3. [Flow 2 — Application Registration & API Key Generation](#3-flow-2--application-registration--api-key-generation)
4. [Flow 3 — Audit Event Ingestion via REST](#4-flow-3--audit-event-ingestion-via-rest)
5. [Flow 4 — Audit Event Ingestion via Kafka (Client Push)](#5-flow-4--audit-event-ingestion-via-kafka-client-push)
6. [Flow 5 — Audit Event Ingestion via Debezium CDC](#6-flow-5--audit-event-ingestion-via-debezium-cdc)
7. [Flow 6 — Audit Event Storage to Cassandra](#7-flow-6--audit-event-storage-to-cassandra)
8. [Flow 7 — Audit Event Search & Query](#8-flow-7--audit-event-search--query)
9. [Flow 8 — Entity History (Full Lifecycle View)](#9-flow-8--entity-history-full-lifecycle-view)
10. [Flow 9 — User Activity Trail](#10-flow-9--user-activity-trail)
11. [Flow 10 — Dashboard Stats](#11-flow-10--dashboard-stats)
12. [Flow 11 — Report Generation (PDF/CSV)](#12-flow-11--report-generation-pdfcsv)
13. [Flow 12 — Scheduled Report Delivery](#13-flow-12--scheduled-report-delivery)
14. [Flow 13 — Event Replay to Kafka](#14-flow-13--event-replay-to-kafka)
15. [Flow 14 — Alert Rule Evaluation](#15-flow-14--alert-rule-evaluation)
16. [Flow 15 — User Authentication (JWT)](#16-flow-15--user-authentication-jwt)
17. [Flow 16 — SAML SSO Authentication](#17-flow-16--saml-sso-authentication)
18. [Flow 17 — Role-Based Access Control (RBAC) Enforcement](#18-flow-17--role-based-access-control-rbac-enforcement)
19. [Flow 18 — Quota Enforcement & Billing Events](#19-flow-18--quota-enforcement--billing-events)
20. [Flow 19 — Data Retention & TTL Expiry](#20-flow-19--data-retention--ttl-expiry)
21. [Flow 20 — Java SDK Integration (Client App)](#21-flow-20--java-sdk-integration-client-app)
22. [Complete Request Lifecycle (End-to-End)](#22-complete-request-lifecycle-end-to-end)
23. [Monolith to Microservices Migration Path](#23-monolith-to-microservices-migration-path)

---

## 1. Monolith Module Structure

The key architectural decision: **one deployable JAR, package-segregated by domain**. Every domain package can become a Spring Boot service later by extracting it with its own `application.yml`. Internal calls use direct Spring beans — no HTTP between them yet. When you split, you replace bean injection with Feign clients.

```
audithub/
├── pom.xml
└── src/main/java/in/audithub/
    │
    ├── AuditHubApplication.java          ← Single Spring Boot main class
    │
    ├── common/                            ← Shared across all modules
    │   ├── config/
    │   │   ├── CassandraConfig.java
    │   │   ├── KafkaConfig.java
    │   │   ├── RedisConfig.java
    │   │   ├── SecurityConfig.java
    │   │   └── AsyncConfig.java
    │   ├── exception/
    │   │   ├── AuditHubException.java
    │   │   ├── QuotaExceededException.java
    │   │   ├── TenantNotFoundException.java
    │   │   ├── UnauthorizedException.java
    │   │   └── GlobalExceptionHandler.java
    │   ├── model/
    │   │   ├── ApiResponse.java           ← Envelope: {success, data, error, traceId}
    │   │   └── PageResponse.java
    │   ├── security/
    │   │   ├── JwtService.java
    │   │   ├── JwtAuthFilter.java
    │   │   ├── ApiKeyAuthFilter.java
    │   │   └── TenantContextHolder.java   ← ThreadLocal orgId/appId/userId
    │   └── util/
    │       ├── MonthBucketUtil.java
    │       └── HashUtil.java
    │
    ├── tenant/                            ← FUTURE: tenant-service
    │   ├── controller/
    │   │   ├── OrganizationController.java
    │   │   └── ApplicationController.java
    │   ├── service/
    │   │   ├── OrganizationService.java
    │   │   ├── ApplicationService.java
    │   │   └── ApiKeyService.java
    │   ├── repository/
    │   │   ├── OrganizationRepository.java  ← JPA
    │   │   └── ApplicationRepository.java
    │   └── model/
    │       ├── Organization.java
    │       ├── Application.java
    │       └── ApiKey.java
    │
    ├── iam/                               ← FUTURE: auth-service
    │   ├── controller/
    │   │   └── AuthController.java
    │   ├── service/
    │   │   ├── AuthService.java
    │   │   ├── UserService.java
    │   │   ├── RoleService.java
    │   │   └── SamlService.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── SessionRepository.java
    │   └── model/
    │       ├── User.java
    │       ├── UserRole.java
    │       └── Session.java
    │
    ├── ingestion/                         ← FUTURE: ingestion-service
    │   ├── controller/
    │   │   └── IngestionController.java
    │   ├── service/
    │   │   ├── IngestionService.java
    │   │   ├── EventEnricher.java
    │   │   ├── IdempotencyService.java
    │   │   └── QuotaService.java
    │   ├── kafka/
    │   │   ├── AuditEventProducer.java
    │   │   └── ClientKafkaConsumer.java   ← Listens to client's Kafka topic
    │   └── model/
    │       ├── AuditEventRequest.java
    │       └── EnrichedAuditEvent.java
    │
    ├── storage/                           ← FUTURE: storage-service
    │   ├── kafka/
    │   │   └── AuditStorageConsumer.java  ← Consumes audit.events.enriched
    │   ├── repository/
    │   │   ├── AuditEventRepository.java  ← Cassandra
    │   │   └── DailyStatsRepository.java
    │   └── service/
    │       ├── StorageService.java
    │       └── RetentionService.java
    │
    ├── query/                             ← FUTURE: query-service
    │   ├── controller/
    │   │   └── EventQueryController.java
    │   ├── service/
    │   │   └── AuditQueryService.java
    │   └── model/
    │       ├── EventSearchRequest.java
    │       └── EventSearchResponse.java
    │
    ├── dashboard/                         ← FUTURE: part of query-service
    │   ├── controller/
    │   │   └── DashboardController.java
    │   └── service/
    │       └── DashboardService.java
    │
    ├── report/                            ← FUTURE: report-service
    │   ├── controller/
    │   │   └── ReportController.java
    │   ├── service/
    │   │   ├── ReportService.java
    │   │   ├── PdfReportGenerator.java
    │   │   └── CsvReportGenerator.java
    │   ├── scheduler/
    │   │   └── ReportScheduler.java
    │   └── repository/
    │       └── GeneratedReportRepository.java
    │
    ├── alert/                             ← FUTURE: alert-service
    │   ├── controller/
    │   │   └── AlertController.java
    │   ├── service/
    │   │   ├── AlertRuleService.java
    │   │   └── AlertEvaluator.java
    │   ├── kafka/
    │   │   └── AlertNotificationProducer.java
    │   └── repository/
    │       └── AlertRuleRepository.java
    │
    ├── replay/                            ← FUTURE: part of query-service
    │   ├── controller/
    │   │   └── ReplayController.java
    │   └── service/
    │       └── ReplayService.java
    │
    └── billing/                           ← FUTURE: billing-service
        ├── service/
        │   ├── BillingService.java
        │   └── RazorpayWebhookHandler.java
        └── repository/
            └── SubscriptionRepository.java
```

### How Split Works Later

```
NOW (monolith):
  IngestionController → IngestionService → QuotaService (same JVM)
                                         ↓
                                    AuditEventProducer → Kafka

AFTER SPLIT:
  ingestion-service pod:
    IngestionController → IngestionService → QuotaService (same pod)
                                           ↓
                                      AuditEventProducer → Kafka
  
  quota now in a separate service? Replace:
    QuotaService quotaService;  →  QuotaServiceClient quotaServiceClient; (Feign)
  
  The package boundary IS the service boundary.
  Internal @Autowired → external @FeignClient
  No code logic changes — only wiring changes.
```

---

## 2. Flow 1 — Organization Onboarding (Sign Up)

### Functional Flow

```
User visits audithub.in/signup
  ↓
Fills form: Company Name, Work Email, Password, GST (optional)
  ↓
Clicks "Create Account"
  ↓
Email OTP / email verification sent
  ↓
User verifies email
  ↓
Auto-created: Default Application ("My First App")
              Default API Key (WRITE scope)
              Admin user role assigned
  ↓
Redirected to Onboarding Wizard (4 steps):
  Step 1: Name your first application
  Step 2: Copy your API key
  Step 3: Send a test audit event
  Step 4: View it on dashboard
  ↓
Dashboard shows first event (onboarding complete)
```

### Technical Flow

```
POST /v1/organizations
  Body: { name, contactEmail, password, gstNumber? }
  
  1. OrganizationController.createOrganization()
     ↓
  2. OrganizationService.create()
     a. Check email uniqueness (users table)
     b. Validate GST format if provided (regex: [0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1})
     c. @Transactional begin
        i.  INSERT organizations (plan=FREE, status=ACTIVE, max_events=10000, retention_days=7)
        ii. INSERT users (role setup later)
        iii.INSERT user_roles (role=OWNER, org-wide)
        iv. INSERT applications (name="Default App", environment=PRODUCTION)
        v.  Generate API key: "ah_live_" + random(32 chars alphanumeric)
            hash = BCrypt.hash(plainKey)
            INSERT api_keys (key_prefix="ah_live_" + first8, key_hash=hash, key_type=WRITE)
        vi. INSERT subscriptions (plan=FREE, trial_end = now + 14 days)
     d. @Transactional commit
     ↓
  3. EmailService.sendVerificationEmail(email, token)
     - Token = JWT(sub=userId, type=EMAIL_VERIFY, exp=24h)
     - Stored in Redis: email:verify:{token} = userId (TTL 24h)
     ↓
  4. Return 201:
     {
       organizationId, 
       userId,
       message: "Verification email sent to ...",
       plainApiKey: "ah_live_xK9mP3..."   ← SHOWN ONCE
     }

POST /v1/auth/verify-email
  Body: { token }
  
  1. Validate JWT token
  2. Check Redis key exists
  3. UPDATE users SET status=ACTIVE WHERE id=userId
  4. DELETE Redis key
  5. Return 200 + JWT access/refresh tokens (user is now logged in)
```

### Business Rules

```
RULE 1: One organization per email domain for Enterprise plans
        (prevents duplicate enterprise accounts)

RULE 2: FREE plan auto-activated, no card required

RULE 3: Organization slug = slugify(name) — must be unique
        Used in: /org/{slug}/dashboard (white-label URL)

RULE 4: First user of an org is always OWNER — cannot be changed
        or demoted. Org can have exactly one OWNER.

RULE 5: API key is shown exactly once — not stored in plaintext
        If lost, must rotate (revoke + create new)

RULE 6: Trial period = 14 days of STARTER features, then drops to FREE
        Trial events don't count against FREE quota during trial
```

---

## 3. Flow 2 — Application Registration & API Key Generation

### Functional Flow

```
Admin logs in → Settings → Applications → "New Application"
  ↓
Form: Name="HDFC NetBanking", Environment=PRODUCTION, Webhook URL (optional)
  ↓
Application created → Application ID shown
  ↓
Go to API Keys → "Generate Key"
  ↓
Select: Application, Key Type (WRITE/READ/ADMIN), Expiry (optional)
  ↓
Key shown ONCE: "ah_live_xK9mP3qR7nL2vB8jT5wY1uA4sE6hG0fD"
  ↓
Developer copies to environment variable / secrets manager
  ↓
Developer integrates SDK with this key
```

### Technical Flow

```
POST /v1/applications
  Headers: Authorization: Bearer {jwt}
  Body: { name, environment, description, webhookUrl? }

  1. AuthFilter validates JWT → sets TenantContextHolder (orgId, userId, role)
  2. RoleCheck: requires ADMIN or OWNER
  3. ApplicationService.create()
     a. Check plan limit: SELECT count(*) FROM applications WHERE org_id=? 
        Compare with org.max_applications
        If exceeded: throw PlanLimitException("Upgrade to add more applications")
     b. slug = slugify(name) — unique within org
     c. INSERT applications
     d. Cache invalidation: redis.delete("org:apps:" + orgId)
  4. Return 201: { id, name, slug, environment, createdAt }

POST /v1/api-keys
  Body: { name, applicationId, keyType, scopes, expiresAt? }

  1. ApiKeyService.generate()
     a. Validate applicationId belongs to requesting org (prevent cross-tenant attack)
     b. Generate key:
        prefix  = "ah_live_"  (or "ah_test_" for DEVELOPMENT env)
        random  = SecureRandom(32 chars: a-z0-9A-Z)
        fullKey = prefix + random            ← "ah_live_xK9mP3qR7nL2vB8jT5wY1uA4sE6hG0fD"
        keyPrefix = fullKey.substring(0, 10) ← "ah_live_xK" stored in DB for display
        keyHash = BCryptPasswordEncoder.encode(fullKey)
     c. INSERT api_keys
     d. Return { id, name, keyPrefix, plainTextKey: fullKey, keyType, scopes, createdAt }
        NOTE: fullKey never stored, never returned again

API Key Validation (every ingest request):
  1. Extract "X-API-Key: ah_live_..." from header
  2. Parse prefix = header.substring(0, 10)
  3. Redis check: GET "apikey:valid:{prefix}"
     HIT  → deserialize ApiKeyPrincipal → set SecurityContext → proceed
     MISS → DB query: SELECT * FROM api_keys WHERE key_prefix=? AND is_active=true
             BCrypt.matches(header, row.key_hash)  ← timing-safe comparison
             If match: cache result in Redis (TTL 5 min)
             If no match: 401 Unauthorized
  4. Check key not expired: key.expires_at > now
  5. Update last_used_at (async, best-effort — don't block request)
```

### Business Rules

```
RULE 1: WRITE keys can only call /ingest/* endpoints
        READ keys can only call /events/*, /dashboard/*, /reports/*
        ADMIN keys can call everything including /replay, /api-keys

RULE 2: Keys for PRODUCTION apps are prefixed "ah_live_"
        Keys for STAGING/DEVELOPMENT apps are prefixed "ah_test_"
        This prevents dev keys being accidentally used in prod

RULE 3: Revoking a key is immediate — Redis cache must be invalidated
        DELETE api_keys SET is_active=false AND delete Redis entry

RULE 4: Max 10 active keys per application (prevent key sprawl)

RULE 5: Key rotation recommended every 90 days for ENTERPRISE
        Dashboard shows warning if key older than 90 days
```

---

## 4. Flow 3 — Audit Event Ingestion via REST

This is the **primary path** — 90% of clients use this.

### Functional Flow

```
Client App (e.g. HDFC NetBanking):
  "User Priya Nair approved Loan #LOAN-2025-001234 for ₹50L"
  ↓
Client calls: POST https://api.audithub.in/v1/ingest/events
  with API Key in header
  ↓
AuditHub accepts, validates, enriches, stores
  ↓
Event available in search within ~2 seconds
  ↓
Dashboard updates within ~30 seconds (Redis cache TTL)
```

### Technical Flow — Step by Step

```
─────────────────────────────────────────────────────────────────
STEP 1: HTTP Layer — API Gateway / Load Balancer
─────────────────────────────────────────────────────────────────
Request hits ALB → routes to Spring Boot pod

Rate limiting at ALB level:
  FREE:         100 req/min
  STARTER:      1000 req/min
  PROFESSIONAL: 5000 req/min
  ENTERPRISE:   unlimited (dedicated infra)

─────────────────────────────────────────────────────────────────
STEP 2: Security Filter Chain
─────────────────────────────────────────────────────────────────
ApiKeyAuthFilter.doFilterInternal():
  a. Extract header: "X-API-Key: ah_live_xK9..."
  b. Validate prefix format (starts with "ah_live_" or "ah_test_")
  c. Redis lookup / DB lookup (see Flow 2 above)
  d. Check key_type == WRITE (ingest requires WRITE or ADMIN key)
  e. Set TenantContextHolder:
       orgId      = key.organization_id
       appId      = key.application_id
       keyType    = WRITE
       scopes     = ["audit:write"]
  f. Set SecurityContext → proceed to controller

─────────────────────────────────────────────────────────────────
STEP 3: Controller
─────────────────────────────────────────────────────────────────
IngestionController.ingestSingle():
  a. @Valid annotation triggers Bean Validation:
     - actor.userId: not blank, max 255 chars
     - action.type: must be valid ActionType enum
     - resource.type: not blank
     - resource.id: not blank
     - changes: each FieldChange must have fieldName
  b. On validation failure: 400 with field-level errors
  c. Pass to IngestionService

─────────────────────────────────────────────────────────────────
STEP 4: Idempotency Check
─────────────────────────────────────────────────────────────────
IdempotencyService.isDuplicate():
  IF request has eventId:
    key = "idem:{orgId}:{eventId}"
    result = redis.get(key)
    IF result exists → return IngestResponse.DUPLICATE (200, not error)
    ELSE → proceed, mark later
  
  IF no eventId → generate UUID → always new event

─────────────────────────────────────────────────────────────────
STEP 5: Quota Check
─────────────────────────────────────────────────────────────────
QuotaService.checkAndIncrement():
  key    = "quota:monthly:{orgId}:{2025-06}"
  current = INCR key (atomic Redis operation)
  
  IF current == 1:
    EXPIRE key 35 days (new month's first event)
  
  IF current > org.max_events_per_month:
    DECR key (rollback)
    Throw QuotaExceededException
    → 507 response with upgrade link
  
  IMPORTANT: Check AFTER increment, rollback if exceeded
  This prevents race conditions between concurrent requests

─────────────────────────────────────────────────────────────────
STEP 6: Event Enrichment
─────────────────────────────────────────────────────────────────
EventEnricher.enrich():
  a. Resolve eventTime: use request.eventTime ?? Instant.now()
     (Allow backdated events for replay scenarios)
  
  b. monthBucket = YearMonth.from(eventTime in IST)
     e.g. "2025-06"
  
  c. GeoIP lookup (non-blocking, cached):
     IF actor.ipAddress != null:
       geoLocation = geoIpService.lookup(ipAddress)
       Maxmind GeoLite2 database (local lookup, no HTTP call)
       → country="IN", city="Mumbai"
  
  d. rawPayload = objectMapper.writeValueAsString(originalRequest)
     (Preserved for future replay)
  
  e. Build EnrichedAuditEvent with all fields

─────────────────────────────────────────────────────────────────
STEP 7: Kafka Publish
─────────────────────────────────────────────────────────────────
AuditEventProducer.publish():
  topic         = "audit.events.enriched"
  partitionKey  = orgId.toString()
  payload       = JSON(enrichedEvent)
  
  KafkaTemplate.send() with callback:
    ON SUCCESS → log offset, proceed
    ON FAILURE → publishToDlq(), log error
                 (event is NOT lost — goes to DLQ for manual review)
  
  Producer config:
    acks=all (wait for all ISR replicas)
    enable.idempotence=true (exactly-once Kafka semantics)
    linger.ms=5 (small batching for throughput)

─────────────────────────────────────────────────────────────────
STEP 8: Mark Idempotency & Return
─────────────────────────────────────────────────────────────────
  a. Redis SET "idem:{orgId}:{eventId}" = "1" EX 86400 (24h TTL)
  b. Return 202:
     {
       "eventId": "550e8400-...",
       "status": "ACCEPTED",
       "message": "Event accepted for processing"
     }

─────────────────────────────────────────────────────────────────
TOTAL LATENCY TARGET: < 100ms (p99)
─────────────────────────────────────────────────────────────────
  Filter chain:   ~5ms
  Validation:     ~1ms
  Redis quota:    ~2ms
  Enrichment:     ~3ms (GeoIP is local)
  Kafka publish:  ~10ms (async callback, we don't wait)
  Response:       ~5ms
  ──────────────────
  Total:          ~26ms typical, <100ms p99
```

### Batch Ingestion Flow

```
POST /v1/ingest/events/batch
  Body: { events: [ ...up to 1000 events... ] }

  1. Validate outer wrapper (@Size(max=1000))
  2. Bulk quota check: INCRBY key count (one Redis call for all)
  3. For each event in parallel (CompletableFuture):
     - Idempotency check (pipelined Redis MGET)
     - Enrich
     - Add to Kafka batch (linger.ms handles actual batching)
  4. Collect results:
     { accepted: 987, duplicates: 10, failed: 3, results: [...] }
  5. Return 202 (even partial failure = 202, not 500)

  OPTIMIZATION: Use Redis PIPELINE for all idempotency checks in one RTT
  redis.executePipelined(() -> {
    for (event : events) redis.opsForValue().get("idem:" + event.eventId);
  });
```

---

## 5. Flow 4 — Audit Event Ingestion via Kafka (Client Push)

For high-volume clients who already use Kafka internally.

### Functional Flow

```
Client's internal Kafka topic: "hdfc.system.events"
  ↓ (client publishes their own events here)
AuditHub subscribes to this topic (with client's permission + credentials)
  ↓
AuditHub maps client's event schema → AuditHub schema (configurable mapping)
  ↓
Validates, enriches, publishes to internal pipeline
  ↓
Same storage path as REST ingestion
```

### Technical Flow

```
APPLICATION CONFIG (stored in DB):
  applications.kafka_ingestion_config = {
    "bootstrapServers": "hdfc-kafka:9092",
    "topic": "hdfc.system.events",
    "consumerGroupId": "audithub-hdfc-consumer",
    "fieldMapping": {
      "actor.userId":    "$.user.id",
      "actor.userEmail": "$.user.email",
      "action.type":     "$.eventType",
      "action.name":     "$.eventName",
      "resource.type":   "$.entity.type",
      "resource.id":     "$.entity.id",
      "eventTime":       "$.timestamp"
    },
    "saslConfig": {
      "mechanism": "SCRAM-SHA-256",
      "username": "audithub-consumer",
      "passwordRef": "aws-secretsmanager://hdfc/kafka-pass"
    }
  }

CONSUMER LIFECYCLE:
  1. On application activation: KafkaConsumerRegistry.register(appConfig)
     → Create new KafkaConsumer with client's broker config
     → Start in separate thread pool (per-tenant consumer)
  
  2. Per message received:
     a. Parse JSON with JsonPath
     b. Apply fieldMapping: extract fields from client event
     c. Build AuditEventRequest (same as REST path)
     d. Set applicationId from consumer's app config
     e. Hand off to IngestionService.ingest()
        (same validation, quota, enrich, Kafka publish path)
  
  3. Error handling:
     - Schema mismatch → DLQ with raw client event + parse error
     - Quota exceeded → Pause consumer (Kafka pause(), not stop)
                        Resume when quota resets (next month)

DYNAMIC CONSUMER MANAGEMENT:
  @Component
  public class KafkaConsumerRegistry {
    private final Map<UUID, Consumer<?,?>> activeConsumers = new ConcurrentHashMap<>();
    
    public void register(ApplicationKafkaConfig config) {
      KafkaConsumer<String, String> consumer = buildConsumer(config);
      executor.submit(() -> pollLoop(consumer, config));
      activeConsumers.put(config.getApplicationId(), consumer);
    }
    
    public void deregister(UUID applicationId) {
      Consumer<?,?> c = activeConsumers.remove(applicationId);
      if (c != null) c.wakeup(); // triggers WakeupException to stop poll loop
    }
  }
```

---

## 6. Flow 5 — Audit Event Ingestion via Debezium CDC

For capturing database changes without modifying application code.

### Functional Flow

```
HDFC's PostgreSQL database has a "transactions" table
  ↓
DBA enables logical replication on that table
  ↓
AuditHub deploys Debezium connector pointed at HDFC's DB
  ↓
Any INSERT/UPDATE/DELETE on transactions table generates a CDC event
  ↓
Debezium publishes to Kafka topic: "dbserver1.public.transactions"
  ↓
AuditHub CDC consumer reads, maps to AuditEvent
  ↓
Stored in Cassandra with action_type = CREATE/UPDATE/DELETE
  and before/after values in changes[]
```

### Technical Flow

```
DEBEZIUM CONNECTOR CONFIG (Kafka Connect):
{
  "name": "hdfc-transactions-cdc",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "hdfc-postgres",
    "database.port": "5432",
    "database.user": "debezium_user",
    "database.password": "${file:/kafka/secrets/hdfc.properties:password}",
    "database.dbname": "hdfcbank",
    "database.server.name": "hdfc-pg",
    "table.include.list": "public.transactions,public.accounts",
    "plugin.name": "pgoutput",
    "publication.autocreate.mode": "filtered",
    "transforms": "unwrap",
    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
    "transforms.unwrap.add.fields": "op,ts_ms,before,after",
    "topic.prefix": "dbserver1"
  }
}

CDC EVENT STRUCTURE (from Debezium):
{
  "before": { "id": "TX-001", "status": "PENDING", "amount": 50000 },
  "after":  { "id": "TX-001", "status": "APPROVED", "amount": 50000 },
  "op":     "u",   // i=insert, u=update, d=delete
  "ts_ms":  1718000000000,
  "source": { "table": "transactions", "db": "hdfcbank" }
}

CDC CONSUMER (AuditHub):
@KafkaListener(topicPattern = "dbserver1\\..+")
public void consumeCdcEvent(String payload, @Header("kafka_topic") String topic) {
  CdcEvent cdc = parse(payload);
  
  // Map operation
  ActionType action = switch (cdc.getOp()) {
    case "i" -> ActionType.CREATE;
    case "u" -> ActionType.UPDATE;
    case "d" -> ActionType.DELETE;
    default  -> ActionType.CUSTOM;
  };
  
  // Build diff from before/after
  List<FieldChange> changes = diffService.diff(cdc.getBefore(), cdc.getAfter());
  
  // Map table name to resource type
  String resourceType = cdcConfig.getResourceTypeMapping().get(cdc.getSource().getTable());
  // e.g. "transactions" → "BankTransaction"
  
  AuditEventRequest event = AuditEventRequest.builder()
    .actor(Actor.system("CDC-CONNECTOR"))   // No human actor for DB changes
    .action(Action.of(action, "db." + cdc.getSource().getTable() + "." + cdc.getOp()))
    .resource(Resource.of(resourceType, extractId(cdc.getAfter()), null))
    .changes(changes)
    .eventTime(Instant.ofEpochMilli(cdc.getTsMs()))
    .severity(determineSeverity(action, resourceType))
    .build();
  
  ingestionService.ingest(appId, event);
}
```

---

## 7. Flow 6 — Audit Event Storage to Cassandra

This runs entirely asynchronously after Kafka publish. The client's REST call already returned 202.

### Technical Flow

```
CONSUMER: AuditStorageConsumer
Topic: audit.events.enriched
Group: audithub-storage-service
Concurrency: 8 threads (one per Kafka partition)

─────────────────────────────────────────────────────────────────
FOR EACH MESSAGE:
─────────────────────────────────────────────────────────────────

STEP 1: Deserialize
  EnrichedAuditEvent event = objectMapper.readValue(payload, EnrichedAuditEvent.class)

STEP 2: Determine TTL
  int ttlSeconds = retentionService.getRetentionTtlSeconds(event.getOrganizationId())
  
  Plan     → TTL
  FREE     → 7 days    = 604800 seconds
  STARTER  → 90 days   = 7776000 seconds
  PRO      → 365 days  = 31536000 seconds
  ENT      → 7 years   = 220752000 seconds
  
  Custom override: org.retention_days (from PostgreSQL, cached 10 min in Redis)

STEP 3: Fan-out Write to Cassandra (5 tables, parallel)
  CompletableFuture.allOf(
    // Table 1: Main table — by org/app/month
    saveToAuditEvents(event, ttlSeconds),
    
    // Table 2: By user — for user activity queries
    saveToAuditEventsByUser(event, ttlSeconds),
    
    // Table 3: By resource — for entity history
    saveToAuditEventsByResource(event, ttlSeconds),
    
    // Table 4: By severity — for alert dashboard
    saveToAuditEventsBySeverity(event, ttlSeconds),
    
    // Table 5: Entity timeline — denormalized for UI
    saveToEntityTimeline(event, ttlSeconds)
  ).join()
  
  WHY 5 TABLES?
  Cassandra doesn't support secondary indexes for large datasets.
  Each query pattern gets its own table with the right partition key.
  "Write once, read many" — Cassandra's sweet spot.

STEP 4: Update Daily Stats (Counter Table)
  UPDATE daily_stats 
    SET total_events  = total_events  + 1,
        success_count = success_count + (outcome==SUCCESS ? 1 : 0),
        failure_count = failure_count + (outcome==FAILURE ? 1 : 0),
        critical_count = critical_count + (severity==CRITICAL ? 1 : 0)
  WHERE organization_id=? AND application_id=? AND stat_date=today AND action_type=?
  
  Cassandra COUNTER type → atomic increment, no read-before-write

STEP 5: Invalidate Dashboard Cache
  redis.delete("dashboard:stats:" + orgId)
  redis.delete("dashboard:stats:" + orgId + ":" + appId)
  
  Next dashboard request will recompute from Cassandra

STEP 6: Alert Evaluation (async, fire-and-forget)
  alertEvaluator.evaluateAsync(event)
  
  (See Flow 14 for alert details)

STEP 7: Acknowledge Kafka Message
  ack.acknowledge()
  
  IF any step fails:
    Do NOT acknowledge → Kafka retries from same offset
    After max retries (3): ErrorHandler routes to audit.events.dlq
    DLQ alert fires to PagerDuty

─────────────────────────────────────────────────────────────────
WRITE CONSISTENCY: LOCAL_QUORUM (2 of 3 replicas must confirm)
  Balances durability vs latency for Mumbai AZ setup
─────────────────────────────────────────────────────────────────
```

---

## 8. Flow 7 — Audit Event Search & Query

### Functional Flow

```
Auditor opens Events page
  ↓
Sets filters: App="NetBanking", Date=Jun 1-13, ActionType=DELETE, Severity=HIGH
  ↓
Table loads with matching events (most recent first)
  ↓
Clicks on an event → side panel shows:
  - Full actor details
  - Before/After diff for every changed field
  - Entity history link
  - User activity link
  ↓
Can paginate with "Next Page" button
  ↓
Can export visible results as CSV
```

### Technical Flow

```
GET /v1/events?applicationId=X&startTime=2025-06-01T00:00:00+05:30
              &endTime=2025-06-13T23:59:59+05:30
              &actionType=DELETE&severity=HIGH&pageSize=50

─────────────────────────────────────────────────────────────────
STEP 1: Auth & RBAC
  JWT filter validates token
  Role check: ADMIN, AUDITOR, or VIEWER (all can read)
  Extract orgId from JWT claims

─────────────────────────────────────────────────────────────────
STEP 2: Build Search Request
  AuditEventSearchRequest:
    organizationId = JWT.orgId
    applicationId  = query param (must belong to this org — validated)
    startTime      = 2025-06-01T00:00:00Z (converted to UTC)
    endTime        = 2025-06-13T23:59:59Z
    actionTypes    = [DELETE]
    severities     = [HIGH]
    pageSize       = 50
    pageToken      = null (first page)

─────────────────────────────────────────────────────────────────
STEP 3: Month Bucket Generation
  The time range 2025-06-01 to 2025-06-13 spans only one bucket: "2025-06"
  
  If range is 2025-05-15 to 2025-06-13:
    buckets = ["2025-05", "2025-06"]  ← query both partitions
  
  WHY BUCKETS?
  Cassandra partition key includes monthBucket to bound partition size.
  Without it, a busy org's partition grows unbounded → performance degrades.
  One bucket ≈ max 10M events (org-level).

─────────────────────────────────────────────────────────────────
STEP 4: Cassandra Query
  SELECT * FROM audit_events
  WHERE organization_id = ?
    AND application_id  = ?
    AND month_bucket    = '2025-06'
    AND event_time      >= '2025-06-01T00:00:00Z'
    AND event_time      <= '2025-06-13T23:59:59Z'
  ORDER BY event_time DESC, event_id ASC
  LIMIT 50
  USING PagingState = null (first page)
  
  IMPORTANT: actionType and severity filters are applied IN-MEMORY
  after Cassandra returns results. Cassandra can only efficiently
  filter on partition key + clustering columns.
  
  For large-scale filtering by non-clustering fields:
  → Enterprise plan: Cassandra Search (DSE Lucene Index)
  → Or: OpenSearch/Elasticsearch sync via Kafka consumer

─────────────────────────────────────────────────────────────────
STEP 5: In-Memory Filter & Map
  results = cassandraResults
    .filter(r -> actionTypes.isEmpty() || actionTypes.contains(r.actionType))
    .filter(r -> severities.isEmpty() || severities.contains(r.severity))
    .map(this::mapToResponse)
    .limit(pageSize)
    .toList()

─────────────────────────────────────────────────────────────────
STEP 6: Pagination Token
  IF cassandraResultSet has more pages:
    nextPageToken = Base64.encode(JSON({
      pagingState: cassandra.pagingState.toString(),
      bucket: "2025-06"
    }))
  ELSE:
    nextPageToken = null

─────────────────────────────────────────────────────────────────
STEP 7: Cache (simple queries only)
  IF no fine-grained filters && first page:
    cacheKey = "query:{orgId}:{appId}:{startHour}:{endHour}:{pageSize}"
    redis.set(cacheKey, result, TTL=5min)

─────────────────────────────────────────────────────────────────
RESPONSE:
{
  "content": [ ...50 events... ],
  "pageToken": "eyJwYWdpbmdTdGF0ZSI6Ii4u...",
  "hasMore": true,
  "totalEstimate": 1247
}
```

---

## 9. Flow 8 — Entity History (Full Lifecycle View)

### Functional Flow

```
Investigator wants to know: "Everything that happened to Loan #LOAN-2025-001234"
  ↓
Clicks entity link in event detail panel
  OR navigates to: /entities/LoanApplication/LOAN-2025-001234
  ↓
Sees a vertical timeline:
  - Jun 13, 10:30 | Priya Nair | APPROVED | creditLimit changed ₹50K→₹1L
  - Jun 12, 09:15 | System     | CREATED  | New loan application submitted
  - Jun 13, 14:20 | Amit Kumar | UPDATED  | status: APPROVED→DISBURSED
  ↓
Each event expandable to show full diff
```

### Technical Flow

```
GET /v1/events/entity/LoanApplication/LOAN-2025-001234
    ?startTime=2025-01-01T00:00:00Z&endTime=2025-06-13T23:59:59Z

  1. Query: audit_events_by_resource (dedicated table)
     SELECT * FROM audit_events_by_resource
     WHERE organization_id = ?
       AND resource_type   = 'LoanApplication'
       AND resource_id     = 'LOAN-2025-001234'
       AND event_time      >= ?
       AND event_time      <= ?
     ORDER BY event_time DESC
     LIMIT 100
  
  2. No additional filtering needed — this table has only this entity's events
  
  3. Map to EntityTimelineResponse:
     {
       resourceType: "LoanApplication",
       resourceId:   "LOAN-2025-001234",
       events: [
         {
           eventTime: "2025-06-13T14:20:00Z",
           actor: { userId, userName, userEmail },
           action: { type: "UPDATE", name: "loan.disbursed" },
           changes: [
             { fieldName: "status", oldValue: "APPROVED", newValue: "DISBURSED" },
             { fieldName: "disbursedAt", oldValue: null, newValue: "2025-06-13T14:20:00Z" }
           ],
           outcome: "SUCCESS",
           severity: "HIGH"
         },
         ...
       ]
     }
```

---

## 10. Flow 9 — User Activity Trail

### Functional Flow

```
Security team wants: "Everything user EMP-12345 did in the last 30 days"
  (compliance requirement: monitor privileged user access)
  ↓
Goes to: Users → EMP-12345 → Activity
  ↓
Sees chronological list of all actions by that user across all applications
  ↓
Can filter by: date range, action type, application
  ↓
Unusual pattern detected (100 DELETEs in 5 minutes) → alert triggered
```

### Technical Flow

```
GET /v1/events/user/EMP-12345
    ?startTime=2025-05-14T00:00:00Z&endTime=2025-06-13T23:59:59Z

  1. Month buckets: ["2025-05", "2025-06"]
  
  2. For each bucket, query: audit_events_by_user
     SELECT * FROM audit_events_by_user
     WHERE organization_id = ?
       AND actor_user_id   = 'EMP-12345'
       AND month_bucket    = '2025-05'
       AND event_time      >= ?
       AND event_time      <= ?
     ORDER BY event_time DESC
     LIMIT 50
  
  3. Merge results from both buckets (already sorted DESC in each)
     Use merge-sort: compare heads of each result set
  
  4. Return paginated response
  
  IMPORTANT: User activity query respects RBAC:
    ADMIN/AUDITOR: can view any user's activity
    VIEWER:        can only view their OWN activity (userId must match JWT.sub)
```

---

## 11. Flow 10 — Dashboard Stats

### Functional Flow

```
Admin opens Dashboard:
  Card 1: "12,847 events today"          ← from Redis/Cassandra counter
  Card 2: "3 CRITICAL events"            ← from severity counter
  Card 3: "Quota: 67% used"              ← from Redis quota counter
  Card 4: "5 active applications"        ← from PostgreSQL count

  Chart: Last 30 days event trend (line chart)
  Donut: Events by severity (LOW/MED/HIGH/CRITICAL)
  Table: Top 5 most active users
```

### Technical Flow

```
GET /v1/dashboard/stats?applicationId={optional}

─────────────────────────────────────────────────────────────────
STEP 1: Check Redis Cache
  key = "dashboard:stats:{orgId}:{appId|all}"
  HIT  → return cached JSON (TTL 2 min)
  MISS → compute from Cassandra

─────────────────────────────────────────────────────────────────
STEP 2: Query Cassandra Counter Table
  SELECT * FROM daily_stats
  WHERE organization_id = ?
    AND application_id  = ?
    AND stat_date       = today

  → total_events, success_count, failure_count, critical_count

─────────────────────────────────────────────────────────────────
STEP 3: Query Redis for Quota
  current = redis.get("quota:monthly:{orgId}:{yearMonth}")
  limit   = org.max_events_per_month (from Redis org cache)
  percent = (current / limit) * 100

─────────────────────────────────────────────────────────────────
STEP 4: Query Top Actors (Cassandra)
  SELECT actor_user_id, actor_user_name, count(*)
  FROM audit_events_by_user
  WHERE organization_id = ?
    AND month_bucket = current_month
  GROUP BY actor_user_id   ← Cassandra GROUP BY supported on partition key only
  LIMIT 5
  
  LIMITATION: Cassandra GROUP BY is limited. In production:
  → Maintain a sorted set in Redis:
    ZINCRBY "top-actors:{orgId}:{month}" 1 "userId:userName"
    ZREVRANGE ... 0 4 WITHSCORES → top 5
  → Updated by StorageService after each event write

─────────────────────────────────────────────────────────────────
STEP 5: Build & Cache Response
  DashboardStats stats = {
    totalEventsToday:      4_847,
    totalEventsThisMonth:  127_392,
    quotaUsedPercent:      12.7,
    criticalEventsToday:   3,
    failedEventsToday:     12,
    activeApplications:    5,
    topActors:             [...],
    topResources:          [...],
    eventsTrend:           [...]  // from daily_stats last 30 days
  }
  
  redis.set("dashboard:stats:{orgId}", stats, TTL=2min)
  return stats
```

---

## 12. Flow 11 — Report Generation (PDF/CSV)

### Functional Flow

```
Compliance officer: "I need the RBI Audit Trail report for June 2025"
  ↓
Reports → New Report → Select Template: "RBI Audit Trail"
  ↓
Set filters: Application=NetBanking, Period=Jun 2025, Severity=HIGH,CRITICAL
  ↓
Click "Generate Report"
  ↓
202 Accepted → UI polls status every 5 seconds
  ↓
Status: PENDING → PROCESSING → COMPLETED
  ↓
"Download PDF" button appears
  ↓
Clicks download → presigned S3 URL → browser downloads PDF
```

### Technical Flow

```
POST /v1/reports
  Body: {
    name: "RBI Audit Trail - June 2025",
    templateId: "rbi-audit-trail-template-uuid",
    format: "PDF",
    filters: {
      applicationId: "netbanking-app-uuid",
      startTime: "2025-06-01T00:00:00+05:30",
      endTime: "2025-06-30T23:59:59+05:30",
      severities: ["HIGH", "CRITICAL"]
    }
  }

─────────────────────────────────────────────────────────────────
STEP 1: Create Report Record
  INSERT generated_reports (status=PENDING, requestedBy=userId)
  Return 202 with { reportId, status: "PENDING" }

─────────────────────────────────────────────────────────────────
STEP 2: Submit to Async Executor
  @Async("reportExecutor")
  CompletableFuture<Void> generateReportAsync(reportId, filters)
  
  Thread pool: 5 threads (reports are memory-intensive, limit concurrency)

─────────────────────────────────────────────────────────────────
STEP 3: Stream Events from Cassandra
  UPDATE generated_reports SET status=PROCESSING
  
  // Don't load all events into memory — stream in pages
  List<AuditEventResponse> allEvents = new ArrayList<>();
  String pageToken = null;
  do {
    AuditEventPage page = queryService.searchEvents(filters, pageToken, 500);
    allEvents.addAll(page.getContent());
    pageToken = page.getPageToken();
    
    // Safety valve: max 5M events per report
    if (allEvents.size() > 5_000_000) break;
  } while (pageToken != null);

─────────────────────────────────────────────────────────────────
STEP 4: Generate PDF (Apache PDFBox)
  PDDocument doc = new PDDocument()
  
  Page 1: Cover Page
    - AuditHub logo + org name
    - Report title, generation time (IST)
    - Compliance framework: RBI IT Framework Circular
    - Filters applied, total event count
  
  Page 2: Summary
    - Events by action type (table)
    - Events by severity (table)
    - Top 10 actors
    - Time range covered
  
  Pages 3-N: Event Data (100 rows per page)
    Table columns:
    | Date/Time (IST) | Actor | Email | IP | Action | Resource | Outcome | Severity |
    
    For UPDATE events: 
    Expanded row shows field diffs (old → new)
  
  Final Page: Audit Certificate
    "This report was generated by AuditHub on [timestamp]"
    "Data integrity hash: [SHA-256 of all event IDs concatenated]"
    "This report is tamper-evident."
  
  byte[] pdfBytes = baos.toByteArray()

─────────────────────────────────────────────────────────────────
STEP 5: Upload to S3
  key = "reports/{orgId}/{yearMonth}/{reportId}.pdf"
  s3.putObject(bucket, key, pdfBytes, metadata)
  
  S3 bucket has:
  - Object Lock (COMPLIANCE mode, 7-year retention for Enterprise)
  - Server-side encryption (SSE-KMS)
  - Versioning enabled

─────────────────────────────────────────────────────────────────
STEP 6: Generate Presigned URL
  url = s3.generatePresignedUrl(key, GET, expiry=1hour)
  
  UPDATE generated_reports 
  SET status=COMPLETED, s3_key=?, presigned_url=?, 
      presigned_url_expires_at=now+1h,
      row_count=?, file_size_bytes=?, completed_at=now

─────────────────────────────────────────────────────────────────
STEP 7: Notify User
  websocket.send(userId, {
    type: "REPORT_COMPLETED",
    reportId: ...,
    downloadUrl: ...,
    rowCount: ...,
    fileSizeMb: ...
  })
  
  OR email notification if user preference is email

GET /v1/reports/{reportId}
  → Poll for status (frontend polls every 5s)
  → When status=COMPLETED, return downloadUrl
  → If URL expired (>1h old): regenerate presigned URL on-the-fly
```

---

## 13. Flow 12 — Scheduled Report Delivery

### Functional Flow

```
Admin sets up: "Send RBI compliance report every 1st of month to audit@hdfc.com"
  ↓
Cron job fires on 1st of every month at midnight IST
  ↓
Report generated automatically (last month's data)
  ↓
Email sent to recipients with PDF attachment or S3 link
```

### Technical Flow

```
DB: report_schedules table
  cron_expression: "0 0 1 * *"   (1st of month, midnight)
  timezone: "Asia/Kolkata"
  recipients: ["audit@hdfc.com", "cto@hdfc.com"]
  template_id: rbi-audit-trail-uuid

@Scheduled IMPLEMENTATION:
  @Component
  public class ReportScheduler {
    
    @Scheduled(cron = "0 * * * * *")  // Run every minute
    public void checkDueSchedules() {
      List<ReportSchedule> due = scheduleRepo.findDueSchedules(Instant.now());
      
      for (ReportSchedule schedule : due) {
        // Build filters for last month
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        ReportFilters filters = buildFiltersForLastMonth(schedule, lastMonth);
        
        // Generate report (same async flow as Flow 11)
        reportService.generateAndEmail(schedule, filters);
        
        // Update next_run_at
        scheduleRepo.updateNextRun(schedule.getId(), 
            computeNextRun(schedule.getCronExpression(), schedule.getTimezone()));
      }
    }
  }

NOTE: For distributed deployment, use ShedLock:
  @SchedulerLock(name = "reportScheduler", lockAtMostFor = "PT5M")
  This prevents multiple pods from running the same schedule.
```

---

## 14. Flow 13 — Event Replay to Kafka

### Functional Flow

```
Scenario: HDFC's downstream fraud detection system was down for 2 hours.
          It missed all audit events from 10 AM to 12 PM on June 13.
          They need those events replayed to their Kafka consumer.
  ↓
Admin goes to: Replay → New Replay Job
  ↓
Sets filters: App=NetBanking, Time=10:00-12:00 Jun 13, ActionType=TRANSFER
  ↓
Sets target topic: "hdfc.fraud.audit.replay"
  ↓
Adds reason: "Fraud detection system downtime recovery"
  ↓
Submits → job runs asynchronously
  ↓
HDFC's fraud system consumes the replayed events from their topic
```

### Technical Flow

```
POST /v1/replay
  Body: {
    filters: { applicationId, startTime, endTime, actionTypes, resourceType },
    targetTopic: "hdfc.fraud.audit.replay",
    reason: "Fraud detection system downtime recovery"
  }

STEP 1: Authorization
  Requires ADMIN role — replay is a sensitive operation
  
STEP 2: Self-Audit the Replay Request
  // The replay request ITSELF is audited (meta-audit!)
  ingestionService.ingest(AuditEventRequest.builder()
    .actor(currentUser)
    .action(Action.of(CUSTOM, "audit.replay.initiated"))
    .resource(Resource.of("ReplayJob", replayId, null))
    .severity(CRITICAL)  // Always CRITICAL — replay is high-risk
    .metadata(Map.of(
        "targetTopic", targetTopic,
        "reason", reason,
        "filterStartTime", startTime.toString(),
        "filterEndTime", endTime.toString()
    ))
    .build()
  )

STEP 3: Async Execution
  @Async("replayExecutor")
  void executeReplay(replayId, filters, targetTopic) {
    // Stream events from Cassandra
    String pageToken = null;
    long replayed = 0;
    
    do {
      AuditEventPage page = queryService.search(filters, pageToken, 500);
      
      for (AuditEventResponse event : page.getContent()) {
        // Reconstruct event with replay metadata added
        EnrichedAuditEvent enriched = buildReplayEvent(event, replayId);
        enriched.getMetadata().put("_replay", "true");
        enriched.getMetadata().put("_replayId", replayId.toString());
        
        // Publish to TARGET topic (client's topic, not internal)
        kafkaTemplate.send(targetTopic, 
            event.getOrganizationId().toString(),
            objectMapper.writeValueAsString(enriched));
        
        replayed++;
      }
      
      pageToken = page.getPageToken();
    } while (pageToken != null);
    
    // Final audit: log completion
    ingestionService.ingest(/* replay.completed event */);
    
    return replayed;
  }

STEP 4: Target Topic Management
  // AuditHub creates the target topic if it doesn't exist
  // (requires Kafka admin credentials for client's cluster)
  // OR: client pre-creates it and shares topic name
```

---

## 15. Flow 14 — Alert Rule Evaluation

### Functional Flow

```
Alert Rule: "If any single user performs more than 10 DELETE actions in 5 minutes → CRITICAL alert"
  ↓
User Priya Nair deletes 15 records in 3 minutes
  ↓
11th DELETE triggers alert evaluation
  ↓
Alert: Email to security@hdfc.com + Webhook to SIEM system
  ↓
Alert appears in AuditHub Alerts dashboard
```

### Technical Flow

```
ALERT RULE SCHEMA:
{
  condition_type: "THRESHOLD",
  condition_config: {
    action_type: "DELETE",
    resource_type: null,       // null = any resource
    actor_user_id: null,       // null = any user
    threshold: 10,             // 10 events
    window_minutes: 5,         // in 5 minutes
    group_by: "actor_user_id"  // count per user
  },
  severity: "CRITICAL",
  notification_channels: ["EMAIL", "WEBHOOK"]
}

EVALUATION (after each stored event):
@Async
public void evaluateAsync(EnrichedAuditEvent event) {
  List<AlertRule> rules = alertRuleCache.get(event.getOrganizationId());
  // Rules cached in Redis (TTL 5 min) — avoid DB hit per event
  
  for (AlertRule rule : rules) {
    if (!matches(event, rule.getConditionConfig())) continue;
    
    // Count events in rolling window using Redis sorted set
    String countKey = "alert:{ruleId}:{groupValue}";
    // groupValue = event.getActor().getUserId() if group_by=actor_user_id
    
    long windowStart = Instant.now().minusSeconds(rule.getWindowMinutes() * 60L).toEpochMilli();
    long windowEnd   = Instant.now().toEpochMilli();
    
    // Add current event timestamp to sorted set
    redis.opsForZSet().add(countKey, event.getEventId().toString(), windowEnd);
    
    // Remove old members outside window
    redis.opsForZSet().removeRangeByScore(countKey, 0, windowStart - 1);
    
    // Count members in window
    long count = redis.opsForZSet().zCard(countKey);
    
    // Set TTL to window size (auto-cleanup)
    redis.expire(countKey, Duration.ofMinutes(rule.getWindowMinutes() + 1));
    
    if (count >= rule.getThreshold()) {
      // Check alert cooldown: don't spam multiple alerts
      String cooldownKey = "alert:cooldown:{ruleId}:{groupValue}";
      Boolean alreadyFired = redis.hasKey(cooldownKey);
      
      if (!alreadyFired) {
        redis.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(15));
        fireAlert(rule, event, count);
      }
    }
  }
}

fireAlert():
  1. Create alert record in PostgreSQL (alert_incidents table)
  2. Publish to Kafka topic: "audit.alerts"
  3. Notification consumer picks up:
     EMAIL:   send via SES/SMTP
     WEBHOOK: POST to rule.webhookUrl with alert payload
     SLACK:   POST to Slack webhook
```

---

## 16. Flow 15 — User Authentication (JWT)

### Technical Flow

```
POST /v1/auth/login
  Body: { email, password, mfaCode? }

─────────────────────────────────────────────────────────────────
STEP 1: Load User
  SELECT u.*, ur.role FROM users u
  JOIN user_roles ur ON u.id = ur.user_id
  WHERE u.email = ? AND u.status = 'ACTIVE'
  
  IF not found: 401 "Invalid credentials" (don't reveal if email exists)

─────────────────────────────────────────────────────────────────
STEP 2: Verify Password
  BCryptPasswordEncoder.matches(password, user.passwordHash)
  IF mismatch: increment failed_login_count in Redis
    IF count > 5: lock account for 15 minutes
    Return 401

─────────────────────────────────────────────────────────────────
STEP 3: MFA Check (if enabled)
  IF user.mfa_enabled:
    IF mfaCode is null: return 202 { mfaRequired: true }
    Validate TOTP: TotpUtil.validate(mfaCode, user.mfa_secret, tolerance=1)
    IF invalid: 401

─────────────────────────────────────────────────────────────────
STEP 4: Generate Tokens
  ACCESS TOKEN (JWT, 15 min):
    {
      "sub": userId,
      "org": orgId,
      "name": userName,
      "email": userEmail,
      "roles": ["ADMIN"],
      "iat": now,
      "exp": now + 900,
      "jti": UUID.randomUUID()  ← for revocation
    }
    Signed with HS256 using JWT_SECRET from Secrets Manager
  
  REFRESH TOKEN (opaque, 30 days):
    token = SecureRandom(64 bytes, base64url)
    hash  = SHA-256(token)
    INSERT sessions (id=jti, user_id, hash, expires_at=now+30d, ip, userAgent)

─────────────────────────────────────────────────────────────────
STEP 5: Audit the Login
  ingestionService.ingest(AuditEventRequest.builder()
    .actor(Actor.of(userId, email, name, ipAddress, userAgent, null))
    .action(Action.of(LOGIN, "user.login.success"))
    .resource(Resource.of("User", userId, name))
    .outcome(SUCCESS)
    .severity(LOW)
    .build()
  )

─────────────────────────────────────────────────────────────────
TOKEN REFRESH:
POST /v1/auth/refresh
  Body: { refreshToken }
  
  1. hash = SHA-256(refreshToken)
  2. SELECT * FROM sessions WHERE hash=? AND is_active=true AND expires_at > now
  3. IF not found: 401
  4. Generate new access token
  5. Optionally rotate refresh token (sliding expiry)

LOGOUT:
POST /v1/auth/logout
  1. Blacklist JWT jti: redis.set("jwt:revoked:{jti}", "1", TTL=15min)
  2. UPDATE sessions SET is_active=false WHERE id=jti
  3. Audit: user.logout event
```

---

## 17. Flow 16 — SAML SSO Authentication

### Technical Flow

```
Enterprise scenario: HDFC uses Azure AD as their IdP.
AuditHub is configured as Service Provider (SP).

SETUP (one-time, by HDFC Admin):
  1. HDFC uploads their IdP metadata XML to AuditHub Settings → SSO
  2. AuditHub generates SP metadata XML for HDFC to configure in Azure AD
  3. AuditHub stores:
     - IdP entity ID
     - SSO URL (HDFC's Azure AD endpoint)
     - IdP certificate (for signature verification)
     - Attribute mappings: Azure AD "mail" → email, "displayName" → name

LOGIN FLOW:
  1. User visits: https://app.audithub.in/login?org=hdfc
  
  2. GET /v1/auth/saml/initiate?orgSlug=hdfc
     → Load org's SAML config from DB
     → Build SAML AuthnRequest XML
     → Sign with AuditHub private key
     → Base64 encode + URL encode
     → Redirect to: https://login.microsoftonline.com/tenant/saml2?SAMLRequest=...
  
  3. User authenticates at Azure AD (credentials, MFA — handled by HDFC's AD)
  
  4. Azure AD posts SAML Response to: POST /v1/auth/saml/callback
     Body: SAMLResponse={base64 XML}
  
  5. AuditHub SAML processor:
     a. Decode SAML Response
     b. Verify signature with stored IdP certificate
     c. Check assertions: not expired, audience=AuditHub entityId
     d. Extract: email, name from attribute statements
     e. JIT (Just-In-Time) provisioning:
        User exists? → load user
        User doesn't exist? → CREATE user (status=ACTIVE, auth_provider=SAML)
        Role assignment: if configured, map AD group → AuditHub role
     f. Generate JWT + refresh token (same as local login)
  
  6. Redirect to dashboard with tokens (via URL fragment or Set-Cookie)
  
  7. Audit: user.login.sso event
```

---

## 18. Flow 17 — Role-Based Access Control (RBAC) Enforcement

### Technical Flow

```
ANNOTATION-BASED ENFORCEMENT:

// Method-level
@PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
public AuditEventPage searchEvents(...) { ... }

@PreAuthorize("hasRole('ADMIN')")
public ReplayResponse replayEvents(...) { ... }

// Data-level (row filtering)
@PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or #userId == authentication.name")
public AuditEventPage getUserActivity(@PathVariable String userId, ...) { ... }

TENANT ISOLATION (most critical):
// Every service method receives orgId from TenantContextHolder
// Never accept orgId from request body or query params for data queries

public AuditEventPage searchEvents(EventSearchRequest req) {
  UUID orgId = TenantContextHolder.getOrganizationId();  // From JWT/API key
  // Never: UUID orgId = req.getOrganizationId()  ← Security bug!
  
  return repository.search(orgId, req);  // orgId always from auth context
}

APPLICATION SCOPE:
// If API key is scoped to specific application, enforce it:
UUID appId = TenantContextHolder.getApplicationId();
if (appId != null && !appId.equals(req.getApplicationId())) {
  throw new AccessDeniedException("API key not authorized for this application");
}

PII MASKING BY ROLE:
public AuditEventResponse mask(AuditEventResponse event, String role) {
  if ("VIEWER".equals(role)) {
    return event.toBuilder()
      .actor(event.getActor().toBuilder()
        .userEmail(maskEmail(event.getActor().getUserEmail()))   // r***@hdfc.com
        .ipAddress(maskIp(event.getActor().getIpAddress()))     // 103.21.*.1
        .build())
      .build();
  }
  return event;
}
```

---

## 19. Flow 18 — Quota Enforcement & Billing Events

### Technical Flow

```
REAL-TIME QUOTA CHECK (per event, synchronous):
  // Already covered in Flow 3 STEP 5
  // Redis INCR is atomic — handles 10K concurrent requests safely

QUOTA WARNING EVENTS:
  // QuotaService checks thresholds
  if (current == (long)(limit * 0.80)) {
    emailService.sendQuotaWarning(orgId, 80);
    // Also: add to Redis to prevent duplicate warnings
    redis.set("quota:warned:80:{orgId}:{month}", "1", Duration.ofDays(35));
  }
  if (current == (long)(limit * 0.90)) {
    emailService.sendQuotaWarning(orgId, 90);
  }
  if (current >= limit) {
    // Event is rejected, email sent
    emailService.sendQuotaExceeded(orgId);
  }

MONTHLY USAGE SNAPSHOT (cron, 1st of month):
  @Scheduled(cron = "0 30 0 1 * *", zone = "Asia/Kolkata")
  void snapshotMonthlyUsage() {
    // Snapshot previous month's usage to PostgreSQL for billing
    INSERT INTO usage_snapshots
    SELECT orgId, appId, lastMonthStart, lastMonthEnd, 
           total_events, storage_bytes, api_calls
    FROM [computed from Redis + Cassandra]
  }

RAZORPAY INTEGRATION:
POST /v1/webhooks/razorpay (Razorpay calls this on subscription events)
  Verify webhook signature: HMAC-SHA256(body, webhookSecret)
  
  Event: payment.captured → activate subscription, upgrade plan
  Event: subscription.cancelled → downgrade to FREE at period end
  Event: payment.failed → send dunning email, restrict to FREE after grace period

PLAN UPGRADE FLOW:
  User clicks "Upgrade to Professional" → Razorpay Checkout page
  Razorpay processes payment → webhook fires → AuditHub upgrades plan
  
  ON PLAN UPGRADE:
  UPDATE organizations SET plan=?, max_events_per_month=?, retention_days=?, max_applications=?
  INVALIDATE Redis cache: "org:config:{orgId}"
  
  Existing events under old TTL are NOT retroactively updated.
  New events use new TTL. Old events expire at old TTL.
  Enterprise can request backfill TTL update (manual process).
```

---

## 20. Flow 19 — Data Retention & TTL Expiry

### Technical Flow

```
CASSANDRA TTL (automatic):
  Every INSERT uses USING TTL {seconds}
  Cassandra auto-tombstones expired rows
  Tombstones are GC'd after gc_grace_seconds (86400 = 24h)
  
  This means: no cron job needed for basic retention!
  Cassandra handles it natively.

COMPLIANCE-AWARE DELETION (Right to Erasure — DPDP Act):
  POST /v1/admin/gdpr/delete-user
  Body: { userId, reason }
  Requires: OWNER role only
  
  IMPLEMENTATION:
  // Cannot delete individual Cassandra rows by userId efficiently
  // (would require full scan of audit_events_by_user partition)
  
  Option A: Write a "DELETED" tombstone event
  // Add a deletion marker — the user's data is obscured:
  INSERT INTO audit_events_by_user (org, userId, ..., actor_user_email, actor_user_name)
  VALUES (..., '[DELETED]', '[DELETED]')  ← Overwrite PII fields
  
  Option B: Cassandra Delete by partition (if userId has dedicated partition)
  DELETE FROM audit_events_by_user
  WHERE organization_id = ? AND actor_user_id = ? AND month_bucket = ?
  
  This must be done for each month bucket the user has events in.
  → Query PostgreSQL for "first event date" of user to know which buckets to purge.

RETENTION POLICY CHANGE (plan upgrade):
  When org upgrades from STARTER (90d) to PRO (365d):
  - Old events already written with 90d TTL continue to expire at 90d
  - New events written with 365d TTL
  - Gap is acceptable (TTL is a "minimum retention" guarantee in T&Cs)
  - Enterprise can request TTL extension (manual Cassandra UPDATE USING TTL)
```

---

## 21. Flow 20 — Java SDK Integration (Client App)

### Technical Flow

```
CLIENT'S SPRING BOOT APP:

─────────────────────────────────────────────────────────────────
STEP 1: Add dependency + configure
─────────────────────────────────────────────────────────────────
pom.xml:
  <dependency>
    <groupId>in.audithub</groupId>
    <artifactId>audithub-spring-boot-starter</artifactId>
    <version>1.0.0</version>
  </dependency>

application.yml:
  audithub:
    api-key: ${AUDITHUB_API_KEY}
    application-id: ${AUDITHUB_APP_ID}
    async: true
    batch-size: 100
    flush-interval-ms: 500

─────────────────────────────────────────────────────────────────
STEP 2: Auto-Configuration
─────────────────────────────────────────────────────────────────
@AutoConfiguration
@ConditionalOnProperty(prefix="audithub", name="enabled", matchIfMissing=true)
public class AuditHubAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public AuditHubClient auditHubClient(AuditHubProperties props) {
    return new AuditHubClient(props, buildRestClient(props));
  }

  @Bean
  public AuditAspect auditAspect(AuditHubClient client, AuditContextProvider provider) {
    return new AuditAspect(client, provider);
  }
  
  @Bean
  public AuditContextProvider auditContextProvider() {
    // Default: reads from Spring Security context
    return new SpringSecurityAuditContextProvider();
  }
}

─────────────────────────────────────────────────────────────────
STEP 3: AuditContextProvider (bridges client's auth to AuditHub)
─────────────────────────────────────────────────────────────────
public class SpringSecurityAuditContextProvider implements AuditContextProvider {
  
  @Override
  public AuditContext current() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    HttpServletRequest req = ((ServletRequestAttributes) 
        RequestContextHolder.currentRequestAttributes()).getRequest();
    
    return AuditContext.builder()
      .userId(auth.getName())
      .userEmail(extractEmail(auth))
      .userName(extractName(auth))
      .ipAddress(getClientIp(req))
      .userAgent(req.getHeader("User-Agent"))
      .sessionId(req.getSession(false) != null ? req.getSession().getId() : null)
      .correlationId(req.getHeader("X-Correlation-ID"))
      .build();
  }
}

─────────────────────────────────────────────────────────────────
STEP 4: SDK Usage in Client Code
─────────────────────────────────────────────────────────────────
// Option A: Annotation
@Service
public class LoanService {
  
  @Audited(
    action = "loan.approved",
    actionType = "UPDATE",
    resourceType = "LoanApplication",
    severity = "HIGH"
  )
  public LoanApplication approve(@AuditResourceId String loanId, BigDecimal amount) {
    return repo.approve(loanId, amount);  // Audit fires automatically
  }
}

// Option B: Programmatic (when you need field-level diff)
@Service
public class AccountService {
  
  @Autowired private AuditHubClient audit;
  
  public Account updateCreditLimit(String accId, BigDecimal old, BigDecimal new_) {
    Account updated = repo.updateCreditLimit(accId, new_);
    
    audit.send(AuditEventRequest.builder()
      .actor(AuditContextHolder.getActor())
      .action(Action.of(UPDATE, "account.credit_limit.changed"))
      .resource(Resource.of("Account", accId))
      .changes(List.of(FieldChange.of("creditLimit", old, new_)))
      .severity(HIGH)
      .metadata(Map.of("currency", "INR"))
      .build());
    
    return updated;
  }
}

─────────────────────────────────────────────────────────────────
STEP 5: SDK Async Batching
─────────────────────────────────────────────────────────────────
// SDK accumulates events in memory queue
// Flushes when:
//   a. Batch size reaches 100, OR
//   b. 500ms elapsed since last flush, OR
//   c. JVM shutdown hook fires

@PreDestroy
public void shutdown() {
  flushRemaining();  // Best-effort drain on shutdown
  executor.shutdown();
}

// Network failure handling:
// SDK retries 3x with exponential backoff (1s, 2s, 4s)
// After 3 failures: events dropped (logged as WARN)
// Business logic NEVER fails because of audit failure
```

---

## 22. Complete Request Lifecycle (End-to-End)

```
TIME →  0ms          5ms         30ms        100ms       500ms       2000ms
        │            │           │           │           │           │
        ▼            ▼           ▼           ▼           ▼           ▼
CLIENT  POST /ingest
        ──────────────────────────────────────────────────────────────────
API GW  Rate limit check
        TLS termination
             │
             ▼
SPRING  API Key filter (Redis)     ←  ~3ms
        Bean Validation             ←  ~1ms
        Idempotency check (Redis)   ←  ~2ms
        Quota check (Redis INCR)    ←  ~2ms
        Enrichment (GeoIP local)    ←  ~3ms
        Kafka publish               ←  ~10ms
        Idem mark (Redis)           ←  ~2ms
        ─────────────────────────────────────
        202 ACCEPTED               ←  ~23ms total
        ─────────────────────────────────────
             │
             │ (async, client doesn't wait)
             ▼
KAFKA   Message in audit.events.enriched partition

STORAGE Consumer picks up message  ←  ~50-100ms after publish
        Cassandra fan-out write    ←  ~20ms (parallel, LOCAL_QUORUM)
        Redis counter increment
        Cache invalidation
        Alert evaluation (async)   ←  best-effort, ~50ms
        Kafka ack

READY   Event available in search  ←  ~200ms total end-to-end
        Dashboard updates          ←  within 2 min (cache refresh)
```

---

## 23. Monolith to Microservices Migration Path

This is the key architectural decision — build monolith NOW, split LATER.

```
PHASE 0 (MVP — Monolith, today):
  ┌──────────────────────────────────────────────┐
  │  audithub-monolith (single JAR, single DB)   │
  │  All packages in one Spring Boot app         │
  │  Kafka for internal async (audit pipeline)   │
  └──────────────────────────────────────────────┘

PHASE 1 (After 100 paying customers):
  Split the first bottleneck: Ingestion (high-write)

  ┌─────────────────────┐  ┌──────────────────────────┐
  │  audithub-core      │  │  audithub-ingestion       │
  │  (auth, tenant,     │  │  (ingestion only)         │
  │   query, reports)   │  │  Scale independently      │
  └─────────────────────┘  └──────────────────────────┘
  
  Split triggers:
  - Ingestion service CPU > 80% while query is idle
  - Need to scale ingestion without scaling query
  
  What changes in code:
  // Before (monolith):
  @Autowired IngestionService ingestionService;
  ingestionService.ingest(orgId, req);
  
  // After (split):
  @Autowired IngestionServiceClient ingestionClient;  // Feign HTTP client
  ingestionClient.ingest(orgId, req);
  
  Everything else (logic, DB, Kafka topics) stays the same.

PHASE 2 (After 500 customers):
  Split storage consumer (high-write to Cassandra)
  
  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
  │  core        │  │  ingestion   │  │  storage     │
  │  (auth,      │  │  (REST API   │  │  (Kafka →    │
  │   query,     │  │   intake)    │  │   Cassandra) │
  │   reports)   │  └──────────────┘  └──────────────┘
  └──────────────┘
  
  Split triggers:
  - Cassandra write lag > 500ms
  - Need to tune GC independently for write-heavy storage

PHASE 3 (Enterprise customers):
  Full microservices
  
  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
  │  auth    │ │  tenant  │ │ ingest   │ │ storage  │ │  query   │
  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
  ┌──────────┐ ┌──────────┐ ┌──────────┐
  │  report  │ │  alert   │ │ billing  │
  └──────────┘ └──────────┘ └──────────┘
  
  Each service:
  - Its own Dockerfile
  - Its own Helm chart
  - Its own HPA config
  - Communicates via Kafka (async) or Feign/gRPC (sync)

GOLDEN RULE:
  Package boundary in monolith = Service boundary in microservices
  No cross-package direct field access (only through service interfaces)
  Each package's service layer is the "API" — treat it like a REST controller
```

---

*flow.md — AuditHub Functional & Technical Flow Reference*  
*Version 1.0 | June 2025 | For internal development use*
