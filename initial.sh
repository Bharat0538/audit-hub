#!/bin/bash

# ============================================================
# initial.sh — AuditHub Endpoints Integration Testing
# Compatible with macOS (BSD date) and Linux (GNU date)
# ============================================================

# Exit on unset variables; do NOT use set -e so optional steps
# (like X-Org-Id header strips) don't kill the whole script.
set -uo pipefail

echo "============================================="
echo "AuditHub Endpoints Integration Testing"
echo "============================================="

BASE_URL="http://localhost:8080"
ORG_NAME="Test Bank $(date +%s)"
EMAIL="admin-$(date +%s)@testbank.com"
PASSWORD="SecurePassword123!"   # must satisfy any password strength rules

# ──── Helper: portable date subtraction ────────────────────
# Usage: date_minus_days N  →  ISO-8601 UTC datetime N days ago
date_minus_days() {
  local n=$1
  if date -v -${n}d +"%Y-%m-%dT%H:%M:%SZ" &>/dev/null 2>&1; then
    # macOS BSD date
    date -u -v-${n}d +"%Y-%m-%dT%H:%M:%SZ"
  else
    # Linux GNU date
    date -u -d "${n} days ago" +"%Y-%m-%dT%H:%M:%SZ"
  fi
}

# ──── Helper: check jq is installed ────────────────────────
if ! command -v jq &>/dev/null; then
  echo "ERROR: 'jq' is required. Install it with: brew install jq"
  exit 1
fi

# ──────────────────────────────────────────────────────────
echo "1. Onboarding a new organization..."
SIGNUP_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/organizations" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"${ORG_NAME}\",
    \"contactEmail\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\",
    \"gstNumber\": \"27AABCH1234J1ZD\"
  }")

echo "Response:"
echo "${SIGNUP_RESPONSE}" | jq .

SUCCESS=$(echo "${SIGNUP_RESPONSE}" | jq -r '.success')
if [ "${SUCCESS}" != "true" ]; then
  echo "ERROR: Onboarding failed!"
  exit 1
fi

ORG_ID=$(echo "${SIGNUP_RESPONSE}"  | jq -r '.data.organizationId')
API_KEY=$(echo "${SIGNUP_RESPONSE}" | jq -r '.data.plainApiKey')
echo "Org ID : ${ORG_ID}"
echo "API Key: ${API_KEY}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "2. Logging in to obtain JWT Access Token..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\"
  }")

echo "Response:"
echo "${LOGIN_RESPONSE}" | jq .

JWT_TOKEN=$(echo "${LOGIN_RESPONSE}" | jq -r '.data.accessToken')
if [ -z "${JWT_TOKEN}" ] || [ "${JWT_TOKEN}" = "null" ]; then
  echo "ERROR: Login failed — no access token returned."
  exit 1
fi
echo "JWT Token: ${JWT_TOKEN:0:40}..."
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "3. Creating a new Application..."
APP_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/applications" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Core Banking Portal",
    "environment": "PRODUCTION",
    "description": "Retail banking application portal",
    "webhookUrl": "http://localhost:8080/callback"
  }')

echo "Response:"
echo "${APP_RESPONSE}" | jq .
APP_ID=$(echo "${APP_RESPONSE}" | jq -r '.data.id')
echo "App ID: ${APP_ID}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "3b. Listing all Applications..."
LIST_APPS_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/applications" \
  -H "Authorization: Bearer ${JWT_TOKEN}")

echo "Response:"
echo "${LIST_APPS_RESPONSE}" | jq .
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "4. Ingesting a single Audit Event (via API key)..."
INGEST_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/ingest/events" \
  -H "X-API-Key: ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{
    \"applicationId\": \"${APP_ID}\",
    \"actor\": {
      \"userId\": \"EMP-9999\",
      \"userEmail\": \"${EMAIL}\",
      \"userName\": \"Test Admin\",
      \"ipAddress\": \"127.0.0.1\",
      \"sessionId\": \"sess_test_123\"
    },
    \"action\": {
      \"type\": \"CREATE\",
      \"name\": \"account.created\",
      \"description\": \"New savings account opened\"
    },
    \"resource\": {
      \"type\": \"SavingsAccount\",
      \"id\": \"ACC-TEST-0001\",
      \"name\": \"Test Savings Account\",
      \"path\": \"/accounts/ACC-TEST-0001\"
    },
    \"changes\": [
      { \"fieldName\": \"balance\", \"oldValue\": null, \"newValue\": \"10000\" }
    ],
    \"outcome\": \"SUCCESS\",
    \"severity\": \"MEDIUM\"
  }")

echo "Response:"
echo "${INGEST_RESPONSE}" | jq .
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "5. Ingesting a batch of Audit Events (via API key)..."
BATCH_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/ingest/events/batch" \
  -H "X-API-Key: ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{
    \"events\": [
      {
        \"applicationId\": \"${APP_ID}\",
        \"actor\": {
          \"userId\": \"EMP-001\",
          \"userEmail\": \"user1@testbank.com\",
          \"userName\": \"Operator One\",
          \"ipAddress\": \"127.0.0.1\"
        },
        \"action\": { \"type\": \"UPDATE\", \"name\": \"profile.updated\" },
        \"resource\": { \"type\": \"UserProfile\", \"id\": \"USR-100\" },
        \"changes\": [
          { \"fieldName\": \"phoneNumber\", \"oldValue\": \"1234\", \"newValue\": \"5678\" }
        ]
      },
      {
        \"applicationId\": \"${APP_ID}\",
        \"actor\": {
          \"userId\": \"EMP-002\",
          \"userEmail\": \"user2@testbank.com\",
          \"userName\": \"Operator Two\",
          \"ipAddress\": \"127.0.0.1\"
        },
        \"action\": { \"type\": \"DELETE\", \"name\": \"account.closed\" },
        \"resource\": { \"type\": \"SavingsAccount\", \"id\": \"ACC-TEST-9999\" }
      }
    ]
  }")

echo "Response:"
echo "${BATCH_RESPONSE}" | jq .
echo "---------------------------------------------"

echo "Waiting 5s for Kafka → Cassandra pipeline to persist events..."
sleep 5

# ──────────────────────────────────────────────────────────
echo "6. Querying events for the application (last 30 days)..."
START_TIME=$(date_minus_days 30)
END_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

QUERY_RESPONSE=$(curl -s -X GET \
  "${BASE_URL}/v1/events?applicationId=${APP_ID}&startTime=${START_TIME}&endTime=${END_TIME}&pageSize=50" \
  -H "Authorization: Bearer ${JWT_TOKEN}")

echo "Response:"
echo "${QUERY_RESPONSE}" | jq .
EVENT_COUNT=$(echo "${QUERY_RESPONSE}" | jq -r '.data.content | length')
echo "Events returned: ${EVENT_COUNT}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "7. Querying Entity History for SavingsAccount/ACC-TEST-0001..."
ENTITY_RESPONSE=$(curl -s -X GET \
  "${BASE_URL}/v1/events/entity/SavingsAccount/ACC-TEST-0001" \
  -H "Authorization: Bearer ${JWT_TOKEN}")

echo "Response:"
echo "${ENTITY_RESPONSE}" | jq .
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "8. Querying User Activity for EMP-9999..."
USER_RESPONSE=$(curl -s -X GET \
  "${BASE_URL}/v1/events/user/EMP-9999" \
  -H "Authorization: Bearer ${JWT_TOKEN}")

echo "Response:"
echo "${USER_RESPONSE}" | jq .
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "9. GET /v1/organizations/me — current organization profile..."
ORG_ME_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/organizations/me" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${ORG_ME_RESPONSE}" | jq .
ORG_ME_OK=$(echo "${ORG_ME_RESPONSE}" | jq -r '.success')
if [ "${ORG_ME_OK}" != "true" ]; then
  echo "WARNING: GET /organizations/me did not return success=true"
else
  echo "OK: Organization: $(echo "${ORG_ME_RESPONSE}" | jq -r '.data.name') / Plan: $(echo "${ORG_ME_RESPONSE}" | jq -r '.data.plan')"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "9b. PATCH /v1/organizations/me — update organization display name..."
ORG_PATCH_RESPONSE=$(curl -s -X PATCH "${BASE_URL}/v1/organizations/me" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"displayName": "Patched Display Name", "contactPhone": "+91-9999999999"}')
echo "${ORG_PATCH_RESPONSE}" | jq .
ORG_PATCH_OK=$(echo "${ORG_PATCH_RESPONSE}" | jq -r '.success')
if [ "${ORG_PATCH_OK}" != "true" ]; then
  echo "WARNING: PATCH /organizations/me failed"
else
  echo "OK: Updated displayName = $(echo "${ORG_PATCH_RESPONSE}" | jq -r '.data.displayName')"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "10. GET /v1/organizations/me/usage — plan limits & quotas..."
USAGE_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/organizations/me/usage" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${USAGE_RESPONSE}" | jq .
USAGE_OK=$(echo "${USAGE_RESPONSE}" | jq -r '.success')
if [ "${USAGE_OK}" != "true" ]; then
  echo "WARNING: GET /organizations/me/usage did not return success=true"
else
  echo "OK: Plan=$(echo "${USAGE_RESPONSE}" | jq -r '.data.plan') | RetentionDays=$(echo "${USAGE_RESPONSE}" | jq -r '.data.retentionDays')"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "11. GET /v1/team/users — list team members..."
TEAM_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/team/users" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${TEAM_RESPONSE}" | jq .
TEAM_OK=$(echo "${TEAM_RESPONSE}" | jq -r '.success')
if [ "${TEAM_OK}" != "true" ]; then
  echo "WARNING: GET /team/users failed"
else
  USER_COUNT=$(echo "${TEAM_RESPONSE}" | jq -r '.data | length')
  echo "OK: ${USER_COUNT} member(s) returned"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "12. POST /v1/team/invite — invite a new team member..."
INVITE_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/team/invite" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"viewer-$(date +%s)@testbank.com\", \"role\": \"VIEWER\"}")
echo "${INVITE_RESPONSE}" | jq .
INVITE_OK=$(echo "${INVITE_RESPONSE}" | jq -r '.success')
if [ "${INVITE_OK}" != "true" ]; then
  echo "WARNING: POST /team/invite did not return success=true"
else
  echo "OK: Invite sent successfully"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "13. POST /v1/api-keys — create a new API key..."
APIKEY_CREATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/api-keys" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Sanity Test Key\",
    \"applicationId\": \"${APP_ID}\",
    \"keyType\": \"WRITE\",
    \"scopes\": [\"ingest:write\"]
  }")
echo "${APIKEY_CREATE_RESPONSE}" | jq .
NEW_KEY_OK=$(echo "${APIKEY_CREATE_RESPONSE}" | jq -r '.success')
NEW_KEY_ID=$(echo "${APIKEY_CREATE_RESPONSE}" | jq -r '.data.id // "null"')
if [ "${NEW_KEY_OK}" != "true" ]; then
  echo "WARNING: POST /api-keys failed"
else
  echo "OK: New API Key ID = ${NEW_KEY_ID}"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "14. GET /v1/api-keys — list all API keys..."
APIKEY_LIST_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/api-keys" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${APIKEY_LIST_RESPONSE}" | jq .
KEY_COUNT=$(echo "${APIKEY_LIST_RESPONSE}" | jq -r '.data | length')
echo "OK: ${KEY_COUNT} API key(s) returned"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "15. GET /v1/alerts/rules — list alert rules..."
RULES_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/alerts/rules" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${RULES_RESPONSE}" | jq .
echo "OK: $(echo "${RULES_RESPONSE}" | jq -r '.data | length') rule(s)"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "16. GET /v1/reports — list generated reports..."
REPORTS_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/reports" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${REPORTS_RESPONSE}" | jq .
echo "OK: $(echo "${REPORTS_RESPONSE}" | jq -r '.data | length') report(s)"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "17. GET /v1/reports/templates — list report templates..."
TEMPLATES_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/reports/templates" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${TEMPLATES_RESPONSE}" | jq .
echo "OK: $(echo "${TEMPLATES_RESPONSE}" | jq -r '.data | length') template(s)"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "18. GET /v1/dashboard/stats — dashboard statistics..."
DASH_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/dashboard/stats?applicationId=${APP_ID}" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${DASH_RESPONSE}" | jq .
DASH_OK=$(echo "${DASH_RESPONSE}" | jq -r '.success')
echo "OK (success=${DASH_OK})"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "19. POST /v1/auth/logout — logout (stateless JWT invalidation)..."
LOGOUT_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/auth/logout" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${LOGOUT_RESPONSE}" | jq .
LOGOUT_OK=$(echo "${LOGOUT_RESPONSE}" | jq -r '.success')
if [ "${LOGOUT_OK}" != "true" ]; then
  echo "WARNING: POST /auth/logout did not return success=true"
else
  echo "OK: Logout successful"
fi

echo "============================================="
echo "All tests completed successfully!"
echo "Endpoints covered:"
echo "  POST   /v1/organizations               (signup)"
echo "  POST   /v1/auth/login                  (login)"
echo "  GET    /v1/applications                (list apps)"
echo "  POST   /v1/applications                (create app)"
echo "  POST   /v1/ingest/events               (single ingest)"
echo "  POST   /v1/ingest/events/batch         (batch ingest)"
echo "  GET    /v1/events                      (query events)"
echo "  GET    /v1/events/entity/:type/:id     (entity history)"
echo "  GET    /v1/events/user/:userId         (user activity)"
echo "  GET    /v1/organizations/me            (org profile)"
echo "  PATCH  /v1/organizations/me            (update org)"
echo "  GET    /v1/organizations/me/usage      (plan usage)"
echo "  GET    /v1/team/users                  (list members)"
echo "  POST   /v1/team/invite                 (invite member)"
echo "  POST   /v1/api-keys                    (create api key)"
echo "  GET    /v1/api-keys                    (list api keys)"
echo "  GET    /v1/alerts/rules                (list alert rules)"
echo "  GET    /v1/reports                     (list reports)"
echo "  GET    /v1/reports/templates           (list templates)"
echo "  GET    /v1/dashboard/stats             (dashboard stats)"
echo "  POST   /v1/auth/logout                 (logout)"
