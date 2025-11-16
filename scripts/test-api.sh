#!/bin/bash

# Script to test API endpoints
# Usage: ./test-api.sh

API_URL="${API_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="${REALM:-wernertest}"
CLIENT_ID="${CLIENT_ID:-wernertest-app}"
CLIENT_SECRET="${CLIENT_SECRET:-your-client-secret-change-in-production}"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "JWT Keycloak Authentication API Tests"
echo "========================================="
echo ""

# Function to get token
get_token() {
  local username=$1
  local password=$2

  curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=$CLIENT_ID" \
    -d "client_secret=$CLIENT_SECRET" \
    -d "username=$username" \
    -d "password=$password" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4
}

# Function to make API call
api_call() {
  local method=$1
  local endpoint=$2
  local token=$3
  local data=$4

  if [ -z "$data" ]; then
    curl -s -X $method "$API_URL$endpoint" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json"
  else
    curl -s -X $method "$API_URL$endpoint" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json" \
      -d "$data"
  fi
}

echo -e "${YELLOW}1. Testing Public Endpoints (No Auth)${NC}"
echo "-------------------------------------------"

echo "GET /api/public/info"
curl -s "$API_URL/api/public/info" | python3 -m json.tool 2>/dev/null || curl -s "$API_URL/api/public/info"
echo ""

echo "GET /api/public/health"
curl -s "$API_URL/api/public/health" | python3 -m json.tool 2>/dev/null || curl -s "$API_URL/api/public/health"
echo ""

echo -e "${YELLOW}2. Getting Tokens for Test Users${NC}"
echo "-------------------------------------------"

ADMIN_TOKEN=$(get_token "admin" "admin123")
if [ -z "$ADMIN_TOKEN" ]; then
  echo -e "${RED}Failed to get admin token. Is Keycloak running?${NC}"
  exit 1
fi
echo -e "${GREEN}Admin token obtained${NC}"

ORGADMIN_TOKEN=$(get_token "orgadmin" "orgadmin123")
echo -e "${GREEN}OrgAdmin token obtained${NC}"

USER_TOKEN=$(get_token "testuser" "testuser123")
echo -e "${GREEN}User token obtained${NC}"
echo ""

echo -e "${YELLOW}3. Testing User Endpoints${NC}"
echo "-------------------------------------------"

echo "GET /api/user/me (as testuser)"
api_call "GET" "/api/user/me" "$USER_TOKEN" | python3 -m json.tool 2>/dev/null
echo ""

echo "GET /api/user/profile (as admin)"
api_call "GET" "/api/user/profile" "$ADMIN_TOKEN" | python3 -m json.tool 2>/dev/null
echo ""

echo -e "${YELLOW}4. Testing Organization Admin Endpoints${NC}"
echo "-------------------------------------------"

echo "GET /api/org-admin/users (as orgadmin)"
api_call "GET" "/api/org-admin/users" "$ORGADMIN_TOKEN" | python3 -m json.tool 2>/dev/null
echo ""

echo "POST /api/org-admin/users - Create new user (as orgadmin)"
NEW_USER=$(api_call "POST" "/api/org-admin/users" "$ORGADMIN_TOKEN" '{
  "username": "apitest-user-'$(date +%s)'",
  "email": "apitest'$(date +%s)'@example.com",
  "firstName": "API",
  "lastName": "Test",
  "password": "TestPassword123!",
  "temporaryPassword": true,
  "roles": ["USER"]
}')
echo "$NEW_USER" | python3 -m json.tool 2>/dev/null || echo "$NEW_USER"
echo ""

echo "GET /api/org-admin/audit-logs (as orgadmin)"
api_call "GET" "/api/org-admin/audit-logs?page=0&size=5" "$ORGADMIN_TOKEN" | python3 -m json.tool 2>/dev/null
echo ""

echo -e "${YELLOW}5. Testing Admin Endpoints${NC}"
echo "-------------------------------------------"

echo "GET /api/admin/organizations (as admin)"
api_call "GET" "/api/admin/organizations" "$ADMIN_TOKEN" | python3 -m json.tool 2>/dev/null
echo ""

echo "POST /api/admin/organizations - Create organization (as admin)"
api_call "POST" "/api/admin/organizations" "$ADMIN_TOKEN" '{
  "name": "TestOrg-'$(date +%s)'",
  "description": "Test organization created via API"
}' | python3 -m json.tool 2>/dev/null
echo ""

echo -e "${YELLOW}6. Testing Authorization (Access Denied)${NC}"
echo "-------------------------------------------"

echo "GET /api/admin/organizations (as USER - should fail)"
api_call "GET" "/api/admin/organizations" "$USER_TOKEN" | python3 -m json.tool 2>/dev/null
echo ""

echo "========================================="
echo -e "${GREEN}API Tests Complete!${NC}"
echo "========================================="
