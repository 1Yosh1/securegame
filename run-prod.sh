#!/bin/zsh

# 1. Replace this with your actual Gmail address
export SPRING_MAIL_USERNAME="eyoas.zewd@gmail.com"

# 2. Replace this with the 16-character App Password generated from Google
export SPRING_MAIL_PASSWORD="yglm vsan djzg ptnj"

# 3. Leave these as default if you set up MariaDB locally with these credentials
export SPRING_DATASOURCE_URL="jdbc:mariadb://localhost:3306/securegamedb"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="root1234"

# 4. Set the active profile to "prod" so Spring Boot uses application-prod.properties
export SPRING_PROFILES_ACTIVE="prod"

echo "Starting SecureGame in Production Mode..."
./mvnw spring-boot:run
