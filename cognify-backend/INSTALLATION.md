# MANUAL INSTALLATION REQUIRED

## Software You MUST Install (I Cannot Do This)

### 1. PostgreSQL Database ⚠️ REQUIRED

**Download:** https://www.postgresql.org/download/windows/

**Why:** The application needs a database to store users, documents, and chunks.

**Installation Steps:**
1. Download PostgreSQL 15 or higher installer
2. Run installer
3. During installation:
   - Set password for 'postgres' user (REMEMBER THIS!)
   - Keep default port: 5432
   - Install pgAdmin 4 (included)
4. After installation, open pgAdmin or psql
5. Create database:
   ```sql
   CREATE DATABASE cognify;
   ```

**How to verify:**
```bash
# Open Command Prompt
psql --version
# Should show: psql (PostgreSQL) 15.x
```

---

### 2. Java Development Kit (JDK) 17 ⚠️ REQUIRED

**Download:** https://adoptium.net/

**Why:** Spring Boot 3.2.1 requires Java 17.

**Installation Steps:**
1. Download Temurin JDK 17 (LTS)
2. Run installer
3. Choose "Add to PATH" option
4. Complete installation

**How to verify:**
```bash
java -version
# Should show: openjdk version "17.x.x"
```

---

### 3. Apache Maven ⚠️ REQUIRED

**Download:** https://maven.apache.org/download.cgi

**Why:** Build tool for Java projects, downloads dependencies.

**Installation Steps:**
1. Download Binary zip archive
2. Extract to: `C:\Program Files\Maven`
3. Add to PATH:
   - System Properties → Environment Variables
   - System Variables → Path → New
   - Add: `C:\Program Files\Maven\bin`
4. Open NEW command prompt

**How to verify:**
```bash
mvn -version
# Should show: Apache Maven 3.x.x
```

**Alternative:** Use included Maven wrapper:
```bash
# Windows
.\mvnw.cmd clean install

# This downloads Maven automatically
```

---

## After Installing Above Software

### Step 1: Configure Database Password

Open: `src/main/resources/application.properties`

Change this line:
```properties
spring.datasource.password=YOUR_POSTGRES_PASSWORD_HERE
```

Replace `YOUR_POSTGRES_PASSWORD_HERE` with the password you set during PostgreSQL installation.

---

### Step 2: Download Project Dependencies

Open terminal in project directory:

```bash
# Using installed Maven
mvn clean install

# OR using Maven wrapper (if Maven not installed)
.\mvnw.cmd clean install
```

**This will:**
- Download all Java libraries (Spring, JWT, PostgreSQL driver, etc.)
- Compile the project
- Run tests
- Take 5-10 minutes first time

**Wait until you see:**
```
[INFO] BUILD SUCCESS
```

---

### Step 3: Run Database Migrations

```bash
mvn flyway:migrate
```

**This will:**
- Connect to PostgreSQL
- Create tables (users, documents, chunks, user_sessions)

**You should see:**
```
[INFO] Successfully applied 4 migrations
```

---

### Step 4: Start Application

```bash
mvn spring-boot:run
```

**Wait for:**
```
Started CognifyBackendApplication in X.X seconds
```

---

## Optional Software (NOT Required for Development)

### 1. Redis (for production caching)

**Download:** https://github.com/microsoftarchive/redis/releases

**Why:** Used for session caching in production (optional for development)

**Note:** Not needed right now. Can add later.

---

### 2. IntelliJ IDEA or VS Code (Recommended IDE)

**IntelliJ IDEA Community Edition:**
- Download: https://www.jetbrains.com/idea/download/
- Free, best for Java development

**VS Code:**
- Download: https://code.visualstudio.com/
- Install "Extension Pack for Java"
- Install "Spring Boot Extension Pack"

**Why:** Better code editing, debugging, autocomplete

**Note:** Can also use Eclipse or any text editor

---

## Verification Checklist

After installing and configuring:

- [ ] PostgreSQL installed and running
- [ ] Database 'cognify' created
- [ ] Java 17 installed (`java -version` works)
- [ ] Maven installed (`mvn -version` works) OR using wrapper
- [ ] Database password configured in application.properties
- [ ] `mvn clean install` completed successfully
- [ ] `mvn flyway:migrate` completed successfully
- [ ] `mvn spring-boot:run` starts application
- [ ] Health endpoint works: http://localhost:8082/api/health

---

## If You Get Stuck

### PostgreSQL Not Installing
- Try restarting computer
- Run installer as Administrator
- Disable antivirus temporarily

### Java Not Found
- Make sure to restart terminal after installation
- Check PATH: `echo %PATH%` should include Java bin directory

### Maven Not Working
- Use Maven wrapper: `.\mvnw.cmd` instead of `mvn`
- No need to install Maven separately if wrapper works

### Database Connection Failed
1. Check PostgreSQL is running:
   - Windows Services → PostgreSQL should be "Running"
2. Check password is correct in application.properties
3. Try connecting with pgAdmin first

### Build Fails
- Make sure internet connection is stable (Maven downloads dependencies)
- Clear Maven cache: Delete `C:\Users\YourName\.m2\repository`
- Run `mvn clean install` again

---

## What I Cannot Do (These Require Your Computer Access)

❌ Install PostgreSQL
❌ Install Java JDK
❌ Install Maven
❌ Set environment variables
❌ Create database
❌ Run Maven commands
❌ Start the application

## What I Have Done ✅

✅ Created all Java code (30+ files)
✅ Created database migration scripts
✅ Configured application.properties
✅ Set up security and authentication
✅ Implemented all API endpoints
✅ Created async processing
✅ Set up file storage
✅ Wrote comprehensive documentation

---

## Quick Start Command Reference

```bash
# 1. Download dependencies (first time only)
mvn clean install

# 2. Create database tables (first time only)
mvn flyway:migrate

# 3. Start application
mvn spring-boot:run

# 4. Test health endpoint
curl http://localhost:8082/api/health

# 5. Register user
curl -X POST http://localhost:8082/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"password123\",\"name\":\"Test User\"}"
```

---

**Once you complete the 3 required installations above, you'll be ready to run the application!**

**Estimated time:** 30-60 minutes for complete setup

**Need help?** Check SETUP.md for detailed instructions
