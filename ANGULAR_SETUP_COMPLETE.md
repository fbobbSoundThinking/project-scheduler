# Complete Angular Frontend Setup

## ✅ Angular Project Created!

Location: `~/Development/scheduler/scheduler-ui`

## 🔧 Step 1: Fix NPM Permissions & Install Dependencies

Run these commands (you'll need to enter your password):

```bash
# Fix npm permissions
sudo chown -R $(whoami) "/Users/frankbobb/.npm"

# Install dependencies
cd ~/Development/scheduler/scheduler-ui
npm install --legacy-peer-deps
```

This will take 2-3 minutes. Wait for it to complete.

## 🎨 Step 2: Install Angular Material

```bash
cd ~/Development/scheduler/scheduler-ui
ng add @angular/material --legacy-peer-deps
```

When prompted, choose:
- Theme: **Indigo/Pink** (or your preference)
- Typography: **Yes**
- Animations: **Yes**

## 📦 Step 3: Install Additional Dependencies

```bash
cd ~/Development/scheduler/scheduler-ui
npm install --legacy-peer-deps \
  chart.js \
  ng2-charts \
  date-fns \
  @angular/cdk
```

## 🚀 Step 4: Start the Angular App

```bash
cd ~/Development/scheduler/scheduler-ui
ng serve
```

Wait for compilation, then open: http://localhost:4200

You should see the default Angular welcome page!

## ✨ Step 5: Let Me Know When Ready

Once you see the Angular app running at http://localhost:4200, I'll:

1. Create Angular services to connect to your Spring Boot API
2. Create models for Project, Developer, Assignment
3. Build components:
   - Project List (with filtering by status)
   - Project Detail view
   - Developer List
   - Assignment Dashboard
4. Add routing
5. Style with Angular Material
6. Create a nice dashboard

## Current Setup

**Backend API:** ✅ Running on http://localhost:8080
**Database:** ✅ SQL Server with 222 projects, 18 developers, 103 assignments
**Frontend:**  ⏳ Ready to start after npm install

## Quick Commands

```bash
# Start Backend (if not running)
cd ~/Development/scheduler/scheduler-api
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"
mvn spring-boot:run

# Start Frontend (after npm install)
cd ~/Development/scheduler/scheduler-ui
ng serve

# Test Backend API
curl http://localhost:8080/api/projects | python3 -m json.tool
```

## File Structure Created

```
scheduler-ui/
├── angular.json              - Angular configuration
├── package.json              - Dependencies
├── src/
│   ├── app/
│   │   ├── app.component.ts  - Root component
│   │   ├── app.config.ts     - App configuration
│   │   └── app.routes.ts     - Routing configuration
│   ├── index.html            - Main HTML
│   ├── main.ts               - Bootstrap file
│   └── styles.scss           - Global styles
└── tsconfig.json             - TypeScript config
```

---

**Next:** Run the npm install commands above, then let me know when Angular is running! 🎉
