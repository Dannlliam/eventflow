"# EventFlow Backend

Enterprise-grade event-driven notification orchestration platform built with Java 17, Spring Boot 3.x, and Hexagonal Architecture.

## 🏗️ Architecture

EventFlow follows **Domain-Driven Design (DDD)** with a **Hexagonal (Ports & Adapters)** architecture, ensuring clean separation between domain logic, application services, and infrastructure concerns.

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer (GraphQL + REST)              │
├─────────────────────────────────────────────────────────────┤
│                    Application Services                      │
├─────────────────────────────────────────────────────────────┤
│            ┌────────────────────────────────────────┐        │
│            │           Domain (Core)                │        │
│            │  ┌─────────┐ ┌──────────┐ ┌────────┐  │        │
│            │  │Template  │ │Notification│ │Provider│  │        │
│            │  │ Bounded  │ │ Bounded   │ │ Bounded│  │        │
│            │  │ Context  │ │ Context   │ │ Context│  │        │
│            │  └─────────┘ └──────────┘ └────────┘  │        │
│            │  ┌─────────┐ ┌──────────────────┐     │        │
│            │  │Identity │ │Analytics Bounded │     │        │
│            │  │ Bounded │ │ Context          │     │        │
│            │  │ Context │ │                  │     │        │
│            │  └─────────┘ └──────────────────┘     │        │
│            └────────────────────────────────────────┘        │
├─────────────────────────────────────────────────────────────┤
│              Infrastructure (Adapters)                       │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────┐  │
│  │PostgreSQL│ │  Redis   │ │  Kafka     │ │SendGrid/Twilio│  │
│  └──────────┘ └──────────┘ └────────────┘ └──────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## ✨ Features

- **📝 Template Management** — Create, version, and manage notification templates with Handlebars templating engine
- **📨 Multi-Channel Delivery** — Email (SendGrid), SMS (Twilio), Push notifications, and Webhooks
- **📊 Provider Abstraction** — Pluggable provider architecture; easily add new delivery providers
- **🔄 Event-Driven** — Fully asynchronous processing via Apache Kafka with retry/DLQ patterns
- **🔐 Security** — JWT-based authentication, API key auth, role-based access control (RBAC)
- **⏱️ Rate Limiting** — Token-bucket rate limiter with Redis backend
- **🆔 Idempotency** — Built-in idempotency key support to prevent duplicate notifications
- **📈 Observability** — OpenTelemetry tracing, Micrometer metrics, Logstash logging, and a readiness health dashboard
- **🧩 GraphQL API** — Flexible querying with Spring GraphQL
- **📖 OpenAPI/Swagger** — Interactive REST API documentation at `/swagger-ui.html`
- **📂 Audit Trail** — Full event sourcing for notification lifecycle tracking

## 🧱 Bounded Contexts

| Context | Package | Responsibility |
|---------|---------|----------------|
| **Template** | `com.eventflow.template` | Template versioning, content management, Channel routing |
| **Notification** | `com.eventflow.notification` | Notification lifecycle, status machine, retry/DLQ logic |
| **Provider** | `com.eventflow.provider` | Delivery provider abstraction (SendGrid, Twilio, etc.) |
| **Identity** | `com.eventflow.identity` | Users, roles, API keys, authentication |
| **Analytics** | `com.eventflow.analytics` | Delivery analytics and reporting |
| **Common** | `com.eventflow.common` | Shared value objects (Channel, BaseEntity), infrastructure (Redis, Kafka) |

## 🛠️ Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.1 |
| **Build** | Apache Maven 3.9.x |
| **Database** | PostgreSQL 16 (via Flyway migrations) |
| **Cache** | Redis 7 (via Redisson + Caffeine) |
| **Messaging** | Apache Kafka (Avro serialization + Schema Registry) |
| **API** | GraphQL (Spring GraphQL) + REST (OpenAPI/Swagger) |
| **Security** | Spring Security + OAuth2 Resource Server (JWT) + API Keys |
| **Monitoring** | Micrometer + OpenTelemetry + Prometheus |
| **Testing** | JUnit 5, TestContainers, PIT Mutation Testing |
| **Templating** | Handlebars (Java), OWASP HTML Sanitizer |

## 📋 Prerequisites

- **Java 17** (Eclipse Adoptium or equivalent)
- **Apache Maven 3.9+**
- **Docker Desktop** (for integration tests via TestContainers)
- **PostgreSQL 16** (for local development without Docker)
- **Redis 7** (for local development without Docker)
- **Apache Kafka** (for local development without Docker)

## 🚀 Getting Started

### 1. Clone and Build

```bash
git clone https://github.com/yourorg/eventflow-backend.git
cd eventflow-backend
mvn clean compile
```

### 2. Run Unit Tests

```bash
mvn test
```

This runs all unit tests (domain logic, services). Integration tests requiring Docker are excluded from the default run.

### 3. Run Integration Tests (Requires Docker)

```bash
# Set Docker context for TestContainers (Windows)
docker context use default

# Run full test suite including integration tests
mvn verify -P integration-test
```

### 4. Configure Environment

Create `application-local.yml` or set environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/eventflow
    username: eventflow
    password: eventflow
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092
```

### 5. Start the Application

```bash
# With local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or build and run JAR
mvn package -DskipTests
java -jar target/eventflow-backend-1.0.0.jar --spring.profiles.active=local
```

### 6. Access APIs

| Endpoint | Description |
|----------|-------------|
| `http://localhost:8080/swagger-ui.html` | Swagger UI (REST API) |
| `http://localhost:8080/graphiql` | GraphiQL IDE (GraphQL API) |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/actuator/metrics` | Micrometer metrics |
| `http://localhost:8080/actuator/prometheus` | Prometheus metrics |

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/eventflow/
│   │   ├── EventFlowApplication.java
│   │   ├── common/
│   │   │   ├── domain/              # Shared value objects, base entity
│   │   │   └── infrastructure/      # Redis, Idempotency, Rate Limiter
│   │   ├── identity/
│   │   │   ├── domain/              # User, ApiKey, EmailAddress, Role
│   │   │   └── infrastructure/      # JPA repositories, security config
│   │   ├── template/
│   │   │   ├── domain/              # Template, TemplateVersion
│   │   │   ├── application/         # Template use cases
│   │   │   └── infrastructure/      # JPA repositories, GraphQL resolvers
│   │   ├── notification/
│   │   │   ├── domain/              # Notification, Recipient, events
│   │   │   ├── application/         # Notification use cases
│   │   │   └── infrastructure/      # Kafka consumers, JPA repositories
│   │   ├── provider/
│   │   │   ├── domain/              # Provider, ProviderType
│   │   │   ├── application/         # Provider dispatch logic
│   │   │   └── infrastructure/      # SendGrid, Twilio dispatchers
│   │   └── analytics/
│   │       └── domain/              # Analytics results
│   └── resources/
│       ├── application.yml          # Main config
│       ├── logback-spring.xml       # Logstash logging config
│       └── avro/                    # Avro schemas for Kafka events
└── test/
    └── java/com/eventflow/
        ├── common/domain/           # BaseEntity, Channel tests
        ├── identity/domain/         # User, ApiKey, EmailAddress tests
        ├── template/domain/         # Template, TemplateVersion tests
        ├── notification/domain/     # Notification, Recipient tests
        ├── provider/domain/         # Provider, ProviderType tests
        └── notification/infrastructure/  # Integration tests
```

## 🔄 Domain Events (Kafka)

| Event | Producer | Consumers | Purpose |
|-------|----------|-----------|---------|
| `NotificationCreatedEvent` | Template Service | Dispatch Service | New notification to process |
| `DispatchRequestedEvent` | Dispatch Service | Provider Dispatchers | Deliver via specific channel |
| `DispatchResultEvent` | Provider Dispatchers | Notification Service | Update notification status |

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests (Docker required)
```bash
mvn verify
```

### Mutation Testing
```bash
mvn pitest:mutationCoverage
open target/pit-reports/index.html
```

### Test Coverage

| Category | Count | Status |
|----------|-------|--------|
| Unit Tests | 98 | ✅ Passing |
| Integration Tests | 9 | ⚠️ Requires Docker |
| **Total** | **107** | **98 passing, 9 environment-dependent** |

## 🔧 Configuration

### Application Profiles

| Profile | Purpose |
|---------|---------|
| `default` | Development with embedded services |
| `test` | TestContainers-based integration tests |
| `local` | Local development with external services |
| `docker` | Docker Compose environment |
| `production` | Production configuration |

### Key Configuration Properties

```yaml
eventflow:
  notification:
    max-retries: 3
    retry-delay-seconds: 60
  rate-limiter:
    default-max-tokens: 100
    default-window-seconds: 60
  idempotency:
    ttl-seconds: 86400  # 24 hours
```

## 🐳 Docker Compose

Start all dependent services:

```bash
docker compose up -d
```

This starts PostgreSQL 16, Redis 7, Kafka, and Schema Registry.

## 📊 Monitoring

### Health Dashboard
```bash
# Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Prometheus configuration
prometheus.yml is provided in the project root
```

### Grafana
A pre-configured Grafana dashboard (`grafana-dashboard.json`) is included for notification delivery metrics.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Run all tests (`mvn test`)
4. Ensure mutation coverage meets thresholds
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 📝 License

This project is proprietary software. See the LICENSE file for details.

## 🏆 Acknowledgments

- Domain-Driven Design principles by Eric Evans
- Hexagonal Architecture by Alistair Cockburn
- Spring Boot team for the excellent framework
"