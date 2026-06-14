
---

## 7. OpenAPI Specification

```yaml
openapi: 3.1.0
info:
  title: AuditHub API
  description: |
    Enterprise Audit Trail Platform API.
    All endpoints require authentication via API Key (X-API-Key header) or Bearer JWT.
    Rate limits apply per organization tier.
  version: 1.0.0
  contact:
    name: AuditHub Support
    email: support@audithub.in
  license:
    name: Commercial

servers:
  - url: https://api.audithub.in/v1
    description: Production
  - url: https://sandbox.audithub.in/v1
    description: Sandbox

security:
  - ApiKeyAuth: []
  - BearerAuth: []

tags:
  - name: Ingestion
    description: Submit audit events
  - name: Query
    description: Search and retrieve audit events
  - name: Reports
    description: Generate compliance reports
  - name: Organizations
    description: Tenant management
  - name: Applications
    description: Application management
  - name: Users
    description: User management
  - name: API Keys
    description: API key management
  - name: Dashboard
    description: Aggregated stats for UI
  - name: Replay
    description: Event replay to Kafka
  - name: Alerts
    description: Alert rule management
  - name: Auth
    description: Authentication

components:
  securitySchemes:
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
      description: "Format: ah_live_xxxxxxxx or ah_test_xxxxxxxx"
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    # ─── AUDIT EVENT ─────────────────────────────────────────────
    Actor:
      type: object
      required: [userId]
      properties:
        userId:
          type: string
          example: "user_123"
        userEmail:
          type: string
          format: email
          example: "rahul@hdfc.com"
        userName:
          type: string
          example: "Rahul Sharma"
        ipAddress:
          type: string
          example: "103.21.58.1"
        userAgent:
          type: string
          example: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        sessionId:
          type: string
          example: "sess_abc123"

    Resource:
      type: object
      required: [type, id]
      properties:
        type:
          type: string
          example: "BankAccount"
        id:
          type: string
          example: "HDFC-ACC-001"
        name:
          type: string
          example: "Savings Account - Rahul Sharma"
        path:
          type: string
          example: "/api/v1/accounts/HDFC-ACC-001"

    Action:
      type: object
      required: [type]
      properties:
        type:
          type: string
          enum: [CREATE, UPDATE, DELETE, READ, LOGIN, LOGOUT, EXPORT, APPROVE, REJECT, TRANSFER, CUSTOM]
          example: "UPDATE"
        name:
          type: string
          example: "account.balance.updated"
        description:
          type: string
          example: "Account balance was updated by branch manager"

    FieldChange:
      type: object
      required: [fieldName]
      properties:
        fieldName:
          type: string
          example: "creditLimit"
        oldValue:
          description: Previous value (null for CREATE events)
          example: "50000"
        newValue:
          description: New value (null for DELETE events)
          example: "100000"

    AuditEventRequest:
      type: object
      required: [actor, action, resource]
      properties:
        eventId:
          type: string
          format: uuid
          description: Client-generated idempotency ID. If omitted, server generates one.
          example: "550e8400-e29b-41d4-a716-446655440000"
        eventTime:
          type: string
          format: date-time
          description: Event timestamp in ISO 8601. Defaults to server time if omitted.
          example: "2025-06-13T10:30:00+05:30"
        applicationId:
          type: string
          format: uuid
          description: Target application. Uses API key's application if omitted.
        actor:
          $ref: '#/components/schemas/Actor'
        action:
          $ref: '#/components/schemas/Action'
        resource:
          $ref: '#/components/schemas/Resource'
        changes:
          type: array
          items:
            $ref: '#/components/schemas/FieldChange'
        outcome:
          type: string
          enum: [SUCCESS, FAILURE, PARTIAL]
          default: SUCCESS
        severity:
          type: string
          enum: [LOW, MEDIUM, HIGH, CRITICAL]
          default: LOW
        correlationId:
          type: string
          example: "req_789xyz"
        metadata:
          type: object
          additionalProperties:
            type: string
          example:
            branchCode: "MUM-001"
            regulatorRef: "RBI-2024-001"
        tags:
          type: array
          items:
            type: string
          example: ["kyc", "high-value", "branch-op"]

    AuditEventBatchRequest:
      type: object
      required: [events]
      properties:
        events:
          type: array
          minItems: 1
          maxItems: 1000
          items:
            $ref: '#/components/schemas/AuditEventRequest'

    AuditEventResponse:
      type: object
      properties:
        eventId:
          type: string
          format: uuid
        organizationId:
          type: string
          format: uuid
        applicationId:
          type: string
          format: uuid
        eventTime:
          type: string
          format: date-time
        actor:
          $ref: '#/components/schemas/Actor'
        action:
          $ref: '#/components/schemas/Action'
        resource:
          $ref: '#/components/schemas/Resource'
        changes:
          type: array
          items:
            $ref: '#/components/schemas/FieldChange'
        outcome:
          type: string
        severity:
          type: string
        correlationId:
          type: string
        metadata:
          type: object
          additionalProperties:
            type: string
        tags:
          type: array
          items:
            type: string
        createdAt:
          type: string
          format: date-time

    IngestResponse:
      type: object
      properties:
        eventId:
          type: string
          format: uuid
        status:
          type: string
          enum: [ACCEPTED, DUPLICATE]
        message:
          type: string

    BatchIngestResponse:
      type: object
      properties:
        accepted:
          type: integer
        duplicates:
          type: integer
        failed:
          type: integer
        results:
          type: array
          items:
            type: object
            properties:
              index:
                type: integer
              eventId:
                type: string
                format: uuid
              status:
                type: string
              error:
                type: string

    # ─── SEARCH ───────────────────────────────────────────────────
    AuditEventPage:
      type: object
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/AuditEventResponse'
        pageToken:
          type: string
          description: Opaque token for next page (Cassandra-native pagination)
          nullable: true
        hasMore:
          type: boolean
        totalEstimate:
          type: integer
          format: int64
          description: Estimated total (not exact due to Cassandra scan)

    # ─── ORGANIZATION ─────────────────────────────────────────────
    Organization:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        slug:
          type: string
        displayName:
          type: string
        plan:
          type: string
        status:
          type: string
        contactEmail:
          type: string
        maxEventsPerMonth:
          type: integer
          format: int64
        retentionDays:
          type: integer
        createdAt:
          type: string
          format: date-time

    CreateOrganizationRequest:
      type: object
      required: [name, contactEmail]
      properties:
        name:
          type: string
          example: "HDFC Bank"
        contactEmail:
          type: string
          format: email
          example: "it-admin@hdfc.com"
        gstNumber:
          type: string
          example: "27AABCH1234J1ZD"

    # ─── APPLICATION ──────────────────────────────────────────────
    Application:
      type: object
      properties:
        id:
          type: string
          format: uuid
        organizationId:
          type: string
          format: uuid
        name:
          type: string
        slug:
          type: string
        description:
          type: string
        environment:
          type: string
        status:
          type: string
        createdAt:
          type: string
          format: date-time

    CreateApplicationRequest:
      type: object
      required: [name, environment]
      properties:
        name:
          type: string
          example: "NetBanking Portal"
        description:
          type: string
          example: "Customer-facing internet banking application"
        environment:
          type: string
          enum: [PRODUCTION, STAGING, DEVELOPMENT]
          default: PRODUCTION
        webhookUrl:
          type: string
          example: "https://alerts.hdfc.com/audithub/webhook"

    # ─── API KEY ──────────────────────────────────────────────────
    CreateApiKeyRequest:
      type: object
      required: [name, keyType]
      properties:
        name:
          type: string
          example: "NetBanking Write Key"
        applicationId:
          type: string
          format: uuid
        keyType:
          type: string
          enum: [WRITE, READ, ADMIN]
        scopes:
          type: array
          items:
            type: string
          example: ["audit:write"]
        expiresAt:
          type: string
          format: date-time

    ApiKeyCreatedResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        keyPrefix:
          type: string
          example: "ah_live_"
        plainTextKey:
          type: string
          description: "SHOWN ONLY ONCE. Store securely."
          example: "ah_live_xK9mP3qR7nL2vB8jT5wY1uA4sE6hG0fD"
        keyType:
          type: string
        scopes:
          type: array
          items:
            type: string
        createdAt:
          type: string
          format: date-time

    # ─── DASHBOARD ────────────────────────────────────────────────
    DashboardStats:
      type: object
      properties:
        totalEventsToday:
          type: integer
          format: int64
        totalEventsThisMonth:
          type: integer
          format: int64
        quotaUsedPercent:
          type: number
          format: double
        criticalEventsToday:
          type: integer
          format: int64
        failedEventsToday:
          type: integer
          format: int64
        activeApplications:
          type: integer
        topActors:
          type: array
          items:
            type: object
            properties:
              userId:
                type: string
              userName:
                type: string
              eventCount:
                type: integer
        topResources:
          type: array
          items:
            type: object
            properties:
              resourceType:
                type: string
              eventCount:
                type: integer
        eventsTrend:
          type: array
          description: Last 30 days daily count
          items:
            type: object
            properties:
              date:
                type: string
                format: date
              count:
                type: integer
              criticalCount:
                type: integer

    # ─── REPORT ───────────────────────────────────────────────────
    GenerateReportRequest:
      type: object
      required: [name, format, filters]
      properties:
        name:
          type: string
          example: "RBI Audit Trail - June 2025"
        templateId:
          type: string
          format: uuid
        format:
          type: string
          enum: [PDF, CSV, XLSX]
        filters:
          type: object
          properties:
            applicationId:
              type: string
              format: uuid
            startTime:
              type: string
              format: date-time
            endTime:
              type: string
              format: date-time
            actorUserId:
              type: string
            resourceType:
              type: string
            actionTypes:
              type: array
              items:
                type: string
            severities:
              type: array
              items:
                type: string
            outcomes:
              type: array
              items:
                type: string
            tags:
              type: array
              items:
                type: string

    GeneratedReport:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        status:
          type: string
          enum: [PENDING, PROCESSING, COMPLETED, FAILED]
        format:
          type: string
        rowCount:
          type: integer
          format: int64
          nullable: true
        fileSizeBytes:
          type: integer
          format: int64
          nullable: true
        downloadUrl:
          type: string
          description: Pre-signed S3 URL, valid for 1 hour
          nullable: true
        errorMessage:
          type: string
          nullable: true
        requestedBy:
          type: string
          format: uuid
        createdAt:
          type: string
          format: date-time
        completedAt:
          type: string
          format: date-time
          nullable: true

    # ─── REPLAY ───────────────────────────────────────────────────
    ReplayRequest:
      type: object
      required: [filters, targetTopic]
      properties:
        filters:
          type: object
          properties:
            applicationId:
              type: string
              format: uuid
            startTime:
              type: string
              format: date-time
            endTime:
              type: string
              format: date-time
            actionTypes:
              type: array
              items:
                type: string
            resourceType:
              type: string
            resourceId:
              type: string
        targetTopic:
          type: string
          example: "hdfc.audit.replay.netbanking"
        reason:
          type: string
          description: Audit reason for the replay request
          example: "Disaster recovery - replaying June 2025 transactions"

    # ─── ERROR ────────────────────────────────────────────────────
    ErrorResponse:
      type: object
      properties:
        code:
          type: string
          example: "QUOTA_EXCEEDED"
        message:
          type: string
          example: "Monthly event quota exceeded. Upgrade your plan."
        details:
          type: object
        timestamp:
          type: string
          format: date-time
        traceId:
          type: string

paths:
  # ═══════════════════════════════════════════════════════════════
  # INGESTION ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /ingest/events:
    post:
      tags: [Ingestion]
      summary: Submit a single audit event
      description: |
        Synchronously validates and enqueues a single audit event.
        Returns 202 Accepted immediately after Kafka publish.
        Idempotent if eventId is provided.
      operationId: ingestEvent
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AuditEventRequest'
            example:
              actor:
                userId: "EMP-12345"
                userEmail: "branch.manager@hdfc.com"
                userName: "Priya Nair"
                ipAddress: "192.168.1.100"
                sessionId: "sess_abc123"
              action:
                type: "UPDATE"
                name: "loan.status.approved"
                description: "Home loan application approved by branch manager"
              resource:
                type: "LoanApplication"
                id: "LOAN-2025-001234"
                name: "Home Loan - Rahul Gupta"
                path: "/api/v1/loans/LOAN-2025-001234"
              changes:
                - fieldName: "status"
                  oldValue: "UNDER_REVIEW"
                  newValue: "APPROVED"
                - fieldName: "approvedAmount"
                  oldValue: null
                  newValue: "5000000"
              outcome: "SUCCESS"
              severity: "HIGH"
              metadata:
                branchCode: "MUM-ANDHERI-001"
                loanOfficerId: "LO-789"
              tags: ["loan", "approval", "home-loan"]
      responses:
        '202':
          description: Event accepted for processing
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/IngestResponse'
        '400':
          description: Invalid event payload
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '401':
          description: Unauthorized
        '429':
          description: Rate limit exceeded
          headers:
            Retry-After:
              schema:
                type: integer
              description: Seconds until rate limit resets
        '507':
          description: Monthly quota exceeded

  /ingest/events/batch:
    post:
      tags: [Ingestion]
      summary: Submit multiple audit events in one request
      description: |
        Batch ingestion for high-throughput scenarios.
        Max 1000 events per batch.
        Partial success is supported — individual failures don't reject the batch.
      operationId: ingestEventBatch
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AuditEventBatchRequest'
      responses:
        '202':
          description: Batch accepted (partial success possible)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BatchIngestResponse'
        '400':
          description: Invalid request
        '401':
          description: Unauthorized
        '429':
          description: Rate limited

  # ═══════════════════════════════════════════════════════════════
  # QUERY ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /events:
    get:
      tags: [Query]
      summary: Search audit events
      description: |
        Search and filter audit events with cursor-based pagination.
        Results are ordered by event_time DESC by default.
        Time range is required to ensure efficient Cassandra queries.
      operationId: searchEvents
      parameters:
        - name: applicationId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter by application
        - name: startTime
          in: query
          required: true
          schema:
            type: string
            format: date-time
          example: "2025-06-01T00:00:00+05:30"
        - name: endTime
          in: query
          required: true
          schema:
            type: string
            format: date-time
          example: "2025-06-13T23:59:59+05:30"
        - name: actorUserId
          in: query
          schema:
            type: string
          description: Filter by actor user ID
        - name: actorUserEmail
          in: query
          schema:
            type: string
        - name: resourceType
          in: query
          schema:
            type: string
          example: "LoanApplication"
        - name: resourceId
          in: query
          schema:
            type: string
          example: "LOAN-2025-001234"
        - name: actionType
          in: query
          schema:
            type: array
            items:
              type: string
          style: form
          explode: false
          example: ["UPDATE", "DELETE"]
        - name: severity
          in: query
          schema:
            type: array
            items:
              type: string
          style: form
          explode: false
        - name: outcome
          in: query
          schema:
            type: array
            items:
              type: string
        - name: tags
          in: query
          schema:
            type: array
            items:
              type: string
          style: form
          explode: false
        - name: pageToken
          in: query
          schema:
            type: string
          description: Opaque pagination token from previous response
        - name: pageSize
          in: query
          schema:
            type: integer
            minimum: 1
            maximum: 500
            default: 50
      responses:
        '200':
          description: Paginated audit events
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuditEventPage'
        '400':
          description: Invalid query parameters

  /events/{eventId}:
    get:
      tags: [Query]
      summary: Get a specific audit event by ID
      operationId: getEvent
      parameters:
        - name: eventId
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: applicationId
          in: query
          required: true
          schema:
            type: string
            format: uuid
        - name: eventTime
          in: query
          required: true
          schema:
            type: string
            format: date-time
          description: Required for efficient Cassandra lookup
      responses:
        '200':
          description: Audit event
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuditEventResponse'
        '404':
          description: Event not found

  /events/entity/{resourceType}/{resourceId}:
    get:
      tags: [Query]
      summary: Get complete history of a specific entity
      description: Returns all audit events for a given resource, ordered by time.
      operationId: getEntityHistory
      parameters:
        - name: resourceType
          in: path
          required: true
          schema:
            type: string
          example: "LoanApplication"
        - name: resourceId
          in: path
          required: true
          schema:
            type: string
          example: "LOAN-2025-001234"
        - name: startTime
          in: query
          schema:
            type: string
            format: date-time
        - name: endTime
          in: query
          schema:
            type: string
            format: date-time
        - name: pageToken
          in: query
          schema:
            type: string
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 50
      responses:
        '200':
          description: Entity audit history
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuditEventPage'

  /events/user/{userId}:
    get:
      tags: [Query]
      summary: Get all audit events for a specific user
      operationId: getUserActivity
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
        - name: startTime
          in: query
          required: true
          schema:
            type: string
            format: date-time
        - name: endTime
          in: query
          required: true
          schema:
            type: string
            format: date-time
        - name: pageToken
          in: query
          schema:
            type: string
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 50
      responses:
        '200':
          description: User activity audit trail
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuditEventPage'

  # ═══════════════════════════════════════════════════════════════
  # DASHBOARD ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /dashboard/stats:
    get:
      tags: [Dashboard]
      summary: Get dashboard statistics
      operationId: getDashboardStats
      parameters:
        - name: applicationId
          in: query
          schema:
            type: string
            format: uuid
          description: Filter to specific application. Null = all apps.
      responses:
        '200':
          description: Dashboard statistics
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DashboardStats'

  /dashboard/trends:
    get:
      tags: [Dashboard]
      summary: Get event trends over time
      operationId: getEventTrends
      parameters:
        - name: applicationId
          in: query
          schema:
            type: string
            format: uuid
        - name: granularity
          in: query
          schema:
            type: string
            enum: [HOUR, DAY, WEEK, MONTH]
            default: DAY
        - name: startTime
          in: query
          required: true
          schema:
            type: string
            format: date-time
        - name: endTime
          in: query
          required: true
          schema:
            type: string
            format: date-time
      responses:
        '200':
          description: Trend data
          content:
            application/json:
              schema:
                type: object
                properties:
                  dataPoints:
                    type: array
                    items:
                      type: object
                      properties:
                        timestamp:
                          type: string
                          format: date-time
                        totalCount:
                          type: integer
                        successCount:
                          type: integer
                        failureCount:
                          type: integer
                        criticalCount:
                          type: integer

  # ═══════════════════════════════════════════════════════════════
  # REPORT ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /reports:
    post:
      tags: [Reports]
      summary: Generate a compliance report
      description: Async operation. Poll /reports/{id} for status.
      operationId: generateReport
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GenerateReportRequest'
      responses:
        '202':
          description: Report generation started
          headers:
            Location:
              description: URL to poll for status
              schema:
                type: string
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GeneratedReport'
    get:
      tags: [Reports]
      summary: List generated reports
      operationId: listReports
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
        - name: status
          in: query
          schema:
            type: string
      responses:
        '200':
          description: List of reports
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/GeneratedReport'
                  totalElements:
                    type: integer
                  totalPages:
                    type: integer

  /reports/{reportId}:
    get:
      tags: [Reports]
      summary: Get report status and download link
      operationId: getReport
      parameters:
        - name: reportId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Report details with download URL when completed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GeneratedReport'

  # ═══════════════════════════════════════════════════════════════
  # REPLAY ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /replay:
    post:
      tags: [Replay]
      summary: Replay audit events to a Kafka topic
      description: |
        Re-publishes matching audit events to a specified Kafka topic.
        The replay itself is logged as a CRITICAL audit event.
        Requires ADMIN role.
      operationId: replayEvents
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ReplayRequest'
      responses:
        '202':
          description: Replay job started
          content:
            application/json:
              schema:
                type: object
                properties:
                  replayId:
                    type: string
                    format: uuid
                  status:
                    type: string
                  estimatedEvents:
                    type: integer

  # ═══════════════════════════════════════════════════════════════
  # ORGANIZATION ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /organizations:
    post:
      tags: [Organizations]
      summary: Create an organization (sign up)
      operationId: createOrganization
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrganizationRequest'
      responses:
        '201':
          description: Organization created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Organization'

  /organizations/me:
    get:
      tags: [Organizations]
      summary: Get current organization details
      operationId: getCurrentOrganization
      responses:
        '200':
          description: Organization
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Organization'
    patch:
      tags: [Organizations]
      summary: Update organization settings
      operationId: updateOrganization
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                displayName:
                  type: string
                contactEmail:
                  type: string
                gstNumber:
                  type: string
      responses:
        '200':
          description: Updated organization
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Organization'

  /organizations/me/usage:
    get:
      tags: [Organizations]
      summary: Get current usage and quota
      operationId: getUsage
      responses:
        '200':
          description: Usage stats
          content:
            application/json:
              schema:
                type: object
                properties:
                  plan:
                    type: string
                  currentPeriodStart:
                    type: string
                    format: date-time
                  currentPeriodEnd:
                    type: string
                    format: date-time
                  eventsUsed:
                    type: integer
                    format: int64
                  eventsLimit:
                    type: integer
                    format: int64
                  usagePercent:
                    type: number
                  storageUsedBytes:
                    type: integer
                    format: int64
                  retentionDays:
                    type: integer

  # ═══════════════════════════════════════════════════════════════
  # APPLICATION ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /applications:
    post:
      tags: [Applications]
      summary: Create an application
      operationId: createApplication
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateApplicationRequest'
      responses:
        '201':
          description: Application created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Application'
    get:
      tags: [Applications]
      summary: List all applications
      operationId: listApplications
      responses:
        '200':
          description: List of applications
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Application'

  /applications/{applicationId}:
    get:
      tags: [Applications]
      summary: Get application details
      operationId: getApplication
      parameters:
        - name: applicationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Application
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Application'
    patch:
      tags: [Applications]
      summary: Update application
      operationId: updateApplication
      parameters:
        - name: applicationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                name:
                  type: string
                description:
                  type: string
                webhookUrl:
                  type: string
      responses:
        '200':
          description: Updated application
    delete:
      tags: [Applications]
      summary: Delete application
      operationId: deleteApplication
      parameters:
        - name: applicationId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Deleted

  # ═══════════════════════════════════════════════════════════════
  # API KEY ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /api-keys:
    post:
      tags: [API Keys]
      summary: Create a new API key
      operationId: createApiKey
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateApiKeyRequest'
      responses:
        '201':
          description: API key created. Plain text shown ONCE.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiKeyCreatedResponse'
    get:
      tags: [API Keys]
      summary: List API keys
      operationId: listApiKeys
      responses:
        '200':
          description: List of API keys (plain text never returned)
          content:
            application/json:
              schema:
                type: array
                items:
                  type: object
                  properties:
                    id:
                      type: string
                      format: uuid
                    name:
                      type: string
                    keyPrefix:
                      type: string
                    keyType:
                      type: string
                    isActive:
                      type: boolean
                    lastUsedAt:
                      type: string
                      format: date-time
                    expiresAt:
                      type: string
                      format: date-time
                      nullable: true

  /api-keys/{keyId}:
    delete:
      tags: [API Keys]
      summary: Revoke an API key
      operationId: revokeApiKey
      parameters:
        - name: keyId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Key revoked

  # ═══════════════════════════════════════════════════════════════
  # AUTH ENDPOINTS
  # ═══════════════════════════════════════════════════════════════

  /auth/login:
    post:
      tags: [Auth]
      summary: Login with email/password
      operationId: login
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [email, password]
              properties:
                email:
                  type: string
                  format: email
                password:
                  type: string
                  format: password
                mfaCode:
                  type: string
                  description: 6-digit TOTP code if MFA enabled
      responses:
        '200':
          description: Login successful
          content:
            application/json:
              schema:
                type: object
                properties:
                  accessToken:
                    type: string
                    description: JWT, expires in 15 minutes
                  refreshToken:
                    type: string
                    description: Opaque, expires in 30 days
                  expiresIn:
                    type: integer
                    example: 900
                  user:
                    type: object
                    properties:
                      id:
                        type: string
                      name:
                        type: string
                      email:
                        type: string
                      role:
                        type: string
        '401':
          description: Invalid credentials
        '202':
          description: MFA code required
          content:
            application/json:
              schema:
                type: object
                properties:
                  mfaRequired:
                    type: boolean
                  mfaToken:
                    type: string

  /auth/refresh:
    post:
      tags: [Auth]
      summary: Refresh access token
      operationId: refreshToken
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [refreshToken]
              properties:
                refreshToken:
                  type: string
      responses:
        '200':
          description: New access token
          content:
            application/json:
              schema:
                type: object
                properties:
                  accessToken:
                    type: string
                  expiresIn:
                    type: integer

  /auth/logout:
    post:
      tags: [Auth]
      summary: Logout and revoke session
      operationId: logout
      responses:
        '204':
          description: Logged out

  /auth/saml/initiate:
    get:
      tags: [Auth]
      summary: Initiate SAML SSO login
      operationId: initiateSaml
      security: []
      parameters:
        - name: orgSlug
          in: query
          required: true
          schema:
            type: string
      responses:
        '302':
          description: Redirect to IdP
```
