# File Processing System with RabbitMQ

A secure, multi-tenant file processing system using Spring Boot, Keycloak authentication, RabbitMQ message broker, and Docker. The system features organization-based user isolation, JWT authentication, and high availability architecture.

## Architecture

This system follows a microservices architecture with four main components:

1. **Keycloak** - Identity and access management (Authentication & Authorization)
2. **Upload Service** - Secure web application for file uploads (Producer)
3. **Batch Processor** - Command-line application for processing files (Consumer)
4. **RabbitMQ** - Message broker for asynchronous communication

## Authentication & Authorization

This system uses **Keycloak** for identity management with **JWT tokens** for API authentication.

### Key Features:
- **Multi-Organization Support**: Each organization has isolated data
- **Role-Based Access Control**: USER, ORG_ADMIN, and ADMIN roles
- **JWT Token Authentication**: Secure, stateless authentication
- **Organization Admins**: Can create and manage users in their organization

### Quick Start with Authentication:

**1. Get an access token:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/file-processing/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=upload-web-app" \
  -d "grant_type=password" \
  -d "username=acme.user1" \
  -d "password=password" | jq -r '.access_token')
```

**2. Upload a file (authenticated):**
```bash
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@myfile.csv"
```

**3. Check upload status (authenticated):**
```bash
curl -X GET http://localhost:8080/api/upload/status/{uploadId} \
  -H "Authorization: Bearer $TOKEN"
```

**📖 For complete authentication guide, see [AUTHENTICATION.md](AUTHENTICATION.md)**

- How to manage users as organization admin
- Pre-configured test accounts
- Access control examples
- Troubleshooting authentication issues

### Data Flow

```
User → Keycloak (Get JWT Token)
        ↓
     Upload Service (with JWT) → File Storage → RabbitMQ → Batch Processor → wiskBat → Database
         (Web API)                               (Message)    (Consumer)      (CSV Extract)
              ↓
         Redis (Upload Status + User/Org Info)
```

## High Availability Architecture

This system is designed for **production deployment on multiple servers** with **no single point of failure**.

### Key HA Features

- **Multi-Server Deployment**: Run upload service on 3+ servers behind a load balancer
- **Distributed Upload Monitoring**: Redis-based status tracking accessible from any server
- **Message Persistence**: RabbitMQ configured for durable queues and persistent messages
- **Shared File Storage**: NFS-ready architecture for highly available file storage
- **24-Hour Status Retention**: Upload status remains queryable for 24 hours after completion

### Components for High Availability

#### 1. Redis (Distributed State Store)

Redis stores upload status that can be queried from any server:
- Upload progress tracking
- Filename, file size, bytes uploaded
- Status (UPLOADING, PROCESSING, COMPLETED, FAILED)
- Automatic TTL expiration (24 hours after completion)

**Development Configuration (Single Instance):**
```properties
spring.data.redis.host=redis
spring.data.redis.port=6379
```

**Production Configuration (Redis Sentinel HA):**
```properties
# Redis Sentinel provides automatic failover
spring.data.redis.sentinel.master=mymaster
spring.data.redis.sentinel.nodes=sentinel1:26379,sentinel2:26379,sentinel3:26379
spring.data.redis.password=redis_password
```

**Redis HA Architecture Options:**

1. **Development/Testing**: Single Redis instance (current docker-compose.yml)
   - Simple setup
   - AOF persistence for data durability
   - **Limitation**: Single point of failure

2. **Production**: Redis Sentinel (docker-compose.redis-ha.yml)
   - 1 master + 2 replicas for redundancy
   - 3 sentinels for automatic failover
   - Quorum of 2 for master election
   - If master fails, sentinel promotes a replica automatically
   - No downtime during failover

3. **Enterprise**: Redis Cluster or Managed Redis
   - Use managed services (AWS ElastiCache, Azure Cache for Redis)
   - Or self-hosted Redis Cluster for massive scale
   - Horizontal scaling with data sharding

**Starting with Redis HA:**
```bash
# Use Redis Sentinel configuration
docker-compose -f docker-compose.redis-ha.yml up -d

# Check Sentinel status
docker exec redis-sentinel-1 redis-cli -p 26379 sentinel masters
docker exec redis-sentinel-1 redis-cli -p 26379 sentinel replicas mymaster
```

#### 2. RabbitMQ (Reliable Message Broker)

Configured for message persistence:
- **Durable queues**: Survive broker restarts
- **Persistent messages**: Not lost on broker failure
- **Publisher confirms**: Guaranteed message delivery
- **Manual acknowledgment**: Messages redelivered on consumer failure

**HA Configuration:**
```properties
# Publisher side (upload-service)
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
spring.rabbitmq.template.mandatory=true

# Consumer side (batch-processor)
spring.rabbitmq.listener.simple.acknowledge-mode=manual
spring.rabbitmq.listener.simple.prefetch=1
```

#### 3. NFS-Ready File Storage

Upload directory configured to work with highly available NFS:
```yaml
volumes:
  upload_data:
    driver: local
    driver_opts:
      type: nfs
      o: addr=nfs.server.com,rw
      device: ":/path/to/shared/uploads"
```

### Upload Status Monitoring

When uploading a file, clients receive a monitoring URL to track progress.

#### Response from Upload

```json
{
  "status": "success",
  "uploadId": "a1b2c3d4-e5f6-7890",
  "statusUrl": "/api/upload/status/a1b2c3d4-e5f6-7890",
  "filename": "data.csv",
  "size": 10485760
}
```

#### Check Upload Status

```bash
curl http://localhost:8080/api/upload/status/a1b2c3d4-e5f6-7890
```

Response:
```json
{
  "uploadId": "a1b2c3d4-e5f6-7890",
  "filename": "data.csv",
  "fileSize": 10485760,
  "bytesUploaded": 5242880,
  "status": "UPLOADING",
  "progressPercentage": "50.00",
  "createdAt": "2025-11-18T10:30:00",
  "isComplete": false,
  "isFailed": false,
  "isProcessing": true
}
```

**Status Values:**
- `UPLOADING`: File upload in progress
- `PROCESSING`: Upload complete, processing file
- `COMPLETED`: File successfully processed
- `FAILED`: Upload or processing failed

**Status Retention:**
- Active uploads: No expiration
- Completed uploads: 24 hours (86400 seconds)
- Failed uploads: 1 hour (3600 seconds)

#### Multi-Server Access

The status endpoint works from **any server** in your deployment:
```bash
# Query from server 1
curl http://server1.example.com/api/upload/status/a1b2c3d4-e5f6-7890

# Query from server 2 (same result)
curl http://server2.example.com/api/upload/status/a1b2c3d4-e5f6-7890

# Query from server 3 (same result)
curl http://server3.example.com/api/upload/status/a1b2c3d4-e5f6-7890
```

All servers share the same Redis instance, providing consistent status information.

### Production Deployment

#### Architecture Diagram

```
                           Load Balancer
                                |
                --------------------------------
                |               |              |
         Upload Service   Upload Service  Upload Service
          (Server 1)       (Server 2)      (Server 3)
                |               |              |
                --------------------------------
                        |              |
                      Redis      RabbitMQ (HA)
                        |              |
                    NFS Storage   Batch Processor
```

#### Deployment Steps

1. **Setup Shared Infrastructure:**
   ```bash
   # Redis (single instance or cluster)
   docker run -d --name redis redis:7-alpine --appendonly yes

   # RabbitMQ (with management, consider clustering)
   docker run -d --name rabbitmq \
     -p 5672:5672 -p 15672:15672 \
     rabbitmq:3.12-management-alpine
   ```

2. **Configure NFS Storage:**
   ```yaml
   # docker-compose.yml on each server
   volumes:
     upload_data:
       driver: local
       driver_opts:
         type: nfs
         o: addr=<NFS_SERVER_IP>,rw
         device: ":/mnt/shared/uploads"
   ```

3. **Deploy Upload Service on 3 Servers:**
   ```bash
   # On each server
   docker-compose up -d upload-service
   ```

4. **Configure Load Balancer:**
   ```nginx
   upstream upload_servers {
     server server1.example.com:8080;
     server server2.example.com:8080;
     server server3.example.com:8080;
   }

   server {
     listen 80;
     location / {
       proxy_pass http://upload_servers;
     }
   }
   ```

5. **Deploy Batch Processor:**
   ```bash
   # Can run on any server with access to NFS and RabbitMQ
   docker-compose up -d batch-processor
   ```

#### Environment Variables for Production

**upload-service:**
```yaml
environment:
  SPRING_RABBITMQ_HOST: rabbitmq.production.local
  SPRING_DATA_REDIS_HOST: redis.production.local
  UPLOAD_DIRECTORY: /mnt/nfs/uploads
```

**batch-processor:**
```yaml
environment:
  SPRING_RABBITMQ_HOST: rabbitmq.production.local
  WISKBAT_ENABLED: "true"
```

### Monitoring and Health Checks

#### Health Endpoints

```bash
# Upload service health
curl http://localhost:8080/api/upload/health

# TUS service health
curl http://localhost:8080/api/tus/health

# Actuator health (includes Redis, RabbitMQ)
curl http://localhost:8080/actuator/health
```

#### RabbitMQ Management UI

Monitor message flow and queue status:
- URL: http://rabbitmq.server:15672
- Check queue depth, message rates, consumer status

#### Redis Monitoring

```bash
# Check Redis connection
redis-cli -h redis.server PING

# Monitor upload status keys
redis-cli -h redis.server KEYS "UploadStatus:*"

# Check TTL on completed uploads
redis-cli -h redis.server TTL "UploadStatus:a1b2c3d4-e5f6-7890"
```

## Features

### Core Functionality
- **Multi-module Maven project structure**
- **Java 21 with Virtual Threads** for improved scalability and performance
- **TUS Protocol for resumable uploads** - recover from network interruptions
- **File upload with metadata tracking** (filename, size, checksum)
- **Asynchronous message processing** via RabbitMQ
- **Automatic batch file generation** for wiskBat processing
- **Support for small and large files** (up to 100MB)
- **Docker containerization** with Docker Compose orchestration
- **Health check endpoints** for monitoring
- **JSON message serialization**

### High Availability Features
- **Distributed upload status tracking** via Redis
- **Multi-server deployment ready** - no single point of failure
- **Real-time progress monitoring** from any server
- **24-hour status retention** after upload completion
- **Persistent message queues** - no message loss on failures
- **Publisher confirms** for guaranteed message delivery
- **NFS-ready file storage** for shared access across servers
- **Load balancer compatible** architecture

## Project Structure

```
.
├── pom.xml                            # Parent POM
├── docker-compose.yml                 # Docker orchestration (dev/test)
├── docker-compose.redis-ha.yml        # Docker orchestration with Redis Sentinel HA
│
├── messaging/                         # Shared messaging module
│   ├── pom.xml
│   └── src/main/java/de/kisters/hmt/cloud/messaging/
│       ├── config/
│       │   └── RabbitMQConfig.java    # RabbitMQ configuration
│       └── dto/
│           └── FileUploadMessage.java # Message DTO
│
├── upload-service/                    # File upload web service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/de/kisters/hmt/cloud/services/upload/
│       ├── UploadServiceApplication.java
│       ├── controller/
│       │   ├── FileUploadController.java
│       │   ├── TusUploadController.java
│       │   └── UploadStatusController.java   # Status monitoring endpoint
│       ├── service/
│       │   ├── FileUploadService.java
│       │   └── UploadStatusService.java      # Redis-based status tracking
│       ├── model/
│       │   └── UploadStatus.java             # Redis entity
│       └── repository/
│           └── UploadStatusRepository.java   # Redis repository
│
└── batch-processor/                   # Batch processing service
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/de/kisters/hmt/cloud/connector/wiski/batch/
        ├── BatchProcessorApplication.java
        ├── consumer/
        │   └── FileUploadConsumer.java
        └── service/
            └── BatchFileService.java
```

## Prerequisites

- Docker and Docker Compose installed
- Java 21 (for local development)
- Maven 3.6+ (for local development)

## Quick Start

### Using Docker Compose (Development/Testing)

**Option 1: Standard Setup (Single Redis)**
```bash
docker-compose up --build
```

**Option 2: High Availability Setup (Redis Sentinel)**
```bash
docker-compose -f docker-compose.redis-ha.yml up --build
```

**Access the services:**
- **Keycloak Admin Console**: http://localhost:8180 (admin: `admin` / `admin`)
- **Upload Service API**: http://localhost:8080 (requires JWT token - see [AUTHENTICATION.md](AUTHENTICATION.md))
- **RabbitMQ Management UI**: http://localhost:15672 (username: `guest`, password: `guest`)
- **Upload Status Monitoring**: http://localhost:8080/api/upload/status/{uploadId} (requires JWT token)

**Test Accounts (see AUTHENTICATION.md for full list):**
- `acme.user1` / `password` - User in ACME Corporation
- `acme.admin` / `password` - Admin for ACME Corporation
- `techstart.user1` / `password` - User in TechStart Inc
- `admin` / `admin123` - System administrator

**Choosing between configurations:**
- Use `docker-compose.yml` for development/testing (simpler, faster startup)
- Use `docker-compose.redis-ha.yml` for production-like testing (Redis HA with automatic failover)

### Local Development

1. **Start RabbitMQ:**
   ```bash
   docker-compose up rabbitmq
   ```

2. **Build all modules:**
   ```bash
   mvn clean install
   ```

3. **Run Upload Service:**
   ```bash
   cd upload-service
   mvn spring-boot:run
   ```

4. **Run Batch Processor (in a separate terminal):**
   ```bash
   cd batch-processor
   mvn spring-boot:run
   ```

## API Usage

### Simple Upload

For basic, small file uploads:

```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@/path/to/your/file.csv"
```

Response:
```json
{
  "status": "success",
  "message": "File uploaded successfully",
  "filename": "file.csv",
  "size": 1024,
  "path": "/app/uploads/abc123-def456.csv"
}
```

### Resumable Upload (TUS Protocol)

For large files or unreliable networks, use the **TUS protocol** for resumable uploads.

#### What is TUS?

TUS is an open protocol for resumable file uploads based on HTTP. It allows:
- **Resume interrupted uploads** from the last successful byte
- **Handle large files** efficiently with chunked uploads
- **Network resilience** - automatic retry on connection failures
- **Upload progress tracking** via standard HTTP headers

#### TUS Endpoints

**Base URL:** `http://localhost:8080/api/tus/upload`

**Supported Methods:**
- `POST` - Create new upload
- `PATCH` - Upload file chunks
- `HEAD` - Check upload status
- `DELETE` - Cancel upload

#### Create Upload

```bash
curl -X POST http://localhost:8080/api/tus/upload \
  -H "Tus-Resumable: 1.0.0" \
  -H "Upload-Length: 1048576" \
  -H "Upload-Metadata: filename dGVzdC5jc3Y=,filetype dGV4dC9jc3Y=" \
  -i
```

Response headers include:
- `Location: /api/tus/upload/{upload-id}`
- `Tus-Resumable: 1.0.0`

#### Upload Data

```bash
curl -X PATCH http://localhost:8080/api/tus/upload/{upload-id} \
  -H "Tus-Resumable: 1.0.0" \
  -H "Upload-Offset: 0" \
  -H "Content-Type: application/offset+octet-stream" \
  --data-binary @file.csv \
  -i
```

Headers:
- `Upload-Offset`: Byte position to resume from (0 for new upload)
- Response includes `Upload-Offset` with bytes received

#### Check Status

```bash
curl -X HEAD http://localhost:8080/api/tus/upload/{upload-id} \
  -H "Tus-Resumable: 1.0.0" \
  -i
```

Response headers:
- `Upload-Offset`: Bytes successfully uploaded
- `Upload-Length`: Total file size

#### Using TUS Client Libraries

For production use, leverage official TUS client libraries:

**JavaScript (Browser/Node.js):**
```javascript
import * as tus from "tus-js-client";

const file = document.querySelector('input[type="file"]').files[0];
const upload = new tus.Upload(file, {
  endpoint: "http://localhost:8080/api/tus/upload",
  retryDelays: [0, 3000, 5000, 10000, 20000],
  metadata: {
    filename: file.name,
    filetype: file.type
  },
  onError: (error) => console.error("Upload failed:", error),
  onProgress: (bytesUploaded, bytesTotal) => {
    const percentage = (bytesUploaded / bytesTotal * 100).toFixed(2);
    console.log(`Progress: ${percentage}%`);
  },
  onSuccess: () => console.log("Upload complete!")
});

upload.start();
```

**Python:**
```python
from tusclient import client

my_client = client.TusClient('http://localhost:8080/api/tus/upload')
uploader = my_client.uploader('path/to/file.csv', chunk_size=5242880)
uploader.upload()
```

**Java:**
```java
TusClient client = new TusClient();
client.setUploadCreationURL(new URL("http://localhost:8080/api/tus/upload"));
TusUploader uploader = client.createUpload(file);
uploader.upload();
```

#### TUS Features Supported

- ✅ **Creation** - POST to create upload
- ✅ **Core** - PATCH to upload data
- ✅ **Termination** - DELETE to cancel
- ✅ **Checksum** - Upload-Checksum validation
- ✅ **Expiration** - Automatic cleanup after 24 hours

#### TUS Configuration

Edit `upload-service/src/main/resources/application.properties`:
```properties
tus.upload.directory=/app/tus-uploads
tus.upload.expiration.period=86400000  # 24 hours
tus.max.upload.size=104857600          # 100MB
```

### Health Check

```bash
curl http://localhost:8080/api/upload/health
```

Response:
```json
{
  "status": "UP",
  "service": "upload-service"
}
```

## Module Details

### 1. Messaging Module

**Package:** `de.kisters.hmt.cloud.messaging`

Shared library containing:
- RabbitMQ configuration (queue, exchange, bindings)
- Message DTOs (FileUploadMessage)
- JSON message converter setup

### 2. Upload Service

**Package:** `de.kisters.hmt.cloud.services.upload`

**Responsibilities:**
- Accepts file uploads via REST API
- Stores files to disk with unique filenames
- Calculates SHA-256 checksum
- Sends file metadata to RabbitMQ

**Configuration:**
- Port: 8080
- Max file size: 100MB
- Upload directory: `/app/uploads`

### 3. Batch Processor

**Package:** `de.kisters.hmt.cloud.connector.wiski.batch`

**Responsibilities:**
- Consumes file upload messages from RabbitMQ
- Generates Windows batch files with metadata
- Invokes wiskBat executable with batch file
- Processes CSV data extraction to database

**Batch File Content:**
The generated batch file includes:
- File metadata (filename, size, path, checksum)
- Environment variables for wiskBat
- wiskBat invocation command

**Configuration:**
- Batch directory: `/app/batch`
- wiskBat executable: `wiskBat.exe`
- wiskBat enabled: `false` (set to `true` in production)

## Java 21 Virtual Threads

This application is built with **Java 21** and leverages **Virtual Threads** (Project Loom) for improved concurrency and scalability.

### What are Virtual Threads?

Virtual Threads are lightweight threads introduced in Java 21 that dramatically reduce the overhead of thread creation and context switching. Unlike traditional platform threads, virtual threads are managed by the JVM rather than the operating system, allowing applications to create millions of threads with minimal resource consumption.

### Benefits in this Application

**Upload Service:**
- Each HTTP request is handled on a virtual thread
- File uploads (especially large files) can be processed concurrently without blocking platform threads
- Improved throughput for concurrent file uploads
- Reduced memory footprint compared to traditional thread-per-request model

**Batch Processor:**
- RabbitMQ message listeners run on virtual threads
- File processing operations (reading, batch file generation, wiskBat execution) don't block platform threads
- Better resource utilization when processing multiple messages concurrently

### Configuration

Virtual threads are enabled via Spring Boot configuration:

```properties
spring.threads.virtual.enabled=true
```

This single property configures Spring Boot to use virtual threads for:
- Web request handling (Tomcat/Jetty)
- `@Async` methods
- `@Scheduled` tasks
- Task executors

### Performance Characteristics

- **Lightweight**: Each virtual thread uses only a few KB of memory (vs. ~1MB for platform threads)
- **Scalable**: Can create millions of virtual threads without exhausting system resources
- **No code changes**: Existing blocking code works seamlessly with virtual threads
- **Simplified concurrency**: No need for complex async/reactive patterns for I/O-bound operations

### Monitoring

To verify virtual threads are in use, check the application logs or use JVM monitoring tools. Virtual threads appear as `VirtualThread[#...]` in thread dumps.

## Testing the System

1. **Start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Upload a test file:**
   ```bash
   curl -X POST http://localhost:8080/api/upload \
     -F "file=@testfile.csv"
   ```

3. **Check the logs:**

   Upload Service logs:
   ```bash
   docker logs upload-service -f
   ```

   Batch Processor logs:
   ```bash
   docker logs batch-processor -f
   ```

4. **Verify in RabbitMQ Management UI:**
   - Open http://localhost:15672
   - Login with `guest`/`guest`
   - Check the Queues tab for message statistics

5. **Check generated batch files:**
   ```bash
   docker exec batch-processor ls -la /app/batch
   ```

## Configuration

### RabbitMQ Settings

Common properties (in both services):
```properties
rabbitmq.queue.file-upload=file.upload.queue
rabbitmq.exchange.file-processing=file.processing.exchange
rabbitmq.routing.key.file-upload=file.upload
```

### Upload Service

Edit `upload-service/src/main/resources/application.properties`:
```properties
upload.directory=/app/uploads
spring.servlet.multipart.max-file-size=100MB
```

### Batch Processor

Edit `batch-processor/src/main/resources/application.properties`:
```properties
batch.directory=/app/batch
wiskbat.executable.path=wiskBat.exe
wiskbat.enabled=true
```

## Docker Configuration

### Environment Variables

In `docker-compose.yml`, you can configure:

**RabbitMQ:**
- `RABBITMQ_DEFAULT_USER`: Username (default: guest)
- `RABBITMQ_DEFAULT_PASS`: Password (default: guest)

**Batch Processor:**
- `WISKBAT_ENABLED`: Enable/disable wiskBat execution (default: false)

### Volumes

- `rabbitmq_data`: RabbitMQ persistent storage
- `upload_data`: Shared volume for uploaded files
- `batch_data`: Batch files generated by processor

## Stopping the Application

```bash
docker-compose down
```

To remove volumes as well:
```bash
docker-compose down -v
```

## Building Individual Modules

Build all modules:
```bash
mvn clean install
```

Build specific module:
```bash
mvn clean install -pl upload-service -am
mvn clean install -pl batch-processor -am
```

## Message Flow Details

1. **File Upload:**
   - User uploads file to Upload Service
   - File saved to `/app/uploads` with unique filename
   - Checksum calculated (SHA-256)

2. **Message Publishing:**
   - FileUploadMessage created with metadata
   - Message sent to `file.processing.exchange`
   - Routing key: `file.upload`

3. **Message Consumption:**
   - Batch Processor listens to `file.upload.queue`
   - Receives FileUploadMessage
   - Processes the file information

4. **Batch Processing:**
   - Generates Windows batch file with metadata
   - Batch file saved to `/app/batch`
   - Calls wiskBat executable (if enabled)
   - wiskBat extracts CSV data to database

## WiskBat Integration

**What is wiskBat?**
wiskBat is an external program that reads uploaded files, extracts CSV data, and stores it to a database.

**Batch File Format:**
The generated batch file contains:
```batch
@echo off
REM File Metadata
set ORIGINAL_FILENAME=data.csv
set FILE_PATH=/app/uploads/abc123-def456.csv
set FILE_SIZE=1024
set CHECKSUM=sha256hash...

echo Processing file: %ORIGINAL_FILENAME%
```

**Configuration:**
- Set `wiskbat.enabled=true` to enable execution
- Configure `wiskbat.executable.path` to point to wiskBat executable
- On non-Windows platforms, the system will attempt to run via bash (for testing)

## Troubleshooting

### Application can't connect to RabbitMQ

- Ensure RabbitMQ container is running: `docker ps`
- Check RabbitMQ health: `docker logs rabbitmq`
- Verify network connectivity

### File upload fails

- Check upload directory permissions
- Verify max file size settings
- Review upload-service logs: `docker logs upload-service`

### Messages not being consumed

- Check batch-processor is running: `docker ps`
- Verify queue bindings in RabbitMQ Management UI
- Review batch-processor logs: `docker logs batch-processor`

### View logs

```bash
# All services
docker-compose logs -f

# Specific service
docker logs upload-service -f
docker logs batch-processor -f
docker logs rabbitmq -f
```

## Technologies Used

- **Spring Boot 3.2.0** with Virtual Threads support
- **Java 21** with Virtual Threads enabled
- **Spring AMQP** (RabbitMQ integration)
- **RabbitMQ 3.12** (with Management Plugin)
- **Docker** and **Docker Compose**
- **Maven** (multi-module build)

## Development Notes

### Adding New Modules

1. Create module directory
2. Add module to parent `pom.xml` `<modules>` section
3. Create module `pom.xml` with parent reference
4. Add module dependency to other modules if needed

### Modifying Message Structure

1. Update `FileUploadMessage` in messaging module
2. Rebuild all dependent modules
3. Update batch file generation logic in `BatchFileService`

## License

This is a demonstration project for learning purposes.
