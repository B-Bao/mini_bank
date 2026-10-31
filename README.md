# Mini-Banking System 🏦

A secure and robust backend banking system built with **Java** and **Spring Boot**, focusing on financial transaction integrity, double-entry ledger accounting, concurrency control, and request idempotency.

---

## 🚀 Tech Stack & Core Technologies

* **Language & Framework:** Java 17+, Spring Boot, Spring Security
* **Data Persistence:** Spring Data JPA, Hibernate, PostgreSQL
* **Database Migration:** Flyway (`V1` to `V8` migrations)
* **Caching & Concurrency:** Redis (Distributed Idempotency & Caching)
* **Authentication:** JWT (JSON Web Tokens)
* **DevOps & Tooling:** Docker, Docker Compose, Maven, GitHub Actions (CI/CD)

---

## 📋 Key Features & Architecture

1. **Authentication & Authorization (`Spring Security & JWT`):**
    * Secure user registration, login, and token-based API authentication.
    * Role-based access control protecting sensitive banking endpoints.

2. **Double-Entry Bookkeeping Ledger (`LedgerEntry`):**
    * Ensures financial consistency by recording every monetary movement as balanced debit/credit ledger entries rather than simply updating raw account balances.

3. **Concurrency Control & Safety:**
    * Implements **Optimistic Locking (`@Version`)** on entity models to prevent lost updates and race conditions during high-concurrency transfers.

4. **Redis-Backed Idempotency (`IdempotencyInterceptor`):**
    * Prevents duplicate transactions caused by network retries or double-clicks using unique idempotency keys stored in Redis.

5. **Database Version Control (`Flyway`):**
    * Automated schema evolution from initial setup to advanced table constraints and indexing scripts.

---

## 🛠️ Project Structure

```text
mini_bank-main/
├── src/main/java/com/example/demo/
│   ├── config/          # Security & Web configurations
│   ├── controller/      # REST Endpoints (Auth, Account, Transaction, Transfer, Ledger)
│   ├── domain/          # Value objects (e.g., Money)
│   ├── dto/             # Request & Response Data Transfer Objects
│   ├── entity/          # Database Entities (User, Account, Transaction, Transfer, LedgerEntry)
│   ├── exception/       # Global exception handler & custom exceptions
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JWT filters, token providers & Idempotency interceptors
│   └── service/         # Business logic implementation
├── src/main/resources/
│   ├── db/migration/    # Flyway SQL migration scripts (V1 - V8)
│   └── application.yml  # Application configurations
└── docker-compose.yml   # Container orchestration for App, DB, and Redis
```

---

## ⚙️ How to Run the Project

Follow these simple steps to run the application seamlessly using Docker Compose (which automatically handles PostgreSQL, Redis, and the Spring Boot application containers).

### Prerequisites
* Make sure you have [Docker](https://www.docker.com/) and Docker Compose installed and running on your machine.
* Git installed.

### Step-by-Step Execution

1. **Clone the Repository**
   Open your terminal and clone the repository, then navigate into the project directory:
   ```bash
   git clone https://github.com/your-username/mini_bank.git
   cd mini_bank/mini_bank-main
   ```

2. **Run with Docker Compose**
   Execute the following command to build and spin up all required containers (Database, Cache, and Backend API):
   ```bash
   docker-compose up --build
   ```

3. **Access the Application**
    * Once all services start successfully, the backend application will be up and running at `http://localhost:8080`.
    * Flyway will automatically execute database migrations (`V1` to `V8`) upon startup.

---

## 🧪 Testing & Postman
* The project includes Postman resources located in the `.postman/` directory for testing endpoints like authentication, account creation, deposits, and money transfers.

---

## 👨‍💻 Author
* **Nguyen Huu Bao** – Backend Developer (Intern/Fresher)