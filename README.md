# Spring Boot with RabbitMQ and Docker

A Spring Boot application demonstrating message queue functionality using RabbitMQ as a message broker, fully containerized with Docker.

## Features

- Spring Boot Web application
- RabbitMQ message broker integration
- REST API for sending messages
- Message producer and consumer
- Docker and Docker Compose configuration
- Health check endpoints
- JSON message serialization

## Prerequisites

- Docker and Docker Compose installed
- Java 17 (for local development)
- Maven 3.6+ (for local development)

## Project Structure

```
.
├── src/
│   └── main/
│       ├── java/com/example/rabbitmq/
│       │   ├── RabbitMqApplication.java
│       │   ├── config/
│       │   │   └── RabbitMQConfig.java
│       │   ├── controller/
│       │   │   └── MessageController.java
│       │   ├── dto/
│       │   │   └── MessageDto.java
│       │   ├── producer/
│       │   │   └── MessageProducer.java
│       │   └── consumer/
│       │       └── MessageConsumer.java
│       └── resources/
│           └── application.properties
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## Quick Start

### Using Docker Compose (Recommended)

1. **Start the application and RabbitMQ:**
   ```bash
   docker-compose up --build
   ```

2. **Access the services:**
   - Spring Boot API: http://localhost:8080
   - RabbitMQ Management UI: http://localhost:15672 (username: `guest`, password: `guest`)

### Local Development

1. **Start RabbitMQ only:**
   ```bash
   docker-compose up rabbitmq
   ```

2. **Build and run the Spring Boot application:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## API Endpoints

### Send a Message

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello RabbitMQ!"}'
```

Response:
```json
{
  "status": "Message sent successfully",
  "content": "Hello RabbitMQ!"
}
```

### Health Check

```bash
curl http://localhost:8080/api/messages/health
```

Response:
```json
{
  "status": "UP"
}
```

### Actuator Health Endpoint

```bash
curl http://localhost:8080/actuator/health
```

## Testing the Message Flow

1. **Send a message via REST API:**
   ```bash
   curl -X POST http://localhost:8080/api/messages/send \
     -H "Content-Type: application/json" \
     -d '{"content": "Test message"}'
   ```

2. **Check the application logs to see:**
   - Message Producer: "Sending message: MessageDto{content='Test message', timestamp=...}"
   - Message Consumer: "Received message: MessageDto{content='Test message', timestamp=...}"

3. **Monitor in RabbitMQ Management UI:**
   - Open http://localhost:15672
   - Login with `guest`/`guest`
   - Check the Queues tab to see message statistics

## Configuration

### RabbitMQ Settings

Edit `src/main/resources/application.properties`:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

rabbitmq.queue.name=message.queue
rabbitmq.exchange.name=message.exchange
rabbitmq.routing.key=message.routing.key
```

### Docker Environment Variables

In `docker-compose.yml`, you can modify:
- RabbitMQ credentials
- Port mappings
- Resource limits

## Stopping the Application

```bash
docker-compose down
```

To remove volumes as well:
```bash
docker-compose down -v
```

## Architecture

1. **Message Flow:**
   - Client sends POST request to `/api/messages/send`
   - MessageController receives the request
   - MessageProducer sends message to RabbitMQ exchange
   - RabbitMQ routes message to queue based on routing key
   - MessageConsumer listens and processes messages from the queue

2. **Components:**
   - **TopicExchange**: Routes messages based on routing keys
   - **Queue**: Stores messages until consumed
   - **Binding**: Links exchange to queue with routing key
   - **Producer**: Sends messages to exchange
   - **Consumer**: Receives and processes messages from queue

## Troubleshooting

### Application can't connect to RabbitMQ

- Ensure RabbitMQ container is running: `docker ps`
- Check RabbitMQ health: `docker logs rabbitmq`
- Verify network connectivity

### Port already in use

- Change ports in `docker-compose.yml`
- Or stop services using those ports

### View application logs

```bash
docker logs spring-rabbitmq-app -f
```

### View RabbitMQ logs

```bash
docker logs rabbitmq -f
```

## Technologies Used

- **Spring Boot 3.2.0**
- **Spring AMQP** (RabbitMQ integration)
- **RabbitMQ 3.12** (with Management Plugin)
- **Docker** and **Docker Compose**
- **Maven** (build tool)
- **Java 17**

## License

This is a demonstration project for learning purposes.
