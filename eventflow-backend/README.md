# EventFlow Backend

Spring Boot backend application for EventFlow notification delivery platform.

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 14+
- Redis 6+
- Apache Kafka 3.0+

## Quick Start

### 1. Start Infrastructure Services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka & Zookeeper (ports 9092, 2181)
- Prometheus (port 9090)
- Grafana (port 3001)

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

Or skip tests:
```bash
./mvnw spring-boot:run -Dmaven.test.skip=true
```

The application will start on `http://localhost:8080`

## API Endpoints

- **GraphQL API**: `http://localhost:8080/graphql`
- **GraphQL Playground**: `http://localhost:8080/graphiql`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`

## Project Structure

```
src/
├── main/
│   ├── java/com/eventflow/
│   │   ├── analytics/          # Analytics & reporting
│   │   ├── common/             # Shared utilities
│   │   ├── identity/           # User & workspace management
│   │   ├── monitoring/         # Health checks & metrics
│   │   ├── notification/       # Core notification domain
│   │   ├── provider/           # Provider adapters
│   │   └── template/           # Template management
│   └── resources/
│       ├── db/migration/       # Flyway migrations
│       ├── graphql/            # GraphQL schema
│       └── application.yml     # Configuration
└── test/                       # Unit & integration tests
```

## Configuration

Key configuration properties in `application.yml`:

- **Database**: PostgreSQL connection settings
- **Redis**: Cache and distributed lock settings
- **Kafka**: Event streaming configuration
- **Security**: JWT and authentication settings
- **Providers**: Email, SMS, Push provider credentials

## Development

### Running Tests

```bash
./mvnw test
```

### Building

```bash
./mvnw clean package
```

### Database Migrations

Migrations are managed by Flyway and run automatically on startup.
Migration files are in `src/main/resources/db/migration/`

## Monitoring

- **Prometheus**: Scrapes metrics from `/actuator/prometheus`
- **Grafana**: Pre-configured dashboard for application metrics
- **Logs**: Application logs in `logs/` directory

## Docker

Build Docker image:
```bash
docker build -t eventflow-backend .
```

## Environment Variables

Key environment variables:

- `SPRING_DATASOURCE_URL`: PostgreSQL connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `SPRING_DATA_REDIS_HOST`: Redis host
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka brokers
- `JWT_SECRET`: Secret key for JWT tokens

## Architecture

EventFlow follows Clean Architecture principles:

- **Domain Layer**: Core business logic and entities
- **Application Layer**: Use cases and business workflows
- **Infrastructure Layer**: Database, messaging, external integrations
- **Interface Layer**: REST APIs, GraphQL resolvers, event consumers

## Tech Stack

- Spring Boot 3.2
- Spring Data JPA
- Spring GraphQL
- Spring Kafka
- Spring Security
- PostgreSQL
- Redis (Redisson)
- Flyway
- Kafka
- Prometheus & Grafana
