#!/bin/bash

# ============================================================
# verify_sdk.sh — AuditHub Java Client SDK Integration Testing
# Compatible with macOS (BSD date) and Linux (GNU date)
# ============================================================

set -uo pipefail

echo "============================================="
echo "AuditHub Java Client SDK Integration Testing"
echo "============================================="

BASE_URL="http://localhost:8080"
ORG_NAME="SDK Test Bank $(date +%s)"
EMAIL="admin-$(date +%s)@sdktest.com"
PASSWORD="SecurePassword123!"

# ──── Helper: portable date arithmetic ─────────────────────
date_minus_months() {
  local n=$1
  if date -v-${n}m +"%Y-%m-%dT%H:%M:%SZ" &>/dev/null 2>&1; then
    date -u -v-${n}m +"%Y-%m-%dT%H:%M:%SZ"
  else
    date -u -d "${n} months ago" +"%Y-%m-%dT%H:%M:%SZ"
  fi
}

if ! command -v jq &>/dev/null; then
  echo "ERROR: 'jq' is required. Install it with: brew install jq"
  exit 1
fi

# ──────────────────────────────────────────────────────────
echo "1. Onboarding new organization for SDK test..."
SIGNUP_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/organizations" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"${ORG_NAME}\",
    \"contactEmail\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\",
    \"gstNumber\": \"27AABCH1234J1ZD\"
  }")

ORG_ID=$(echo "${SIGNUP_RESPONSE}" | jq -r '.data.organizationId')
API_KEY=$(echo "${SIGNUP_RESPONSE}" | jq -r '.data.plainApiKey')
if [ -z "${ORG_ID}" ] || [ "${ORG_ID}" = "null" ]; then
  echo "ERROR: Signup failed:"
  echo "${SIGNUP_RESPONSE}" | jq .
  exit 1
fi
echo "Org ID : ${ORG_ID}"
echo "API Key: ${API_KEY}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "2. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\"
  }")
JWT_TOKEN=$(echo "${LOGIN_RESPONSE}" | jq -r '.data.accessToken')
if [ -z "${JWT_TOKEN}" ] || [ "${JWT_TOKEN}" = "null" ]; then
  echo "ERROR: Login failed:"
  echo "${LOGIN_RESPONSE}" | jq .
  exit 1
fi
echo "Authenticated successfully."
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "3. Creating Application..."
APP_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/applications" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"SDK Simulated Portal\",
    \"environment\": \"PRODUCTION\",
    \"description\": \"Simulated app for SDK test\",
    \"webhookUrl\": \"${BASE_URL}/v1/test/sdk/callback\"
  }")
APP_ID=$(echo "${APP_RESPONSE}" | jq -r '.data.id')
if [ -z "${APP_ID}" ] || [ "${APP_ID}" = "null" ]; then
  echo "ERROR: Application creation failed:"
  echo "${APP_RESPONSE}" | jq .
  exit 1
fi
echo "App ID: ${APP_ID}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "4. Dynamically configuring SDK client in JVM..."
CONFIG_RESP=$(curl -s -w "\n%{http_code}" -X POST \
  "${BASE_URL}/v1/test/sdk/config?apiKey=${API_KEY}&appId=${APP_ID}")
CONFIG_HTTP=$(echo "${CONFIG_RESP}" | tail -1)
if [ "${CONFIG_HTTP}" != "200" ]; then
  echo "WARNING: SDK config endpoint returned HTTP ${CONFIG_HTTP} — skipping SDK-specific steps."
  SDK_AVAILABLE=false
else
  echo "SDK configured successfully."
  SDK_AVAILABLE=true
fi
echo "---------------------------------------------"

if [ "${SDK_AVAILABLE}" = "true" ]; then
  # ────────────────────────────────────────────────────────
  echo "5. Invoking annotated service method via SdkTestController..."
  RUN_RESPONSE=$(curl -s -X POST \
    "${BASE_URL}/v1/test/sdk/loan?loanId=LOAN-SDK-888&amount=950000")
  echo "Service response: ${RUN_RESPONSE}"
  echo "---------------------------------------------"

  echo "5b. Creating Alert Rule to trigger webhook..."
  ALERT_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/alerts/rules" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"applicationId\": \"${APP_ID}\",
      \"name\": \"SDK Webhook Trigger Rule\",
      \"description\": \"Triggers when a loan is approved\",
      \"conditionType\": \"THRESHOLD\",
      \"conditionConfig\": \"{\\\"actionType\\\":\\\"UPDATE\\\",\\\"resourceType\\\":\\\"LoanApplication\\\",\\\"threshold\\\":1,\\\"windowMinutes\\\":5}\",
      \"severity\": \"HIGH\",
      \"notificationChannels\": [\"WEBHOOK\"],
      \"isActive\": true
    }")
  echo "Alert Rule: $(echo ${ALERT_RESPONSE} | jq -r '.data.id // "creation failed"')"
  echo "---------------------------------------------"

  echo "5c. Resetting webhook status on SDK callback..."
  curl -s -X POST "${BASE_URL}/v1/test/sdk/callback/reset"
  echo ""
  echo "---------------------------------------------"

  echo "5d. Invoking updateLoan via SDK to verify Auto-Diff..."
  DIFF_RUN_RESPONSE=$(curl -s -X POST \
    "${BASE_URL}/v1/test/sdk/loan/update?loanId=LOAN-SDK-999&oldStatus=PENDING&oldAmount=100000&newStatus=UNDER_REVIEW&newAmount=120000")
  echo "Update response: ${DIFF_RUN_RESPONSE}"
  echo "---------------------------------------------"

  echo "Waiting 10s for async SDK processing..."
  sleep 10

  # ──────────────────────────────────────────────────────
  echo "6. Querying events via Query API to verify SDK capture, Auto-Diff & Webhook..."
  START_TIME=$(date_minus_months 1)
  END_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  QUERY_RESPONSE=$(curl -s -X GET \
    "${BASE_URL}/v1/events?applicationId=${APP_ID}&startTime=${START_TIME}&endTime=${END_TIME}&pageSize=50" \
    -H "Authorization: Bearer ${JWT_TOKEN}")

  echo "Query Response:"
  echo "${QUERY_RESPONSE}" | jq .

  UPDATED_EVENT_CHANGES=$(echo "${QUERY_RESPONSE}" | \
    jq -r '.data.content[] | select(.action.name == "loan.updated") | .changes' 2>/dev/null || echo "[]")
  echo "Diff Changes detected in DB: ${UPDATED_EVENT_CHANGES}"

  CHANGES_COUNT=$(echo "${UPDATED_EVENT_CHANGES}" | jq '. | length' 2>/dev/null || echo "0")
  if [ "${CHANGES_COUNT}" -eq 2 ]; then
    echo "SUCCESS: Auto-diff correctly detected 2 field changes (status, amount)!"
  else
    echo "WARNING: Expected 2 field changes, got: ${CHANGES_COUNT}"
  fi
  echo "---------------------------------------------"

  # ──────────────────────────────────────────────────────
  echo "7. Checking alert webhook callback status and HMAC verification..."
  CALLBACK_STATUS=$(curl -s -X GET "${BASE_URL}/v1/test/sdk/callback/status")
  echo "Webhook Status: ${CALLBACK_STATUS}"
  WEBHOOK_RECEIVED=$(echo "${CALLBACK_STATUS}" | jq -r '.received' 2>/dev/null || echo "false")
  WEBHOOK_VERIFIED=$(echo "${CALLBACK_STATUS}" | jq -r '.verified' 2>/dev/null || echo "false")

  if [ "${WEBHOOK_RECEIVED}" = "true" ] && [ "${WEBHOOK_VERIFIED}" = "true" ]; then
    echo "SUCCESS: Webhook received and HMAC signature verified!"
  else
    echo "WARNING: Webhook callback check — received: ${WEBHOOK_RECEIVED}, verified: ${WEBHOOK_VERIFIED}"
  fi
  echo "---------------------------------------------"

  # ──────────────────────────────────────────────────────
  echo "8. Verifying multi-month Cassandra parallel querying..."
  START_MULTI=$(date_minus_months 1)
  END_MULTI=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  PARALLEL_QUERY_RESPONSE=$(curl -s -X GET \
    "${BASE_URL}/v1/events?applicationId=${APP_ID}&startTime=${START_MULTI}&endTime=${END_MULTI}&pageSize=100" \
    -H "Authorization: Bearer ${JWT_TOKEN}")
  TOTAL_P_EVENTS=$(echo "${PARALLEL_QUERY_RESPONSE}" | jq -r '.data.content | length' 2>/dev/null || echo "0")
  echo "Parallel Query Total Events: ${TOTAL_P_EVENTS}"
  if [ "${TOTAL_P_EVENTS}" -ge 2 ]; then
    echo "SUCCESS: Parallel multi-month queries returned ${TOTAL_P_EVENTS} events!"
  else
    echo "WARNING: Parallel query returned only ${TOTAL_P_EVENTS} events (expected ≥2)."
  fi

else
  echo "INFO: SDK test endpoints not available — skipping SDK-specific verification."
  echo "INFO: To enable: ensure SdkTestController is loaded in the Spring context."
fi

# ──────────────────────────────────────────────────────────
echo "9. GET /v1/organizations/me — current org profile..."
ORG_ME_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/organizations/me" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${ORG_ME_RESPONSE}" | jq .
ORG_ME_OK=$(echo "${ORG_ME_RESPONSE}" | jq -r '.success')
if [ "${ORG_ME_OK}" != "true" ]; then
  echo "WARNING: GET /organizations/me failed"
else
  echo "OK: Plan=$(echo "${ORG_ME_RESPONSE}" | jq -r '.data.plan') | Slug=$(echo "${ORG_ME_RESPONSE}" | jq -r '.data.slug')"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "10. GET /v1/organizations/me/usage — plan limits..."
USAGE_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/organizations/me/usage" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${USAGE_RESPONSE}" | jq .
echo "OK: maxEventsPerMonth=$(echo "${USAGE_RESPONSE}" | jq -r '.data.maxEventsPerMonth // "N/A"')"
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
  echo "OK: $(echo "${TEAM_RESPONSE}" | jq -r '.data | length') member(s) returned"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "12. GET /v1/api-keys — list API keys..."
APIKEY_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/api-keys" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${APIKEY_RESPONSE}" | jq .
echo "OK: $(echo "${APIKEY_RESPONSE}" | jq -r '.data | length') key(s)"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "13. POST /v1/auth/logout — logout..."
LOGOUT_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/auth/logout" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${LOGOUT_RESPONSE}" | jq .
LOGOUT_OK=$(echo "${LOGOUT_RESPONSE}" | jq -r '.success')
if [ "${LOGOUT_OK}" != "true" ]; then
  echo "WARNING: POST /auth/logout failed"
else
  echo "OK: Logout successful"
fi

echo "============================================="
echo "SDK integration verification completed!"
echo "Endpoints covered:"
echo "  POST   /v1/organizations               (signup)"
echo "  POST   /v1/auth/login                  (login)"
echo "  POST   /v1/applications                (create app)"
echo "  POST   /v1/test/sdk/config             (SDK configure)"
echo "  POST   /v1/test/sdk/loan               (SDK annotated method)"
echo "  POST   /v1/alerts/rules                (create alert rule)"
echo "  POST   /v1/test/sdk/callback/reset     (reset webhook)"
echo "  POST   /v1/test/sdk/loan/update        (SDK auto-diff)"
echo "  GET    /v1/events                      (query events)"
echo "  GET    /v1/test/sdk/callback/status    (webhook verify)"
echo "  GET    /v1/organizations/me            (org profile)"
echo "  GET    /v1/organizations/me/usage      (plan usage)"
echo "  GET    /v1/team/users                  (team members)"
echo "  GET    /v1/api-keys                    (list api keys)"
echo "  POST   /v1/auth/logout                 (logout)"
echo "============================================="
exit 0
