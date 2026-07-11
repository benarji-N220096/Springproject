# Employee Portal Security API 🛡️

A comprehensive and beginner-friendly Spring Boot API designed to demonstrate a robust **JWT (JSON Web Token) Security Execution Flow**. This project implements stateless authentication, Role-Based Access Control (RBAC), and secure token management (Access & Refresh tokens) within an Employee Management domain context.

## 🚀 Tech Stack

* **Java Version:** 21
* **Framework:** Spring Boot 3.5.15
* **Security:** Spring Security
* **Database:** MySQL & Spring Data JPA
* **Authentication:** JWT (io.jsonwebtoken 0.11.5)
* **Utilities:** Lombok, Validation, Mail

## ✨ Key Features

1. **User Registration & Secure Passwords:** New users can register with a role. Passwords are securely hashed using `BCryptPasswordEncoder`.
2. **JWT Login & Token Generation:** Upon successful login, the API issues short-lived **Access Tokens** (15 mins) and long-lived **Refresh Tokens** (7 days).
3. **Stateless Authentication:** Every protected request is intercepted by a custom `JwtAuthenticationFilter` to validate the token mathematically—no server-side sessions required.
4. **Role-Based Access Control (RBAC):** Endpoints are protected based on user authorities (e.g., only an `ADMIN` can access specific employee management endpoints).
5. **Refresh Token Mechanism:** When an access token expires, clients can use the refresh token to silently request a new access token without requiring the user to log in again.
6. **Global Exception Handling:** Clean, predictable JSON error responses for events like expired tokens, bad credentials, or invalid requests.

## 🧠 Security Execution Flow

The security architecture revolves around several key components working in harmony:

### 1. The Components
* **SecurityConfig:** Defines public vs. private URLs and enforces a stateless session policy.
* **JwtAuthenticationFilter:** Intercepts HTTP requests, extracts the JWT from the `Authorization: Bearer <token>` header, and validates it.
* **JwtService:** Handles the cryptographic generation, signing, and validation of JWTs.
* **CustomUserDetailsService:** Loads user credentials and roles from the MySQL database based on the username inside the token.
* **AuthService:** Contains the business logic for user registration, password verification, and token coordination.

### 2. Point-to-Point Flows

* **Registration:** `POST /api/auth/register` -> `AuthController` -> `AuthService` -> Hashes Password -> Saves to Database.
* **Login:** `POST /api/auth/login` -> Authenticates via `AuthenticationManager` -> `JwtService` issues Access & Refresh Tokens -> Returns JSON response.
* **Accessing Data:** `GET /api/employees` -> Client attaches Token -> `JwtAuthenticationFilter` validates Token & extracts Roles -> Populates `SecurityContextHolder` -> `EmployeeController` returns data if `@PreAuthorize` allows it.
* **Refreshing Tokens:** `POST /api/auth/refresh` -> Client sends Refresh Token -> Validated -> New Access Token is generated and returned.

## 🛠️ Getting Started

### Prerequisites
* Java 21+
* Maven
* MySQL Server

### Configuration
1. Clone the repository.
2. Ensure MySQL is running and create a database as defined in your `application.properties` (or configure the URL/credentials accordingly).
3. Update `src/main/resources/application.properties` with your local database username and password.

### Running the Application
Use Maven to run the Spring Boot application:
```bash
mvn spring-boot:run
```
The server will start (typically on `http://localhost:8080`). You can use tools like **Postman** to test the registration, login, and protected endpoints.
