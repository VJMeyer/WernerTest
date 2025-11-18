# File Processing System with RabbitMQ

A multi-module Maven project demonstrating a file processing system using Spring Boot, RabbitMQ message broker, and Docker. The system consists of a file upload service (producer), a batch processor (consumer), and shared messaging infrastructure.

## Architecture

This system follows a microservices architecture with three main components:

1. **Upload Service** - Web application for file uploads (Producer)
2. **Batch Processor** - Command-line application for processing files (Consumer)
3. **RabbitMQ** - Message broker for asynchronous communication

### Data Flow

```
User → Upload Service → File Storage → RabbitMQ → Batch Processor → wiskBat → Database
         (Web API)                     (Message)    (Consumer)      (CSV Extract)
```

## Features

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

## Project Structure

```
.
├── pom.xml                           # Parent POM
├── docker-compose.yml                # Docker orchestration
│
├── messaging/                        # Shared messaging module
│   ├── pom.xml
│   └── src/main/java/de/kisters/hmt/cloud/messaging/
│       ├── config/
│       │   └── RabbitMQConfig.java   # RabbitMQ configuration
│       └── dto/
│           └── FileUploadMessage.java # Message DTO
│
├── upload-service/                   # File upload web service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/de/kisters/hmt/cloud/services/upload/
│       ├── UploadServiceApplication.java
│       ├── controller/
│       │   └── FileUploadController.java
│       └── service/
│           └── FileUploadService.java
│
└── batch-processor/                  # Batch processing service
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

### Using Docker Compose (Recommended)

1. **Start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Access the services:**
   - Upload Service API: http://localhost:8080
   - RabbitMQ Management UI: http://localhost:15672 (username: `guest`, password: `guest`)

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
