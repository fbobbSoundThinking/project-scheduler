# Complete Setup Steps - Spring Boot + Angular

## ✅ Already Completed
- Java 21 installed
- Maven 3.9.12 installed  
- Node.js 24.12.0 installed
- npm 11.6.2 installed
- SQL Server running in Docker

## 🔧 Install Angular CLI (Run Manually)

You need to run this command and enter your password:
```bash
sudo npm install -g @angular/cli
```

After installation, verify:
```bash
ng version
```

## 📦 Create Spring Boot Backend

### Option 1: Using Spring Initializr Website
1. Go to: https://start.spring.io/
2. Configure:
   - Project: **Maven**
   - Language: **Java**
   - Spring Boot: **3.2.1** (or latest 3.x)
   - Java: **21**
   - Packaging: **Jar**
   - Group: **com.example**
   - Artifact: **scheduler-api**
   - Dependencies:
     - Spring Web
     - Spring Data JPA
     - MS SQL Server Driver
     - Lombok
     - Validation
     - Spring Boot DevTools

3. Click "Generate" and download
4. Extract to `~/Development/scheduler/scheduler-api`

### Option 2: I'll Create It For You
Let me know and I'll generate the complete Spring Boot project with:
- Entity models (Project, Developer, Assignment)
- JPA Repositories
- Service layer
- REST Controllers
- CORS configuration
- Database connection to your SQL Server

## 🎨 Create Angular Frontend

After Angular CLI is installed:
```bash
cd ~/Development/scheduler
ng new scheduler-ui --routing --style=scss

cd scheduler-ui
npm install @angular/material
```

## 🚀 Quick Start Commands

### Terminal 1 - Backend
```bash
cd ~/Development/scheduler/scheduler-api
mvn spring-boot:run
```
Access: http://localhost:8080

### Terminal 2 - Frontend  
```bash
cd ~/Development/scheduler/scheduler-ui
ng serve
```
Access: http://localhost:4200

### Terminal 3 - Database
```bash
# Already running
docker ps | grep mssql
```

## Next Steps After Setup

1. Create entity models for tp database
2. Build REST APIs
3. Create Angular services
4. Build UI components
5. Connect frontend to backend

---

**Ready when you are! Let me know when Angular CLI is installed.**
