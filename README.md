# SecureGame

SecureGame is a gamified cybersecurity training application designed for university-level computer science students. It bridges the gap between theoretical knowledge and practical security instincts through interactive, adversarial scenarios, with a built-in campaign mode for structured progression and an arcade mode for continuous practice.

## Features

- **Gamified Campaign Mode:** Angry Birds-style level map tracking your progression across various cybersecurity topics.
- **Dynamic Arcade Mode:** Survive endless threats to earn a high score.
- **Topics Covered:** Basic Authentication, Malware Fundamentals, Social Engineering, Network Security, Cryptography, Web Vulnerabilities (OWASP), Zero Trust, and more.
- **Multiplayer Capabilities:** Classroom teacher/student roles, room lobbies, and real-time topic delivery over authenticated WebSocket/STOMP.

## Course Alignment and Review

The review compared SecureGame with course lecture PDFs supplied locally under `SW/` (the PDFs are not checked into this repository). See the [assessment report](docs/PROJECT_ASSESSMENT_REPORT.md) for evidence, partial/missing features, and remaining risks, and the [class and methods report](docs/PROJECT_CLASS_AND_METHODS_REPORT.md) for implementation-level examples. Lecture material is not a signed-off project acceptance rubric, so the review does not claim that every instructor requirement is met.

## Prerequisites

- **Java 21** or later
- **Maven** (optional, uses the included wrapper `mvnw`)
- **Python 3** (for running the integration test scripts)

## Getting Started

### 1. Build and Run the Backend

Navigate to the project directory and run the Spring Boot application using the Maven wrapper:

```bash
# On Linux / macOS
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

The application starts with the local H2 database and serves on port `8080`. Run the automated tests first with `./mvnw test`. For quick game/UI checks, open `http://localhost:8080`; email login requires an SMTP server. The local default points to `localhost:2525` and does not print OTPs to the console. To use a real email provider, set its SMTP host, port, username and password through environment variables; never commit credentials.

### 2. Access the Application

Open your browser and navigate to:
```
http://localhost:8080
```
From here you can register an account or log in with an existing one. In local development, OTP email is sent to the SMTP server configured at `localhost:2525`; the app does not print OTPs to the console. Configure a local test SMTP service or mail provider to use email login.

### 3. Run the Automated Tests

```bash
./mvnw test
```

For a quick JavaScript syntax check (requires Node.js):

```bash
node --check src/main/resources/static/minigames.js
node --check src/main/resources/static/game.js
node --check src/main/resources/static/campaign.js
```

Optional browser integration scripts such as `test_auth.py` need their Python dependencies from `requirements.txt`, a running app, and a configured SMTP test service.

## Security

This project contains explicit educational demonstrations of vulnerabilities and adversarial logic. Do NOT run this application on a public-facing unhardened server without adjusting the default test configurations.

### 4. Running with Docker (Production/Full Stack)
If you prefer to run the application fully containerized with a persistent MariaDB database, you can use the provided Docker setup.

Set `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, `SECUREGAME_TOTP_ENCRYPTION_KEY`, and `SECUREGAME_ADMIN_API_KEY` in your shell or an ignored, local-only environment file before using Compose. The default SMTP service is Mailpit; configure `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_AUTH`, `SPRING_MAIL_STARTTLS`, `SPRING_MAIL_USERNAME`, and `SPRING_MAIL_PASSWORD` for an authenticated provider. Use unique values; the production profile has no fallback database credentials, and TOTP encryption/admin keys are required. Back up the TOTP key securely; losing it makes encrypted TOTP secrets unreadable.

First, build the Java application package:
```bash
./mvnw clean package -DskipTests
```

Next, start the containers using Docker Compose:
```bash
docker compose up -d --build
```
This will spin up **MariaDB database**, **Mailpit email catcher**, and **SecureGame app**.
- SecureGame app: `http://localhost:8080`
- Mailpit web mailbox (view OTP emails): `http://localhost:8025`


To stop the containers later, run:
```bash
docker compose down
```
