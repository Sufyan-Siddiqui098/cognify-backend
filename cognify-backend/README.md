# Cognify Backend - Spring Boot Application

## Overview

This is the Java Spring Boot backend for the Cognify RAG (Retrieval-Augmented Generation) system. It handles document management, authentication, and will integrate with a Python service for AI processing.

## Key Features Implemented

### ✅ Completed
- **Authentication & Authorization**: JWT-based auth with Spring Security
- **Document Upload**: Multipart file upload with validation
- **Local File Storage**: Files stored in `C:/cognify-uploads/{user_id}/` instead of S3
- **Async Processing**: Uses `@Async` and `CompletableFuture` instead of RabbitMQ
- **Database**: PostgreSQL with Flyway migrations
- **RESTful API**: Complete CRUD operations with proper response structure
- **Error Handling**: Global exception handler with proper HTTP status codes
- **Security**: BCrypt password hashing, CORS configuration, JWT tokens

### 🔄 Placeholder Implementation (Ready for Python Integration)
- **Text Extraction**: Currently simulated, will use Python service (PyPDF2, python-docx)
- **Semantic Chunking**: Placeholder logic, will use Python service
- **Embedding Generation**: Returns placeholder 768-dimensional vectors
- **Vector Storage**: Stores placeholder vector IDs, ready for actual vector DB
- **Recall/Search API**: Returns placeholder responses

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.1
- **Database**: PostgreSQL 15+
- **Migration**: Flyway
- **Security**: Spring Security + JWT
- **Build Tool**: Maven
- **Async**: Spring @Async with ThreadPoolTaskExecutor

## Prerequisites

### Required Software
1. **Java 17** or higher
2. **PostgreSQL 15+**
3. **Maven 3.8+** (or use included wrapper)
4. **Redis** (optional, for production caching)

### PostgreSQL Setup
```bash
# Create database
createdb cognify

# Or using psql
psql -U postgres
CREATE DATABASE cognify;
\q
```

## Installation & Setup

### 1. Clone and Navigate
```bash
cd "cognify-backend"
```

### 2. Configure Database
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cognify
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### 3. Install Dependencies
```bash
mvn clean install
```

### 4. Run Flyway Migrations
```bash
mvn flyway:migrate
```

### 5. Start Application
```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8082**

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/logout` - Logout user
- `GET /api/auth/profile` - Get user profile

### Documents
- `POST /api/documents/upload` - Upload document
- `GET /api/documents` - List user documents (paginated)
- `GET /api/documents/{id}` - Get document details
- `DELETE /api/documents/{id}` - Delete document
- `POST /api/documents/{id}/reindex` - Reindex document

### Recall (Placeholder)
- `POST /api/recall/search` - Semantic search (pending Python service)
- `POST /api/recall/{id}/feedback` - Submit feedback

### Health
- `GET /api/health` - Health check endpoint

## Configuration

### File Storage
```properties
# Local folder storage (instead of S3)
cognify.storage.base-path=C:/cognify-uploads
cognify.storage.allowed-types=application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain
cognify.storage.max-file-size=52428800  # 50MB
```

### Async Processing
```properties
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=20
spring.task.execution.pool.queue-capacity=100
```

### JWT Configuration
```properties
cognify.jwt.secret=your-secret-key-here
cognify.jwt.access-expiration=900000      # 15 minutes
cognify.jwt.refresh-expiration=604800000  # 7 days
```

### Embedding Dimensions
```properties
cognify.embedding.dimensions=768  # Base model
```

## Project Structure

```
src/main/java/com/cognify/cognify_backend/
├── config/
│   ├── AsyncConfig.java           # Async task executor
│   ├── SecurityConfig.java        # Spring Security config
│   ├── CorsConfig.java            # CORS configuration
│   ├── JwtConfig.java             # JWT settings
│   └── StorageConfig.java         # File storage settings
├── controller/
│   ├── AuthController.java        # Authentication endpoints
│   ├── DocumentController.java    # Document management
│   ├── RecallController.java      # Search endpoints (placeholder)
│   └── HealthController.java      # Health check
├── dto/
│   ├── ApiResponse.java           # Standard API response wrapper
│   ├── LoginRequest.java
│   ├── SignupRequest.java
│   ├── AuthResponse.java
│   ├── UserDto.java
│   └── DocumentDto.java
├── entity/
│   ├── User.java                  # User entity
│   ├── Document.java              # Document entity
│   ├── Chunk.java                 # Text chunk entity
│   └── UserSession.java           # Session management
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── FileStorageException.java
├── repository/
│   ├── UserRepository.java
│   ├── DocumentRepository.java
│   ├── ChunkRepository.java
│   └── UserSessionRepository.java
├── security/
│   ├── JwtTokenProvider.java      # JWT token generation/validation
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetailsService.java
│   └── UserPrincipal.java
└── service/
    ├── AuthService.java           # Authentication logic
    ├── DocumentService.java       # Document CRUD
    ├── FileStorageService.java    # Local file storage
    └── DocumentProcessingService.java  # Async processing
```

## Testing the API

### 1. Health Check
```bash
curl http://localhost:8082/api/health
```

### 2. Register User
```bash
curl -X POST http://localhost:8082/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "Test User"
  }'
```

### 3. Login
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 4. Upload Document
```bash
curl -X POST http://localhost:8082/api/documents/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@document.pdf"
```

### 5. List Documents
```bash
curl http://localhost:8082/api/documents \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## What You Need to Do Manually

### 1. Install PostgreSQL
Download and install PostgreSQL from: https://www.postgresql.org/download/

### 2. Create Database
```sql
CREATE DATABASE cognify;
```

### 3. Update Configuration
Update `application.properties` with your PostgreSQL credentials.

### 4. (Optional) Install Redis
For production, install Redis for session caching:
```bash
# Windows: Download from https://redis.io/download
# Or use Docker
docker run -d -p 6379:6379 redis:7-alpine
```

## Python Service Integration (Future)

When the Python service is ready, you'll need to:

1. **Update DocumentProcessingService.java**:
   - Replace placeholder methods with HTTP calls to Python service
   - Use RestTemplate or WebClient for communication

2. **Add Python Service Configuration**:
```properties
cognify.python.service.url=http://localhost:8083
cognify.python.service.timeout=30000
```

3. **Integration Points**:
   - Text extraction: `/api/extract`
   - Chunking: `/api/chunk`
   - Embedding generation: `/api/embed`
   - Vector storage: Update to use actual vector DB

## Development Notes

### Async Processing Flow
1. User uploads document → Saved to local folder
2. Document entity created with status `PROCESSING`
3. `@Async` method triggered with CompletableFuture
4. Background processing simulates:
   - Text extraction
   - Chunking (512 token chunks, 50 token overlap)
   - Embedding generation (768 dimensions)
   - Database storage
5. Document status updated to `INDEXED`

### File Storage Structure
```
C:/cognify-uploads/
└── {user_id}/
    ├── document1_timestamp.pdf
    ├── document2_timestamp.docx
    └── document3_timestamp.txt
```

### Security Features
- Passwords hashed with BCrypt (cost factor 12)
- JWT tokens with RS256-compatible signing
- Access tokens expire in 15 minutes
- Refresh tokens expire in 7 days
- User data isolation at query level
- File access validated by user ownership

## Troubleshooting

### Port Already in Use
Change server port in `application.properties`:
```properties
server.port=8083
```

### Database Connection Failed
Check PostgreSQL is running:
```bash
pg_isready
```

### File Upload Failed
Check storage path exists and has write permissions:
```bash
# Windows
mkdir C:\cognify-uploads
```

## Next Steps

1. ✅ Test all authentication endpoints
2. ✅ Test document upload and processing
3. ⏳ Develop Python service for AI processing
4. ⏳ Integrate vector database (Qdrant/Pinecone)
5. ⏳ Implement actual text extraction
6. ⏳ Implement semantic search with embeddings
7. ⏳ Add chat/conversation endpoints

## Questions or Issues?

If you have questions or need clarification on any part of the implementation, please ask!

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: February 2026  
**Status**: Development - Core functionality complete, Python integration pending
