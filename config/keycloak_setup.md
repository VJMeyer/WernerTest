# Keycloak Setup Guide

This guide will help you set up Keycloak for JWT authentication with this application.

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
   - **Authentication flow**: Standard flow + Direct access grants
   - Click "Next"
5. Login settings:
   - **Valid redirect URIs**: `http://localhost:5000/*`
   - **Web origins**: `http://localhost:5000`
   - Click "Save"

### 5. Get Client Secret

1. Go to "Clients" → "my-app"
2. Click "Credentials" tab
3. Copy the "Client secret"

### 6. Create a Test User

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

### 7. Configure Your Application

Update your `.env` file:

```bash
SECRET_KEY=your-super-secret-key-here
KEYCLOAK_SERVER_URL=http://localhost:8080
KEYCLOAK_REALM=myapp
KEYCLOAK_CLIENT_ID=my-app
KEYCLOAK_CLIENT_SECRET=<paste-your-client-secret>
```

### 8. Test the Setup

1. Start the Flask application:
   ```bash
   python app.py
   ```
2. Open http://localhost:5000
3. Click "Get Started" or "Login"
4. Enter credentials:
   - Username: `testuser`
   - Password: (the password you set)
5. You should be redirected to the dashboard

## Advanced Configuration

### Adding Roles

1. Go to "Realm roles" in the left menu
2. Click "Create role"
3. Add roles like `admin`, `user`, `moderator`
4. Assign roles to users:
   - Go to "Users" → Select user
   - Click "Role mapping" tab
   - Click "Assign role"
   - Select the roles

### Adding Client Roles

1. Go to "Clients" → "my-app"
2. Click "Roles" tab
3. Click "Create role"
4. Add client-specific roles
5. Assign to users via "Role mapping"

### Configuring Token Lifetime

1. Go to "Realm settings"
2. Click "Tokens" tab
3. Configure:
   - Access Token Lifespan: 5-15 minutes (default: 5 minutes)
   - Client Session Idle: 30 minutes
   - Client Session Max: 10 hours

### Enabling Email Verification

1. Go to "Realm settings"
2. Click "Login" tab
3. Enable "Verify email"
4. Configure SMTP in "Email" tab

## Troubleshooting

### Common Issues

1. **Connection refused to Keycloak**
   - Ensure Keycloak is running: `docker ps`
   - Check the server URL in `.env`

2. **Invalid client secret**
   - Verify the secret matches in Keycloak admin console
   - Client authentication must be enabled

3. **Token verification fails**
   - Check realm name is correct
   - Ensure client ID matches
   - Verify the audience claim includes your client ID

4. **CORS errors**
   - Add your app URL to "Web origins" in client settings
   - Ensure proper redirect URIs are configured

### Useful Docker Commands

```bash
# View logs
docker logs keycloak

# Stop Keycloak
docker stop keycloak

# Start Keycloak
docker start keycloak

# Remove container
docker rm keycloak

# Get into container shell
docker exec -it keycloak /bin/bash
```

## Production Considerations

1. **Use HTTPS** for both Keycloak and your application
2. **Change default admin credentials**
3. **Use external database** (PostgreSQL recommended)
4. **Configure proper token lifetimes**
5. **Enable brute force detection**
6. **Set up proper logging and monitoring**
7. **Configure backup strategy**

## Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Keycloak REST API](https://www.keycloak.org/docs-api/latest/rest-api/)
- [Python-Keycloak Library](https://python-keycloak.readthedocs.io/)
