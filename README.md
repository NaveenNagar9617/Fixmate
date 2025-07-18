# 🔧 FixMate - Campus Maintenance Complaint System

A full-stack web application for managing campus maintenance complaints. Built for streamlining the process of reporting, tracking, and resolving infrastructure issues in educational institutions.

## ✨ Features

- **Student Portal** - Submit complaints with photo evidence, track status, upvote issues
- **Staff Dashboard** - View assigned complaints, update resolution status, add comments
- **Admin Panel** - Manage staff, view analytics, handle escalations, broadcast announcements
- **Real-time Notifications** - WebSocket-powered instant updates on complaint status changes
- **SLA Tracking** - Automatic deadline monitoring with escalation for overdue complaints
- **Analytics Dashboard** - Visual insights with charts for complaint trends, category breakdown, staff performance
- **Photo Comparison** - Before/after photo comparison for resolved issues
- **Smart Assignment** - Workload-based automatic staff assignment

## 🛠 Tech Stack

### Backend
- **Java 17** with **Spring Boot 3**
- **Spring Security** + JWT Authentication
- **Spring Data JPA** with PostgreSQL
- **Spring WebSocket** for real-time notifications
- **Redis** for caching and session management
- **MinIO** for file/photo storage
- **Swagger/OpenAPI** for API documentation

### Frontend
- **React 18** with **TypeScript**
- **Vite** for fast development and building
- **TailwindCSS** for styling
- **Zustand** for state management
- **React Query** for server state and caching
- **Recharts** for analytics visualizations
- **SockJS + STOMP** for WebSocket communication

### DevOps
- **Docker** for containerization
- **Nginx** as reverse proxy
- **GitHub Actions** for CI/CD pipeline

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Redis
- MinIO (or compatible S3 storage)

### Backend Setup
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at `http://localhost:5173` and the backend API at `http://localhost:8080`.

## 📁 Project Structure

```
FixMate/
├── backend/                  # Spring Boot application
│   └── src/main/java/com/fixmate/
│       ├── config/           # Security, Redis, WebSocket, MinIO config
│       ├── controller/       # REST API controllers
│       ├── dto/              # Data Transfer Objects
│       ├── exception/        # Custom exceptions & global handler
│       ├── mapper/           # Entity-DTO mappers
│       ├── model/            # JPA entities & enums
│       ├── repository/       # Data access layer
│       ├── scheduler/        # SLA check cron jobs
│       ├── security/         # JWT filter, provider, user details
│       └── service/          # Business logic layer
├── frontend/                 # React + Vite application
│   └── src/
│       ├── components/       # Reusable UI components
│       │   ├── analytics/    # Charts and stats components
│       │   ├── common/       # Shared components
│       │   ├── complaint/    # Complaint-specific components
│       │   ├── layout/       # App layout, sidebar, topbar
│       │   ├── notification/ # Notification components
│       │   └── ui/           # Base UI primitives
│       ├── hooks/            # Custom React hooks
│       ├── lib/              # Axios client, utilities
│       ├── pages/            # Route-level page components
│       ├── store/            # Zustand state stores
│       └── tests/            # Unit tests
├── nginx/                    # Nginx reverse proxy config
└── .github/                  # CI/CD workflow
```

## 📝 API Documentation

Once the backend is running, visit `http://localhost:8080/swagger-ui.html` for interactive API documentation.

## 📄 License

This project is developed as part of an academic initiative.
