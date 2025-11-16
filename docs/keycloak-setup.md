# Keycloak Setup Guide

This guide will help you set up Keycloak for JWT authentication with this Spring Boot application.

## Prerequisites

- Docker (recommended) or Java 11+
- This application configured and running

## Quick Start with Docker

### 1. Start Keycloak

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

### 2. Access Keycloak Admin Console

1. Open http://localhost:8080
2. Click "Administration Console"
3. Login with:
   - Username: `admin`
   - Password: `admin`

### 3. Create a Realm

1. Click the dropdown next to "master" realm
2. Click "Create realm"
3. Set Realm name: `myapp` (or your preferred name)
4. Click "Create"

### 4. Create a Client

1. Go to "Clients" in the left menu
2. Click "Create client"
3. Configure:
   - **Client ID**: `my-app`
   - **Client type**: OpenID Connect
   - Click "Next"
4. Authentication settings:
   - **Client authentication**: ON
   - **Authorization**: OFF (unless needed)
   - **Authentication flow**: Standard flow (checked)
   - Click "Next"
5. Login settings:
   - **Valid redirect URIs**: `http://localhost:8081/*`
   - **Valid post logout redirect URIs**: `http://localhost:8081/*`
   - **Web origins**: `http://localhost:8081`
   - Click "Save"

### 5. Get Client Secret

1. Go to "Clients" → "my-app"
2. Click "Credentials" tab
3. Copy the "Client secret"

### 6. Configure Client Scopes (Optional but Recommended)

To include roles in the token:

1. Go to "Client scopes"
2. Click "roles"
3. Click "Mappers" tab
4. Click "realm roles"
5. Ensure "Add to ID token" and "Add to access token" are ON
6. Click "Save"

### 7. Create a Test User

1. Go to "Users" in the left menu
2. Click "Add user"
3. Configure:
   - **Username**: `testuser`
   - **Email**: `test@example.com`
   - **First name**: `Test`
   - **Last name**: `User`
   - **Email verified**: ON
   - Click "Create"
4. Go to "Credentials" tab
5. Click "Set password"
6. Enter password and confirm
7. Set "Temporary" to OFF
8. Click "Save"

### 8. Assign Roles (Optional)

1. Go to "Realm roles" in the left menu
2. Click "Create role"
3. Add roles like `user`, `admin`, `moderator`
4. Go to "Users" → Select your user
5. Click "Role mapping" tab
6. Click "Assign role"
7. Select the roles you want to assign

### 9. Configure Your Application

Update your `application.yml` or set environment variables:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: my-app
            client-secret: <your-client-secret>
        provider:
          keycloak:
            issuer-uri: http://localhost:8080/realms/myapp
```

Or use environment variables:

```bash
export KEYCLOAK_CLIENT_ID=my-app
export KEYCLOAK_CLIENT_SECRET=your-client-secret
export KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/myapp
```

### 10. Test the Setup

1. Start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Open http://localhost:8081
3. Click "Get Started" or "Login"
4. You will be redirected to Keycloak login page
5. Enter credentials:
   - Username: `testuser`
   - Password: (the password you set)
6. You should be redirected back to the dashboard

## Advanced Configuration

### Token Lifetime Configuration

1. Go to "Realm settings"
2. Click "Tokens" tab
3. Configure:
   - Access Token Lifespan: 5-15 minutes (default: 5 minutes)
   - Client Session Idle: 30 minutes
   - Client Session Max: 10 hours

### Enabling Social Login

1. Go to "Identity providers" in the left menu
2. Click "Add provider"
3. Select your provider (Google, GitHub, etc.)
4. Follow the provider-specific setup instructions

### Configuring Password Policies

1. Go to "Authentication" in the left menu
2. Click "Policies" tab
3. Click "Password policy"
4. Add policies like:
   - Minimum Length
   - Special Characters
   - Uppercase Characters
   - Lowercase Characters
   - Digits

### Enabling Two-Factor Authentication

1. Go to "Authentication"
2. Click "Required actions"
3. Enable "Configure OTP"
4. Users will be prompted to set up 2FA on next login

## Troubleshooting

### Common Issues

1. **"redirect_uri_mismatch" error**
   - Check "Valid redirect URIs" in client settings
   - Ensure it matches your application URL exactly
   - Include trailing `/*` for flexibility

2. **"Invalid token" error**
   - Verify the issuer-uri matches your Keycloak realm URL
   - Check client ID and secret are correct
   - Ensure the realm exists

3. **No roles appearing in dashboard**
   - Check that roles are added to ID token in client scopes
   - Verify user has roles assigned
   - Check "Mappers" configuration in client scopes

4. **CORS errors**
   - Add your app URL to "Web origins" in client settings
   - Ensure both root URL and redirect URIs are configured

5. **Session expires immediately**
   - Check token lifetime settings in realm
   - Verify session timeout in Spring Boot configuration
   - Check cookie settings

### Useful Docker Commands

```bash
# View logs
docker logs keycloak

# Follow logs
docker logs -f keycloak

# Stop Keycloak
docker stop keycloak

# Start Keycloak
docker start keycloak

# Remove container
docker rm keycloak

# Get into container shell
docker exec -it keycloak /bin/bash

# Export realm configuration
docker exec keycloak /opt/keycloak/bin/kc.sh export --dir /tmp/export --realm myapp
```

## Production Considerations

1. **Use HTTPS** for both Keycloak and your application
2. **Use external database** (PostgreSQL recommended)
   ```bash
   docker run -d \
     --name keycloak-prod \
     -p 8080:8080 \
     -e KC_DB=postgres \
     -e KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak \
     -e KC_DB_USERNAME=keycloak \
     -e KC_DB_PASSWORD=password \
     -e KEYCLOAK_ADMIN=admin \
     -e KEYCLOAK_ADMIN_PASSWORD=strong-password \
     quay.io/keycloak/keycloak:latest start
   ```
3. **Change default admin credentials**
4. **Configure proper hostname**
5. **Enable brute force detection** (Authentication → Realm Settings)
6. **Set up proper logging and monitoring**
7. **Configure backup strategy**
8. **Use Kubernetes or similar for high availability**

## Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak REST API](https://www.keycloak.org/docs-api/latest/rest-api/)
- [Spring Security OAuth2 Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Keycloak Spring Boot Adapter (Legacy)](https://www.keycloak.org/docs/latest/securing_apps/#_spring_boot_adapter)
