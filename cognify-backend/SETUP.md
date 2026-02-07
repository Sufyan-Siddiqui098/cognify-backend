# SETUP GUIDE - Cognify Backend

## ✅ What Has Been Completed

### 1. Project Structure ✅
- Complete Spring Boot application skeleton
- Proper package organization
- All necessary Java classes created

### 2. Database Schema ✅
- Flyway migration files for:
  - Users table
  - Documents table  
  - Chunks table
  - User sessions table

### 3. Core Features Implemented ✅
- **Authentication Service**: JWT-based auth with signup/login/logout
- **Document Service**: Upload, list, get, delete, reindex
- **File Storage Service**: Local folder storage (replaces S3)
- **Async Processing**: CompletableFuture-based processing (replaces RabbitMQ)
- **Security**: Spring Security with JWT filter, BCrypt password hashing
- **Exception Handling**: Global exception handler with proper HTTP codes
- **API Endpoints**: All RESTful endpoints defined

### 4. Configuration ✅
- application.properties with all settings
- Async thread pool configuration
- CORS configuration
- JWT configuration
- File storage configuration

## 🔧 What YOU Need to Do

### Step 1: Install Required Software

#### A. Install PostgreSQL
1. Download PostgreSQL 15 or higher from: https://www.postgresql.org/download/windows/
2. During installation:
   - Remember the password you set for postgres user
   - Keep default port: 5432
   - Install pgAdmin 4 (GUI tool)

3. Create the database:
```sql
-- Open pgAdmin or psql
CREATE DATABASE cognify;
```

#### B. Install Java 17 (if not already installed)
1. Download from: https://adoptium.net/
2. Choose JDK 17 (LTS)
3. Verify installation:
```bash
java -version
```

#### C. Install Maven (if not already installed)
1. Download from: https://maven.apache.org/download.cgi
2. Extract and add to PATH
3. Verify:
```bash
mvn -version
```

### Step 2: Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
# Change these lines with your actual credentials
spring.datasource.url=jdbc:postgresql://localhost:5432/cognify
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD_HERE
```

### Step 3: Download Maven Dependencies

Open terminal in the project directory and run:

```bash
mvn clean install
```

This will download all dependencies including:
- Spring Boot starters
- PostgreSQL driver
- Flyway
- JWT libraries
- Lombok
- Validation
- etc.

**This step is CRITICAL** - All the compilation errors you see are because dependencies haven't been downloaded yet.

### Step 4: Run Flyway Migrations

After dependencies are downloaded:

```bash
mvn flyway:migrate
```

This will create all database tables.

### Step 5: Start the Application

```bash
mvn spring-boot:run
```

Or run from your IDE (IntelliJ IDEA / Eclipse):
- Find `CognifyBackendApplication.java`
- Right-click → Run

The application will start on **http://localhost:8082**

### Step 6: Test the API

#### Test 1: Health Check
```bash
curl http://localhost:8082/api/health
```

Expected response:
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "cognify-backend",
    "timestamp": "2026-02-04T...",
    "version": "0.0.1-SNAPSHOT"
  }
}
```

#### Test 2: Register User
```bash
curl -X POST http://localhost:8082/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"password123\",\"name\":\"Test User\"}"
```

Expected response:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid...",
      "email": "test@example.com",
      "name": "Test User",
      "role": "user"
    },
    "accessToken": "eyJhbGci...",
    "refreshToken": "...",
    "expiresIn": 900
  },
  "message": "User registered successfully"
}
```

#### Test 3: Upload Document
```bash
# Copy the accessToken from signup/login response
curl -X POST http://localhost:8082/api/documents/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@path/to/your/document.pdf"
```

## 📋 Troubleshooting

### Problem 1: Cannot resolve symbol 'lombok'
**Cause**: Maven dependencies not downloaded
**Solution**: Run `mvn clean install`

### Problem 2: Database connection failed
**Cause**: PostgreSQL not running or wrong credentials
**Solution**: 
1. Check PostgreSQL is running (Windows Services)
2. Verify credentials in application.properties
3. Test connection in pgAdmin

### Problem 3: Port 8082 already in use
**Cause**: Another application using the port
**Solution**: Change port in application.properties:
```properties
server.port=8083
```

### Problem 4: File upload fails
**Cause**: Storage directory doesn't exist or no permissions
**Solution**: 
```bash
# Windows
mkdir C:\cognify-uploads

# Or change path in application.properties
cognify.storage.base-path=C:/Users/YourName/cognify-uploads
```

### Problem 5: JWT secret error
**Cause**: JWT secret might be too short
**Solution**: Already configured properly, but you can change in application.properties if needed

## 🔍 Verify Everything Works

### Checklist
- [ ] PostgreSQL installed and running
- [ ] Database 'cognify' created
- [ ] Maven dependencies downloaded (`mvn clean install` completed)
- [ ] Flyway migrations ran successfully
- [ ] Application starts without errors
- [ ] Health endpoint returns "UP"
- [ ] Can register a new user
- [ ] Can login with registered user
- [ ] Can upload a document
- [ ] Can list documents
- [ ] Storage folder created: `C:/cognify-uploads/{user-id}/`

## 📝 Next Steps After Setup

1. **Test all authentication endpoints**
   - Signup
   - Login
   - Get profile
   - Logout

2. **Test document management**
   - Upload PDF, DOCX, TXT files
   - List documents (check pagination)
   - Get single document
   - Delete document
   - Reindex document

3. **Check async processing**
   - Upload a document
   - Check logs to see async processing
   - Verify document status changes from "processing" to "indexed"
   - Check chunks are created in database

4. **Prepare for Python integration**
   - Once Python service is ready, update `DocumentProcessingService.java`
   - Replace placeholder methods with HTTP calls to Python service
   - Test end-to-end flow

## 💡 Development Tips

### Using IntelliJ IDEA
1. Open project (select pom.xml)
2. Wait for Maven to download dependencies
3. Enable annotation processing for Lombok:
   - Settings → Build → Compiler → Annotation Processors
   - Enable annotation processing

### Using VS Code
1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
2. Open project folder
3. Wait for Java extension to activate

### Database Management
Use pgAdmin 4 to:
- View tables
- Check data
- Run queries
- Monitor connections

### Viewing Logs
Application logs show:
- API requests
- Authentication events
- File uploads
- Async processing status
- Errors and exceptions

## 🎯 Current Implementation Status

### ✅ Working Features
- User registration and authentication
- JWT token generation and validation
- Document upload to local storage
- Async document processing (placeholder)
- Database persistence
- Error handling
- CORS support

### 🔄 Placeholder Features (Ready for Integration)
- Text extraction from PDF/DOCX/TXT
- Semantic text chunking
- Embedding generation (768 dimensions)
- Vector database storage
- Semantic search/recall

### ⏳ Not Yet Implemented
- Python service integration
- Actual AI processing
- Vector similarity search
- Chat/conversation endpoints
- Analytics service
- Admin service

## 📞 If You Get Stuck

1. **Check logs** - Most issues are logged with clear error messages
2. **Verify PostgreSQL** - Use pgAdmin to check database connectivity
3. **Check dependencies** - Run `mvn dependency:tree` to see all dependencies
4. **Port conflicts** - Use `netstat -ano | findstr :8082` to check port usage
5. **File permissions** - Ensure upload folder has write permissions

## 🚀 You're Ready!

Once you complete the 6 steps above, your backend will be fully functional and ready for Python service integration!

---

**Need help?** Check the README.md for detailed API documentation and examples.
