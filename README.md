# SecureGame

SecureGame is a gamified cybersecurity training application designed for university-level computer science students. It bridges the gap between theoretical knowledge and practical security instincts through interactive, adversarial scenarios, with a built-in campaign mode for structured progression and an arcade mode for continuous practice.

## Features

- **Gamified Campaign Mode:** Angry Birds-style level map tracking your progression across various cybersecurity topics.
- **Dynamic Arcade Mode:** Survive endless threats to earn a high score.
- **Topics Covered:** Basic Authentication, Malware Fundamentals, Social Engineering, Network Security, Cryptography, Web Vulnerabilities (OWASP), Zero Trust, and more.
- **Multiplayer Capabilities:** Supports classroom integrations with a teacher/student role framework and lobby system.

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

The application will start the web server, typically on port `8080`.

### 2. Access the Application

Open your browser and navigate to:
```
http://localhost:8080
```
From here you can register an account or log in with an existing one. OTPs are sent via simulated email to the server terminal console by default in local environments.

### 3. Running the Python Tests

To run the authentication test scripts, ensure you have installed the dependencies from `requirements.txt`:

```bash
pip install -r requirements.txt
python test_auth.py
```

## Security

This project contains explicit educational demonstrations of vulnerabilities and adversarial logic. Do NOT run this application on a public-facing unhardened server without adjusting the default test configurations.
