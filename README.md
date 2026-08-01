# EventFlow

A scalable, multi-channel notification delivery platform built with Spring Boot and Next.js.

## Project Structure

```
EventFlow/
├── eventflow-backend/          # Spring Boot backend application
│   ├── src/                   # Java source code
│   ├── docker/                # Docker configuration
│   ├── logs/                  # Application logs
│   ├── pom.xml               # Maven configuration
│   └── docker-compose.yml    # Docker Compose for backend services
│
├── eventflow-frontend/         # Next.js frontend application
│   ├── app/                   # Next.js app directory (pages)
│   ├── components/            # React components
│   ├── lib/                   # GraphQL queries & utilities
│   ├── contexts/              # React contexts
│   └── package.json           # npm dependencies
│
├── PRD.md                     # Product Requirements Document
└── rules.md                   # Development rules and guidelines
```

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 14+
- Redis 6+
- Kafka 3.0+

### Backend Setup

```bash
cd eventflow-backend
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### Frontend Setup

```bash
cd eventflow-frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:3000`

## Features

- **Multi-Channel Support**: Email, SMS, Push Notifications, Webhooks
- **Provider Management**: Configure and manage multiple delivery providers
- **Template System**: Dynamic template rendering with versioning
- **Retry Logic**: Exponential backoff with configurable retry policies
- **Dead Letter Queue**: Failed message handling and replay
- **Analytics Dashboard**: Real-time metrics and performance monitoring
- **Audit Logging**: Complete audit trail for compliance
- **Idempotency**: Built-in duplicate detection
- **Rate Limiting**: Per-provider rate limiting
- **GraphQL API**: Modern API with type safety

## Documentation

- **PRD.md**: Complete product requirements and architecture
- **rules.md**: Development guidelines and coding standards

## Tech Stack

### Backend
- Spring Boot 3.2
- PostgreSQL (primary database)
- Redis (caching & distributed locks)
- Kafka (event streaming)
- GraphQL (API layer)

### Frontend
- Next.js 16
- React 19
- TypeScript
- Apollo Client (GraphQL)
- Tailwind CSS
- shadcn/ui components

## License

Proprietary
