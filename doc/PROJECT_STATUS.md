# Scheduler Application - Project Status

## ✅ Spring Boot Backend - COMPLETE & RUNNING

**Status**: ✅ Running on http://localhost:8080  
**Database**: ✅ Connected to SQL Server (localhost:1433, database: tp)

### API Endpoints Available:

**Projects:**
- GET    /api/projects                    - Get all projects
- GET    /api/projects/{id}               - Get project by ID
- GET    /api/projects/status/{status}    - Get projects by status
- GET    /api/projects/search?keyword=    - Search projects
- POST   /api/projects                    - Create project
- PUT    /api/projects/{id}               - Update project
- DELETE /api/projects/{id}               - Delete project

**Developers:**
- GET    /api/developers                  - Get all developers
- GET    /api/developers/{id}             - Get developer by ID
- GET    /api/developers/team/{teamId}    - Get developers by team

**Assignments:**
- GET    /api/assignments                           - Get all assignments
- GET    /api/assignments/developer/{developerId}   - Get by developer
- GET    /api/assignments/project/{projectId}       - Get by project
- POST   /api/assignments                           - Create assignment
- DELETE /api/assignments/{id}                      - Delete assignment

### Test Commands:
```bash
curl http://localhost:8080/api/projects | json_pp
curl http://localhost:8080/api/developers | json_pp
curl http://localhost:8080/api/assignments | json_pp
```

### Project Structure:
```
scheduler-api/
├── src/main/java/com/example/scheduler/
│   ├── SchedulerApiApplication.java    - Main application
│   ├── model/
│   │   ├── Project.java                - Project entity
│   │   ├── Developer.java              - Developer entity
│   │   └── Assignment.java             - Assignment entity
│   ├── repository/
│   │   ├── ProjectRepository.java
│   │   ├── DeveloperRepository.java
│   │   └── AssignmentRepository.java
│   └── controller/
│       ├── ProjectController.java       - REST endpoints for projects
│       ├── DeveloperController.java     - REST endpoints for developers
│       └── AssignmentController.java    - REST endpoints for assignments
├── src/main/resources/
│   └── application.properties           - Database config & CORS
└── pom.xml                              - Maven dependencies

```

## ⏳ Angular Frontend - NEXT STEP

Create Angular application with:
```bash
cd ~/Development/scheduler
ng new scheduler-ui --routing --style=scss
```

Then install dependencies:
```bash
cd scheduler-ui
ng add @angular/material
npm install chart.js ng2-charts date-fns
```

## Running the Application

### Terminal 1 - Backend (Currently Running)
```bash
cd ~/Development/scheduler/scheduler-api
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"
mvn spring-boot:run
```

### Terminal 2 - Frontend (After creation)
```bash
cd ~/Development/scheduler/scheduler-ui
ng serve
```

### Terminal 3 - Database (Already Running)
```bash
docker ps | grep mssql
```

## Technologies

**Backend:**
- ✅ Java 21
- ✅ Spring Boot 3.2.1
- ✅ Spring Data JPA
- ✅ Hibernate
- ✅ Lombok
- ✅ Maven 3.9.12
- ✅ SQL Server Driver

**Frontend (To Be Created):**
- Angular 21
- Angular Material
- TypeScript
- RxJS
- SCSS

**Database:**
- SQL Server (Azure SQL Edge in Docker)
- 222 projects
- 18 developers  
- 103 assignments

## Next Steps

1. Create Angular frontend
2. Generate Angular services to call APIs
3. Create components for:
   - Project list/detail/create
   - Developer list
   - Assignment management
   - Dashboard
4. Add routing
5. Style with Angular Material
6. Add charts and visualizations

---

**Backend is ready and waiting for frontend! 🚀**
