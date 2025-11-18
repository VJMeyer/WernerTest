# Authentication & Authorization Guide

## Overview

This file processing system uses **Keycloak** as the identity provider with **JWT (JSON Web Tokens)** for authentication and authorization. The system supports **multi-organization tenancy** where each organization can have its own users and admins.

## Table of Contents

1. [Architecture](#architecture)
2. [User Roles](#user-roles)
3. [Organization Structure](#organization-structure)
4. [Keycloak Configuration](#keycloak-configuration)
5. [Getting Access Tokens](#getting-access-tokens)
6. [Making Authenticated Requests](#making-authenticated-requests)
7. [Managing Users as Organization Admin](#managing-users-as-organization-admin)
8. [Security Features](#security-features)
9. [Troubleshooting](#troubleshooting)

---

## Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────────┐
│   Client    │ ◄─JWT── │   Keycloak   │         │ Upload Service  │
│             │         │  (IdP/AuthZ) │         │  (Resource Srv) │
└─────────────┘         └──────────────┘         └─────────────────┘
       │                                                   │
       │  1. Request Token (username/password)            │
       │ ─────────────────────────────────────────►       │
       │                                                   │
       │  2. Return JWT Token                             │
       │ ◄─────────────────────────────────────────       │
       │                                                   │
       │  3. API Request + JWT in Authorization header    │
       │ ──────────────────────────────────────────────────►
       │                                                   │
       │  4. Validate JWT (signature, expiry, claims)     │
       │                                           ◄───────┤
       │                                                   │
       │  5. Check permissions (organization, role)       │
       │                                           ◄───────┤
       │                                                   │
       │  6. Response (200 OK or 403 Forbidden)           │
       │ ◄──────────────────────────────────────────────────
```

### Key Components

1. **Keycloak Server**: Runs in its own Docker container on port 8180
   - Manages users, roles, and organizations
   - Issues JWT tokens upon successful authentication
   - Provides admin console for user management

2. **Upload Service**: Spring Boot application with OAuth2 Resource Server
   - Validates JWT tokens using Keycloak's public keys
   - Enforces role-based access control (RBAC)
   - Implements organization-based data isolation

3. **JWT Token**: Contains user identity and authorization information
   - Signed by Keycloak (RS256 algorithm)
   - Includes roles, organization ID, email, etc.
   - Expires after 1 hour (configurable)

---

## User Roles

The system defines three main roles:

### 1. **USER**
- **Permissions**:
  - Upload files to their organization
  - View their own upload status
  - Cannot see other users' uploads (even within the same organization)

### 2. **ORG_ADMIN** (Organization Administrator)
- **Permissions**:
  - All USER permissions
  - View all uploads within their organization
  - Create and manage users in their organization via Keycloak
  - Assign roles to users in their organization

### 3. **ADMIN** (System Administrator)
- **Permissions**:
  - View all uploads across all organizations
  - Full access to Keycloak admin console
  - Manage all users and organizations

---

## Organization Structure

Organizations are managed through Keycloak **Groups**. Each organization has:

- **Organization Name**: Human-readable name (e.g., "ACME Corporation")
- **Organization ID**: Machine-readable identifier (e.g., "acme-corp")
- **Sub-groups**:
  - `Admins`: Members with ORG_ADMIN role
  - `Users`: Regular members with USER role

### Pre-configured Organizations

The system comes with two example organizations:

#### 1. ACME Corporation (`acme-corp`)
- **Admin**: `acme.admin` / `password`
  - Name: John Doe
  - Email: john.doe@acme.com
- **User**: `acme.user1` / `password`
  - Name: Alice Smith
  - Email: alice.smith@acme.com

#### 2. TechStart Inc (`techstart-inc`)
- **Admin**: `techstart.admin` / `password`
  - Name: Jane Williams
  - Email: jane.williams@techstart.com
- **User**: `techstart.user1` / `password`
  - Name: Bob Johnson
  - Email: bob.johnson@techstart.com

---

## Keycloak Configuration

### Accessing Keycloak Admin Console

1. **Start the services**:
   ```bash
   docker-compose up -d
   ```

2. **Open Keycloak**:
   - URL: http://localhost:8180
   - Admin username: `admin`
   - Admin password: `admin`

3. **Select the Realm**:
   - Click on the dropdown at the top-left
   - Select **"file-processing"** realm

### Realm Configuration

The `file-processing` realm includes:

- **Clients**:
  - `upload-service`: Backend service (confidential client)
  - `upload-web-app`: Frontend application (public client)

- **Realm Roles**:
  - USER
  - ADMIN
  - ORG_ADMIN

- **Groups** (Organizations):
  - Organizations/ACME Corporation
  - Organizations/TechStart Inc

- **Token Settings**:
  - Access Token Lifespan: 60 minutes
  - Refresh Token Enabled: Yes
  - Token includes custom claims: `organization`, `orgId`, `roles`

---

## Getting Access Tokens

### Method 1: Direct Grant (Password Flow)

**For testing and development only** - not recommended for production.

```bash
curl -X POST http://localhost:8180/realms/file-processing/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=upload-web-app" \
  -d "grant_type=password" \
  -d "username=acme.user1" \
  -d "password=password"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 3600,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

### Method 2: Refresh Token

Use a refresh token to get a new access token:

```bash
curl -X POST http://localhost:8180/realms/file-processing/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=upload-web-app" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=YOUR_REFRESH_TOKEN"
```

### Decoding the JWT Token

You can decode the JWT token at [https://jwt.io](https://jwt.io) to inspect its contents:

```json
{
  "exp": 1700000000,
  "iat": 1699996400,
  "sub": "f1234567-89ab-cdef-0123-456789abcdef",
  "preferred_username": "acme.user1",
  "email": "alice.smith@acme.com",
  "roles": ["USER"],
  "organization": "ACME Corporation",
  "orgId": "acme-corp",
  "groups": [
    "Admins",
    "ACME Corporation"
  ]
}
```

---

## Making Authenticated Requests

### 1. Upload a File

```bash
# Get access token first
TOKEN=$(curl -s -X POST http://localhost:8180/realms/file-processing/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=upload-web-app" \
  -d "grant_type=password" \
  -d "username=acme.user1" \
  -d "password=password" | jq -r '.access_token')

# Upload file with JWT token
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@myfile.csv"
```

Response:
```json
{
  "status": "success",
  "message": "File uploaded successfully",
  "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "filename": "myfile.csv",
  "size": 1024,
  "path": "/app/uploads/...",
  "checksum": "sha256:...",
  "statusUrl": "/api/upload/status/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 2. Check Upload Status

```bash
# Use the uploadId from previous response
curl -X GET http://localhost:8080/api/upload/status/a1b2c3d4-e5f6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer $TOKEN"
```

Response (for regular user):
```json
{
  "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "filename": "myfile.csv",
  "fileSize": 1024,
  "bytesUploaded": 1024,
  "status": "COMPLETED",
  "progressPercentage": "100.00",
  "createdAt": "2025-11-18T10:30:00",
  "completedAt": "2025-11-18T10:30:15",
  "filePath": "/app/uploads/...",
  "checksum": "sha256:...",
  "isComplete": true,
  "isFailed": false,
  "isProcessing": false
}
```

Response (for admin - includes additional fields):
```json
{
  ...
  "uploadedBy": "acme.user1",
  "organization": "ACME Corporation"
}
```

### 3. Access Control Examples

#### Scenario A: User tries to view their own upload ✅
```bash
# acme.user1 views their own upload
curl -H "Authorization: Bearer $TOKEN_USER1" \
  http://localhost:8080/api/upload/status/upload-by-user1

# Response: 200 OK (success)
```

#### Scenario B: User tries to view another user's upload ❌
```bash
# acme.user1 tries to view acme.user2's upload
curl -H "Authorization: Bearer $TOKEN_USER1" \
  http://localhost:8080/api/upload/status/upload-by-user2

# Response: 403 Forbidden
{
  "error": "Access denied",
  "message": "You do not have permission to view this upload"
}
```

#### Scenario C: Org Admin views all uploads in their org ✅
```bash
# acme.admin views any ACME user's upload
curl -H "Authorization: Bearer $TOKEN_ACME_ADMIN" \
  http://localhost:8080/api/upload/status/upload-by-user1

# Response: 200 OK (success)
```

#### Scenario D: Org Admin tries to view another org's upload ❌
```bash
# acme.admin tries to view TechStart upload
curl -H "Authorization: Bearer $TOKEN_ACME_ADMIN" \
  http://localhost:8080/api/upload/status/techstart-upload

# Response: 403 Forbidden
```

#### Scenario E: System Admin views all uploads ✅
```bash
# System admin views uploads from any organization
curl -H "Authorization: Bearer $TOKEN_SYSTEM_ADMIN" \
  http://localhost:8080/api/upload/status/any-upload

# Response: 200 OK (success)
```

---

## Managing Users as Organization Admin

Organization admins can create and manage users in their organization using the Keycloak admin console.

### Step 1: Login to Keycloak

1. Navigate to http://localhost:8180
2. Click **"Administration Console"**
3. Login with organization admin credentials (e.g., `acme.admin` / `password`)

### Step 2: Create a New User

1. Click **"Users"** in the left menu
2. Click **"Create user"** button
3. Fill in user details:
   - Username: `acme.user2`
   - Email: `user2@acme.com`
   - First name: `New`
   - Last name: `User`
   - Email verified: Toggle ON

4. Click **"Create"**

### Step 3: Set User Password

1. Click on the newly created user
2. Go to **"Credentials"** tab
3. Click **"Set password"**
4. Enter password and confirm
5. Set **"Temporary"** to OFF
6. Click **"Save"**

### Step 4: Assign to Organization

1. Go to **"Groups"** tab
2. Click **"Join Group"**
3. Select your organization's Users group:
   - For ACME: `Organizations/ACME Corporation/Users`
   - For TechStart: `Organizations/TechStart Inc/Users`
4. Click **"Join"**

### Step 5: Assign Roles

1. Go to **"Role mappings"** tab
2. Click **"Assign role"**
3. Select **"USER"** role
4. Click **"Assign"**

### Step 6: Set Organization Attributes

1. Go to **"Attributes"** tab
2. Add attributes:
   - Key: `organization`, Value: `ACME Corporation`
   - Key: `orgId`, Value: `acme-corp`
3. Click **"Save"**

### Making a User an Organization Admin

To promote a user to organization admin:

1. Follow steps above but in Step 4, add to **Admins** subgroup instead
2. In Step 5, assign both **"USER"** and **"ORG_ADMIN"** roles

---

## Security Features

### 1. JWT Token Validation

Every request is validated for:
- **Signature**: Token must be signed by Keycloak
- **Expiration**: Token must not be expired
- **Issuer**: Must be from the correct Keycloak realm
- **Audience**: Must be intended for this service

### 2. Role-Based Access Control (RBAC)

Endpoints are protected with `@PreAuthorize` annotations:

```java
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ORG_ADMIN')")
public ResponseEntity<?> uploadFile(...) {
    // Only authenticated users with these roles can access
}
```

### 3. Organization-Based Data Isolation

Upload data includes organization information:

```java
// Stored in Redis
{
  "uploadId": "...",
  "username": "acme.user1",
  "organization": "ACME Corporation",
  "orgId": "acme-corp",
  ...
}
```

Access control logic ensures users only see data from their organization:

```java
private boolean canAccessUpload(UploadStatus upload) {
    JwtUserInfo currentUser = JwtUserInfo.fromSecurityContext();

    // Check organization match
    if (!upload.getOrgId().equals(currentUser.getOrgId())) {
        return false; // Different organization - deny
    }

    // Check user or admin
    return currentUser.isAdmin() ||
           upload.getUsername().equals(currentUser.getUsername());
}
```

### 4. HTTPS in Production

For production deployment:

1. Configure SSL/TLS certificates
2. Update Keycloak issuer URI to use HTTPS
3. Enable `sslRequired: external` in Keycloak realm settings
4. Use secure cookies for web applications

### 5. Token Refresh

Tokens expire after 1 hour. Use refresh tokens to get new access tokens without re-authentication:

```bash
# Use refresh token
curl -X POST http://localhost:8180/realms/file-processing/protocol/openid-connect/token \
  -d "client_id=upload-web-app" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=$REFRESH_TOKEN"
```

---

## Troubleshooting

### Problem: "401 Unauthorized" when making requests

**Cause**: Missing or invalid JWT token

**Solution**:
1. Verify you're including the Authorization header:
   ```
   Authorization: Bearer YOUR_JWT_TOKEN
   ```
2. Check if token has expired (tokens expire after 1 hour)
3. Get a new token using the password or refresh token flow

### Problem: "403 Forbidden" when accessing upload status

**Cause**: User doesn't have permission to view the upload

**Solution**:
1. Check if the upload belongs to your organization
2. Verify you're using the correct upload ID
3. If you're a regular user, you can only see your own uploads
4. Contact your organization admin for access

### Problem: "Invalid token" errors

**Cause**: Token signature validation failed

**Solution**:
1. Verify Keycloak is running: `docker ps | grep keycloak`
2. Check the issuer URI configuration matches Keycloak
3. Ensure Keycloak realm is `file-processing`
4. Check for clock skew between client and server

### Problem: Cannot login to Keycloak admin console

**Cause**: Wrong credentials or Keycloak not ready

**Solution**:
1. Wait for Keycloak to start (check: `docker logs keycloak`)
2. Use admin credentials: `admin` / `admin`
3. Ensure you're accessing http://localhost:8180 (not 8080)

### Problem: User created but can't get token

**Cause**: Missing organization attributes or roles

**Solution**:
1. Check user has required roles assigned (USER, ADMIN, or ORG_ADMIN)
2. Verify user is member of an organization group
3. Ensure organization attributes are set:
   - `organization`: Organization name
   - `orgId`: Organization identifier

### Problem: "Failed to validate token" in logs

**Cause**: Keycloak not reachable from upload-service

**Solution**:
1. Check both services are on same Docker network
2. Verify environment variables in docker-compose.yml:
   ```yaml
   SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/file-processing
   ```
3. Test connectivity: `docker exec upload-service curl http://keycloak:8080`

---

## Best Practices

### 1. Secure Password Management
- Never commit passwords in configuration files
- Use strong passwords for production
- Enable password policies in Keycloak
- Force password reset on first login

### 2. Token Handling
- Store tokens securely (HttpOnly cookies for web apps)
- Never log full JWT tokens
- Implement automatic token refresh
- Clear tokens on logout

### 3. Organization Setup
- Use meaningful organization IDs (e.g., company domain)
- Maintain consistent naming conventions
- Document organization structure
- Regular audit of user access

### 4. Monitoring
- Log authentication attempts
- Monitor failed login attempts
- Track token issuance
- Alert on suspicious patterns

### 5. Production Deployment
- Use external Keycloak database (PostgreSQL)
- Enable HTTPS everywhere
- Configure firewall rules
- Regular security updates
- Backup Keycloak realm configuration

---

## Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [JWT.io - Decode and verify JWT tokens](https://jwt.io)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
