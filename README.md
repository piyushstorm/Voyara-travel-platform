# Voyara Travel Platform ✈️🏨

A production-grade, real-time travel booking platform for flights, hotels, and group journeys.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security (JWT), Spring WebSocket (STOMP), PostgreSQL, Redis |
| **Frontend** | React 18, Vite, TailwindCSS, React Router, React Query (TanStack), Axios, Recharts, STOMP.js |
| **Infra** | Docker + Docker Compose, GitHub Actions CI |
| **Testing** | JUnit 5 + Mockito |
| **Docs** | springdoc-openapi (Swagger UI) |

## Features

### Core Platform
- User registration/login (JWT, role-based USER/ADMIN), password reset
- Flight search & results (filters: price, airline, stops, time; sort options)
- Hotel search & results (filters: price, star rating, amenities; sort options)
- Booking flow with mock payment gateway
- User dashboard (upcoming/past bookings, refund tracker, loyalty points)

### Deep-Dive Features
1. **Live Flight Status** — Real-time WebSocket push of flight status transitions
2. **Dynamic Pricing Engine** — Rules-based pricing with live updates and price freeze
3. **Cancellation & Refund System** — Time-based refund calculation with status tracking
4. **Interactive Seat & Room Selection** — SVG seat map with holds, room type grid
5. **Review & Rating System** — Stars, photos, replies, moderation, helpful votes
6. **Personalized Recommendations** — Hybrid content-based + collaborative filtering

### Differentiators
- Group booking with split payment
- Loyalty points/tier system
- Admin analytics dashboard (Recharts)

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌──────────────┐
│   React (Vite)  │────▶│  Spring Boot API │────▶│  PostgreSQL  │
│   Port 5173     │     │  Port 8080       │     │  Port 5432   │
└─────────────────┘     └──────────────────┘     └──────────────┘
                               │
                        ┌──────┴──────┐
                        │    Redis    │
                        │  Port 6379  │
                        └─────────────┘
```

### Backend Layered Architecture
```
Controller → Service → Repository → Database
    ↓           ↓
  DTOs      Entities
```

## Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- Docker & Docker Compose

### Quick Start (Docker)

```bash
# Start databases
docker compose up -d postgres redis

# Start backend (from backend/)
cd backend
./mvnw spring-boot:run

# Start frontend (from frontend/)
cd frontend
npm install
npm run dev
```

### Full Docker Compose
```bash
# Build and start everything
docker compose up --build
```

### Environment Variables

Copy `.env.example` to `.env` and configure:
```bash
cp .env.example .env
```

### API Documentation

Once the backend is running, visit:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs

## Development

```bash
# Backend
cd backend
./mvnw clean test          # Run tests
./mvnw spring-boot:run     # Start dev server

# Frontend
cd frontend
npm run dev                # Start Vite dev server
npm run build              # Production build
npm run lint               # Lint code
```

## Project Structure

```
├── backend/                 # Spring Boot backend
│   ├── src/main/java/com/travelplatform/
│   │   ├── config/          # Security, Redis, OpenAPI config
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data transfer objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Global exception handling
│   │   ├── repository/      # Spring Data repositories
│   │   ├── security/        # JWT, filters
│   │   └── service/         # Business logic
│   └── src/test/            # Unit tests
├── frontend/                # React + Vite frontend
│   ├── src/
│   │   ├── api/             # Axios config
│   │   ├── components/      # Reusable UI components
│   │   ├── context/         # React Context (auth)
│   │   ├── hooks/           # Custom hooks
│   │   └── pages/           # Route pages
│   └── public/
├── .github/workflows/       # CI/CD
├── docker-compose.yml       # Local dev services
└── README.md
```

## License

This project is for educational purposes.
