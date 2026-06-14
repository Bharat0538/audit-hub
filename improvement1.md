# AuditHub — Improvements Roadmap

> **Purpose:** Every known gap, missing feature, technical debt item, and future enhancement  
> organized by priority, effort, and business impact.  
> Use this as your backlog. Work top-down within each section.

---

## Table of Contents

1. [Critical Gaps (Build Before Launch)](#1-critical-gaps-build-before-launch)
2. [Phase 1 Improvements (Month 1–3)](#2-phase-1-improvements-month-13)
3. [Phase 2 Improvements (Month 4–6)](#3-phase-2-improvements-month-46)
4. [Phase 3 Improvements (Month 7–12)](#4-phase-3-improvements-month-712)
5. [Security Hardening Improvements](#5-security-hardening-improvements)
6. [Performance & Scalability Improvements](#6-performance--scalability-improvements)
7. [Cassandra Query Limitations & Fixes](#7-cassandra-query-limitations--fixes)
8. [Compliance Improvements (DPDP / RBI / SEBI)](#8-compliance-improvements-dpdp--rbi--sebi)
9. [Developer Experience Improvements](#9-developer-experience-improvements)
10. [Frontend / UX Improvements](#10-frontend--ux-improvements)
11. [Observability Improvements](#11-observability-improvements)
12. [Enterprise Feature Improvements](#12-enterprise-feature-improvements)
13. [Billing & Monetization Improvements](#13-billing--monetization-improvements)
14. [SDK Improvements](#14-sdk-improvements)
15. [Architecture Evolution Improvements](#15-architecture-evolution-improvements)
16. [Business Logic Gaps](#16-business-logic-gaps)

---

## 1. Critical Gaps (Build Before Launch)

These are **must-have** items that are missing from the current design. The product cannot ship without them.

---

### 1.1 Email Service Integration

**Gap:** The design references `emailService.sendVerificationEmail()` but there's no email provider configured.

**Fix:**
```java
// Add to pom.xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>ses</artifactId>
</dependency>

// EmailService.java
@Service
public class AwsSesEmailService implements EmailService {
  
  private final SesClient sesClient;
  
  @Override
  public void sendVerificationEmail(String to, String token) {
    String verifyUrl = "https://app.audithub.in/verify-email?token=" + token;
    
    SendEmailRequest request = SendEmailRequest.builder()
      .destination(Destination.builder().toAddresses(to).build())
      .source("noreply@audithub.in")
      .message(Message.builder()
        .subject(Content.builder().data("Verify your AuditHub account").build())
        .body(Body.builder()
          .html(Content.builder().data(buildVerificationHtml(verifyUrl)).build())
          .build())
        .build())
      .build();
    
    sesClient.sendEmail(request);
  }
  
  @Override
  public void sendQuotaWarning(UUID orgId, int percent) {
    // Load org email from DB, send warning template
  }
  
  @Override
  public void sendReportReady(String to, String reportName, String downloadUrl) {
    // Report completion notification
  }
}
```

**Templates needed (HTML):**
- Email verification
- Password reset
- Welcome email (after verification)
- Quota warning (80%, 90%, 100%)
- Report ready
- Alert fired
- Monthly usage summary
- Payment failed / dunning

---

### 1.2 Password Reset Flow

**Gap:** No forgot password / reset password flow defined.

**Fix:**
```
POST /v1/auth/forgot-password
  Body: { email }
  
  1. Find user by email (don't reveal if exists — always return 200)
  2. IF user exists:
     a. Generate reset token: UUID (not JWT — simpler, safer)
     b. Redis: SET "pwd:reset:{token}" = userId EX 3600 (1 hour)
     c. Send email with link: https://app.audithub.in/reset-password?token={token}
  3. Return: { message: "If that email exists, you'll receive a reset link" }

POST /v1/auth/reset-password
  Body: { token, newPassword }
  
  1. Redis GET "pwd:reset:{token}"
  2. IF null: 400 "Token expired or invalid"
  3. Validate password strength:
     - Min 8 chars
     - At least 1 uppercase, 1 number, 1 special char
  4. UPDATE users SET password_hash = BCrypt(newPassword), password_changed_at = now
  5. Redis DEL "pwd:reset:{token}" (single-use)
  6. Revoke all existing sessions (force re-login everywhere)
  7. Audit: user.password.reset event
  8. Send confirmation email
```

---

### 1.3 Onboarding Wizard — Test Event Verification

**Gap:** Onboarding Step 3 says "Send a test event" but there's no mechanism to confirm receipt and show it.

**Fix:**
```java
// OnboardingService.java

// Generate a unique onboarding correlation ID per user session
POST /v1/onboarding/test-event
  // Sends a sample event using their own API key
  // Returns the eventId
  {
    correlationId: "onboard-{userId}-{timestamp}",
    actor: { userId: "test-user", userName: "Test User" },
    action: { type: "CREATE", name: "test.event.sent" },
    resource: { type: "OnboardingTest", id: "test-001" }
  }

GET /v1/onboarding/test-event/{correlationId}/status
  // Polls until event found in Cassandra (max wait 10s)
  // Returns { found: true, eventId, message: "Your first audit event arrived!" }
  // Frontend polls this every 1s after sending test event
```

---

### 1.4 Webhook Signature Verification (Outbound Alerts)

**Gap:** Alert webhooks are sent to client URLs but there's no signature for clients to verify authenticity.

**Fix:**
```java
// When sending webhook:
String payload = objectMapper.writeValueAsString(alertPayload);
String secret  = application.getWebhookSecret();  // stored encrypted

// Compute HMAC-SHA256
String signature = HMAC_SHA256(secret, payload);

// Add to headers:
headers.put("X-AuditHub-Signature-256", "sha256=" + signature);
headers.put("X-AuditHub-Delivery", UUID.randomUUID().toString());
headers.put("X-AuditHub-Event", "alert.fired");
headers.put("X-AuditHub-Timestamp", Instant.now().toString());

// Client verifies:
// HMAC_SHA256(their-secret, body) == X-AuditHub-Signature-256 header

// Document this in SDK:
public boolean verifyWebhook(String body, String header, String secret) {
  String expected = "sha256=" + HMAC_SHA256(secret, body);
  return MessageDigest.isEqual(expected.getBytes(), header.getBytes());
  // MessageDigest.isEqual is timing-safe (prevents timing attacks)
}
```

---

### 1.5 Soft Delete for Organizations and Applications

**Gap:** DELETE endpoints exist but hard delete in Cassandra is complex. Soft delete is safer.

**Fix:**
```sql
-- Already have deleted_at column in schema
-- Implement soft delete everywhere:

-- Application delete:
UPDATE applications SET status='DELETED', deleted_at=NOW() WHERE id=?

-- Organization delete:
UPDATE organizations SET status='DELETED', deleted_at=NOW() WHERE id=?

-- Cascade: mark all applications as DELETED
-- DO NOT delete Cassandra data — retention TTL handles expiry
-- DO NOT delete PostgreSQL users/roles — needed for report attribution

-- Reactivation window: 30 days (support can restore)
-- After 30 days: permanent purge job cleans PostgreSQL
```

---

### 1.6 Input Sanitization for `metadata` and `tags`

**Gap:** `metadata` is `Map<String, String>` and `tags` is `List<String>` — no size limits defined, injectable.

**Fix:**
```java
@Value
@Builder
public class AuditEventRequest {
  
  // Metadata limits
  @Size(max = 50, message = "Maximum 50 metadata keys allowed")
  @Nullable
  private Map<
    @Size(max = 100) String,       // key max 100 chars
    @Size(max = 500) String        // value max 500 chars
  > metadata;
  
  // Tags limits
  @Size(max = 20, message = "Maximum 20 tags allowed")
  @Nullable
  private List<@Size(max = 100) @Pattern(regexp = "^[a-z0-9-_]+$") String> tags;
  // Tags must be lowercase alphanumeric + hyphen/underscore only
  
  // Add global payload size limit in Spring config:
  // spring.servlet.multipart.max-request-size=1MB
  // spring.mvc.pathmatch.use-suffix-pattern=false
}

// Additional: sanitize changes.oldValue and changes.newValue
// Reject if either value exceeds 10KB (prevents Cassandra cell size issues)
@AssertTrue(message = "Field change values must not exceed 10KB")
public boolean isChangesValid() {
  if (changes == null) return true;
  return changes.stream().allMatch(c -> 
    (c.getOldValue() == null || c.getOldValue().toString().length() < 10240) &&
    (c.getNewValue() == null || c.getNewValue().toString().length() < 10240)
  );
}
```

---

## 2. Phase 1 Improvements (Month 1–3)

These make the product significantly better for early adopters.

---

### 2.1 OpenSearch/Elasticsearch Integration for Advanced Search

**Problem:** Current Cassandra-only design can't efficiently filter on non-partition-key fields (severity, outcome, actionType, tags, metadata values). Results are filtered in-memory, which means Cassandra returns MORE data than needed.

**Solution:** Dual-write events to both Cassandra (source of truth) and OpenSearch (search index).

```java
// StorageService — add after Cassandra write
@Async
public void indexToOpenSearch(EnrichedAuditEvent event) {
  OpenSearchEvent doc = OpenSearchEvent.builder()
    .id(event.getEventId().toString())
    .organizationId(event.getOrganizationId())
    .applicationId(event.getApplicationId())
    .eventTime(event.getEventTime())
    .actorUserId(event.getActor().getUserId())
    .actorUserEmail(event.getActor().getUserEmail())
    .actionType(event.getAction().getType())
    .actionName(event.getAction().getName())
    .resourceType(event.getResource().getType())
    .resourceId(event.getResource().getId())
    .outcome(event.getOutcome())
    .severity(event.getSeverity())
    .tags(event.getTags())
    .metadata(event.getMetadata())
    .correlationId(event.getCorrelationId())
    .build();
  
  opensearchClient.index(i -> i
    .index("audit-events-" + event.getOrganizationId())  // per-tenant index
    .id(doc.getId())
    .document(doc));
}

// QueryService — use OpenSearch for filtered queries
public AuditEventPage searchWithFilters(EventSearchRequest req) {
  if (hasNonPartitionFilters(req)) {
    return searchViaOpenSearch(req);  // For complex filters
  }
  return searchViaCassandra(req);     // For simple time range queries
}

// OpenSearch index settings per tenant:
{
  "settings": {
    "number_of_shards": 1,     // Scale up as needed
    "number_of_replicas": 1,
    "index.lifecycle.name": "audit-events-policy"  // ILM policy
  },
  "mappings": {
    "properties": {
      "eventTime":    { "type": "date" },
      "actorUserId":  { "type": "keyword" },
      "actionType":   { "type": "keyword" },
      "resourceType": { "type": "keyword" },
      "resourceId":   { "type": "keyword" },
      "outcome":      { "type": "keyword" },
      "severity":     { "type": "keyword" },
      "tags":         { "type": "keyword" },
      "metadata":     { "type": "flattened" },  // search nested keys
      "actionName":   { "type": "text", "fields": { "keyword": { "type": "keyword" } } }
    }
  }
}
```

**Business impact:** Enables queries like:
- "All events where metadata.branchCode=MUM-001"
- "All CRITICAL DELETE events on Account resources in last 30 days"
- Full-text search on action description

---

### 2.2 Real-Time Dashboard via WebSocket

**Problem:** Dashboard currently requires manual refresh or short polling (every 30s). Enterprise clients want live streaming.

**Solution:** WebSocket push from server using Spring WebSocket + STOMP.

```java
// WebSocket Config
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
  }
  
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
      .setAllowedOriginPatterns("https://*.audithub.in")
      .withSockJS();
  }
}

// Push update after each event stored
@Component
public class DashboardPushService {
  
  private final SimpMessagingTemplate messagingTemplate;
  
  public void pushEventStored(EnrichedAuditEvent event) {
    // Push to org-specific topic
    String topic = "/topic/org/" + event.getOrganizationId() + "/events";
    
    LiveEventUpdate update = LiveEventUpdate.builder()
      .eventId(event.getEventId())
      .eventTime(event.getEventTime())
      .actorUserName(event.getActor().getUserName())
      .actionName(event.getAction().getName())
      .resourceType(event.getResource().getType())
      .severity(event.getSeverity())
      .outcome(event.getOutcome())
      .build();
    
    messagingTemplate.convertAndSend(topic, update);
  }
}

// Frontend:
const client = new Client({
  brokerURL: 'wss://api.audithub.in/ws',
  connectHeaders: { Authorization: `Bearer ${token}` }
});

client.subscribe(`/topic/org/${orgId}/events`, (message) => {
  const event = JSON.parse(message.body);
  prependEventToTable(event);
  incrementDashboardCounter();
});
```

---

### 2.3 Event Integrity Hash & Tamper Detection

**Problem:** Currently events are stored in Cassandra but there's no way to prove they haven't been tampered with (Cassandra doesn't have immutability guarantees at the application level).

**Solution:** SHA-256 hash chain (blockchain-lite approach).

```java
// When storing each event:
public String computeEventHash(EnrichedAuditEvent event, String previousHash) {
  String input = event.getEventId() +
                 event.getOrganizationId() +
                 event.getEventTime() +
                 event.getActor().getUserId() +
                 event.getAction().getType() +
                 event.getResource().getId() +
                 previousHash;  // Chained hash
  
  return DigestUtils.sha256Hex(input);
}

// Store in Cassandra:
ALTER TABLE audit_events ADD event_hash TEXT;
ALTER TABLE audit_events ADD previous_hash TEXT;

// Verification endpoint (Auditor role):
GET /v1/events/verify?applicationId=X&startTime=Y&endTime=Z

// Returns:
{
  "verified": true,
  "eventCount": 1247,
  "firstEventHash": "abc123...",
  "lastEventHash": "xyz789...",
  "chainIntegrity": "INTACT",  // or "BROKEN" with first broken event ID
  "verifiedAt": "2025-06-13T10:30:00Z"
}

// RBI compliance: this provides cryptographic proof of audit trail integrity
// Store the final hash in S3 with Object Lock for tamper-evident archival
```

---

### 2.4 Multi-Factor Authentication (TOTP)

**Problem:** MFA is mentioned in the schema but the implementation isn't detailed.

**Solution:**
```java
// MFA Setup:
POST /v1/auth/mfa/setup
  1. Generate TOTP secret: new GoogleAuthenticator().createCredentials().getKey()
  2. Return: { secret, qrCodeUrl, backupCodes: [8 codes] }
     QR URL: "otpauth://totp/AuditHub:{email}?secret={secret}&issuer=AuditHub"
  3. Do NOT enable MFA yet — user must confirm with first code

POST /v1/auth/mfa/confirm
  Body: { code }
  1. Validate TOTP: GoogleAuthenticator.authorize(secret, parseInt(code))
  2. If valid: UPDATE users SET mfa_enabled=true, mfa_secret=AES_ENCRYPT(secret)
               INSERT mfa_backup_codes (hashed) — 8 one-time codes
  3. Audit: user.mfa.enabled event

// MFA in Login (already in Flow 15):
// Login attempt → mfa_required: true → frontend shows TOTP input
// POST /v1/auth/login with mfaCode → validated before token generation

// Backup code usage:
// If user loses authenticator, accept backup code
// Each code single-use: DELETE from mfa_backup_codes after use
// Audit: user.mfa.backup_code.used (CRITICAL severity)

// MFA enforcement by org:
// ENTERPRISE: org can mandate MFA for all users
// organizations.mfa_required = true
// IF mfa_required=true AND user.mfa_enabled=false → 
//   block login after grace period of 7 days
```

---

### 2.5 DLQ (Dead Letter Queue) Recovery UI

**Problem:** Failed events go to `audit.events.dlq` Kafka topic but there's no UI or process to handle them.

**Solution:**
```java
// DLQ Consumer — internal admin only
@KafkaListener(topics = "audit.events.dlq", groupId = "audithub-dlq-monitor")
public void consumeDlq(String payload) {
  DlqMessage msg = deserialize(payload);
  
  // Store in PostgreSQL for visibility
  INSERT INTO dlq_events (
    original_event_id, organization_id, error_reason, 
    error_timestamp, retry_count, raw_payload, status
  ) VALUES (...)
  
  // Alert ops team (PagerDuty or Slack)
  alertOps("DLQ event received: " + msg.getErrorReason());
}

// Admin API:
GET  /v1/admin/dlq?status=PENDING&orgId=X
POST /v1/admin/dlq/{eventId}/retry
  // Re-publishes raw_payload to audit.events.enriched
  // Admin-only, requires AuditHub internal admin role (separate from org roles)
POST /v1/admin/dlq/{eventId}/discard
  // Mark as discarded with reason

// UI: Internal ops dashboard (not tenant-visible)
// Shows: error message, event payload, retry count, original timestamp
```

---

### 2.6 Tenant-Level GeoIP Restriction

**Problem:** A banking client might want to reject audit events from unexpected geographies (events from outside India should be flagged).

**Solution:**
```java
// Organization settings:
ALTER TABLE organizations ADD allowed_countries TEXT[]; 
// e.g. ['IN'] — null means all countries allowed

// In IngestionService, after enrichment:
if (org.getAllowedCountries() != null && !org.getAllowedCountries().isEmpty()) {
  String eventCountry = enriched.getGeoCountry();
  if (eventCountry != null && !org.getAllowedCountries().contains(eventCountry)) {
    // Don't reject — but flag as suspicious
    enriched = enriched.toBuilder()
      .severity(Severity.max(enriched.getSeverity(), Severity.HIGH))
      .tags(append(enriched.getTags(), "geo-anomaly"))
      .metadata(put(enriched.getMetadata(), "geo.flagged", "true"))
      .metadata(put(enriched.getMetadata(), "geo.country", eventCountry))
      .build();
    
    // Trigger alert if rule exists for geo-anomaly tag
    alertEvaluator.evaluateGeoAnomaly(enriched);
  }
}
```

---

## 3. Phase 2 Improvements (Month 4–6)

---

### 3.1 Anomaly Detection Engine

**Problem:** Threshold-based alerts (>10 DELETEs in 5 min) miss sophisticated patterns.

**Solution:** Statistical anomaly detection using Redis time-series.

```java
@Service
public class AnomalyDetector {
  
  // Maintain 30-day baseline per (orgId, userId, actionType)
  // Compare current hour's rate to historical baseline
  
  public void analyze(EnrichedAuditEvent event) {
    String baselineKey = String.format(
      "baseline:%s:%s:%s", 
      event.getOrganizationId(),
      event.getActor().getUserId(),
      event.getAction().getType()
    );
    
    // Get average events per hour over last 30 days
    double avgPerHour = getHistoricalAverage(baselineKey);
    
    // Current hour's count
    long currentHourCount = getCurrentHourCount(event);
    
    // Z-score anomaly: > 3 standard deviations = anomaly
    double zScore = computeZScore(currentHourCount, avgPerHour);
    if (zScore > 3.0) {
      fireAnomalyAlert(event, zScore, avgPerHour, currentHourCount);
    }
  }
  
  // Detectable patterns:
  // - User accessing data at 3 AM when they normally work 9-5
  // - Bulk export of customer data (1000s of READ events)
  // - Login from new country/device combination
  // - Privilege escalation attempts (multiple FAILED action sequences)
}
```

---

### 3.2 Audit Event Masking / PII Tokenization

**Problem:** Clients might send PII in `changes.newValue` (e.g. Aadhaar numbers, PAN, bank accounts). This shouldn't be stored in plaintext.

**Solution:**
```java
// Application-level setting: masking rules
applications.masking_rules = [
  { "fieldPath": "changes[*].newValue", "pattern": "\\d{12}", "mask": "AADHAAR" },
  { "fieldPath": "changes[*].newValue", "pattern": "[A-Z]{5}[0-9]{4}[A-Z]", "mask": "PAN" },
  { "fieldPath": "changes[*].newValue", "pattern": "\\d{16}", "mask": "CARD_NUMBER" }
]

// EventEnricher applies masking before storage:
public EnrichedAuditEvent applyMasking(EnrichedAuditEvent event, Application app) {
  if (app.getMaskingRules() == null) return event;
  
  List<FieldChange> maskedChanges = event.getChanges().stream()
    .map(change -> {
      String maskedNew = applyRules(change.getNewValue(), app.getMaskingRules());
      String maskedOld = applyRules(change.getOldValue(), app.getMaskingRules());
      return new FieldChange(change.getFieldName(), maskedOld, maskedNew);
    })
    .toList();
  
  return event.toBuilder().changes(maskedChanges).build();
}

// For ENTERPRISE: Field-level encryption instead of masking
// Encrypt with org-specific key (AWS KMS)
// Decrypt only when Auditor with specific permission requests
```

---

### 3.3 Audit Event Comments & Annotations

**Problem:** Auditors need to annotate events during investigations ("Reviewed — approved by CISO").

**Solution:**
```sql
CREATE TABLE audit_event_annotations (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  organization_id UUID NOT NULL,
  event_id        UUID NOT NULL,       -- references Cassandra event
  event_time      TIMESTAMP NOT NULL,  -- needed for Cassandra lookup
  application_id  UUID NOT NULL,
  comment         TEXT NOT NULL,
  annotation_type VARCHAR(50) NOT NULL DEFAULT 'NOTE',  -- NOTE/REVIEW/ESCALATE/RESOLVE
  created_by      UUID NOT NULL REFERENCES users(id),
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- API:
POST /v1/events/{eventId}/annotations
  Body: { comment, annotationType: "REVIEW" }
  Requires: AUDITOR or ADMIN role

GET /v1/events/{eventId}/annotations
  Returns: chronological list of annotations

-- Event detail UI shows annotation thread below event details
-- AUDITOR+ can add annotations
-- All annotation actions are themselves audited!
```

---

### 3.4 Investigation / Case Management

**Problem:** For enterprise security teams, multiple events are related (same attack, same actor). Need to group them into "cases".

**Solution:**
```sql
CREATE TABLE investigations (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  organization_id UUID NOT NULL,
  title           VARCHAR(500) NOT NULL,
  description     TEXT,
  status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',  -- OPEN/IN_PROGRESS/RESOLVED/CLOSED
  severity        VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
  assigned_to     UUID REFERENCES users(id),
  created_by      UUID NOT NULL REFERENCES users(id),
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  resolved_at     TIMESTAMP WITH TIME ZONE,
  resolution_notes TEXT
);

CREATE TABLE investigation_events (
  investigation_id  UUID NOT NULL REFERENCES investigations(id),
  event_id          UUID NOT NULL,
  event_time        TIMESTAMP NOT NULL,
  application_id    UUID NOT NULL,
  added_by          UUID NOT NULL,
  added_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  notes             TEXT,
  PRIMARY KEY (investigation_id, event_id)
);

-- UI: "Create Investigation" button in event detail panel
--     Add multiple events to the same investigation
--     Assign to team member
--     Export investigation report (all linked events + annotations)
```

---

### 3.5 Compliance Scorecard

**Problem:** Enterprises need to know "how well are we complying?" not just view raw events.

**Solution:**
```java
// Scheduled weekly computation
public ComplianceScore computeScore(UUID orgId) {
  ComplianceScore score = new ComplianceScore();
  
  // Rule 1: Admin actions must be approved by 2nd person (4-eye principle)
  long adminActionsTotal   = countActions(orgId, "APPROVE", CRITICAL);
  long singleApproverCount = countSingleApproverActions(orgId);
  score.setFourEyeCompliance((adminActionsTotal - singleApproverCount) / adminActionsTotal * 100);
  
  // Rule 2: Off-hours access
  long offHoursEvents = countOffHoursEvents(orgId);  // events between 10PM-6AM IST
  long totalEvents    = countAllEvents(orgId);
  score.setOffHoursScore(offHoursEvents < (totalEvents * 0.02) ? 100 : 60);
  
  // Rule 3: Failed login rate < 1%
  long failedLogins = countFailedLogins(orgId);
  long totalLogins  = countTotalLogins(orgId);
  score.setLoginSecurityScore(failedLogins / totalLogins < 0.01 ? 100 : 70);
  
  // Rule 4: Data export events reviewed within 24h
  long unreviewed = countUnreviewedExports(orgId);
  score.setDataExportScore(unreviewed == 0 ? 100 : 50);
  
  score.setOverallScore(average(all scores));
  score.setRbiReadiness(score.getOverallScore() > 85 ? "READY" : "NEEDS_IMPROVEMENT");
  
  return score;
}

// UI: Compliance Dashboard page
// Shows: Overall score, per-rule breakdown, trend over 90 days
// Download: Compliance Report for auditors
```

---

## 4. Phase 3 Improvements (Month 7–12)

---

### 4.1 AI-Powered Audit Intelligence

**Problem:** Manual review of millions of audit events is infeasible for human auditors.

**Solution:** LLM-based audit summaries.

```java
// Natural language query
POST /v1/ai/query
  Body: { query: "Who deleted the most records last week and why?" }
  
  1. Translate NL query to Cassandra/OpenSearch query (LLM)
  2. Execute query, get events
  3. Summarize results using LLM: "Priya Nair (branch manager, MUM-001) deleted 
     47 records between Jun 6-13. 45 were test accounts cleaned up as part of 
     Q2 housekeeping (confirmed by annotation on Jun 10 by CISO). 
     2 were unusual (accounts with active balances) — recommend investigation."
  4. Return structured + natural language response

// Weekly AI digest email:
"This week's audit summary for HDFC NetBanking:
 ✅ No CRITICAL events in customer-facing systems
 ⚠️ 3 unusual off-hours access events by EMP-234 (Ravi Kumar)
 📊 Event volume up 12% from last week (expected — month-end processing)
 🔐 0 failed login attempts (excellent)
 Action required: Review off-hours access by Ravi Kumar"
```

---

### 4.2 Multi-Region Data Residency

**Problem:** BFSI clients outside India (UAE, Singapore) need local data residency.

**Solution:**
```
Regions:
  ap-south-1  (Mumbai)     — Indian clients, default
  ap-southeast-1 (Singapore) — APAC clients
  me-central-1 (UAE)       — Middle East clients

Implementation:
  - Organization has region attribute set at signup
  - Cassandra keyspace has region-local topology
  - API routes to region-local pods
  - Cross-region replication ONLY for control plane (PostgreSQL — org settings)
  - Audit data NEVER leaves the client's selected region
  
  CloudFront + Route53 geolocation routing:
  IN traffic → Mumbai pods
  SG traffic → Singapore pods
  AE traffic → UAE pods

DPDP Act: Indian resident data must stay in India (ap-south-1)
```

---

### 4.3 SOC 2 Type II Preparation

**Problem:** Enterprise clients will demand SOC 2 certification.

**Improvements needed:**
```
1. Audit log of AuditHub itself (meta-audit):
   - All admin actions on the AuditHub platform logged
   - Separate immutable store (S3 CloudTrail)
   - Cannot be deleted or modified by any tenant

2. Access review automation:
   - Monthly: List all users with each role
   - Auto-email to org OWNER: "Please review and confirm these access levels"
   - Orphaned access: flag users who haven't logged in 90+ days

3. Change management:
   - All infrastructure changes via Terraform (IaC)
   - No manual console changes (AWS Config rules)
   - Deployment approval workflow (GitHub Actions + manual approval)

4. Vulnerability management:
   - Snyk dependency scanning in CI
   - OWASP ZAP for API scanning
   - AWS Inspector for container scanning
   - Penetration test annually
```

---

## 5. Security Hardening Improvements

### 5.1 Rate Limiting Improvements

**Current gap:** Rate limiting is at API Gateway level only. Needs application-level defense too.

```java
// Add Bucket4j rate limiting at application level (defense in depth)
@Component
public class ApplicationRateLimiter {
  
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  
  public boolean isAllowed(String orgId, String apiKeyId) {
    String key = orgId + ":" + apiKeyId;
    Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(orgId));
    return bucket.tryConsume(1);
  }
  
  private Bucket buildBucket(String orgId) {
    Organization org = orgCache.get(UUID.fromString(orgId));
    int ratePerMinute = switch (org.getPlan()) {
      case "FREE"         -> 100;
      case "STARTER"      -> 1000;
      case "PROFESSIONAL" -> 5000;
      case "ENTERPRISE"   -> 50000;
      default             -> 100;
    };
    
    return Bucket.builder()
      .addLimit(Bandwidth.classic(ratePerMinute, Refill.greedy(ratePerMinute, Duration.ofMinutes(1))))
      .build();
  }
}
```

### 5.2 SQL Injection Prevention Audit

```java
// All PostgreSQL queries must use parameterized statements — audit all:
// ✅ Spring Data JPA repositories (safe)
// ✅ @Query("SELECT ... WHERE id = :id") (safe if named params)
// ❌ String concatenation: "WHERE name='" + name + "'" — NEVER do this
// ❌ Native queries with string building — replace with criteria API

// Cassandra: QueryBuilder always (never String-built CQL)
// Already enforced in design — maintain this discipline
```

### 5.3 Secrets Management

```java
// Never hardcode secrets. Current design uses env vars — improve to:
// AWS Secrets Manager + Spring Cloud AWS

@Bean
public SecretsManagerClient secretsManagerClient() {
  return SecretsManagerClient.builder().region(Region.AP_SOUTH_1).build();
}

// In application.yml:
spring:
  config:
    import: "aws-secretsmanager:/audithub/prod"

# AWS Secrets Manager stores:
# /audithub/prod/db        → { host, username, password }
# /audithub/prod/jwt       → { secret }
# /audithub/prod/cassandra → { username, password }
# /audithub/prod/redis     → { password }
# /audithub/prod/razorpay  → { keyId, keySecret, webhookSecret }

# Rotation: Enable automatic rotation for DB passwords (60 days)
# Lambda rotator updates RDS password + updates Secrets Manager + notifies app
```

### 5.4 CORS Configuration

```java
@Configuration
public class CorsConfig {
  
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    // NEVER use "*" in production
    config.setAllowedOrigins(List.of(
      "https://app.audithub.in",
      "https://www.audithub.in"
    ));
    
    // For white-label: allow tenant-specific subdomains
    // config.setAllowedOriginPatterns(List.of("https://*.audithub.in"));
    
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "X-API-Key", "Content-Type", "X-Correlation-ID"));
    config.setExposedHeaders(List.of("X-Total-Count", "X-Page-Token"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/v1/**", config);
    return source;
  }
}
```

---

## 6. Performance & Scalability Improvements

### 6.1 Cassandra Query Optimization

**Current gap:** Fetching events across multiple month buckets requires sequential queries. Fix with parallel fan-out.

```java
// Current (sequential — slow for multi-month range):
for (String bucket : monthBuckets) {
  results.addAll(queryBucket(bucket));
}

// Improved (parallel):
List<CompletableFuture<List<AuditEvent>>> futures = monthBuckets.stream()
  .map(bucket -> CompletableFuture.supplyAsync(() -> queryBucket(bucket), cassandraExecutor))
  .toList();

List<AuditEvent> all = futures.stream()
  .map(CompletableFuture::join)
  .flatMap(Collection::stream)
  .sorted(Comparator.comparing(AuditEvent::getEventTime).reversed())
  .limit(pageSize)
  .toList();

// Thread pool tuned for Cassandra:
@Bean("cassandraExecutor")
public Executor cassandraExecutor() {
  return new ThreadPoolExecutor(
    10, 50,         // core and max threads
    60L, SECONDS,
    new ArrayBlockingQueue<>(1000),
    new ThreadPoolExecutor.CallerRunsPolicy()  // backpressure: caller executes if queue full
  );
}
```

### 6.2 Connection Pool Tuning

```yaml
# Cassandra driver (DataStax Java Driver 4.x)
datastax-java-driver:
  advanced:
    connection:
      pool:
        local:
          size: 2  # connections per node per local DC
        remote:
          size: 1
    request:
      timeout: 3 seconds
      consistency: LOCAL_QUORUM
      page-size: 500
    retry-policy:
      class: DefaultRetryPolicy
    load-balancing-policy:
      local-datacenter: ap-south-1
      slow-replica-avoidance: true

# Kafka producer tuning:
kafka:
  producer:
    buffer-memory: 33554432  # 32MB
    batch-size: 65536         # 64KB batch
    linger-ms: 5              # batch up to 5ms
    compression-type: lz4
    max-in-flight-requests-per-connection: 5
    # With idempotence=true, max 5 in-flight is safe

# Kafka consumer tuning:
kafka:
  consumer:
    fetch-min-size: 1024      # Wait for 1KB before returning (reduces requests)
    fetch-max-wait-ms: 500    # But no more than 500ms wait
    max-poll-records: 500
    max-poll-interval-ms: 300000  # 5 min (for slow batch processing)
```

### 6.3 N+1 Query Prevention

```java
// Problem: Loading dashboard top actors makes N DB calls
// BAD:
List<TopActor> actors = getTopActorIds(orgId);  // 1 query
actors.forEach(a -> a.setUserName(userService.getName(a.getUserId())));  // N queries

// GOOD: Batch lookup
List<String> actorIds = getTopActorIds(orgId);  // 1 Redis sorted set call
Map<String, String> names = userService.getNames(actorIds);  // 1 DB query with IN clause
// Map actor IDs to names in memory
```

---

## 7. Cassandra Query Limitations & Fixes

### 7.1 Large Partition Problem

**Problem:** A very active org with 1M events/day in one app will have partitions:
`(orgId, appId, "2025-06")` with 30M rows. Performance degrades above ~100K rows per partition.

**Fix: Sub-bucket partitioning**

```cql
-- Change month_bucket from "2025-06" to "2025-06-W1" (weekly buckets)
-- Or even daily: "2025-06-13"

-- New partition key:
PRIMARY KEY ((organization_id, application_id, day_bucket), event_time, event_id)

-- day_bucket = "2025-06-13"
-- Max ~1M events per day partition (manageable for most orgs)
-- For ultra-high-volume: hour_bucket = "2025-06-13-10"

-- Migration: generate day_bucket in EventEnricher:
String dayBucket = event.getEventTime()
    .atZone(ZoneId.of("Asia/Kolkata"))
    .toLocalDate()
    .toString();  // "2025-06-13"

-- Query impact: for a 30-day query, now query 30 partitions instead of 1
-- BUT: each partition is manageable size
-- Parallel fan-out (improvement 6.1) makes this fast
```

### 7.2 COUNT(*) Performance

**Problem:** `SELECT COUNT(*) FROM audit_events WHERE ...` is expensive in Cassandra — full partition scan.

**Fix:** Never use COUNT in Cassandra. Use the counter table instead.

```java
// BAD:
long count = cassandraOps.count(AuditEvent.class, where("organization_id").is(orgId));

// GOOD: Read from daily_stats counter table
long count = dailyStatsRepository.getTotalForOrg(orgId, startDate, endDate);

// For approximate real-time count: Redis counter
long count = Long.parseLong(redis.opsForValue().get("quota:monthly:" + orgId + ":" + yearMonth));
```

### 7.3 Tombstone Accumulation

**Problem:** Cassandra uses tombstones for deleted/expired data. Too many tombstones slow reads.

**Fix:**
```yaml
# Cassandra config (cassandra.yaml):
tombstone_warn_threshold: 1000
tombstone_failure_threshold: 100000

# Use TimeWindowCompactionStrategy (already in schema) — it minimizes tombstones
# For expiring data (TTL), TWCS groups data by write time, 
# so entire SSTables expire together (no tombstones!)

# Monitoring: Alert if tombstone count > 10K in a single query
# Add to StorageConsumer:
ExecutionInfo info = resultSet.getExecutionInfo();
// Check warnings in execution info for tombstone warnings
```

---

## 8. Compliance Improvements (DPDP / RBI / SEBI)

### 8.1 DPDP Act 2023 — Data Principal Rights

```java
// Right to Access (Section 11):
GET /v1/dpdp/my-data
  // Authenticated user can download ALL their personal data stored in AuditHub
  // Response: ZIP file containing:
  //   - All audit events where actor = this user
  //   - All reports generated by this user
  //   - Account creation data
  
// Right to Erasure (Section 13):
POST /v1/dpdp/delete-my-data
  Body: { reason }
  // Pseudonymize actor fields in Cassandra for this userId
  // Keep event structure (required by RBI) but mask PII:
  // actor_user_email: "[deleted-user@audithub.in]"
  // actor_user_name:  "[Deleted User]"
  // actor_ip_address: "0.0.0.0"
  
// Data Processing Register (Section 4):
// AuditHub must maintain a register of what personal data it processes
// Implement: GET /v1/admin/data-processing-register (internal)
```

### 8.2 SEBI Audit Trail Circular — Source System Audit

```java
// SEBI requires audit at the source system level (the client's app)
// AuditHub must generate SEBI-specific report format

// Template: SEBI_TRADING_AUDIT_TRAIL
{
  "templateType": "SEBI_TRADING_AUDIT_TRAIL",
  "requiredFields": [
    "actorUserId",       // Dealer ID
    "actorUserName",     // Dealer name
    "eventTime",         // Trade timestamp (IST)
    "actionType",        // BUY/SELL/MODIFY/CANCEL
    "resourceType",      // Order/Trade
    "resourceId",        // Order ID
    "changes.price",     // Price modification
    "changes.quantity",  // Quantity modification
    "ipAddress",         // Terminal IP
    "outcome"            // Order status
  ],
  "sortOrder": "eventTime ASC",  // SEBI requires chronological
  "reportPeriod": "DAILY",
  "submissionDeadline": "T+1 by 9:00 AM"
}
```

### 8.3 RBI IT Framework — Privileged User Monitoring

```java
// RBI mandates monitoring of "Privileged Users" (DBAs, system admins, super users)
// Implementation:
CREATE TABLE privileged_users (
  organization_id     UUID,
  user_id             TEXT,
  privilege_level     TEXT,  -- SUPERUSER/DBA/SYSADMIN/DEVELOPER
  added_by            UUID,
  added_at            TIMESTAMP,
  review_due_at       TIMESTAMP  -- Quarterly review required
);

// Privileged user events are automatically escalated:
// IF actor.userId in privileged_users → minimum severity = HIGH
// Alert on any privileged user action outside business hours
// Weekly report: All privileged user activities (mandatory for RBI compliance)

// Access Review Workflow:
// Every 90 days: Email to OWNER listing privileged users
// "Please confirm these users still require privileged access"
// If not confirmed within 14 days: auto-revoke and alert
```

---

## 9. Developer Experience Improvements

### 9.1 API Playground (Swagger UI + Live Testing)

```java
// Add SpringDoc OpenAPI
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.3.0</version>
</dependency>

// Config:
springdoc:
  swagger-ui:
    path: /swagger
    try-it-out-enabled: true
    operations-sorter: method
    tags-sorter: alpha
  api-docs:
    path: /v3/api-docs

// Sandbox environment:
// api.audithub.in → production
// sandbox.audithub.in → sandbox (real Cassandra, but separate org/data)
// Sandbox API keys: "ah_test_..."
// Sandbox events never bill or count against quota
```

### 9.2 Postman Collection Generation

```bash
# Auto-generate from OpenAPI spec
npx @apidevtools/swagger-cli bundle /api-docs > audithub-openapi.json
npx openapi-to-postmanv2 convert -s audithub-openapi.json -o audithub.postman.json

# Publish: https://www.postman.com/audithub/workspace/audithub-api
# Include: environment variables for API key, base URL
# Include: pre-request scripts to set org/app IDs
```

### 9.3 Webhook Tester

```java
// POST /v1/webhooks/test
// Sends a sample alert payload to the configured webhook URL
// Returns: { delivered: true, responseCode: 200, responseTime: 45ms }
// If delivery fails: { delivered: false, error: "Connection refused" }

// Implementation:
public WebhookTestResult testWebhook(String webhookUrl, String secret) {
  AlertPayload sample = buildSampleAlertPayload();
  
  Instant start = Instant.now();
  try {
    ResponseEntity<String> response = restTemplate.postForEntity(
      webhookUrl, 
      buildSignedPayload(sample, secret),
      String.class
    );
    long elapsed = Duration.between(start, Instant.now()).toMillis();
    return WebhookTestResult.success(response.getStatusCodeValue(), elapsed);
  } catch (Exception e) {
    return WebhookTestResult.failure(e.getMessage());
  }
}
```

---

## 10. Frontend / UX Improvements

### 10.1 Saved Search Filters

```sql
CREATE TABLE saved_searches (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  organization_id UUID NOT NULL,
  user_id         UUID NOT NULL,
  name            VARCHAR(255) NOT NULL,
  filters         JSONB NOT NULL,
  is_shared       BOOLEAN NOT NULL DEFAULT FALSE,  -- visible to all org members
  created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- UI: "Save as..." button next to filter bar
-- "My Searches" dropdown with saved filters
-- Org-wide shared searches for common compliance queries
-- e.g. "RBI Month-End Review" = { severity: [CRITICAL,HIGH], actionType: [DELETE,APPROVE] }
```

### 10.2 Keyboard Shortcuts

```typescript
// EventsPage keyboard shortcuts:
// /         → focus search bar
// Escape    → close event detail panel
// j/k       → navigate events (vim-style)
// Enter     → open selected event detail
// r         → refresh results
// Ctrl+E    → export CSV
// g d       → go to dashboard
// g e       → go to events
// g r       → go to reports

// Implementation using useHotkeys:
useHotkeys('/', (e) => { e.preventDefault(); searchRef.current?.focus(); });
useHotkeys('j', () => selectNextEvent());
useHotkeys('k', () => selectPrevEvent());
useHotkeys('escape', () => setSelectedEvent(null));
```

### 10.3 Dark Mode

```typescript
// ThemeProvider wrapping entire app
// Store preference in localStorage AND user profile
// Respect prefers-color-scheme media query on first visit

// In Tailwind:
module.exports = {
  darkMode: 'class',  // Toggle via class="dark" on <html>
  ...
}

// Toggle:
const { theme, setTheme } = useTheme();
// 'light' | 'dark' | 'system'
```

### 10.4 Mobile App (React Native)

```
Priority: LOW (enterprise users work on desktop)
But: On-call engineers need mobile alerts

Implementation plan:
  Phase 1: Responsive web (already in design system)
  Phase 2: PWA (installable, offline basic support)
  Phase 3: React Native app (share query/types packages)
            - Push notifications for CRITICAL alerts
            - View recent events
            - Approve/reject flagged events
            - NOT full audit trail (too complex for mobile)
```

---

## 11. Observability Improvements

### 11.1 Distributed Tracing

```java
// Add OpenTelemetry (OTel) — vendor-neutral
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

// Every request gets a trace ID automatically via Spring Boot 3.x Observation API
// Propagated: HTTP header "traceparent" (W3C standard)
//             Kafka header "traceparent"
//             Cassandra query metadata

// Trace visualization: Grafana Tempo (self-hosted) or AWS X-Ray

// Key spans to instrument:
// 1. Entire HTTP request (auto by Spring)
// 2. Redis operations (auto by Lettuce tracing)
// 3. Kafka publish (add manual span)
// 4. Cassandra writes per table (add manual span)
// 5. Report generation (add manual span with event count attribute)

// Custom trace attributes:
Span.current()
  .setAttribute("audit.org_id", orgId.toString())
  .setAttribute("audit.event_count", 1)
  .setAttribute("audit.quota_used_percent", quotaPercent);
```

### 11.2 Business Metrics Dashboard (Grafana)

```
Panel: Events ingested per minute (by org tier)
Panel: Ingestion latency p50/p95/p99
Panel: Kafka consumer lag (critical metric — if lag grows, storage is falling behind)
Panel: Cassandra write latency p99
Panel: Redis hit rate for API key cache
Panel: Active WebSocket connections
Panel: Report generation queue depth
Panel: DLQ event count (should be 0)
Panel: Monthly quota usage distribution across orgs

Alerts:
- Kafka consumer lag > 10K messages → page on-call
- Ingestion latency p99 > 500ms → warn
- DLQ has any messages → warn within 5 min
- Redis hit rate < 80% → investigate cache config
- Any pod OOM killed → immediate alert
```

### 11.3 Structured Logging

```java
// Use Logback with JSON encoder for log aggregation (ELK/CloudWatch Logs Insights)
@Slf4j
public class IngestionService {
  
  public IngestResponse ingest(UUID orgId, ...) {
    // Always include these fields for searchability:
    MDC.put("orgId", orgId.toString());
    MDC.put("requestId", correlationId);
    MDC.put("eventId", eventId.toString());
    
    log.info("event.ingested",
      kv("orgId", orgId),
      kv("eventId", eventId),
      kv("actionType", request.getAction().getType()),
      kv("severity", request.getSeverity()),
      kv("durationMs", elapsed)
    );
    
    // JSON output:
    // {"timestamp":"2025-06-13T10:30:00Z","level":"INFO","logger":"IngestionService",
    //  "message":"event.ingested","orgId":"uuid","eventId":"uuid",
    //  "actionType":"UPDATE","severity":"HIGH","durationMs":23,"traceId":"abc"}
  }
}

// Logback config (logback-spring.xml):
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <providers>
    <mdc/><timestamp/><message/><logLevel/>
    <threadName/><loggerName/>
    <stackTrace/>
  </providers>
</encoder>
```

---

## 12. Enterprise Feature Improvements

### 12.1 White-Label / Custom Domain

```
Feature: Enterprise clients can deploy AuditHub at their own domain
         audit.hdfc.com instead of app.audithub.in

Implementation:
  1. Organization setting: custom_domain = "audit.hdfc.com"
  2. HDFC creates CNAME: audit.hdfc.com → audithub.in (CloudFront)
  3. ACM Certificate Manager: auto-provision SSL for audit.hdfc.com
  4. Frontend: read org branding from /v1/branding (logo, colors)
     if custom domain → show HDFC logo + brand colors
  5. Emails: sent from "noreply@audit.hdfc.com" (via SES domain verification)

Branding config stored in DB:
  organizations.branding = {
    "logoUrl": "https://hdfc.com/logo.png",
    "primaryColor": "#003087",
    "companyName": "HDFC Bank Audit Portal"
  }
```

### 12.2 Approval Workflows (4-Eye Principle)

```java
// For RBI compliance: Some actions require dual approval
// e.g. "Fund transfer > ₹10L requires 2 approvers"

CREATE TABLE approval_workflows (
  id                  UUID PRIMARY KEY,
  organization_id     UUID NOT NULL,
  trigger_condition   JSONB NOT NULL,  -- {"actionType": "TRANSFER", "amount_gt": 1000000}
  required_approvers  INT NOT NULL DEFAULT 2,
  approver_roles      TEXT[],          -- ["ADMIN", "AUDITOR"]
  timeout_hours       INT NOT NULL DEFAULT 24,
  on_timeout          TEXT NOT NULL DEFAULT 'ESCALATE'  -- APPROVE/REJECT/ESCALATE
);

CREATE TABLE pending_approvals (
  id                  UUID PRIMARY KEY,
  workflow_id         UUID NOT NULL,
  event_id            UUID NOT NULL,
  status              TEXT NOT NULL DEFAULT 'PENDING',
  approvals           JSONB NOT NULL DEFAULT '[]',  -- [{approverId, timestamp, decision}]
  created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  expires_at          TIMESTAMP WITH TIME ZONE
);

-- Flow:
-- 1. High-value event submitted via SDK
-- 2. SDK includes: needsApproval: true
-- 3. AuditHub creates pending_approval, notifies approvers
-- 4. Approvers get email/in-app notification
-- 5. 2 approvers click "Approve" in AuditHub dashboard
-- 6. Event status updated to APPROVED, original system notified via webhook
```

### 12.3 Tenant Self-Hosting (Enterprise Edition)

```
Self-hosted deployment means client runs AuditHub on their own infrastructure.

Package: Docker Compose (small/dev) + Helm charts (production)

docker-compose.yml includes:
  - audithub-monolith (or all services)
  - Cassandra (single node for dev, 3-node for prod)
  - PostgreSQL
  - Redis
  - Kafka + Zookeeper
  - AuditHub UI (nginx)

License enforcement:
  - License key checked on startup
  - Key tied to: max events/month, feature set, expiry date
  - Phone-home (optional): sends aggregated usage stats to AuditHub
  - Air-gapped mode: no phone-home, manual license renewal

Upgrade path:
  - Docker image update: docker pull audithub/server:latest
  - DB migrations: Flyway (auto-run on startup)
  - Cassandra schema migrations: manual (versioned CQL scripts)
```

---

## 13. Billing & Monetization Improvements

### 13.1 Usage-Based Billing (Pay-Per-Event)

**Current:** Flat monthly tiers (10K / 1M / 10M)  
**Better:** Usage-based pricing with committed minimum

```
STARTER+ — ₹499/month base (includes 500K events)
  + ₹0.50 per 1K events above 500K
  
PROFESSIONAL+ — ₹2999/month base (includes 5M events)
  + ₹0.25 per 1K events above 5M

Benefits for customer:
  - Don't pay for unused quota
  - Scale naturally without plan changes

Razorpay implementation:
  - Usage tracked in Redis monthly counter
  - On 1st of month: snapshot usage → compute bill
  - Raise invoice via Razorpay API
  - Charge saved payment method
```

### 13.2 Volume Discounts for Enterprise

```
> 50M events/month  → 15% discount
> 100M events/month → 25% discount
> 500M events/month → Custom pricing (call sales)

Annual commitment:
  Pay annually → 2 months free (16.7% discount)

Multi-org pack:
  Parent company + 3 subsidiaries → bundle pricing
```

### 13.3 Free Tier Improvements

```
CURRENT: 10K events/month (too low — no real use case fits)
IMPROVE: 50K events/month (more realistic for evaluation)

Add: Forever-free hobbyist tier for open-source projects
     - 100K events/month
     - Must be public GitHub repo
     - AuditHub badge in README required
     - Social media share of usage = 6 months free

Why: Viral growth through developer adoption → word-of-mouth → enterprise leads
```

---

## 14. SDK Improvements

### 14.1 Automatic Field-Level Diff Detection

**Current:** Client must manually provide `changes[]` in programmatic mode.  
**Improvement:** SDK auto-diffs Java objects.

```java
// Instead of:
audit.send(event.withChanges(List.of(
  FieldChange.of("status", "PENDING", "APPROVED"),
  FieldChange.of("amount", 50000, 100000)
)));

// Enable:
@Audited(action = "account.updated", resourceType = "Account")
public Account update(Account before, Account after) {
  // SDK auto-generates diff via reflection
}

// SDK implementation:
public List<FieldChange> diff(Object before, Object after) {
  return ReflectionUtils.getFields(before.getClass()).stream()
    .filter(field -> !field.isAnnotationPresent(AuditIgnore.class))
    .map(field -> {
      Object oldVal = field.get(before);
      Object newVal = field.get(after);
      if (!Objects.equals(oldVal, newVal)) {
        return FieldChange.of(field.getName(), oldVal, newVal);
      }
      return null;
    })
    .filter(Objects::nonNull)
    .toList();
}

// @AuditIgnore annotation for sensitive fields:
public class Account {
  private String accountNumber;
  @AuditIgnore  // Don't include in diff
  private String internalRiskScore;
  private BigDecimal balance;
}
```

### 14.2 Python SDK

```python
# pip install audithub-sdk
from audithub import AuditHubClient, AuditEvent, Actor, Action, Resource

client = AuditHubClient(api_key=os.environ['AUDITHUB_API_KEY'])

client.send(AuditEvent(
  actor=Actor(user_id="user_123", user_email="priya@example.com"),
  action=Action(type="UPDATE", name="policy.updated"),
  resource=Resource(type="InsurancePolicy", id="POL-001"),
  outcome="SUCCESS",
  severity="HIGH"
))

# Django middleware:
MIDDLEWARE = ['audithub.django.AuditHubMiddleware']
AUDITHUB_API_KEY = os.environ['AUDITHUB_API_KEY']
AUDITHUB_AUTO_AUDIT = ['POST', 'PUT', 'PATCH', 'DELETE']
```

### 14.3 SDK Circuit Breaker

```java
// Current SDK: retries 3x then drops
// Improvement: Circuit breaker pattern

@Component
public class AuditHubClient {
  
  private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("audithub");
  
  public void send(AuditEventRequest event) {
    circuitBreaker.executeRunnable(() -> {
      if (props.isAsync()) {
        queue.offer(event);
      } else {
        sendImmediate(event);
      }
    });
  }
  
  // If AuditHub API is unreachable:
  // - CLOSED (normal): requests flow through
  // - OPEN (50% failures): fail fast, don't even try (saves client latency)
  // - HALF_OPEN: test with limited requests
  
  // Events during OPEN state: logged locally, NOT lost
  // Fallback: write to local file / database for later replay
}
```

---

## 15. Architecture Evolution Improvements

### 15.1 Event Versioning Strategy

**Problem:** As AuditEvent schema evolves, old events in Cassandra have old schema. Need backward compatibility.

```java
// Add schema version to all events:
EnrichedAuditEvent {
  int schemaVersion = 1;  // Increment when schema changes
  // ...
}

// Consumer always checks version:
EnrichedAuditEvent event = deserialize(payload);
if (event.getSchemaVersion() < CURRENT_VERSION) {
  event = migrationService.migrate(event);
}

// MigrationService:
public EnrichedAuditEvent migrate(EnrichedAuditEvent old) {
  return switch (old.getSchemaVersion()) {
    case 1 -> migrateV1toV2(old);
    case 2 -> migrateV2toV3(old);
    default -> old;
  };
}

// This allows evolving the schema without breaking existing consumers
```

### 15.2 Caching Layer Redesign for Scale

```
CURRENT: Redis for everything (quota, config, query cache, idempotency)
PROBLEM: Single Redis cluster becomes bottleneck at scale

IMPROVEMENT: Tiered caching

L1: In-process cache (Caffeine) — ultrafast, per-pod
    - API key validation: TTL 30s (acceptable staleness for revocation)
    - Org config: TTL 30s
    - No quota here (needs exact counts)

L2: Redis cluster — shared across pods
    - Quota counters: INCR (Redis-native, atomic)
    - Idempotency keys: SET NX EX
    - Dashboard stats: TTL 2min

L3: PostgreSQL — source of truth for config
    - Org settings, user data, API keys (hashed)

IMPLEMENTATION:
@Cacheable(value = "org-config", key = "#orgId", cacheManager = "localCacheManager")
public Organization getOrg(UUID orgId) {
  // L1 miss → L2 miss → DB
  String cached = redis.get("org:config:" + orgId);
  if (cached != null) return deserialize(cached);
  Organization org = orgRepo.findById(orgId).orElseThrow();
  redis.set("org:config:" + orgId, serialize(org), 600);  // 10 min
  return org;
}
```

### 15.3 Kafka Schema Registry

```
CURRENT: JSON schema is implicit (no validation at Kafka level)
PROBLEM: Producer and consumer can get out of sync (schema drift)

IMPROVEMENT: Apache Schema Registry (Confluent or AWS Glue)

Producer: Serialize using Avro with schema registered
Consumer: Deserialize using registered schema — auto-fails on incompatible change

Avro schema for EnrichedAuditEvent:
{
  "type": "record",
  "name": "EnrichedAuditEvent",
  "namespace": "in.audithub.event.v1",
  "fields": [
    {"name": "eventId",          "type": "string"},
    {"name": "organizationId",   "type": "string"},
    {"name": "eventTime",        "type": {"type": "long", "logicalType": "timestamp-millis"}},
    {"name": "actor",            "type": "Actor"},
    // ...
  ]
}

// Backward compatibility: new fields must have defaults
// {"name": "geoCity", "type": ["null", "string"], "default": null}
// Old events without geoCity deserialize cleanly (use null default)
```

---

## 16. Business Logic Gaps

### 16.1 Application Deletion — Event Ownership

**Gap:** If an application is deleted, what happens to its audit events?

```
DECISION:
Events are NEVER deleted when an application is deleted.
Applications are soft-deleted (status=DELETED).
Events remain in Cassandra until TTL expires naturally.
Events still queryable with applicationId — just the app metadata is gone.
Reports can still reference deleted application's events.

IMPLEMENTATION:
DELETE /v1/applications/{id}
  1. UPDATE applications SET status='DELETED', deleted_at=NOW()
  2. Revoke all API keys for this application
  3. Do NOT touch Cassandra
  4. Audit: application.deleted event
  
Query service: 
  If applicationId belongs to deleted app → still return events
  Just label in UI: "App: NetBanking (Deleted)"
```

### 16.2 Event Time Backdating Policy

**Gap:** Clients can submit events with any `eventTime` (past or future). No validation defined.

```java
// Policy:
public void validateEventTime(Instant eventTime) {
  Instant now = Instant.now();
  
  // Reject future events (within 5 min tolerance for clock skew)
  if (eventTime.isAfter(now.plusSeconds(300))) {
    throw new ValidationException("eventTime cannot be in the future");
  }
  
  // Reject events older than retention policy
  int retentionDays = retentionService.getRetentionDays(orgId);
  if (eventTime.isBefore(now.minus(retentionDays, ChronoUnit.DAYS))) {
    throw new ValidationException(
      "eventTime is older than your retention policy (" + retentionDays + " days)"
    );
  }
  
  // Warn on events older than 24h (likely a late arrival, not a bug)
  if (eventTime.isBefore(now.minus(24, ChronoUnit.HOURS))) {
    log.warn("Late arrival: event {} is {} hours old", eventId, hoursBetween(eventTime, now));
    // Tag the event for visibility
    event.getTags().add("late-arrival");
  }
}
```

### 16.3 Organization User Limit Per Plan

**Gap:** No limit on number of users per organization is defined.

```
FREE:         3 users
STARTER:      10 users
PROFESSIONAL: 50 users
ENTERPRISE:   Unlimited

Implementation:
  UserService.inviteUser():
    long currentCount = userRepo.countByOrgId(orgId);
    long limit = planLimits.getUserLimit(org.getPlan());
    if (currentCount >= limit) {
      throw new PlanLimitException("User limit reached. Upgrade to add more users.");
    }

  API response when limit reached:
  {
    "code": "USER_LIMIT_REACHED",
    "message": "Your STARTER plan supports up to 10 users. Upgrade to Professional.",
    "upgradeUrl": "https://app.audithub.in/settings/billing"
  }
```

### 16.4 Concurrent Report Generation Limit

**Gap:** What if a user submits 50 report generation requests simultaneously?

```java
// Per-org concurrent report limit:
FREE:         1 at a time
STARTER:      2 at a time
PROFESSIONAL: 5 at a time
ENTERPRISE:   20 at a time

Implementation in ReportService:
  public GeneratedReport submitReport(UUID orgId, ...) {
    // Count in-progress reports for this org
    long inProgress = reportRepo.countByOrgAndStatus(orgId, "PROCESSING");
    long limit = planLimits.getConcurrentReportLimit(org.getPlan());
    
    if (inProgress >= limit) {
      throw new PlanLimitException(
        "Too many reports in progress. Wait for existing reports to complete."
      );
    }
    // ... proceed
  }
```

### 16.5 Audit Event Schema Versioning for Clients

**Gap:** Client uses SDK v1. AuditHub adds new required field in v2. Client's old events break.

```
DECISION: All new fields in AuditEventRequest are optional (nullable).
          No required fields added after v1 launch.
          SDK version tracked in event metadata:
            metadata["_sdk_version"] = "1.0.0"
            metadata["_sdk_lang"]    = "java"
          
URL versioning: /v1/ingest (stable) vs /v2/ingest (new features)
Old clients on /v1 continue to work indefinitely.
/v1 deprecated only with 12-month notice.
```

---

*improvement1.md — AuditHub Improvements Roadmap*  
*Version 1.0 | June 2025*  
*Review this quarterly and move items to active development backlog.*
