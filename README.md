# 🌱 Gharsih (غرسي) — Smart Plant Care Backend

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=spring-boot)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-blue)

**A comprehensive backend for a smart plant care mobile application built for Palestinian agriculture.**

*Graduation Project — 2025/2026*

</div>

---

## 📖 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Deployment](#deployment)
- [API Documentation](#api-documentation)

---

## Overview

**Gharsih** (غرسي — Arabic for "my plant") is a smart agricultural assistant mobile application designed for users in Palestine. The backend powers a complete plant care ecosystem that helps users:

- Discover the best plants to grow based on their environment, experience, and preferences
- Track their planted crops through the full lifecycle (planned → planted → harvested)
- Receive intelligent care reminders (watering, fertilizing, pruning, pest control, etc.)
- Stay informed with weather data, a planting calendar, and daily motivational quotes

The system features a full **admin panel** for managing all content, users, and system settings.

---

## Features

### 🔐 Authentication & Security
- **JWT Authentication** with access & refresh tokens (JJWT 0.12.6)
- **OAuth2 Social Login** (Google, Facebook) with Flutter deep-link callback
- **Role-Based Access Control (RBAC)** with dynamic roles and granular permissions
- **Session Management** — track active sessions, logout others, logout all
- **Password Reset** via email with tokenized reset links
- **Rate Limiting** with Bucket4j — per-IP throttling, IP blocking/unblocking, admin controls
- **CORS** configured for cross-origin mobile/web access

### 🏠 User — Home Screen
- **Home Dashboard** — aggregated data for the user's home screen
- **Live Weather** — real-time weather via OpenWeatherMap API based on user's location
- **Daily Quotes** — rotating motivational/agricultural quotes (Arabic + English)
- **Planting Calendar** — month-by-month guide of what to plant in Palestine

### 🌱 User — My Crops
- **Lifecycle Management** — `PLANNED` → `PLANTED` → `HARVESTED`
- **Add Plants** from recommendations or manual selection
- **Planting Steps** — step-by-step guided instructions per plant
- **Task Management** — auto-generated care tasks (watering, fertilizing, pruning, etc.)
- **Task Actions** — mark complete, snooze 1 hour, snooze until tomorrow
- **Overview Tab** — plant health summary with upcoming tasks
- **Info Tab** — detailed plant information (light, soil, water, care, harvest, uses)

### 🧠 Plant Recommendation Engine
- **Interactive Questionnaire** — multi-choice questions about user's environment
- **Scoring Algorithm** — plants scored via suitability matrix (plant × question option)
- **Weather-Aware** — temperature compatibility factored using GPS or saved location
- **Ranked Results** — top recommendations with individual suitability breakdown
- **One-Click Add** — select a recommended plant directly into "My Crops"

### 🔔 Notifications
- **In-App Notifications** — stored, paginated, filterable by type
- **FCM Push Notifications** — Firebase Cloud Messaging for Android/iOS
- **Scheduled Reminders** — automated plant care reminders via Spring Scheduler
- **Unread Count** — real-time unread badge count
- **Mark Read** — individual or bulk mark-as-read
- **Admin Broadcast** — send notifications to all users or active users

### ⚙️ User Preferences
- **Location Setting** — select from Palestinian cities or set GPS coordinates
- **Notification Preferences** — enable/disable push, set morning/evening reminder times
- **FCM Token Management** — register/update device tokens for push notifications
- **Language Preference** — Arabic or English

### 📧 Email System
- **Thymeleaf Templates** — beautiful HTML email templates
- **Welcome Email** — sent after registration
- **Password Reset Email** — secure tokenized reset link
- **Watering Reminder** — plant care email reminders
- **Fertilizing Reminder** — scheduled fertilization prompts
- **Verification Code** — email verification with 6-digit code

### 🛠️ Admin Panel

| Module | Description |
|--------|-------------|
| **Plants** | Full CRUD with categories, difficulty levels, bilingual content |
| **Plant Images** | Upload, bulk upload, reorder, set primary, update alt text |
| **Planting Questions** | Manage questionnaire with options, ordering, toggle active |
| **Plant Suitability** | Configure scoring matrix (plant × option), bulk operations |
| **Task Types** | Define care task types (watering, pruning, etc.) with icons |
| **Plant Tasks** | Assign recurring tasks to plants with intervals |
| **Months** | Palestinian calendar months with season & weather descriptions |
| **Month-Plants** | Link plants to their planting months with notes |
| **Quotes** | Manage daily quotes with categories, toggle active |
| **Notifications** | Send to users, broadcast, manage, cleanup old |
| **Users** | Create admin, promote/demote, check admin existence |
| **Dashboard** | Overview stats, user growth, plant stats, content stats |
| **Audit Logs** | Full action history with search, filter, statistics, cleanup |
| **Rate Limiting** | View stats, block/unblock IPs, reset limits, configuration |

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Framework** | Spring Boot | 4.0.1 |
| **Language** | Java (JDK) | 21 |
| **Database** | PostgreSQL | 16+ |
| **ORM** | Spring Data JPA / Hibernate | — |
| **Security** | Spring Security + JWT (JJWT) | 0.12.6 |
| **OAuth2** | Spring OAuth2 Client | — |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) | 2.8.3 |
| **Email** | Spring Mail + Thymeleaf | — |
| **Push Notifications** | Firebase Admin SDK (FCM) | 9.4.3 |
| **Rate Limiting** | Bucket4j + Caffeine Cache | 8.10.1 |
| **AOP** | Spring AOP (Audit Logging) | — |
| **Build Tool** | Maven | — |
| **Code Gen** | Lombok | — |
| **Testing** | JUnit 5 + Mockito + Spring Test | — |

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                    Mobile App (Flutter)                │
│                  (Android / iOS / Web)                 │
└────────────────────┬─────────────────────────────────┘
                     │ HTTPS / REST
                     ▼
┌──────────────────────────────────────────────────────┐
│              Spring Boot Backend (Port 8081)           │
│                                                        │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │
│  │  Security    │  │   User      │  │    Admin      │  │
│  │  Module      │  │   Module    │  │    Module     │  │
│  │             │  │             │  │              │  │
│  │ • Auth      │  │ • Home      │  │ • Plants     │  │
│  │ • JWT       │  │ • My Crops  │  │ • Questions  │  │
│  │ • OAuth2    │  │ • Recommend │  │ • Suitability│  │
│  │ • Roles     │  │ • Notifs    │  │ • Tasks      │  │
│  │ • Sessions  │  │ • Prefs     │  │ • Months     │  │
│  │ • Password  │  │ • Weather   │  │ • Quotes     │  │
│  └─────────────┘  └─────────────┘  │ • Dashboard  │  │
│                                     │ • Audit      │  │
│  ┌──────────────────────────────┐  │ • Rate Limit │  │
│  │        Common Module          │  └──────────────┘  │
│  │ • Email • Models • Enums     │                     │
│  │ • FCM  • Rate Limit • AOP   │                     │
│  └──────────────────────────────┘                     │
└────────────────────┬────────────┬────────────────────┘
                     │            │
              ┌──────▼──┐  ┌─────▼──────┐
              │PostgreSQL│  │  External   │
              │ Database │  │  Services   │
              │          │  │             │
              │ 18 Tables│  │• OpenWeather│
              │          │  │• Firebase   │
              │          │  │• Gmail SMTP │
              │          │  │• Google OAuth│
              └──────────┘  └─────────────┘
```

### Module Breakdown

| Module | Package | Responsibility |
|--------|---------|---------------|
| **Security** | `backend.Security` | Authentication, authorization, JWT, OAuth2, RBAC, sessions |
| **User** | `backend.user` | User-facing features: home, crops, recommendations, notifications, preferences |
| **Admin** | `backend.admin` | Admin panel: CRUD for all entities, dashboard, audit, user management |
| **Common** | `backend.common` | Shared models, repositories, enums, email, FCM, rate limiting, AOP |

---

## Database Schema

### Entity-Relationship Overview

The system uses **18 domain entities** plus **7 security entities** (25 tables total):

#### Security Entities
| Entity | Description |
|--------|-------------|
| `User` | System users with email, password, roles, provider |
| `Role` | RBAC roles (ROLE_USER, ROLE_ADMIN, custom) |
| `Permission` | Granular permissions (resource + action) |
| `RefreshToken` | JWT refresh tokens with expiry |
| `PasswordResetToken` | Time-limited password reset tokens |
| `UserSession` | Active login sessions with device info |
| `AuthProvider` | OAuth2 provider enum (LOCAL, GOOGLE, FACEBOOK) |

#### Domain Entities
| Entity | Description |
|--------|-------------|
| `Plant` | Plant catalog with bilingual names, care info, images |
| `PlantImage` | Multiple images per plant with ordering and alt text |
| `PlantingQuestion` | Recommendation questionnaire questions |
| `QuestionOption` | Answer options for each question |
| `PlantSuitability` | Scoring matrix (plant × option → score 1-10) |
| `PlantRecommendation` | User recommendation sessions with results |
| `UserQuestionResponse` | User's answers to recommendation questions |
| `UserPlant` | User's plants with status lifecycle |
| `TaskType` | Care task categories (watering, pruning, etc.) |
| `PlantTask` | Task templates assigned to plants |
| `UserPlantTask` | Individual task instances for user's plants |
| `WateringHistory` | Historical watering records |
| `Month` | Calendar months with Palestinian seasonal data |
| `MonthPlant` | Which plants to grow in which months |
| `DailyQuote` | Motivational/agricultural quotes |
| `Notification` | In-app notifications with type and read status |
| `UserPreference` | User settings (location, notifications, language) |
| `AuditLog` | System audit trail for all admin actions |

### Key Enums
| Enum | Values |
|------|--------|
| `PlantCategory` | VEGETABLES, FRUITS, AROMATIC_HERBS, ORNAMENTAL, INDOOR, TREES |
| `DifficultyLevel` | EASY, MEDIUM, HARD |
| `PlantStatus` | PLANNED, PLANTED, HARVESTED |
| `TaskStatus` | PENDING, COMPLETED, SKIPPED, SNOOZED |
| `Season` | SPRING, SUMMER, AUTUMN, WINTER |
| `NotificationType` | WATERING_REMINDER, FERTILIZING_REMINDER, GENERAL, DAILY_TIP, ... |
| `QuoteCategory` | GENERAL, MOTIVATIONAL, AGRICULTURAL, SEASONAL |

---

## API Endpoints

The system exposes **250 REST API endpoints** across **27 controllers**.

### Summary by Module

| Module | Controller | Endpoints | Auth Required |
|--------|-----------|-----------|---------------|
| **Auth** | AuthController | 4 | No |
| **Password** | PasswordController | 5 | Partial |
| **OAuth2** | OAuth2Controller | 2 | No |
| **Users** | UserController | 9 | Yes |
| **Sessions** | SessionController | 5 | Yes |
| **Roles** | RoleController | 12 | Admin |
| **Permissions** | PermissionController | 13 | Admin |
| **Health** | HealthController | 2 | No |
| **Home** | UserHomeController | 5 | Yes |
| **My Crops** | UserMyCropsController | 13 | Yes |
| **Recommendations** | UserPlantRecommendationController | 5 | Yes |
| **Notifications** | UserNotificationController | 7 | Yes |
| **Preferences** | UserPreferenceController | 6 | Yes |
| **Admin Plants** | AdminPlantController | 8 | Admin |
| **Plant Images** | AdminPlantImageController | 8 | Admin |
| **Questions** | AdminPlantingQuestionController | 21 | Admin |
| **Suitability** | AdminPlantSuitabilityController | 20 | Admin |
| **Tasks** | AdminPlantTaskController | 11 | Admin |
| **Months** | AdminMonthController | 16 | Admin |
| **Month-Plants** | AdminMonthPlantController | 15 | Admin |
| **Quotes** | AdminQuoteController | 9 | Admin |
| **Admin Notifs** | AdminNotificationController | 13 | Admin |
| **Admin Users** | AdminUserController | 4 | Admin |
| **Dashboard** | AdminDashboardController | 8 | Admin |
| **Audit** | AdminAuditController | 13 | Admin |
| **Rate Limit** | AdminRateLimitController | 11 | Admin |
| **Email Test** | EmailController | 5 | Admin |

### Key API Flows

#### 1. User Registration & Login
```
POST /api/auth/register        → Create account
POST /api/auth/login           → Get JWT token
POST /api/auth/refresh-token   → Refresh expired token
POST /api/auth/logout          → Invalidate session
```

#### 2. Plant Recommendation Flow
```
GET  /api/user/plant-recommendation/questions          → Get questionnaire
POST /api/user/plant-recommendation/submit-answers     → Submit answers + GPS
GET  /api/user/plant-recommendation/recommendations/{id} → View results
POST /api/user/plant-recommendation/select             → Add to My Crops
```

#### 3. My Crops Lifecycle
```
POST /api/user/my-crops/add/{plantId}           → Add plant (PLANNED)
GET  /api/user/my-crops/planned                 → View planned plants
POST /api/user/my-crops/mark-planted            → Mark as planted
GET  /api/user/my-crops/{id}/tasks              → View care tasks
POST /api/user/my-crops/tasks/action            → Complete/Snooze task
POST /api/user/my-crops/{id}/harvest            → Mark as harvested
```

#### 4. Home Screen Data
```
GET /api/user/home              → Full home screen data
GET /api/user/home/weather      → Live weather for user's location
GET /api/user/home/daily-quote  → Today's quote
GET /api/user/home/calendar     → Planting calendar overview
```

> 📦 **Full Postman Collection**: Import `postman/Gharsih_Complete_API_Collection.postman_collection.json` into Postman for all 250 endpoints with sample request bodies, auto-saved tokens, and organized folders.

---

## Getting Started

### Prerequisites

- **Java 21** (JDK 21+)
- **PostgreSQL 16+**
- **Maven 3.9+** (or use included `mvnw` wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/gharsih-backend.git
   cd gharsih-backend
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE graduation_db;
   ```

3. **Configure environment** (copy and edit `.env.example`)
   ```bash
   cp .env.example .env
   ```

4. **Run the application**
   ```bash
   # Using Maven wrapper (recommended)
   ./mvnw spring-boot:run

   # Or on Windows
   mvnw.cmd spring-boot:run
   ```

5. **Verify it's running**
   ```
   http://localhost:8081/api/security/health
   http://localhost:8081/swagger-ui.html
   ```

### Default Admin Account
On first startup, the system automatically creates an admin account:
- **Email**: `admin@gharsih.ps`
- **Password**: `admin123`

> ⚠️ Change the default admin credentials in production via environment variables.

---

## Configuration

All secrets are externalized using environment variables with sensible defaults for development.

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/graduation_db` |
| `DATABASE_USERNAME` | DB username | `postgres` |
| `DATABASE_PASSWORD` | DB password | `12345` |
| `JWT_SECRET` | JWT signing secret (256+ bits) | Dev default |
| `ADMIN_EMAIL` | Default admin email | `admin@gharsih.ps` |
| `ADMIN_PASSWORD` | Default admin password | `admin123` |
| `MAIL_USERNAME` | SMTP email address | `gharsihapp@gmail.com` |
| `MAIL_PASSWORD` | SMTP app password | — |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | — |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret | — |
| `WEATHER_API_KEY` | OpenWeatherMap API key | Dev default |
| `FIREBASE_CONFIG_PATH` | Path to Firebase service account JSON | — |

### Database Migrations

Flyway-style SQL migrations are included in `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| `V2` | Make audit user_id nullable |
| `V3` | Add system user for audit |
| `V4` | Seed Palestinian calendar months (12 months with weather data) |
| `V5` | Seed task types (Watering, Fertilizing, Pruning, Harvesting, Pest Control, Repotting, Sun Exposure) |

### Rate Limiting Configuration

```yaml
rate-limit:
  enabled: true
  default-requests-per-minute: 60
  default-requests-per-hour: 1000
  block-duration-seconds: 300      # 5 minutes
  warnings-before-block: 3
  user-type-limits:
    anonymous-requests-per-minute: 30
    user-requests-per-minute: 60
    admin-requests-per-minute: 200
```

---

## Project Structure

```
src/main/java/group/g/graduation/backend/
│
├── GraduationBackendApplication.java          # Main entry point
│
├── Security/                                   # 🔐 Authentication & Authorization
│   ├── config/                                #   Security config, CORS, Firebase
│   ├── controller/                            #   Auth, User, Role, Permission, Session, Health
│   ├── dto/                                   #   Request/Response DTOs
│   ├── exception/                             #   Auth exceptions
│   ├── filter/                                #   JWT auth filter
│   ├── jwt/                                   #   JWT provider, token generation
│   ├── model/                                 #   User, Role, Permission, Session entities
│   ├── oauth2/                                #   OAuth2 handlers & user service
│   ├── repository/                            #   JPA repositories
│   ├── service/                               #   Auth, user, role, permission services
│   ├── token/                                 #   Refresh token service
│   └── util/                                  #   Security utilities
│
├── user/                                       # 👤 User-Facing Features
│   ├── controller/
│   │   ├── UserHomeController.java            #   Home screen, weather, calendar, quote
│   │   ├── UserMyCropsController.java         #   Plant lifecycle, tasks, harvest
│   │   ├── UserPlantRecommendationController.java  # Recommendation engine
│   │   ├── UserNotificationController.java    #   In-app notifications
│   │   └── UserPreferenceController.java      #   Location, notification settings
│   ├── dto/                                   #   User DTOs
│   ├── model/                                 #   UserPlant, UserPlantTask (if any)
│   ├── repository/                            #   User-specific queries
│   └── service/
│       ├── UserHomeService.java               #   Home screen aggregation
│       ├── UserMyCropsService.java            #   Crop management logic
│       ├── UserPlantRecommendationService.java #  Scoring algorithm
│       ├── UserNotificationService.java       #   Notification CRUD
│       ├── UserPreferenceService.java         #   Preferences management
│       ├── WeatherService.java                #   OpenWeatherMap integration
│       ├── UserDailyQuoteService.java         #   Quote rotation
│       ├── MonthlyCalendarService.java        #   Calendar data
│       └── PlantCareReminderScheduler.java    #   Scheduled care reminders
│
├── admin/                                      # 🛠️ Admin Panel
│   ├── controller/                            #   13 admin controllers
│   ├── dto/                                   #   Admin DTOs
│   ├── mapper/                                #   Entity ↔ DTO mappers
│   └── service/                               #   Admin business logic
│
└── common/                                     # 🔧 Shared Infrastructure
    ├── annotation/                            #   Custom annotations (@Auditable)
    ├── aspect/                                #   AOP audit logging aspect
    ├── config/                                #   App config, Firebase config
    ├── dto/                                   #   Shared DTOs
    ├── email/                                 #   Email service + templates
    ├── enums/                                 #   PlantCategory, DifficultyLevel, Season, etc.
    ├── exception/                             #   Global exception handler
    ├── model/                                 #   18 JPA entities
    ├── ratelimit/                             #   Bucket4j rate limiting
    ├── repository/                            #   JPA repositories
    └── service/
        └── FcmService.java                    #   Firebase push notification service

src/main/resources/
├── application.yml                             # Main configuration
├── application.properties                      # Additional properties
├── db/migration/                               # SQL seed data (V2-V5)
└── templates/email/                            # Thymeleaf email templates
    ├── welcome.html
    ├── password-reset.html
    ├── verification-code.html
    ├── watering-reminder.html
    └── fertilizing-reminder.html
```

---

## Testing

The project includes **63 unit tests** covering critical business logic:

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `UserMyCropsServiceTest` | 33 | Crop lifecycle, task actions, validation |
| `UserNotificationServiceTest` | 13 | CRUD, ownership checks, mark read |
| `PlantCareReminderSchedulerTest` | 9 | Scheduled jobs, notification generation |
| `FcmServiceTest` | 7 | Push notification sending, error handling |
| `GraduationBackendApplicationTests` | 1 | Application context loads |

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserMyCropsServiceTest

# Run with verbose output
./mvnw test -Dsurefire.useFile=false
```

---

## Deployment

### Production Checklist

1. **Set all environment variables** (never use defaults in production)
2. **Use a strong JWT secret** (256+ bit random string)
3. **Change default admin credentials**
4. **Enable HTTPS** (use a reverse proxy like Nginx)
5. **Configure proper CORS origins**
6. **Set up Firebase** for push notifications
7. **Configure Gmail App Password** for email sending
8. **Set up PostgreSQL** with proper credentials and backups
9. **Review rate limiting** settings for production traffic

### Build for Production

```bash
# Create production JAR
./mvnw clean package -DskipTests

# Run the JAR
java -jar target/graduation-backend-0.0.1-SNAPSHOT.jar
```

### Docker (Optional)

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/graduation-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## API Documentation

### Swagger UI
Available at: `http://localhost:8081/swagger-ui.html`

### Postman Collection
A complete Postman collection with **255 requests** organized in **23 folders** is available at:

```
postman/Gharsih_Complete_API_Collection.postman_collection.json
```

**Features:**
- Auto-saves JWT token after login
- Bilingual sample data (Arabic + English)
- Organized by module with descriptive names
- Collection variables for dynamic IDs
- Test scripts for token management

### Import into Postman
1. Open Postman → **Import**
2. Select `Gharsih_Complete_API_Collection.postman_collection.json`
3. Run **"Login - Admin"** first (token auto-saved)
4. Test any endpoint — authorization header is automatic

---

## External Service Integration

| Service | Purpose | Setup |
|---------|---------|-------|
| **OpenWeatherMap** | Real-time weather data | Free API key at [openweathermap.org](https://openweathermap.org/api) |
| **Firebase (FCM)** | Push notifications | Service account JSON from Firebase Console |
| **Gmail SMTP** | Transactional emails | App password from Google Account settings |
| **Google OAuth2** | Social login | Credentials from Google Cloud Console |
| **Facebook OAuth2** | Social login | App ID from Meta Developer Portal |

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is developed as a **Graduation Project** for academic purposes.

---

<div align="center">

**Built with ❤️ in Palestine 🇵🇸**

*Gharsih — Because every plant deserves smart care* 🌱

</div>
