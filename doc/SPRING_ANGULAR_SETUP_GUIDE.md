# Spring Boot + Angular Application Setup Guide

## Architecture Overview
```
project-root/
├── backend/          # Spring Boot REST API
│   ├── src/
│   ├── pom.xml
│   └── application.properties
├── frontend/         # Angular UI
│   ├── src/
│   ├── package.json
│   └── angular.json
└── README.md
```

## Prerequisites Check

### Required Software
1. **Java JDK** (17 or 21 recommended)
   ```bash
   java -version
   ```

2. **Maven** (3.6+)
   ```bash
   mvn -version
   ```

3. **Node.js** (18+ LTS)
   ```bash
   node -v
   npm -v
   ```

4. **Angular CLI**
   ```bash
   npm install -g @angular/cli
   ng version
   ```

5. **IDE**
   - IntelliJ IDEA (recommended for Spring Boot)
   - VS Code with extensions (Java, Spring Boot, Angular)

## Step 1: Create Spring Boot Backend

### Option A: Using Spring Initializr (Recommended)
Visit: https://start.spring.io/

**Configuration:**
- Project: Maven
- Language: Java
- Spring Boot: 3.2.x (latest stable)
- Java: 17 or 21
- Packaging: Jar
- Group: com.example
- Artifact: scheduler-api
- Dependencies:
  - Spring Web
  - Spring Data JPA
  - MS SQL Server Driver
  - Lombok
  - Spring Boot DevTools
  - Validation

**Download and Extract:**
```bash
cd ~/Development/scheduler
unzip scheduler-api.zip
cd scheduler-api
```

### Option B: Using Maven Command
```bash
cd ~/Development/scheduler
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=scheduler-api \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

## Step 2: Configure Spring Boot Application

### application.properties
```properties
# Server Configuration
server.port=8080
spring.application.name=scheduler-api

# Database Configuration
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=tp;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=karst-habit-&01
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA Configuration
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
spring.jpa.properties.hibernate.format_sql=true

# CORS Configuration (for Angular)
spring.web.cors.allowed-origins=http://localhost:4200
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
spring.web.cors.allow-credentials=true

# Logging
logging.level.com.example=DEBUG
logging.level.org.springframework.web=DEBUG
```

### pom.xml Dependencies
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>com.microsoft.sqlserver</groupId>
        <artifactId>mssql-jdbc</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok (reduces boilerplate) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- DevTools (hot reload) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Step 3: Create Angular Frontend

```bash
cd ~/Development/scheduler
ng new scheduler-ui --routing --style=scss
cd scheduler-ui
```

**Angular CLI Prompts:**
- Would you like to add Angular routing? **Yes**
- Which stylesheet format? **SCSS**

### Install Additional Dependencies
```bash
# Angular Material (UI components)
ng add @angular/material

# HTTP Client (already included in Angular 18+)
# Bootstrap (optional)
npm install bootstrap

# Chart libraries (if needed)
npm install chart.js ng2-charts

# Date handling
npm install date-fns
```

## Step 4: Project Structure

### Backend Structure
```
scheduler-api/
├── src/main/java/com/example/scheduler/
│   ├── SchedulerApiApplication.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   └── WebConfig.java
│   ├── controller/
│   │   ├── ProjectController.java
│   │   ├── DeveloperController.java
│   │   └── AssignmentController.java
│   ├── service/
│   │   ├── ProjectService.java
│   │   ├── DeveloperService.java
│   │   └── AssignmentService.java
│   ├── repository/
│   │   ├── ProjectRepository.java
│   │   ├── DeveloperRepository.java
│   │   └── AssignmentRepository.java
│   ├── model/
│   │   ├── Project.java
│   │   ├── Developer.java
│   │   └── Assignment.java
│   └── dto/
│       ├── ProjectDTO.java
│       └── AssignmentDTO.java
└── src/main/resources/
    └── application.properties
```

### Frontend Structure
```
scheduler-ui/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── services/
│   │   │   │   ├── api.service.ts
│   │   │   │   ├── project.service.ts
│   │   │   │   └── developer.service.ts
│   │   │   ├── models/
│   │   │   │   ├── project.model.ts
│   │   │   │   └── developer.model.ts
│   │   │   └── interceptors/
│   │   │       └── auth.interceptor.ts
│   │   ├── features/
│   │   │   ├── projects/
│   │   │   │   ├── project-list/
│   │   │   │   ├── project-detail/
│   │   │   │   └── project-create/
│   │   │   ├── developers/
│   │   │   └── assignments/
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   └── pipes/
│   │   └── app.component.ts
│   ├── assets/
│   └── environments/
│       ├── environment.ts
│       └── environment.prod.ts
└── angular.json
```

## Step 5: First API Endpoint (Backend)

### Create REST Controller
```java
package com.example.scheduler.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "message", "Scheduler API is running"
        );
    }
}
```

### Run Spring Boot
```bash
cd scheduler-api
mvn spring-boot:run
# Or
./mvnw spring-boot:run
```

Test: http://localhost:8080/api/health

## Step 6: Connect Angular to API

### Create API Service
```typescript
// src/app/core/services/api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  healthCheck(): Observable<any> {
    return this.http.get(`${this.apiUrl}/health`);
  }
}
```

### Environment Configuration
```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### Test in Component
```typescript
// src/app/app.component.ts
import { Component, OnInit } from '@angular/core';
import { ApiService } from './core/services/api.service';

@Component({
  selector: 'app-root',
  template: `
    <h1>Scheduler App</h1>
    <p>API Status: {{ apiStatus }}</p>
  `
})
export class AppComponent implements OnInit {
  apiStatus = 'Checking...';

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    this.apiService.healthCheck().subscribe({
      next: (data) => this.apiStatus = data.message,
      error: (err) => this.apiStatus = 'API Down'
    });
  }
}
```

### Run Angular
```bash
cd scheduler-ui
ng serve
# Or with custom port
ng serve --port 4200
```

Visit: http://localhost:4200

## Step 7: Development Workflow

### Terminal Setup (3 terminals)
```bash
# Terminal 1: Backend
cd ~/Development/scheduler/scheduler-api
mvn spring-boot:run

# Terminal 2: Frontend
cd ~/Development/scheduler/scheduler-ui
ng serve

# Terminal 3: Database (already running)
docker ps | grep mssql
```

### Hot Reload Configuration

**Backend (Spring DevTools):**
- Automatic restart on code changes
- Already configured with spring-boot-devtools dependency

**Frontend (Angular):**
- Automatic browser refresh
- Built into `ng serve`

## Next Steps

1. **Create Entity Models** (Projects, Developers, Assignments)
2. **Create JPA Repositories**
3. **Implement Service Layer**
4. **Build REST Controllers**
5. **Create Angular Components**
6. **Implement Routing**
7. **Add Authentication/Authorization**
8. **Style with Angular Material**
9. **Add Form Validation**
10. **Implement Error Handling**

## Quick Commands Reference

```bash
# Backend
mvn clean install          # Build
mvn spring-boot:run        # Run
mvn test                   # Test

# Frontend  
ng generate component name # Create component
ng generate service name   # Create service
ng build                   # Build for production
ng test                    # Run tests
ng serve --open            # Run and open browser
```

## Recommended VS Code Extensions

- Java Extension Pack
- Spring Boot Extension Pack
- Angular Language Service
- Angular Snippets
- Prettier
- ESLint

## Recommended IntelliJ Plugins

- Angular and AngularJS
- Spring Boot
- Lombok
- .ignore

---

Ready to start? Let me know which step you'd like help with!
