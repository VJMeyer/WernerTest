# JWT Authentication with Keycloak

A modern, responsive frontend for JWT authentication using Keycloak identity provider. Built with **Java 21**, **Spring Boot 3.2**, **HTMX**, and **Thymeleaf** for server-side rendering - fast, secure, and enterprise-ready.

## Features

- **JWT Authentication** - Secure OAuth2/OIDC authentication with Keycloak
- **Spring Security** - Enterprise-grade security framework with OAuth2 client
- **HTMX + SSR** - Fast, responsive UI with minimal JavaScript using server-side rendering
- **Modern UI** - Clean, responsive design with dark/light mode support
- **Role-Based Access** - Automatic role extraction from Keycloak tokens
- **Real-time Feedback** - Toast notifications and loading indicators
- **Thymeleaf Templates** - Type-safe template engine with layout support

## Quick Start

### Prerequisites

- Java 21
- Maven 3.8+
- Keycloak server (local or remote)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd WernerTest
   ```

2. **Configure Keycloak**

   Update `src/main/resources/application.yml`:
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             keycloak:
               client-id: my-app
               client-secret: your-client-secret
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

3. **Build the application**
   ```bash
   ./mvnw clean package
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Open in browser**
   ```
   http://localhost:8081
   ```

## Keycloak Setup

See [docs/keycloak-setup.md](docs/keycloak-setup.md) for detailed Keycloak configuration instructions.

### Quick Docker Setup

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

## Project Structure

```
WernerTest/
├── pom.xml                              # Maven configuration
├── src/main/java/com/auth/keycloak/
│   ├── Application.java                 # Main Spring Boot application
│   ├── config/
│   │   ├── SecurityConfig.java         # Spring Security configuration
│   │   └── ThymeleafConfig.java        # Thymeleaf layout configuration
│   ├── controller/
│   │   ├── HomeController.java         # Landing and login pages
│   │   ├── DashboardController.java    # Protected dashboard
│   │   ├── ApiController.java          # HTMX API endpoints
│   │   └── HealthController.java       # Health check endpoint
│   ├── dto/
│   │   ├── UserInfo.java               # User information DTO
│   │   └── TokenInfo.java              # Token information DTO
│   └── service/
│       └── UserService.java            # User and token processing
├── src/main/resources/
│   ├── application.yml                  # Main configuration
│   ├── application-dev.yml              # Development profile
│   ├── templates/
│   │   ├── layout.html                 # Base layout template
│   │   ├── index.html                  # Landing page
│   │   ├── login.html                  # Login page
│   │   ├── dashboard.html              # Protected dashboard
│   │   ├── profile.html                # User profile page
│   │   └── fragments/
│   │       ├── profile-card.html       # HTMX profile fragment
│   │       └── token-info.html         # HTMX token info fragment
│   └── static/
│       ├── css/style.css               # Modern CSS with dark mode
│       └── js/main.js                  # Frontend JavaScript
└── docs/
    └── keycloak-setup.md               # Keycloak setup guide
```

## Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/` | GET | Landing page | No |
| `/login` | GET | Login page (redirects to Keycloak) | No |
| `/oauth2/authorization/keycloak` | GET | Initiate Keycloak OAuth2 flow | No |
| `/dashboard` | GET | Protected dashboard | Yes |
| `/profile` | GET | User profile page | Yes |
| `/logout` | POST | Logout user (clears session & Keycloak) | Yes |
| `/api/profile` | GET | Profile fragment (HTMX) | Yes |
| `/api/token-info` | GET | Token info fragment (HTMX) | Yes |
| `/health` | GET | Health check | No |

## Key Technologies

- **Java 21** - Latest LTS version with modern features
- **Spring Boot 3.2** - Production-ready framework
- **Spring Security 6** - OAuth2/OIDC client support
- **Thymeleaf** - Server-side template engine
- **HTMX** - High power tools for HTML
- **Lombok** - Boilerplate reduction

## Security Features

- OAuth2/OpenID Connect authentication flow
- Automatic JWT token validation
- CSRF protection with cookie-based tokens
- Session management with configurable timeout
- Role extraction from Keycloak tokens (realm and client roles)
- Secure logout with Keycloak session termination
- Protected routes with Spring Security

## Development

### Running in Development Mode

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

With hot reload:
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
```

### Adding New Protected Routes

```java
@Controller
public class MyController {

    @GetMapping("/protected-resource")
    public String protectedResource(Authentication authentication, Model model) {
        // Authentication object contains user details
        return "my-template";
    }
}
```

### Creating HTMX Fragments

1. Create a fragment in `templates/fragments/`:
```html
<div th:fragment="my-fragment">
    <!-- Fragment content -->
</div>
```

2. Add a controller method:
```java
@GetMapping("/api/my-data")
public String getMyData(Model model) {
    model.addAttribute("data", myData);
    return "fragments/my-fragment :: my-fragment";
}
```

3. Use in HTML:
```html
<button
    hx-get="/api/my-data"
    hx-target="#content"
    hx-swap="innerHTML"
>
    Load Data
</button>
```

## Customization

### Theming

The CSS uses CSS variables for easy customization. Edit `static/css/style.css`:

```css
:root {
    --primary: #6366f1;
    --primary-dark: #4f46e5;
    /* ... more variables */
}
```

### Role-Based Authorization

Add method-level security:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin")
public String adminPage() {
    return "admin";
}
```

Or use in templates:

```html
<div sec:authorize="hasRole('ADMIN')">
    Admin-only content
</div>
```

## Configuration Options

### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: ${KEYCLOAK_CLIENT_ID:my-app}
            client-secret: ${KEYCLOAK_CLIENT_SECRET:}
            scope:
              - openid
              - profile
              - email
              - roles
        provider:
          keycloak:
            issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/master}

server:
  port: ${SERVER_PORT:8081}
  servlet:
    session:
      timeout: 30m
```

## Troubleshooting

### Common Issues

1. **"redirect_uri_mismatch" error**
   - Verify redirect URIs in Keycloak client settings
   - Should be: `http://localhost:8081/*`

2. **No roles appearing**
   - Check client scopes configuration in Keycloak
   - Ensure roles are included in ID token
   - Verify role mapper is configured

3. **Session expires immediately**
   - Check token lifetime in Keycloak
   - Verify session timeout in application.yml
   - Ensure cookies are enabled

4. **HTMX not updating**
   - Check browser console for errors
   - Verify CSRF token is included in requests
   - Check endpoint returns correct fragment

## Production Deployment

### Build JAR

```bash
./mvnw clean package -DskipTests
java -jar target/keycloak-jwt-auth-1.0.0.jar
```

### Docker Deployment

```dockerfile
FROM eclipse-temurin:21-jre
COPY target/keycloak-jwt-auth-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Production Configuration

1. Use HTTPS for both app and Keycloak
2. Set proper environment variables
3. Configure session persistence (Redis, JDBC)
4. Enable production profile
5. Set up proper logging and monitoring

Example:
```bash
java -jar app.jar \
  --spring.profiles.active=prod \
  --server.ssl.enabled=true \
  --server.ssl.key-store=/path/to/keystore.p12
```

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and feature requests, please open an issue in the repository.
