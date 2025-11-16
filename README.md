# Environmental Monitoring OData API

A Spring Boot web application implementing OData v4 standard for environmental measurements data management, following hexagonal architecture principles with Spring Modulith for module separation.

## Architecture Overview

### Hexagonal Architecture (Ports & Adapters)

```
                    ┌─────────────────────────────────────┐
                    │           OData Web Layer           │
                    │      (Driving/Inbound Adapter)      │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │         Input Ports (Use Cases)     │
                    │   MeasuringSiteUseCase              │
                    │   StationUseCase                    │
                    │   SamplingUseCase, etc.             │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │       Application Services          │
                    │    (Business Logic Orchestration)   │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │          Domain Model               │
                    │   (Core Business Entities)          │
                    │   MeasuringSite, Station, Sample    │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │        Output Ports                 │
                    │   (Repository Interfaces)           │
                    └─────────────┬───────────────────────┘
                                  │
                    ┌─────────────▼───────────────────────┐
                    │       Persistence Adapters          │
                    │     (Driven/Outbound Adapter)       │
                    │          JPA/Hibernate 6.5          │
                    └─────────────────────────────────────┘
```

### Spring Modulith Module Structure

- **domain** - Core business entities (shared module)
- **application** - Use cases, ports, and service implementations
- **adapter** - Technical implementations (web/persistence)

## Technology Stack

- **Spring Boot 3.2.5** - Application framework
- **Spring Modulith 1.1.4** - Module boundaries and verification
- **Apache Olingo 4.10.0** - OData v4 protocol implementation
- **Hibernate 6.5.0** - JPA implementation
- **H2 / PostgreSQL** - Database support
- **JNDI DataSource** - Production datasource configuration
- **Lombok** - Boilerplate code reduction

## Domain Model

### Entity Relationships

```
MeasuringSite (1) ──── (*) Station
                            │
                            │
                           (1)
                            │
                           (*)
                        Sampling
                            │
                           (1)
                            │
                           (*)
                         Sample
                            │
                           (1)
                            │
                           (*)
                       SampleValue ──── (*) ParameterType
```

### Entities

- **MeasuringSite** - Physical location where measurements are taken
- **Station** - Monitoring station within a site
- **ParameterType** - Type of measurement (temperature, pH, etc.)
- **Sampling** - Sampling event/campaign
- **Sample** - Individual sample collected during sampling
- **SampleValue** - Actual measured value for a parameter

## OData v4 Features

### Projection Support

The API implements intelligent projection based on the request type:

- **Collection Endpoints** (`GET /odata/MeasuringSites`): Returns entities **without** embedded collections to minimize payload size
- **Single Entity Endpoints** (`GET /odata/MeasuringSites(guid'...')`): Returns complete entity **with** all embedded collections

### Available Entity Sets

- `/odata/MeasuringSites`
- `/odata/Stations`
- `/odata/ParameterTypes`
- `/odata/Samplings`
- `/odata/Samples`
- `/odata/SampleValues`

### Example Requests

```bash
# Get all measuring sites (projection - no embedded stations)
GET http://localhost:8080/api/odata/MeasuringSites

# Get single measuring site with embedded stations
GET http://localhost:8080/api/odata/MeasuringSites(guid'...')

# Get OData service metadata
GET http://localhost:8080/api/odata/$metadata

# Get service document
GET http://localhost:8080/api/odata/
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) PostgreSQL for production

### Running Locally

```bash
# Clone the repository
git clone <repository-url>
cd environmental-monitoring-odata

# Build the project
mvn clean install

# Run with H2 in-memory database (development)
mvn spring-boot:run

# Run with production profile
mvn spring-boot:run -Dspring-boot.run.profiles=production
```

### Development Database

The application uses H2 in-memory database for development with automatic schema creation and sample data initialization.

Access H2 Console: `http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:mem:envmonitoringdb`
- User: `sa`
- Password: (empty)

### JNDI DataSource Configuration (Production)

For production deployment in Tomcat:

1. Configure `src/main/webapp/META-INF/context.xml` with your database credentials
2. Enable JNDI in `application-production.yml`:
   ```yaml
   spring:
     datasource:
       jndi-name: java:comp/env/jdbc/EnvironmentalMonitoringDS
   ```

## Building for Deployment

```bash
# Create WAR file
mvn clean package -DskipTests

# The WAR file will be in target/environmental-monitoring-odata-1.0.0-SNAPSHOT.war
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run Spring Modulith architecture verification
mvn test -Dtest=ModulithArchitectureTest
```

### Architecture Verification

The `ModulithArchitectureTest` verifies:
- Module boundaries are respected
- Dependencies follow hexagonal architecture rules
- No cyclic dependencies exist

## Project Structure

```
src/main/java/com/environmental/monitoring/
├── EnvironmentalMonitoringApplication.java    # Main entry point
├── domain/
│   ├── package-info.java                      # Module definition
│   └── model/                                 # Domain entities
│       ├── MeasuringSite.java
│       ├── Station.java
│       ├── ParameterType.java
│       ├── Sampling.java
│       ├── Sample.java
│       └── SampleValue.java
├── application/
│   ├── package-info.java                      # Module definition
│   ├── port/
│   │   ├── in/                               # Input ports (use cases)
│   │   └── out/                              # Output ports (repositories)
│   └── service/                              # Service implementations
├── adapter/
│   ├── package-info.java                      # Module definition
│   ├── in/web/odata/                         # OData web adapter
│   │   ├── EdmProvider.java                  # Entity Data Model
│   │   ├── EntityCollectionProcessor.java   # Collection requests
│   │   ├── EntityProcessor.java             # Single entity requests
│   │   ├── EntityMapper.java                # Entity <-> OData mapping
│   │   ├── ODataServlet.java                # OData servlet
│   │   └── ODataWebConfig.java              # Servlet registration
│   └── out/persistence/                      # JPA persistence adapter
│       ├── jpa/                              # Spring Data JPA repos
│       ├── *RepositoryAdapter.java           # Port implementations
│       ├── JndiDataSourceConfig.java        # JNDI configuration
│       └── DataInitializer.java             # Sample data
```

## Configuration

### Application Properties

Key configuration options in `application.yml`:

- **Database**: H2 (dev) or PostgreSQL (prod)
- **JPA/Hibernate**: DDL auto-generation, SQL logging
- **OData**: Service root path `/api/odata`
- **Logging**: Debug levels for different packages
- **Spring Modulith**: Module detection strategy

## License

See [LICENSE](LICENSE) file for details.
