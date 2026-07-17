# 🛡️ Employee Portal Security API

A **production-grade, feature-rich Spring Boot REST API** that demonstrates a complete enterprise-level backend architecture. This project covers everything from stateless JWT authentication and role-based access control, to Redis caching, asynchronous email processing, background scheduled tasks, and secure file management — all built in a clean, beginner-friendly way.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.15 |
| **Security** | Spring Security + JWT (JJWT 0.11.5) |
| **Database** | MySQL + Spring Data JPA (Hibernate) |
| **Caching** | Redis + Spring Cache Abstraction |
| **Email** | Spring Boot Mail (SMTP / Gmail) |
| **Async Processing** | Spring `@Async` + `ThreadPoolTaskExecutor` |
| **Scheduling** | Spring `@Scheduled` Tasks |
| **File Storage** | Local Filesystem via `MultipartFile` |
| **Utilities** | Lombok, Bean Validation (Jakarta) |
| **Build Tool** | Apache Maven |

---

## ✨ Features Overview

### 1. 🔐 JWT Authentication & Stateless Security

The core of the application is a stateless JWT security model. No sessions are stored on the server; instead, every request carries a self-contained signed token.

- **User Registration** (`POST /api/auth/register`): New users can register with a `username`, `email`, `password`, and a `role` (`ADMIN` or `EMPLOYEE`). Passwords are securely hashed using **BCryptPasswordEncoder** before being stored in the database — the plaintext password is never saved.

- **User Login** (`POST /api/auth/login`): Users authenticate with their credentials. On success, the API issues two tokens:
  - **Access Token** — Short-lived (15 minutes), used to access protected endpoints.
  - **Refresh Token** — Long-lived (7 days), used to silently obtain a new access token without re-login.

- **Token Refresh** (`POST /api/auth/refresh`): When the access token expires, the client sends the refresh token to receive a brand new access token, providing a seamless user experience.

- **`JwtAuthenticationFilter`**: A custom filter that intercepts every incoming HTTP request. It extracts the JWT from the `Authorization: Bearer <token>` header, validates it cryptographically using the `JwtService`, and populates the Spring Security `SecurityContextHolder` — all without a database lookup for every request.

- **`JwtService`**: Handles all cryptographic operations — generating, signing (HMAC-SHA256), and parsing JWTs. The secret key and expiration times are configurable via `application.properties`.

---

### 2. 👮 Role-Based Access Control (RBAC)

Endpoints are secured at the method level using `@PreAuthorize`, ensuring users can only access resources they are authorized for.

| Role | Permissions |
|---|---|
| `ADMIN` | Create, Read, Update, Delete employees; Upload/Download files |
| `EMPLOYEE` | Read employees; Upload/Download their own files |

This is enforced by `@EnableMethodSecurity` in `SecurityConfig`, which unlocks the use of `@PreAuthorize("hasAuthority('ADMIN')")` on controller methods.

---

### 3. 👥 Employee Management (Full CRUD API)

A complete set of REST endpoints for managing employee records, secured by RBAC.

| Method | Endpoint | Role Required | Description |
|---|---|---|---|
| `POST` | `/api/employees` | `ADMIN` | Create a new employee |
| `GET` | `/api/employees` | `ADMIN`, `EMPLOYEE` | Get all employees |
| `GET` | `/api/employees/{id}` | `ADMIN`, `EMPLOYEE` | Get a single employee by ID |
| `PUT` | `/api/employees/{id}` | `ADMIN` | Update an employee's details |
| `DELETE` | `/api/employees/{id}` | `ADMIN` | Delete an employee and their files |

---

### 4. ⚡ Redis Caching (Advanced Caching Strategy)

To dramatically reduce database load, employee data is cached in **Redis** using Spring's cache abstraction. The `RedisConfig` sets a default **TTL (Time-To-Live) of 60 minutes** and uses `GenericJackson2JsonRedisSerializer` to store data as readable JSON.

Three powerful caching annotations are used in `EmployeeService`:

| Annotation | Method | Effect |
|---|---|---|
| `@Cacheable("employees", key="#id")` | `getEmployeeById` | On first call → fetches from DB and stores in Redis. On subsequent calls → serves directly from Redis (Cache Hit), **no DB query**. |
| `@Cacheable("allEmployees")` | `getAllEmployees` | Caches the entire employee list. Subsequent calls are served from Redis. |
| `@CachePut("employees", key="#id")` | `updateEmployee`, `uploadProfileImage`, `uploadResume` | Updates the specific entry in Redis whenever data changes, keeping the cache fresh. |
| `@CacheEvict` | `deleteEmployee`, `updateEmployee` | Removes stale entries from Redis when data is modified or deleted. |

**How to observe it live:** Call `GET /api/employees/1` twice. The first call will print a `"Fetching from Database"` log in the console (Cache Miss). The second call will return silently with no log (Cache Hit — served by Redis).

---

### 5. 📁 File Upload & Download

Employees can have a **profile image** and a **resume** uploaded and stored on the server.

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/api/employees/{id}/profile-image` | `ADMIN`, `EMPLOYEE` | Upload a profile image (JPG, JPEG, PNG) |
| `GET` | `/api/employees/{id}/profile-image` | `ADMIN`, `EMPLOYEE` | Download the profile image |
| `POST` | `/api/employees/{id}/resume` | `ADMIN`, `EMPLOYEE` | Upload a resume (PDF only) |
| `GET` | `/api/employees/{id}/resume` | `ADMIN`, `EMPLOYEE` | Download the resume |

**`FileStorageService`** handles all file operations:
- **Extension Validation**: Rejects files that don't match the allowed extension list (e.g., `jpg,jpeg,png` or `pdf`).
- **UUID Filename Generation**: Each file is stored with a unique UUID prefix to prevent filename collisions.
- **Path Traversal Protection**: Filenames containing `..` are rejected.
- **Old File Cleanup**: When a new file is uploaded, the old one is automatically deleted from disk.
- **Cache Sync**: Uploading a file triggers `@CachePut` and `@CacheEvict` to keep Redis in sync.

File metadata (name, type, path) is stored in the `Employee` entity in MySQL. Files are physically stored in the `uploads/` directory (configurable via `file.upload-dir` in `application.properties`). Max file size is **5MB**.

---

### 6. 📧 Asynchronous Email Processing

Email operations are handled **asynchronously** using Spring's `@Async` annotation so they never block the HTTP response thread.

- **Welcome Email**: When a new employee is created, a welcome email is dispatched to the employee's email address **on a separate background thread** (`AsyncWorkerThread-X`). The API returns the `201 Created` response immediately without waiting for the email to be sent.

- **Admin Report Email**: The daily scheduler (see below) triggers an async email to the configured admin address containing the total employee count.

**`AsyncConfig`** configures a dedicated `ThreadPoolTaskExecutor` bean named `asyncExecutor` with:
- **Core Pool Size**: 2 threads (always kept alive)
- **Max Pool Size**: 5 threads (created on demand under load)
- **Queue Capacity**: 50 tasks (tasks queue here when all threads are busy)
- **Thread Name Prefix**: `AsyncWorkerThread-` (easily identifiable in logs)

Email is sent via Gmail SMTP configured in `application.properties`.

---

### 7. ⏰ Scheduled Background Tasks

The application runs automated background jobs using Spring's `@Scheduled` annotation. `SchedulingConfig` enables this with `@EnableScheduling`.

Two tasks run automatically in the background:

**Task 1 — Daily Employee Report** (`fixedRate = 600000ms / 10 minutes`):
- Queries the database for the total number of employees.
- Logs the count to the console.
- Asynchronously emails the report to the configured admin email.
- Runs on a fixed-rate schedule (can be changed to a daily cron expression for production: `@Scheduled(cron = "0 0 9 * * ?")`).

**Task 2 — Department Summary** (`initialDelay = 10s`, `fixedDelay = 120s`):
- Waits 10 seconds after startup, then runs every 2 minutes after the previous execution completes (`fixedDelay`).
- Fetches all employees and groups them by department using Java Streams.
- Logs a formatted department-wise headcount summary to the console.

---

### 8. 🚨 Global Exception Handling

Instead of returning raw Java stack traces, the API returns clean, structured JSON error responses for all failure cases using `@RestControllerAdvice`. Handled scenarios include:
- Expired or invalid JWT tokens
- Bad login credentials
- Validation errors on request body fields (`@Valid`)
- Resource not found errors
- Duplicate username or email during registration

---

## 📁 Project Structure

```
src/main/java/com/employeeportal/securityapi/
├── config/
│   ├── AsyncConfig.java          # ThreadPoolTaskExecutor setup for @Async
│   ├── RedisConfig.java          # Redis Cache TTL & Serialization config
│   └── SchedulingConfig.java     # Enables @Scheduled tasks
├── controller/
│   ├── AuthController.java       # /api/auth/** endpoints (register, login, refresh)
│   └── EmployeeController.java   # /api/employees/** CRUD + file endpoints
├── dto/
│   ├── AuthResponse.java         # Token response model
│   ├── EmployeeDTO.java          # Employee data transfer object
│   ├── LoginRequest.java         # Login body model
│   ├── RefreshTokenRequest.java  # Refresh token body model
│   └── RegisterRequest.java      # Registration body model
├── entity/
│   ├── Employee.java             # Employee JPA Entity (with file metadata)
│   ├── Role.java                 # ADMIN / EMPLOYEE Role enum
│   └── User.java                 # User JPA Entity (credentials + role)
├── exception/
│   └── GlobalExceptionHandler.java # @RestControllerAdvice for clean errors
├── repository/
│   ├── EmployeeRepository.java
│   └── UserRepository.java
├── scheduler/
│   └── ScheduledTaskService.java # Background scheduled tasks
├── security/
│   ├── CustomUserDetails.java            # Wraps User entity for Spring Security
│   ├── CustomUserDetailsService.java     # Loads user from DB by username
│   ├── JwtAuthenticationFilter.java      # Intercepts requests and validates JWT
│   ├── JwtService.java                   # JWT generation, signing & validation
│   └── SecurityConfig.java               # Security filter chain + RBAC rules
└── service/
    ├── AuthService.java          # Register, Login, Refresh token business logic
    ├── EmailService.java         # Sync + Async email sending
    ├── EmployeeService.java      # CRUD + File + Redis Cache logic
    └── FileStorageService.java   # File store, load, delete + validation
```

---

## 🛠️ Getting Started

### Prerequisites

- ✅ Java 21+
- ✅ Apache Maven
- ✅ MySQL Server (running on port 3306)
- ✅ Redis Server (running on port 6379)

### 1. Clone the Repository

```bash
git clone https://github.com/benarji-N220096/Springproject.git
cd Springproject
```

### 2. Configure `application.properties`

Open `src/main/resources/application.properties` and update these values:

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/employee_portal_db?createDatabaseIfNotExist=true
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD

# Gmail SMTP
spring.mail.username=YOUR_GMAIL_ADDRESS
spring.mail.password=YOUR_GMAIL_APP_PASSWORD

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# File Upload Directory
file.upload-dir=uploads
```

> **Note:** For Gmail, you must generate an **App Password** (not your regular Gmail password). Go to Google Account → Security → 2-Step Verification → App Passwords.

### 3. Run the Application

```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`.

---

## 🧪 Testing with Postman

### Step 1 — Register a New User
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "mypassword123",
    "role": "EMPLOYEE"
}
```

### Step 2 — Login & Get Tokens
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "username": "newuser",
    "password": "mypassword123"
}
```
Response will contain:
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Step 3 — Use the Token
For all protected endpoints, go to the **Authorization** tab in Postman, select **Bearer Token**, and paste the `accessToken`.

### Step 4 — Create an Employee (ADMIN only)
```
POST http://localhost:8080/api/employees
Authorization: Bearer <accessToken>
Content-Type: application/json

{
    "name": "Benarji",
    "email": "benarji@example.com",
    "department": "Engineering",
    "salary": 75000
}
```

### Step 5 — Upload Profile Image
```
POST http://localhost:8080/api/employees/1/profile-image
Authorization: Bearer <accessToken>
Body: form-data → Key: "file" (type: File) → Value: [select your image]
```

### Step 6 — Download Profile Image
```
GET http://localhost:8080/api/employees/1/profile-image
Authorization: Bearer <accessToken>
```

### Step 7 — Refresh an Expired Token
```
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## 🔑 Security Architecture Flow

```
Client Request
     │
     ▼
JwtAuthenticationFilter
     │ Extracts "Bearer <token>" from Authorization header
     │ Validates token signature & expiry via JwtService
     │ Loads UserDetails from DB via CustomUserDetailsService
     │ Sets Authentication in SecurityContextHolder
     ▼
SecurityConfig (Filter Chain)
     │ /api/auth/** → permitAll (public)
     │ /api/employees/** → authenticated
     ▼
Controller
     │ @PreAuthorize checks role (ADMIN / EMPLOYEE)
     ▼
Service Layer (Business Logic + Redis Cache)
     ▼
Repository Layer (MySQL via JPA)
```

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

---

## 👤 Author

**Benarji** — [@benarji-N220096](https://github.com/benarji-N220096)
