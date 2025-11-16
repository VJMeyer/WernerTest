# Distributed File Upload Service

A highly available file upload service with monitoring capabilities, designed to run on multiple servers with no single point of failure.

## Features

- **Upload Monitoring URL**: Each upload returns a unique URL to monitor upload progress
- **Distributed State**: Upload status stored in Redis, accessible from any server
- **No Single Point of Failure**: Runs on 3+ virtual servers with shared state
- **Message Persistence**: RabbitMQ ensures upload events are not lost
- **24-Hour TTL**: Completed upload status expires 24 hours after file retrieval
- **NFS Storage**: Files stored on highly available NFS mount

## Architecture

```
                    ┌─────────────┐
                    │   Nginx LB  │
                    └──────┬──────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
     ┌──────▼─────┐ ┌──────▼─────┐ ┌──────▼─────┐
     │  Server 1  │ │  Server 2  │ │  Server 3  │
     └──────┬─────┘ └──────┬─────┘ └──────┬─────┘
            │              │              │
            └──────────────┼──────────────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
     ┌──────▼─────┐ ┌──────▼─────┐ ┌──────▼─────┐
     │   Redis    │ │  RabbitMQ  │ │  NFS Store │
     └────────────┘ └────────────┘ └────────────┘
```

## API Endpoints

### Upload File
```bash
POST /api/uploads
Content-Type: multipart/form-data

Response:
{
  "uploadId": "uuid",
  "monitoringUrl": "http://server/api/uploads/{uploadId}/status",
  "message": "File uploaded successfully",
  "status": { ... }
}
```

### Monitor Upload Status
```bash
GET /api/uploads/{uploadId}/status

Response:
{
  "uploadId": "uuid",
  "fileName": "document.pdf",
  "fileSize": 1048576,
  "bytesTransferred": 524288,
  "state": "UPLOADING",
  "message": "Uploading... 50% complete",
  "createdAt": "2025-11-16T10:00:00Z",
  "updatedAt": "2025-11-16T10:00:30Z",
  "progressPercentage": 50,
  "serverId": "server1:8080"
}
```

### Download File
```bash
GET /api/uploads/{uploadId}/download

# Marks upload as RETRIEVED and starts 24-hour TTL
```

## Upload States

1. **PENDING** - Upload initiated
2. **UPLOADING** - File transfer in progress
3. **PROCESSING** - File being saved to storage
4. **COMPLETED** - Upload successful
5. **RETRIEVED** - File downloaded, 24-hour TTL started
6. **FAILED** - Upload failed
7. **EXPIRED** - Status removed after TTL

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for local development)
- Maven 3.9+ (for local development)

### Using Docker Compose

```bash
# Start all services (3 app instances, Redis, RabbitMQ, Nginx)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

Services will be available at:
- **Load Balancer**: http://localhost:80
- **Server 1**: http://localhost:8081
- **Server 2**: http://localhost:8082
- **Server 3**: http://localhost:8083
- **RabbitMQ Management**: http://localhost:15672 (admin/admin123)

### Example Usage

```bash
# Upload a file (via load balancer)
curl -X POST -F "file=@myfile.pdf" http://localhost/api/uploads

# Check status (can be called from any server)
curl http://localhost:8082/api/uploads/{uploadId}/status

# Download file
curl -O http://localhost:8083/api/uploads/{uploadId}/download
```

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Application port |
| `REDIS_HOST` | localhost | Redis server host |
| `REDIS_PORT` | 6379 | Redis server port |
| `RABBITMQ_HOST` | localhost | RabbitMQ server host |
| `RABBITMQ_PORT` | 5672 | RabbitMQ server port |
| `UPLOAD_STORAGE_PATH` | /mnt/nfs/uploads | NFS mount path |
| `UPLOAD_BASE_URL` | http://localhost:8080 | Base URL for monitoring URLs |

## Production Deployment

For production deployment on 3 virtual servers:

1. **Redis Cluster**: Deploy Redis in cluster mode for high availability
2. **RabbitMQ Cluster**: Configure RabbitMQ clustering with mirrored queues
3. **NFS Mount**: Mount shared NFS storage to `/mnt/nfs/uploads` on all servers
4. **Load Balancer**: Use external load balancer (HAProxy, AWS ALB, etc.)

### NFS Volume Configuration

In `docker-compose.yml`, replace the local volume with NFS:

```yaml
volumes:
  shared_uploads:
    driver: local
    driver_opts:
      type: nfs
      o: addr=nfs-server.example.com,nolock,soft,rw
      device: ":/exports/uploads"
```

## Development

### Build locally

```bash
mvn clean package
```

### Run locally

```bash
# Requires Redis and RabbitMQ running locally
mvn spring-boot:run
```

### Run tests

```bash
mvn test
```

## License

Apache License 2.0
