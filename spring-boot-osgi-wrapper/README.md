# Spring Boot JAR with OSGi Lifecycle Wrapper

This project demonstrates a **JAR-based approach** where:

- **Spring Boot runs completely independently** with its own embedded Tomcat
- **Karaf/OSGi only provides**:
  - Lifecycle management (activate/deactivate)
  - JNDI DataSource from OSGi service registry
  - Optional configuration via ConfigAdmin

The key insight: **minimal OSGi integration** - Spring Boot controls everything except lifecycle and DataSource.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Apache Karaf                      │
│  ┌───────────────────────────────────────────────┐  │
│  │         OSGi Lifecycle Wrapper (Bundle)       │  │
│  │  ┌─────────────────────────────────────────┐  │  │
│  │  │     SpringBootLifecycleManager (DS)     │  │  │
│  │  │                                         │  │  │
│  │  │  activate()  ──► Start Spring Boot      │  │  │
│  │  │  deactivate() ─► Stop Spring Boot       │  │  │
│  │  │                                         │  │  │
│  │  │  @Reference DataSource ─────┐           │  │  │
│  │  └─────────────────────────────┼───────────┘  │  │
│  │                                │              │  │
│  │  ┌─────────────────────────────▼───────────┐  │  │
│  │  │    Spring Boot Application (Embedded)   │  │  │
│  │  │                                         │  │  │
│  │  │    ┌─────────────┐  ┌───────────────┐   │  │  │
│  │  │    │ Embedded    │  │  JDBC Layer   │   │  │  │
│  │  │    │ Tomcat      │  │  (uses DS)    │   │  │  │
│  │  │    │ Port: 8090  │  └───────────────┘   │  │  │
│  │  │    └─────────────┘                      │  │  │
│  │  └─────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  ┌───────────────────────────────────────────────┐  │
│  │           OSGi Service Registry               │  │
│  │  ┌──────────────────┐  ┌──────────────────┐   │  │
│  │  │  DataSource      │  │   ConfigAdmin    │   │  │
│  │  │  (from JNDI)     │  │   (properties)   │   │  │
│  │  └──────────────────┘  └──────────────────┘   │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                          │
                          ▼
              HTTP Access: localhost:8090
```

## Key Differences from WAR Approach

| Aspect | WAR (PAX Web) | JAR (This Approach) |
|--------|---------------|---------------------|
| **HTTP Server** | Karaf's Jetty (PAX Web) | Spring Boot's Tomcat |
| **Port** | 8181 (Karaf's) | 8090 (Spring Boot's) |
| **Servlet Container** | External (Karaf) | Embedded (Spring Boot) |
| **Lifecycle** | Web Application Bundle | OSGi DS Component |
| **DataSource** | JNDI in Karaf's context | Injected via @Reference |
| **Control** | Karaf manages web context | Spring Boot has full control |
| **Packaging** | war (with OSGi manifest) | bundle (wraps jar) |

## Project Structure

```
spring-boot-osgi-wrapper/
├── pom.xml                           # Parent POM
├── spring-boot-app/                  # Spring Boot Application (JAR)
│   ├── pom.xml
│   └── src/main/java/com/example/app/
│       ├── SpringBootJdbcApplication.java  # Main app
│       ├── ApplicationLauncher.java        # Programmatic launcher (KEY!)
│       ├── config/
│       │   └── DataSourceConfig.java       # Accepts external DS
│       ├── controller/
│       ├── model/
│       ├── repository/
│       └── service/
├── osgi-wrapper/                     # OSGi Lifecycle Wrapper (Bundle)
│   ├── pom.xml
│   └── src/main/java/com/example/osgi/
│       ├── SpringBootLifecycleManager.java # DS Component with activate/deactivate
│       └── SpringBootBundleActivator.java  # Alternative BundleActivator approach
└── karaf/                            # Karaf deployment configs
    ├── features.xml
    ├── datasource.cfg
    ├── springboot.cfg
    └── karaf-deployment.txt
```

## Quick Start

### Build All Modules

```bash
mvn clean install
```

This creates:
- `spring-boot-app/target/spring-boot-jdbc-app-1.0.0-SNAPSHOT.jar` (executable)
- `osgi-wrapper/target/osgi-lifecycle-wrapper-1.0.0-SNAPSHOT.jar` (OSGi bundle)

### Run Standalone (No OSGi)

```bash
cd spring-boot-app
java -jar target/spring-boot-jdbc-app-1.0.0-SNAPSHOT-exec.jar
```

Access: http://localhost:8080/api/products

### Run in Karaf (OSGi Managed)

```bash
# 1. Start Karaf
$KARAF_HOME/bin/karaf

# 2. Install prerequisites
karaf@root()> feature:install jdbc jndi scr
karaf@root()> bundle:install -s mvn:com.h2database/h2/2.2.224

# 3. Configure DataSource
# Copy karaf/datasource.cfg to $KARAF_HOME/etc/org.ops4j.datasource-AppDataSource.cfg

# 4. Deploy the wrapper bundle
karaf@root()> bundle:install -s mvn:com.example/osgi-lifecycle-wrapper/1.0.0-SNAPSHOT
```

Access: http://localhost:8090/api/products

## The Key Component: ApplicationLauncher

The magic happens in `ApplicationLauncher.java`:

```java
public class ApplicationLauncher {
    private ConfigurableApplicationContext context;

    public ApplicationLauncher withDataSource(DataSource ds) {
        this.externalDataSource = ds;
        return this;
    }

    public boolean start() {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringBootJdbcApplication.class);

        // Inject external DataSource as a Spring bean
        builder.initializers(ctx -> {
            ctx.getBeanFactory().registerSingleton("externalDataSource", externalDataSource);
        });

        context = builder.run();
        return true;
    }

    public boolean stop() {
        context.close();
        return true;
    }
}
```

This allows **programmatic control** of Spring Boot's lifecycle, including injection of external resources.

## The Key Component: OSGi Lifecycle Manager

The `SpringBootLifecycleManager` is an OSGi Declarative Services component:

```java
@Component(name = "spring-boot-jdbc-app", immediate = true)
public class SpringBootLifecycleManager {

    @Reference(target = "(dataSourceName=AppDataSource)")
    public void setDataSource(DataSource ds) {
        this.dataSource = ds;
    }

    @Activate
    public void activate() {
        launcher = new ApplicationLauncher()
                .withDataSource(dataSource)
                .withPort(8090);

        launcher.start();
    }

    @Deactivate
    public void deactivate() {
        launcher.stop();
    }
}
```

OSGi calls `activate()` when the bundle starts → Spring Boot starts
OSGi calls `deactivate()` when the bundle stops → Spring Boot stops

## DataSource Flow

1. **Karaf/OSGi** registers DataSource service via JNDI configuration
2. **SCR (Declarative Services)** injects DataSource into wrapper component
3. **Wrapper** passes DataSource to ApplicationLauncher
4. **Spring Boot** uses the provided DataSource instead of creating its own

```java
// In DataSourceConfig.java
@Bean
public DataSource dataSource() {
    if (useExternalDataSource) {
        return applicationContext.getBean("externalDataSource", DataSource.class);
    }
    return createHikariDataSource();  // fallback for standalone
}
```

## Configuration via ConfigAdmin

Karaf's ConfigAdmin allows runtime configuration:

```bash
# Edit $KARAF_HOME/etc/com.example.springboot.cfg
server.port = 9090
server.context-path = /api
spring.profiles.active = osgi,production
```

The `@Modified` method in the lifecycle manager receives these updates.

## Advantages of This Approach

1. **Full Spring Boot Control** - Spring Boot manages its own server, classloading, etc.
2. **Standard Spring Boot JAR** - No special packaging, no WAR, no servlet API dependencies
3. **Clear Separation** - OSGi for lifecycle and resources, Spring Boot for everything else
4. **Easy Testing** - Run Spring Boot standalone without OSGi
5. **Minimal OSGi Knowledge** - Only need to understand lifecycle and service injection
6. **No PAX Web Conflicts** - Spring Boot's Tomcat doesn't interfere with Karaf's Jetty

## Disadvantages

1. **Separate Port** - Spring Boot runs on its own port, not Karaf's HTTP service
2. **Large Bundle** - The wrapper bundle embeds the entire Spring Boot fat JAR
3. **Memory Usage** - Running full Spring Boot with embedded Tomcat uses more memory
4. **No OSGi Service Export** - Spring beans aren't automatically OSGi services

## Using Karaf's Proxy (Optional)

If you need to expose Spring Boot through Karaf's port, configure a reverse proxy:

### Option 1: HTTP Proxy Feature

```bash
karaf@root()> feature:install http-proxy
# Configure proxy rules in etc/org.ops4j.pax.web.proxy.cfg
```

### Option 2: External Proxy (nginx)

```nginx
server {
    listen 80;
    location /spring-app/ {
        proxy_pass http://localhost:8090/;
    }
}
```

### Option 3: Register as HTTP Whiteboard Service

Expose Spring Boot endpoints as OSGi HTTP Whiteboard services (advanced).

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/products | List all products |
| GET | /api/products/{id} | Get product by ID |
| POST | /api/products | Create product |
| PUT | /api/products/{id} | Update product |
| DELETE | /api/products/{id} | Delete product |
| GET | /api/products/health | Health check |
| GET | /api/info | Runtime info |

## Testing Lifecycle

```bash
# In Karaf console

# Check component status
karaf@root()> scr:list
# spring-boot-jdbc-app [active]

# Stop the bundle (triggers deactivate)
karaf@root()> bundle:stop <id>
# Logs: "=== OSGi Lifecycle: DEACTIVATING Spring Boot Application ==="

# Start the bundle (triggers activate)
karaf@root()> bundle:start <id>
# Logs: "=== OSGi Lifecycle: ACTIVATING Spring Boot Application ==="

# Spring Boot is now running again with same DataSource
```

## When to Use This Approach

**Use JAR with OSGi Wrapper when:**
- Spring Boot needs full control over its runtime
- You want standard Spring Boot JAR (easy to debug, test)
- You prefer embedded Tomcat over Karaf's Jetty
- You need minimal OSGi integration
- Hot-swapping web context isn't required

**Use WAR as WAB when:**
- You want single HTTP port (Karaf's)
- You need OSGi HTTP Whiteboard features
- You prefer PAX Web's Jetty
- You want OSGi-style web context management

## Troubleshooting

```bash
# Component not starting
karaf@root()> scr:info spring-boot-jdbc-app
# Check: "unsatisfied reference: dataSource"

# DataSource not available
karaf@root()> jdbc:ds-list
# Ensure DataSource config is in etc/ directory

# Check logs for Spring Boot startup
karaf@root()> log:tail | grep SpringBoot

# Port conflict
# Edit $KARAF_HOME/etc/com.example.springboot.cfg
# Change server.port = 9090
```

## License

MIT License
