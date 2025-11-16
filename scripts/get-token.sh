#!/bin/bash

# Script to get JWT token from Keycloak
# Usage: ./get-token.sh [username] [password]

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="${REALM:-wernertest}"
CLIENT_ID="${CLIENT_ID:-wernertest-app}"
CLIENT_SECRET="${CLIENT_SECRET:-your-client-secret-change-in-production}"

USERNAME="${1:-admin}"
PASSWORD="${2:-admin123}"

echo "Getting token for user: $USERNAME"
echo "Keycloak URL: $KEYCLOAK_URL/realms/$REALM"
echo ""

RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD")

# Check if response contains access_token
if echo "$RESPONSE" | grep -q "access_token"; then
  echo "Token obtained successfully!"
  echo ""

  # Extract and display token
  ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
  EXPIRES_IN=$(echo "$RESPONSE" | grep -o '"expires_in":[0-9]*' | cut -d':' -f2)

  echo "Access Token:"
  echo "$ACCESS_TOKEN"
  echo ""
  echo "Expires in: ${EXPIRES_IN}s"
  echo ""

  # Decode JWT payload (middle part)
  PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
  # Add padding if needed
  PADDING=$(( 4 - ${#PAYLOAD} % 4 ))
  if [ $PADDING -ne 4 ]; then
    PAYLOAD="${PAYLOAD}$(printf '%0.s=' $(seq 1 $PADDING))"
  fi

  echo "Token Claims (decoded):"
  echo "$PAYLOAD" | base64 -d 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "$PAYLOAD" | base64 -d 2>/dev/null

  echo ""
  echo "Use this token in Authorization header:"
  echo "Authorization: Bearer $ACCESS_TOKEN"
else
  echo "Failed to get token!"
  echo "Response: $RESPONSE"
  exit 1
fi
