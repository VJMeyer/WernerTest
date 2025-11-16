# JWT Authentication with Keycloak

A complete Spring Boot application demonstrating JWT-based authentication using Keycloak as the Identity Provider. This implementation supports multi-tenancy where organization administrators can manage their own users.

## Features

- **JWT Authentication**: Secure API endpoints using JWT tokens issued by Keycloak
- **Multi-Tenancy**: Organizations are isolated with their own user groups
- **Role-Based Access Control**: Three-tier role system (USER, ORG_ADMIN, ADMIN)
- **User Management**: Organization admins can create, update, delete, and manage users
- **Audit Logging**: All user management actions are logged for compliance
- **Docker Support**: Complete Docker setup for Keycloak and PostgreSQL
- **OpenAPI Documentation**: Swagger UI for API exploration

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Client App    │────▶│  Spring Boot API │────▶│   PostgreSQL    │
│  (Frontend)     │     │  (Resource Server)│     │   (App Data)    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
         │                        │
         │                        │
         ▼                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Keycloak (Docker)                        │
│  ┌───────────────┐    ┌───────────────┐    ┌─────────────────┐ │
│  │   Identity    │    │    Token      │    │   PostgreSQL    │ │
│  │   Provider    │    │   Service     │    │   (Keycloak)    │ │
│  └───────────────┘    └───────────────┘    └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Start Keycloak Infrastructure

```bash
cd docker
docker-compose up -d
```

This starts:
- Keycloak server (http://localhost:8180)
- PostgreSQL for Keycloak (port 5433)
- PostgreSQL for Application (port 5432)
- pgAdmin (http://localhost:5050)

Wait for Keycloak to be healthy (~60 seconds):
```bash
docker-compose logs -f keycloak
```

### 2. Run the Spring Boot Application

```bash
# Using Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or with production settings
./mvnw spring-boot:run
```

Application will be available at: http://localhost:8080

### 3. Access Services

- **Application API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Keycloak Admin Console**: http://localhost:8180/admin (admin/admin)
- **pgAdmin**: http://localhost:5050 (admin@wernertest.com/admin)

## Pre-configured Users

The Keycloak realm comes with test users:

| Username  | Password      | Role      | Organization        |
|-----------|---------------|-----------|---------------------|
| admin     | admin123      | ADMIN     | DefaultOrganization |
| orgadmin  | orgadmin123   | ORG_ADMIN | DefaultOrganization |
| testuser  | testuser123   | USER      | DefaultOrganization |

## API Endpoints

### Public Endpoints (No Authentication)
- `GET /api/public/info` - Application information
- `GET /api/public/health` - Health check

### User Endpoints (Authenticated Users)
- `GET /api/user/me` - Get current user info
- `GET /api/user/profile` - Get user profile

### Organization Admin Endpoints (ORG_ADMIN role)
- `POST /api/org-admin/users` - Create new user
- `GET /api/org-admin/users` - List organization users
- `GET /api/org-admin/users/{userId}` - Get user details
- `PUT /api/org-admin/users/{userId}` - Update user
- `DELETE /api/org-admin/users/{userId}` - Delete user
- `POST /api/org-admin/users/{userId}/reset-password` - Reset password
- `POST /api/org-admin/users/{userId}/enable` - Enable user
- `POST /api/org-admin/users/{userId}/disable` - Disable user
- `GET /api/org-admin/audit-logs` - View audit logs

### System Admin Endpoints (ADMIN role)
- `POST /api/admin/organizations` - Create organization
- `GET /api/admin/organizations` - List organizations
- `GET /api/admin/organizations/{name}` - Get organization
- `PUT /api/admin/organizations/{name}` - Update organization

## Getting a JWT Token

### Using Direct Access Grant (Testing)

```bash
# Get token for admin user
curl -X POST "http://localhost:8180/realms/wernertest/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=wernertest-app" \
  -d "client_secret=your-client-secret-change-in-production" \
  -d "username=admin" \
  -d "password=admin123"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 3600,
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer"
}
```

### Using the Token

```bash
# Call protected endpoint
curl -X GET "http://localhost:8080/api/user/me" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Creating Users (Organization Admin)

```bash
# As an organization admin
curl -X POST "http://localhost:8080/api/org-admin/users" \
  -H "Authorization: Bearer ORG_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "firstName": "New",
    "lastName": "User",
    "password": "Password123!",
    "temporaryPassword": true,
    "roles": ["USER"]
  }'
```

## Configuration

### Application Configuration

Key settings in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/wernertest
          jwk-set-uri: http://localhost:8180/realms/wernertest/protocol/openid-connect/certs

keycloak:
  auth-server-url: http://localhost:8180
  realm: wernertest
  admin:
    realm: master
    username: admin
    password: admin
    client-id: admin-cli
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KEYCLOAK_CLIENT_SECRET` | Client secret for wernertest-app | your-client-secret |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password | admin |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | (none) |

## Role Hierarchy

1. **USER**: Basic authenticated user
   - Can view own profile
   - Access standard application features

2. **ORG_ADMIN**: Organization Administrator (includes USER permissions)
   - Manage users within their organization
   - Create, update, delete organization users
   - Reset user passwords
   - View audit logs for their organization

3. **ADMIN**: System Administrator (includes ORG_ADMIN and USER permissions)
   - Create and manage organizations
   - Full system access
   - Cross-organization management

## Multi-Tenancy Support

Each user belongs to an organization, stored as:
- Keycloak user attribute: `organization`
- Keycloak group membership
- Isolated user management per organization

Organization admins can only manage users within their organization.

## Security Features

- **JWT Validation**: Tokens are validated against Keycloak's JWKS endpoint
- **Role-Based Authorization**: Method-level security using Spring Security
- **CORS Configuration**: Configurable allowed origins
- **CSRF Protection**: Disabled for stateless JWT authentication
- **Brute Force Protection**: Configured in Keycloak realm
- **Audit Logging**: All administrative actions are logged
- **Password Policies**: Enforced through Keycloak

## Production Deployment

### 1. Update Secrets

```bash
# Change all default passwords
# Update keycloak client secret
# Configure proper database credentials
```

### 2. Enable HTTPS

```yaml
keycloak:
  auth-server-url: https://keycloak.yourdomain.com
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.yourdomain.com/realms/wernertest
```

### 3. Use Production Keycloak

```bash
# Instead of start-dev, use:
./kc.sh start --optimized
```

### 4. Full Stack Deployment

```bash
cd docker
docker-compose -f docker-compose.full.yml up -d
```

## Keycloak Admin Tasks

### Create New Organization

1. Login to Keycloak Admin Console
2. Navigate to Groups
3. Create a new group with organization name
4. Use the Admin API to register the organization in the application

### Assign Organization Admin

1. Create user in Keycloak with `organization` attribute
2. Assign `ORG_ADMIN` role to the user
3. Add user to organization group

### Custom Realm Configuration

The realm configuration is located at:
`docker/keycloak/config/wernertest-realm.json`

Customize:
- Token lifespans
- Password policies
- Required actions
- Brute force settings
- Event logging

## Troubleshooting

### Common Issues

1. **Connection refused to Keycloak**
   - Ensure Keycloak container is running and healthy
   - Check if port 8180 is accessible
   - Verify issuer-uri matches Keycloak configuration

2. **Invalid token**
   - Check token expiration
   - Verify client secret matches
   - Ensure realm name is correct

3. **User not found in organization**
   - Verify `organization` attribute is set in Keycloak
   - Check user belongs to correct group

4. **CORS errors**
   - Add your frontend URL to `app.cors.allowed-origins`

### Logs

```bash
# Application logs
tail -f logs/application.log

# Keycloak logs
docker logs keycloak-server

# Database logs
docker logs keycloak-postgres
docker logs app-postgres
```

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security OAuth2 Resource Server**
- **Keycloak 23.0.0**
- **PostgreSQL 15**
- **Lombok**
- **SpringDoc OpenAPI 2.3.0**
- **Docker & Docker Compose**

## Project Structure

```
├── src/main/java/com/wernertest/auth/
│   ├── JwtKeycloakAuthApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── KeycloakAdminConfig.java
│   │   └── OpenApiConfig.java
│   ├── controller/
│   │   ├── AdminController.java
│   │   ├── OrganizationAdminController.java
│   │   ├── PublicController.java
│   │   └── UserController.java
│   ├── dto/
│   │   ├── ApiResponse.java
│   │   ├── CreateUserRequest.java
│   │   ├── CurrentUserDTO.java
│   │   ├── OrganizationDTO.java
│   │   ├── ResetPasswordRequest.java
│   │   ├── UpdateUserRequest.java
│   │   └── UserDTO.java
│   ├── entity/
│   │   ├── AuditLog.java
│   │   └── Organization.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── OrganizationNotFoundException.java
│   │   └── UserManagementException.java
│   ├── repository/
│   │   ├── AuditLogRepository.java
│   │   └── OrganizationRepository.java
│   └── service/
│       ├── AuditService.java
│       ├── CurrentUserService.java
│       ├── OrganizationService.java
│       └── UserManagementService.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-docker.yml
├── docker/
│   ├── docker-compose.yml
│   ├── docker-compose.full.yml
│   ├── .env.example
│   └── keycloak/config/
│       └── wernertest-realm.json
├── Dockerfile
└── pom.xml
```

## License

This project is licensed under the MIT License.
