# Project Scheduler Application

Full-stack project management and scheduling application built with Spring Boot and Angular.

## 🚀 Features

- **Project Management**: View, search, and filter 222+ projects
- **Developer Tracking**: Manage 18 developers across 5 teams
- **Assignment Management**: Track 103+ project-developer assignments
- **Real-time Filtering**: Search and filter by status, priority, urgency
- **Sortable Grid**: Click any column header to sort data
- **Live Data**: Connected to SQL Server database via REST API
- **Monday.com Integration**: Python scripts to sync data from Monday.com

## 🏗️ Architecture

### Backend (Spring Boot 3.2.1)
- **Framework**: Spring Boot with Java 21
- **Database**: SQL Server (Azure SQL Edge in Docker)
- **ORM**: Hibernate/JPA
- **API**: RESTful endpoints with CORS enabled
- **Build Tool**: Maven 3.9.12

### Frontend (Angular 21)
- **Framework**: Angular 21 with TypeScript
- **Styling**: SCSS with custom components
- **HTTP Client**: Angular HttpClient
- **Routing**: Angular Router
- **Build Tool**: Angular CLI

### Database
- **Platform**: SQL Server 2022 (Azure SQL Edge)
- **Deployment**: Docker container
- **Port**: 1433
- **Tables**: projects, developers, assignments, teams

## 📋 Prerequisites

- **Java**: OpenJDK 21+
- **Node.js**: 18+ LTS
- **Maven**: 3.6+
- **Angular CLI**: 21+
- **Docker**: For SQL Server
- **Python**: 3.9+ (for Monday.com sync scripts)

## 🛠️ Installation

### 1. Clone Repository
```bash
git clone <repository-url>
cd scheduler
```

### 2. Database Setup
```bash
cd ~/Development/Docker
docker-compose -f azure-sql-edge-db-compose.yml up -d
```

### 3. Backend Setup
```bash
cd scheduler-api
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"
mvn clean install
mvn spring-boot:run
```

Backend will run on: http://localhost:8080

### 4. Frontend Setup
```bash
cd scheduler-ui
npm install --legacy-peer-deps
ng serve
```

Frontend will run on: http://localhost:4200

## 🔗 API Endpoints

### Projects
- `GET /api/projects` - Get all projects
- `GET /api/projects/{id}` - Get project by ID
- `GET /api/projects/status/{status}` - Filter by status
- `GET /api/projects/search?keyword={keyword}` - Search projects
- `POST /api/projects` - Create project
- `PUT /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project

### Developers
- `GET /api/developers` - Get all developers
- `GET /api/developers/{id}` - Get developer by ID
- `GET /api/developers/team/{teamId}` - Get by team

### Assignments
- `GET /api/assignments` - Get all assignments
- `GET /api/assignments/developer/{developerId}` - Get by developer
- `GET /api/assignments/project/{projectId}` - Get by project
- `POST /api/assignments` - Create assignment
- `DELETE /api/assignments/{id}` - Delete assignment

## 📊 Project Structure

```
scheduler/
├── scheduler-api/              # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/scheduler/
│   │   │   │   ├── model/          # JPA Entities
│   │   │   │   ├── repository/     # JPA Repositories
│   │   │   │   ├── controller/     # REST Controllers
│   │   │   │   └── SchedulerApiApplication.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
│
├── scheduler-ui/               # Angular Frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/
│   │   │   │   ├── models/         # TypeScript interfaces
│   │   │   │   └── services/       # API services
│   │   │   ├── features/
│   │   │   │   └── projects/       # Project components
│   │   │   ├── app.ts
│   │   │   ├── app.routes.ts
│   │   │   └── app.config.ts
│   │   └── styles.scss
│   ├── angular.json
│   └── package.json
│
├── sync_monday_to_mssql.py     # Monday.com project sync
├── sync_assignments_to_mssql.py # Monday.com assignment sync
├── start-backend.sh            # Backend startup script
└── README.md
```

## 🎨 UI Features

### Project Grid Columns
1. Project Name (sortable)
2. Status (color-coded badges)
3. Level (sortable)
4. Start Date (sortable)
5. QA Ready Date (sortable)
6. Target Prod Date (sortable)
7. Primary Team (TEJAS, CUST, GARVIT, OJASVII, SAANT)
8. Priority (1-10, color-coded)
9. Urgency (1-10, color-coded)
10. Dev Hours (sortable)
11. WF Hours (sortable)
12. Assigned Developers (badges)

### Filtering & Search
- **Search**: Real-time text search across project names
- **Status Filter**: Filter by In Progress, Backlog, Pending Authorization, Internal Tracking
- **Live Count**: Shows total and filtered project counts

### Sorting
- Click any column header to sort
- Click again to reverse sort direction
- Visual indicators: ⇅ (unsorted), ↑ (asc), ↓ (desc)

## 🔄 Monday.com Integration

### Sync Projects
```bash
python3 sync_monday_to_mssql.py
```
Syncs 208 projects from Monday.com board to database.

### Sync Assignments
```bash
python3 sync_assignments_to_mssql.py
```
Syncs developer assignments with timeline dates.

### Configuration
Create configuration files (not tracked in git):
- `monday.txt` - Monday.com API credentials
- `tp-database.txt` - Database connection details

## 🧪 Testing

### Backend Tests
```bash
cd scheduler-api
mvn test
```

### Frontend Tests
```bash
cd scheduler-ui
ng test
```

### API Testing
```bash
# Test projects endpoint
curl http://localhost:8080/api/projects | json_pp

# Test developers endpoint
curl http://localhost:8080/api/developers | json_pp

# Test assignments endpoint
curl http://localhost:8080/api/assignments | json_pp
```

## 🐳 Docker Commands

### Start Database
```bash
cd ~/Development/Docker
docker-compose -f azure-sql-edge-db-compose.yml up -d
```

### Stop Database
```bash
docker-compose -f azure-sql-edge-db-compose.yml down
```

### View Logs
```bash
docker-compose -f azure-sql-edge-db-compose.yml logs -f
```

## 📝 Development Workflow

1. **Start Database**: `docker-compose up -d`
2. **Start Backend**: `cd scheduler-api && mvn spring-boot:run`
3. **Start Frontend**: `cd scheduler-ui && ng serve`
4. **Access App**: http://localhost:4200
5. **API Docs**: http://localhost:8080/api

## 🌐 Environment Variables

### Backend (application.properties)
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=tp
spring.datasource.username=sa
spring.datasource.password=<password>
server.port=8080
```

### Frontend (environment.ts)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

## 📈 Database Schema

### Projects Table
- projects_id (PK)
- project_name
- status
- level
- target_prod_date
- start_date
- qa_ready_date
- priority
- urgency
- dev_hours
- wf_hours
- primary_team_id
- primary_app_name

### Developers Table
- developers_id (PK)
- first_name
- last_name
- teams_id
- position

### Assignments Table
- assignments_id (PK)
- projects_id (FK)
- developers_id (FK)
- start_date
- end_date
- ratio

## 🚧 Future Enhancements

- [ ] Dashboard with charts and statistics
- [ ] Developer workload view
- [ ] Timeline/Gantt chart visualization
- [ ] Project creation and editing UI
- [ ] Assignment management interface
- [ ] Export to Excel/PDF
- [ ] User authentication
- [ ] Real-time notifications
- [ ] Mobile responsive enhancements

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

This project is proprietary software.

## 👥 Team

- Backend: Spring Boot 3.2.1
- Frontend: Angular 21
- Database: SQL Server 2022
- Integration: Monday.com API

## 📞 Support

For issues or questions, please open an issue on GitHub.

---

**Built with ❤️ using Spring Boot and Angular**
