#!/bin/bash
# Script to start Spring Boot Backend

echo "Starting Scheduler API Backend..."
echo "================================="

cd ~/Development/scheduler/scheduler-api

# Set JAVA_HOME to Java 21
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"

# Start Spring Boot
echo "Starting Spring Boot on port 8080..."
nohup mvn spring-boot:run > api-output.log 2>&1 &

