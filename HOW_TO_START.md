# How to Start the Scheduler Application

## Method 1: Using the Startup Script (Easiest)

```bash
cd ~/Development/scheduler
./start-backend.sh
```

## Method 2: Manual Start

Open a terminal and run:

```bash
cd ~/Development/scheduler/scheduler-api
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"
mvn spring-boot:run
```

## Method 3: IntelliJ IDEA (If you're using it)

1. Open the project folder: `~/Development/scheduler/scheduler-api`
2. Wait for Maven to import dependencies
3. Find `SchedulerApiApplication.java`
4. Click the green ▶️ play button next to the main method
5. Or right-click → Run 'SchedulerApiApplication'

## Verify It's Running

Wait about 10-20 seconds for startup, then test:

```bash
# Test in browser
open http://localhost:8080/api/projects

# Or test with curl
curl http://localhost:8080/api/projects

# Or test with curl and format JSON
curl http://localhost:8080/api/projects | python3 -m json.tool
```

## Expected Output

When running successfully, you should see:
```
Started SchedulerApiApplication in X.XXX seconds
Tomcat started on port 8080 (http)
```

## Troubleshooting

### Port Already in Use
If you get "Port 8080 is already in use":

```bash
# Find what's using port 8080
lsof -i :8080

# Kill the process (replace PID with actual number)
kill -9 <PID>
```

### Java Version Error
Make sure JAVA_HOME is set:

```bash
echo $JAVA_HOME
# Should show: /opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home

# If not set, add to ~/.zshrc:
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

### Database Connection Error
Make sure SQL Server is running:

```bash
docker ps | grep mssql
# Should show a running container

# If not running, start it:
cd ~/Development/Docker
docker-compose -f azure-sql-edge-db-compose.yml up -d
```

## Stop the Application

Press `Ctrl + C` in the terminal where it's running.

## Quick Reference

**Start Backend:**
```bash
cd ~/Development/scheduler/scheduler-api
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"
mvn spring-boot:run
```

**API Base URL:** http://localhost:8080

**Endpoints:**
- GET /api/projects
- GET /api/developers
- GET /api/assignments

**Logs:** Spring Boot will show logs in the terminal

---

Ready to start? Run:
```bash
cd ~/Development/scheduler
./start-backend.sh
```
