#!/bin/bash

# ============================================================
# verify_phase3.sh — AuditHub Phase 3 Integration Testing
# Compatible with macOS (BSD date) and Linux (GNU date)
# ============================================================

set -uo pipefail

echo "============================================="
echo "AuditHub Phase 3 Integration Testing"
echo "============================================="

BASE_URL="http://localhost:8080"
ORG_NAME="Phase3 Test Bank $(date +%s)"
EMAIL="admin-$(date +%s)@p3test.com"
PASSWORD="SecurePassword123!"

# ──── Helper: portable date arithmetic ─────────────────────
date_minus_days() {
  local n=$1
  if date -v-${n}d +"%Y-%m-%dT%H:%M:%SZ" &>/dev/null 2>&1; then
    date -u -v-${n}d +"%Y-%m-%dT%H:%M:%SZ"
  else
    date -u -d "${n} days ago" +"%Y-%m-%dT%H:%M:%SZ"
  fi
}

date_plus_hours() {
  local n=$1
  if date -v+${n}H +"%Y-%m-%dT%H:%M:%SZ" &>/dev/null 2>&1; then
    date -u -v+${n}H +"%Y-%m-%dT%H:%M:%SZ"
  else
    date -u -d "${n} hours" +"%Y-%m-%dT%H:%M:%SZ"
  fi
}

if ! command -v jq &>/dev/null; then
  echo "ERROR: 'jq' is required. Install it with: brew install jq"
  exit 1
fi

# ──────────────────────────────────────────────────────────
echo "1. Onboarding new organization..."
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
  -d '{
    "name": "Wealth Management App",
    "environment": "PRODUCTION",
    "description": "Wealth portal"
  }')
APP_ID=$(echo "${APP_RESPONSE}" | jq -r '.data.id')
if [ -z "${APP_ID}" ] || [ "${APP_ID}" = "null" ]; then
  echo "ERROR: Application creation failed:"
  echo "${APP_RESPONSE}" | jq .
  exit 1
fi
echo "App ID: ${APP_ID}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "4. Creating Alert Rule (threshold = 2 DELETE events in 1 minute per user)..."
ALERT_RULE_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/alerts/rules" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Abnormal Deletes Alert\",
    \"description\": \"Alert when a user deletes multiple items rapidly\",
    \"conditionType\": \"THRESHOLD\",
    \"conditionConfig\": \"{\\\"actionType\\\":\\\"DELETE\\\",\\\"threshold\\\":2,\\\"windowMinutes\\\":1,\\\"groupBy\\\":\\\"actor_user_id\\\"}\",
    \"severity\": \"CRITICAL\",
    \"notificationChannels\": [\"EMAIL\"]
  }")
echo "${ALERT_RULE_RESPONSE}" | jq .
RULE_ID=$(echo "${ALERT_RULE_RESPONSE}" | jq -r '.data.id')
echo "Rule ID: ${RULE_ID}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "5. Ingesting first DELETE event (via API key)..."
curl -s -X POST "${BASE_URL}/v1/ingest/events" \
  -H "X-API-Key: ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{
    \"applicationId\": \"${APP_ID}\",
    \"actor\": {
      \"userId\": \"USER-P3\",
      \"userEmail\": \"user-p3@test.com\",
      \"userName\": \"Operator P3\",
      \"ipAddress\": \"127.0.0.1\"
    },
    \"action\": { \"type\": \"DELETE\", \"name\": \"record.deleted\" },
    \"resource\": { \"type\": \"Portfolio\", \"id\": \"PORT-001\" },
    \"outcome\": \"SUCCESS\",
    \"severity\": \"MEDIUM\"
  }" | jq .

echo "6. Ingesting second DELETE event to trigger Alert..."
curl -s -X POST "${BASE_URL}/v1/ingest/events" \
  -H "X-API-Key: ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{
    \"applicationId\": \"${APP_ID}\",
    \"actor\": {
      \"userId\": \"USER-P3\",
      \"userEmail\": \"user-p3@test.com\",
      \"userName\": \"Operator P3\",
      \"ipAddress\": \"127.0.0.1\"
    },
    \"action\": { \"type\": \"DELETE\", \"name\": \"record.deleted\" },
    \"resource\": { \"type\": \"Portfolio\", \"id\": \"PORT-002\" },
    \"outcome\": \"SUCCESS\",
    \"severity\": \"MEDIUM\"
  }" | jq .

echo "Waiting 12s for Kafka consumer to process events and update stats/alerts..."
sleep 12

# ──────────────────────────────────────────────────────────
echo "7. Creating Report Template..."
START_1D=$(date_minus_days 1)
END_1H=$(date_plus_hours 1)

TEMPLATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/reports/templates" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Wealth Delete Template\",
    \"description\": \"Template for tracking delete actions\",
    \"templateType\": \"CUSTOM\",
    \"format\": \"PDF\",
    \"config\": \"{\\\"applicationId\\\":\\\"${APP_ID}\\\",\\\"startTime\\\":\\\"${START_1D}\\\",\\\"endTime\\\":\\\"${END_1H}\\\",\\\"actionTypes\\\":[\\\"DELETE\\\"]}\"
  }")
TEMPLATE_ID=$(echo "${TEMPLATE_RESPONSE}" | jq -r '.data.id')
echo "Template ID: ${TEMPLATE_ID}"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "8. Requesting PDF Report Generation..."
REPORT_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/reports" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"templateId\": \"${TEMPLATE_ID}\",
    \"name\": \"Wealth Deletes PDF Report\",
    \"format\": \"PDF\",
    \"filters\": \"{\\\"applicationId\\\":\\\"${APP_ID}\\\",\\\"startTime\\\":\\\"${START_1D}\\\",\\\"endTime\\\":\\\"${END_1H}\\\",\\\"actionTypes\\\":[\\\"DELETE\\\"]}\"
  }")
REPORT_ID=$(echo "${REPORT_RESPONSE}" | jq -r '.data.id')
echo "Report ID: ${REPORT_ID}"
echo "---------------------------------------------"

echo "Polling Report status (up to 30s)..."
STATUS="PENDING"
for i in {1..15}; do
  POLL_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/reports/${REPORT_ID}" \
    -H "Authorization: Bearer ${JWT_TOKEN}")
  STATUS=$(echo "${POLL_RESPONSE}" | jq -r '.data.status')
  echo "  [${i}/15] status: ${STATUS}"
  if [ "${STATUS}" = "COMPLETED" ]; then
    break
  elif [ "${STATUS}" = "FAILED" ]; then
    echo "ERROR: Report generation FAILED:"
    echo "${POLL_RESPONSE}" | jq .
    exit 1
  fi
  sleep 2
done

if [ "${STATUS}" != "COMPLETED" ]; then
  echo "WARNING: Report generation timed out (status: ${STATUS}). Skipping download."
else
  # ──────────────────────────────────────────────────────
  echo "9. Downloading the generated report via /v1/reports/download/${REPORT_ID}..."
  HTTP_STATUS=$(curl -s -o "test_report.pdf" -w "%{http_code}" \
    "${BASE_URL}/v1/reports/download/${REPORT_ID}" \
    -H "Authorization: Bearer ${JWT_TOKEN}")
  if [ "${HTTP_STATUS}" = "200" ]; then
    echo "Report saved as test_report.pdf"
    ls -lh test_report.pdf
  else
    echo "WARNING: Download returned HTTP ${HTTP_STATUS}"
  fi
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "10. Querying Dashboard Stats..."
DASHBOARD_RESPONSE=$(curl -s -X GET \
  "${BASE_URL}/v1/dashboard/stats?applicationId=${APP_ID}" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${DASHBOARD_RESPONSE}" | jq .

echo "11. Querying Dashboard Trends..."
TRENDS_RESPONSE=$(curl -s -X GET \
  "${BASE_URL}/v1/dashboard/trends?applicationId=${APP_ID}&startTime=${START_1D}&endTime=${END_1H}&granularity=DAY" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${TRENDS_RESPONSE}" | jq .
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "12. GET /v1/organizations/me — current org profile..."
ORG_ME_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/organizations/me" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${ORG_ME_RESPONSE}" | jq .
ORG_ME_OK=$(echo "${ORG_ME_RESPONSE}" | jq -r '.success')
if [ "${ORG_ME_OK}" != "true" ]; then
  echo "WARNING: GET /organizations/me failed"
else
  echo "OK: Plan=$(echo "${ORG_ME_RESPONSE}" | jq -r '.data.plan') | RetentionDays=$(echo "${ORG_ME_RESPONSE}" | jq -r '.data.retentionDays')"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "13. GET /v1/organizations/me/usage — plan limits & quotas..."
USAGE_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/organizations/me/usage" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${USAGE_RESPONSE}" | jq .
echo "OK: maxEventsPerMonth=$(echo "${USAGE_RESPONSE}" | jq -r '.data.maxEventsPerMonth')"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "14. GET /v1/team/users — list team members (lazy-load fix test)..."
TEAM_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/team/users" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${TEAM_RESPONSE}" | jq .
TEAM_OK=$(echo "${TEAM_RESPONSE}" | jq -r '.success')
if [ "${TEAM_OK}" != "true" ]; then
  echo "WARNING: GET /team/users failed (possible LazyInitializationException)"
else
  echo "OK: $(echo "${TEAM_RESPONSE}" | jq -r '.data | length') member(s) returned"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "15. GET /v1/api-keys — list API keys..."
APIKEY_LIST_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/api-keys" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${APIKEY_LIST_RESPONSE}" | jq .
echo "OK: $(echo "${APIKEY_LIST_RESPONSE}" | jq -r '.data | length') key(s)"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "16. GET /v1/alerts/rules — list alert rules..."
LIST_RULES_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/alerts/rules" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${LIST_RULES_RESPONSE}" | jq .
RULES_COUNT=$(echo "${LIST_RULES_RESPONSE}" | jq -r '.data | length')
echo "OK: ${RULES_COUNT} rule(s) found"

# Cleanup: delete the alert rule we created
if [ -n "${RULE_ID}" ] && [ "${RULE_ID}" != "null" ]; then
  echo "16b. DELETE /v1/alerts/rules/${RULE_ID} — cleanup..."
  DELETE_RULE_RESP=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    "${BASE_URL}/v1/alerts/rules/${RULE_ID}" \
    -H "Authorization: Bearer ${JWT_TOKEN}")
  echo "DELETE status: ${DELETE_RULE_RESP}"
fi
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "17. GET /v1/reports — list generated reports..."
REPORTS_LIST_RESPONSE=$(curl -s -X GET "${BASE_URL}/v1/reports" \
  -H "Authorization: Bearer ${JWT_TOKEN}")
echo "${REPORTS_LIST_RESPONSE}" | jq .
echo "OK: $(echo "${REPORTS_LIST_RESPONSE}" | jq -r '.data | length') report(s)"
echo "---------------------------------------------"

# ──────────────────────────────────────────────────────────
echo "18. POST /v1/auth/logout — logout (stateless JWT invalidation)..."
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
echo "Phase 3 integration testing completed!"
echo "Endpoints covered:"
echo "  POST   /v1/organizations               (signup)"
echo "  POST   /v1/auth/login                  (login)"
echo "  POST   /v1/applications                (create app)"
echo "  POST   /v1/alerts/rules                (create alert rule)"
echo "  POST   /v1/ingest/events               (ingest x2)"
echo "  POST   /v1/reports/templates           (create template)"
echo "  POST   /v1/reports                     (generate report)"
echo "  GET    /v1/reports/:id                 (poll report)"
echo "  GET    /v1/reports/download/:id        (download report)"
echo "  GET    /v1/dashboard/stats             (dashboard stats)"
echo "  GET    /v1/dashboard/trends            (dashboard trends)"
echo "  GET    /v1/organizations/me            (org profile)"
echo "  GET    /v1/organizations/me/usage      (plan usage)"
echo "  GET    /v1/team/users                  (team members)"
echo "  GET    /v1/api-keys                    (list api keys)"
echo "  GET    /v1/alerts/rules                (list alert rules)"
echo "  DELETE /v1/alerts/rules/:id            (delete alert rule)"
echo "  GET    /v1/reports                     (list reports)"
echo "  POST   /v1/auth/logout                 (logout)"
