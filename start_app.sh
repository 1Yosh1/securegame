#!/bin/bash
./mvnw spring-boot:run > app.log 2>&1 &
echo $! > app.pid
