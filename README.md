# 🧠 Cognify Backend — Privacy-First Local RAG System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Python](https://img.shields.io/badge/Python-3.10%2B-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109%2B-teal.svg)](https://fastapi.tiangolo.com/)
[![Qdrant](https://img.shields.io/badge/Vector%20DB-Qdrant-red.svg)](https://qdrant.tech/)
[![Ollama](https://img.shields.io/badge/LLM-Ollama%20(Llama%203.2)-purple.svg)](https://ollama.ai/)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL%2015%2B-blue.svg)](https://www.postgresql.org/)

**Cognify** is a 100% local, privacy-first Retrieval-Augmented Generation (RAG) platform. It allows users to upload documents (PDF, Word, Text, WhatsApp exports), automatically parse, sanitize, chunk, and embed them into a local vector database, and perform high-precision conversational retrieval backed by a local Large Language Model (LLM) — **with zero external cloud API calls or data leaks**.

---

## 📑 Table of Contents

- [Architectural Overview](#-architectural-overview)
- [System Architecture & Data Flow](#-system-architecture--data-flow)
  - [1. Document Ingestion Pipeline](#1-document-ingestion-pipeline)
  - [2. Semantic Retrieval & RAG Generation Pipeline](#2-semantic-retrieval--rag-generation-pipeline)
  - [3. Multi-Turn Conversation & Citation Flow](#3-multi-turn-conversation--citation-flow)
- [Microservices & Components](#-microservices--components)
  - [1. Core Java Spring Boot Backend (`cognify-backend`)](#1-core-java-spring-boot-backend-cognify-backend)
  - [2. Local Python AI Microservice (`cognify-ai-service`)](#2-local-python-ai-microservice-cognify-ai-service)
  - [3. Datastores & Infrastructure](#3-datastores--infrastructure)
- [Technology Stack](#-technology-stack)
- [Database Schema & Migrations](#-database-schema--migrations)
- [Project Directory Layout](#-project-directory-layout)
- [Prerequisites & Environment Setup](#-prerequisites--environment-setup)
- [Step-by-Step Installation & Running Guide](#-step-by-step-installation--running-guide)
  - [Step 1: Start Infrastructure (Qdrant & Ollama)](#step-1-start-infrastructure-qdrant--ollama)
  - [Step 2: Start Python AI Microservice](#step-2-start-python-ai-microservice)
  - [Step 3: Setup PostgreSQL & Run Java Backend](#step-3-setup-postgresql--run-java-backend)
- [Comprehensive API Reference](#-comprehensive-api-reference)
  - [Authentication & User Profile](#1-authentication--user-profile)
  - [Document Management](#2-document-management)
  - [Conversations & Chat (RAG Search)](#3-conversations--chat-rag-search)
  - [User Analytics](#4-user-analytics)
  - [Admin Platform Management](#5-admin-platform-management)
  - [System Health](#6-system-health)
- [Configuration Reference](#-configuration-reference)
- [Troubleshooting & FAQs](#-troubleshooting--faqs)

---

## 🏛️ Architectural Overview

Cognify is designed around a dual-service decoupled architecture:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Frontend Client                               │
│              (React / Next.js / Mobile / REST API Client)              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP / REST / JWT
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│             Core Application Backend (Spring Boot 3.4.2)               │
│                            Port: 8082                                  │
│                                                                        │
│  • JWT Auth & Security           • Local File Storage Service          │
│  • User & Session Management     • Async Task Dispatcher (@Async)      │
│  • Document Metadata Management  • Conversation & Message Tracking     │
│  • Adjacent Chunk Merging Engine • Citation Snapshot Preserver         │
│  • Admin Control Panel           • User Analytics Aggregator           │
└───────────────────┬──────────────────────────────┬─────────────────────┘
                    │                              │ Internal HTTP REST
                    │ JDBC / Flyway                │ (Port 8000)
                    ▼                              ▼
┌───────────────────────────────┐  ┌─────────────────────────────────────┐
│    PostgreSQL Database 15+    │  │    Local AI Service (FastAPI)       │
│                               │  │             Port: 8000              │
│  • users & user_sessions      │  │                                     │
│  • documents & chunks         │  │  • PyMuPDF4LLM & python-docx parser │
│  • conversations & messages   │  │  • Recursive Character Chunking     │
│  • message_sources (citations)│  │  • Chunk Quality Gating             │
└───────────────────────────────┘  │  • Sentence-Transformers (768-dim)  │
                                   │  • Reading-Order & Intent Scoring   │
                                   │  • Extractive Fallback Engine       │
                                   └───────┬─────────────────────┬───────┘
                                           │                     │
                                           │ Cosine Vector API   │ HTTP API
                                           ▼ (Port 6333)         ▼ (Port 11434)
                        ┌──────────────────────┐  ┌──────────────────────┐
                        │ Qdrant Vector Engine │  │  Ollama Local LLM    │
                        │ (cognify_vectors)    │  │  (Llama 3.2 3B/8B)   │
                        └──────────────────────┘  └──────────────────────┘
```

---

## 🔄 System Architecture & Data Flow

### 1. Document Ingestion Pipeline

```
[User Upload] (PDF / DOCX / TXT / WhatsApp)
       │
       ▼
[Spring Boot: DocumentController]
       │ 1. Validate magic bytes (PDF %, ZIP PK, text) & file size (<50MB)
       │ 2. Save file to disk: ./uploads/{userId}/{sanitizedName}_{timestamp}.ext
       │ 3. Insert record in PostgreSQL `documents` (Status: PROCESSING)
       │ 4. Dispatch @Async background task to DocumentProcessingService
       ▼
[FastAPI: /api/v1/process-document]
       │ 1. DocumentProcessor: parses document into clean markdown & page maps
       │ 2. ChunkingService: recursive text splitting (chunk_size: 750, overlap: 120)
       │ 3. Quality Gate (chunk_quality.py): filters noise, repetitive headers, footers
       │ 4. EmbeddingService: batch encodes chunks using `all-mpnet-base-v2` (768-dim)
       │ 5. VectorStore: upserts vectors + metadata payload to Qdrant (filtered by user_id)
       ▼
[Spring Boot Callback Handler]
       │ 1. Saves Chunk entities with page numbers & vector IDs into PostgreSQL
       │ 2. Updates Document status to INDEXED with total indexed chunk counts
       ▼
[User Notified: Ready for Recall & Chat]
```

### 2. Semantic Retrieval & RAG Generation Pipeline

```
[User Query] ("Summarize the key requirements from Section 3")
       │
       ▼
[Spring Boot: ChatController -> RagService]
       │ 1. Verifies user has active documents (friendly error if empty)
       │ 2. Saves User message into PostgreSQL `messages` table
       │ 3. Calls AI Service: POST /api/v1/search (query, user_id, top_k=15)
       ▼
[FastAPI: Vector Search & Ranking]
       │ 1. Encodes query into 768-dim embedding via SentenceTransformers
       │ 2. Searches Qdrant collection with `user_id` payload match
       │ 3. Applies strict similarity threshold (MIN_RETRIEVAL_SCORE: 0.60, fallback: 0.45)
       │ 4. Re-ranks based on reading order or topic coverage heuristics
       ▼
[Spring Boot: Context Assembly & Ollama Dispatch]
       │ 1. Fetches Chunk rows from PostgreSQL; cleans any stale deleted vectors
       │ 2. Merges adjacent chunks (`mergeAdjacentChunks` with overlap deduplication)
       │ 3. Gathers last 5 messages from conversation history for conversational context
       │ 4. Calls AI Service: POST /api/v1/rag/generate
       ▼
[FastAPI: Local LLM Generation via Ollama]
       │ 1. Checks query intent (Brief summary vs Detailed vs Reading order)
       │ 2. Budgets context window (LLM_MAX_CONTEXT_CHARS: 10,000)
       │ 3. Dispatches prompt to Ollama (`llama3.2:latest`) with retry/backoff
       │ 4. Extractive fallback: if LLM returns blank/no-answer, synthesizes evidence directly
       ▼
[Spring Boot: Citation Snapshot & Response]
       │ 1. Saves Assistant message into PostgreSQL
       │ 2. Saves citations in `message_sources` (stores full snippet, doc name, page, score)
       │ 3. Graceful degradation: if Ollama is offline, returns evidence cards cleanly
       ▼
[Client Receives Assistant Answer + Complete Evidence Citations]
```

### 3. Multi-Turn Conversation & Citation Flow

- **Snapshot Preservation (Flyway V009 & V011)**: When an AI answer is generated, the exact document name, page number, full chunk content snippet, and similarity score are stored directly in `message_sources`. Even if a source document is subsequently deleted or re-indexed, historical conversation citations remain intact and readable.
- **Idempotency Protection (Flyway V010)**: Clients can provide an `X-Idempotency-Key` header with chat messages. If network timeouts cause duplicate submissions, the backend ensures the query is not re-processed twice.
- **Data Isolation**: Multi-tenant data segregation is strictly enforced at every level:
  - Database queries are scoped by `user_id`.
  - Qdrant queries use a mandatory payload filter: `must=[FieldCondition(key="user_id", match=MatchValue(value=user_id))]`.
  - Local disk storage segregates files into `./uploads/{user_id}/`.

---

## 🧩 Microservices & Components

### 1. Core Java Spring Boot Backend (`cognify-backend`)

Located in `cognify-backend/`. Built with **Spring Boot 3.4.2** and **Java 17**.

- **`config/`**:
  - `SecurityConfig`: Configures stateless Spring Security, BCrypt password hashing, route authorization, and JWT filters.
  - `CorsConfig`: CORS policies allowing frontend origins (`localhost:3000`, `localhost:5173`).
  - `AsyncConfig`: Dedicated `ThreadPoolTaskExecutor` for non-blocking file processing.
  - `StorageConfig`: Base upload directory and file constraints.
  - `AiServiceConfig`: RestTemplate endpoint configurations pointing to Python AI service.
- **`controller/`**:
  - `AuthController`: User registration, login, JWT token rotation (`/refresh`), logout, and password change.
  - `DocumentController`: Multipart document upload, paginated listing, detail inspection, soft delete, and reindexing.
  - `ChatController`: Conversation lifecycle (create, rename, delete) and conversational RAG messaging.
  - `AnalyticsController`: User-specific analytics (storage used, chunk distribution, 30-day activity timeline).
  - `AdminController`: Platform-wide administrative oversight (user management, status toggling, cross-user document inspection, platform metrics).
  - `HealthController`: System readiness and liveness check.
- **`service/`**:
  - `RagService`: Orchestrates the vector retrieval, chunk stitching, adjacent chunk overlap merging, Ollama generation, fallback handling, and citation snapshotting.
  - `DocumentProcessingService`: Asynchronously delegates document extraction and embedding tasks to the Python service.
  - `FileStorageService`: Validates magic bytes/signatures and stores files safely on disk.
  - `AuthService`, `ConversationService`, `ChatService`, `AnalyticsService`, `AdminService`.

### 2. Local Python AI Microservice (`cognify-ai-service`)

Located in `cognify-ai-service/`. Built with **FastAPI** and **Uvicorn**.

- **`app/services/document_processor.py`**:
  - Extracts text from `.pdf` using `pymupdf4llm` to preserve tables, headers, and markdown structure while generating character-to-page offset maps.
  - Extracts text and tables from `.docx` via `python-docx`.
  - Extracts plain text from `.txt` and parses WhatsApp chat exports (`whatstk`).
- **`app/services/text_sanitizer.py` & `chunk_quality.py`**:
  - Strips PDF artifact noise, form feed markers, and broken unicode sequences.
  - Evaluates chunk information density; filters out empty fragments, repetitive boilerplate, and image-only residue before embedding.
- **`app/services/chunking_service.py`**:
  - Recursively splits text into overlapping windows (default: 750 characters with 120-character overlap) preserving sentence and paragraph boundaries.
- **`app/services/embedding_service.py`**:
  - Generates 768-dimensional dense vector embeddings locally using `sentence-transformers/all-mpnet-base-v2`. Supports fully offline air-gapped caching.
- **`app/services/vector_store.py`**:
  - Manages Qdrant vector collections (`cognify_vectors`), cosine similarity indexes, payload-filtered searches, and document vector deletion.
- **`app/services/llm_service.py` & `query_signals.py`**:
  - Communicates with Ollama (`llama3.2:latest`).
  - Detects query intent (summarization, chronological/timeline reading order, broad coverage vs specific lookup).
  - Implements dynamic context budgeting (up to 10,000 characters) and token prediction limits (450 to 1,600 tokens).
  - Manages cold-start timeouts (up to 300s for first-time model loading) with exponential backoff retries.
  - Provides an extractive fallback summary if the LLM produces a non-responsive answer despite valid evidence.

### 3. Datastores & Infrastructure

- **PostgreSQL 15+**: Relational store for users, credentials, session tokens, document metadata, chunks, conversations, messages, and citation snapshots.
- **Qdrant Vector Database**: Vector similarity engine running on port 6333.
- **Ollama Engine**: Local LLM runner on port 11434 serving Llama 3.2.
- **Docker Compose**: Orchestrates Qdrant and Ollama with automated model downloading.

---

## 🛠️ Technology Stack

| Layer | Component | Technologies / Libraries |
|---|---|---|
| **Core Backend** | Language & Framework | Java 17, Spring Boot 3.4.2 |
| | Security | Spring Security, JJWT (io.jsonwebtoken 0.12.6), BCrypt (strength 12) |
| | Persistence | Spring Data JPA, Hibernate, PostgreSQL Driver, HikariCP |
| | Migrations | Flyway Core & Flyway PostgreSQL (v11.x) |
| | Utilities | Lombok 1.18.38, Apache Commons IO 2.17.0, Commons Lang3 |
| **AI Microservice** | Language & Framework | Python 3.10+, FastAPI 0.109.2, Uvicorn 0.27.1, Pydantic v2 |
| | Document Parsers | PyMuPDF (fitz), pymupdf4llm, python-docx, whatstk |
| | NLP & Text Processing | LangChain Text Splitters, Regex Sanitizers |
| | Embeddings | Sentence-Transformers 2.3.1 (`all-mpnet-base-v2` — 768 dimensions) |
| | Vector Database Client | Qdrant Client 1.7.3 |
| | Testing | Pytest 8.0.0 |
| **Infrastructure** | Vector Database | Qdrant (Docker `qdrant/qdrant:latest`) |
| | Local LLM Engine | Ollama (Docker `ollama/ollama:latest` with `llama3.2`) |
| | Relational Database | PostgreSQL 15+ |

---

## 🗄️ Database Schema & Migrations

Database versioning is managed automatically via **Flyway migrations** (`db/migration`):

```
V001__create_users_table.sql
  └── users: id, email, password_hash, name, role (USER/ADMIN), status (ACTIVE/INACTIVE), timestamps

V002__create_documents_table.sql
  └── documents: id, user_id (FK), name, original_name, type, size_bytes, storage_path, status, total_chunks

V003__create_chunks_table.sql
  └── chunks: id, document_id (FK), chunk_index, content, page_number, token_count, vector_id

V004__create_user_sessions_table.sql
  └── user_sessions: id, user_id (FK), refresh_token, device_info, ip_address, expires_at

V005__create_conversations_table.sql
  └── conversations: id, user_id (FK), title, timestamps

V006__create_messages_table.sql
  └── messages: id, conversation_id (FK), sender (USER/ASSISTANT), content, timestamps

V007__create_message_sources_table.sql
  └── message_sources: id, message_id (FK), chunk_id (FK), relevance_score, timestamps

V008__add_soft_delete_to_documents.sql
  └── Adds `deleted_at` timestamp and DELETED status enum to `documents`

V009__preserve_message_source_snapshots.sql
  └── Adds document_id, document_name, page_number, snippet (TEXT) to `message_sources`

V010__message_idempotency_and_reply_link.sql
  └── Adds `idempotency_key` and `reply_to_message_id` to `messages`

V011__backfill_full_snippet_in_message_sources.sql
  └── Ensures full snippet preservation without arbitrary character truncation
```

### Entity Relationship Diagram (ERD)

```
        ┌─────────────────────────┐
        │          USERS          │
        │  id (PK, UUID)          │
        │  email, password, role  │
        └───────┬─────────┬───────┘
                │         │
    ┌───────────┘         └───────────┐
    ▼                                 ▼
┌───────────────┐             ┌───────────────┐
│ USER_SESSIONS │             │  CONVERSATIONS│
│ id, user_id   │             │  id, user_id  │
└───────────────┘             └───────┬───────┘
                                      │
    ┌─────────────────────────────────┘
    ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│        DOCUMENTS        │   │        MESSAGES         │
│  id (PK, UUID)          │   │  id (PK, UUID)          │
│  user_id (FK), name     │   │  conversation_id (FK)   │
│  storage_path, status   │   │  sender, content        │
└───────────┬─────────────┘   └───────────┬─────────────┘
            │                             │
            ▼                             ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│         CHUNKS          │   │     MESSAGE_SOURCES     │
│  id (PK, UUID)          │◄──┤  (Citation Snapshots)   │
│  document_id (FK)       │   │  message_id (FK)        │
│  content, page_number   │   │  chunk_id (FK), snippet │
│  vector_id (Qdrant UUID)│   │  score, page, doc_name  │
└─────────────────────────┘   └─────────────────────────┘
```

---

## 📁 Project Directory Layout

```
cognify-backend/
├── README.md                           # Master Architecture & Runbook (This file)
├── docker-compose.yml                  # Root orchestration for Qdrant, Ollama, & DB (optional)
│
├── cognify-ai-service/                 # 🐍 Python FastAPI AI Microservice
│   ├── Dockerfile                      # Production container build (Python 3.10-slim + PyTorch CPU)
│   ├── docker-compose.yml              # Qdrant & Ollama services + auto Llama-3.2 model puller
│   ├── requirements.txt                # FastAPI, sentence-transformers, qdrant-client, PyMuPDF, etc.
│   ├── app/
│   │   ├── main.py                     # FastAPI application entrypoint, CORS, lifespan startup hooks
│   │   ├── config.py                   # Pydantic v2 settings (ports, thresholds, context sizes)
│   │   ├── models.py                   # Pydantic request & response schemas
│   │   ├── api/
│   │   │   └── routes.py               # REST endpoints: /process-document, /search, /rag/generate
│   │   └── services/
│   │       ├── document_processor.py   # PDF (PyMuPDF4LLM), DOCX, TXT, WhatsApp extractors
│   │       ├── chunking_service.py     # Recursive character text chunker with page offsets
│   │       ├── chunk_quality.py        # Quality gate: drops noise & low-density chunks
│   │       ├── embedding_service.py    # Local Sentence-Transformers (all-mpnet-base-v2)
│   │       ├── vector_store.py         # Qdrant client, collection creation, vector upsert & search
│   │       ├── llm_service.py          # Ollama client, retries, context budgeting, prompts
│   │       ├── query_signals.py        # Query intent detection (summary, chronology, focus)
│   │       ├── rag_service.py          # Vector search orchestration & retrieval filtering
│   │       └── text_sanitizer.py       # Markdown/PDF syntax cleaner
│   └── tests/                          # Pytest test suite for parsers, chunking, and vectors
│
└── cognify-backend/                    # ☕ Java Spring Boot Application
    ├── pom.xml                         # Maven dependencies & build definitions (Spring Boot 3.4.2)
    ├── Dockerfile                      # Multi-stage JDK 17 build -> JRE 17 runtime
    ├── mvnw / mvnw.cmd                 # Maven wrapper scripts (Linux & Windows)
    └── src/
        ├── main/
        │   ├── java/com/cognify/cognify_backend/
        │   │   ├── CognifyBackendApplication.java
        │   │   ├── config/             # Security, CORS, Async, JWT, Storage, RestTemplate configs
        │   │   ├── controller/         # Auth, Document, Chat, Analytics, Admin, Health controllers
        │   │   ├── dto/                # Request & response data transfer objects
        │   │   ├── entity/             # JPA Entities: User, Document, Chunk, Conversation, Message, etc.
        │   │   ├── exception/          # GlobalExceptionHandler & custom exceptions
        │   │   ├── repository/         # Spring Data JPA repositories
        │   │   ├── security/           # JwtTokenProvider, JwtAuthFilter, UserPrincipal
        │   │   └── service/            # RagService, DocumentProcessingService, FileStorageService, etc.
        │   └── resources/
        │       ├── application.properties # Spring Boot configuration properties
        │       └── db/migration/       # Flyway database migrations (V001 through V011)
        └── test/                       # JUnit & Spring Boot integration test suite
```

---

## ⚙️ Prerequisites & Environment Setup

Before starting the system, ensure the following software is installed on your host machine:

1. **Java Development Kit (JDK)**: Version 17 or higher (`java -version`).
2. **Python**: Version 3.10 or 3.11 (`python --version`).
3. **Docker & Docker Compose**: For running Qdrant and Ollama.
4. **PostgreSQL**: Version 15 or higher (local service or Docker container).
5. **Git**: For version control.

---

## 🚀 Step-by-Step Installation & Running Guide

### Step 1: Start Infrastructure (Qdrant & Ollama)

Navigate to the `cognify-ai-service` directory and launch Qdrant and Ollama using Docker Compose:

```bash
cd cognify-ai-service
docker compose up -d
```

This starts:
- **Qdrant Vector DB** at `http://localhost:6333` (Web UI at `http://localhost:6333/dashboard`).
- **Ollama Engine** at `http://localhost:11434`.
- **Ollama Model Puller** which automatically downloads `llama3.2:latest` (approx. 2.0 GB).

To verify Ollama and the model are ready:
```bash
curl http://localhost:11434/api/tags
# Output should show "llama3.2:latest"
```

---

### Step 2: Start Python AI Microservice

In the `cognify-ai-service/` directory, set up a virtual environment and install dependencies:

```bash
cd cognify-ai-service

# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows (PowerShell):
.\venv\Scripts\Activate.ps1
# Linux / macOS:
source venv/bin/activate

# Install CPU PyTorch first (recommended for faster download & reliability)
pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch==2.5.1+cpu

# Install remaining dependencies
pip install -r requirements.txt
```

*(Optional)* Create a `.env` file in `cognify-ai-service/.env` to customize settings:
```ini
SERVICE_PORT=8000
QDRANT_HOST=localhost
QDRANT_PORT=6333
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:latest
EMBEDDING_MODEL=sentence-transformers/all-mpnet-base-v2
MIN_RETRIEVAL_SCORE=0.60
CHUNK_QUALITY_THRESHOLD=0.35
LOG_LEVEL=INFO
```

Launch the FastAPI service:
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The AI microservice will start on **http://localhost:8000**.
Swagger API Documentation is available at **http://localhost:8000/docs**.

---

### Step 3: Setup PostgreSQL & Run Java Backend

1. **Create the PostgreSQL Database**:
```bash
psql -U postgres -c "CREATE DATABASE cognify;"
```

2. **Configure Database Credentials**:
Edit `cognify-backend/src/main/resources/application.properties` (or set environment variables):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cognify
spring.datasource.username=postgres
spring.datasource.password=your_postgres_password

cognify.storage.base-path=./uploads
cognify.ai-service.url=http://localhost:8000
```

3. **Compile & Run the Backend**:
Navigate to the `cognify-backend/cognify-backend/` directory:

```bash
cd cognify-backend

# On Windows PowerShell:
.\mvnw.cmd spring-boot:run

# On Linux / macOS:
chmod +x mvnw
./mvnw spring-boot:run
```

Flyway will automatically execute migrations `V001` through `V011` upon startup.
The backend will start on **http://localhost:8082**.

---

## 📡 Comprehensive API Reference

Base URL: `http://localhost:8082`  
All protected endpoints require the HTTP Authorization header:
```http
Authorization: Bearer <accessToken>
```

### 1. Authentication & User Profile

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/signup` | Public | Register a new user account |
| `POST` | `/api/auth/login` | Public | Authenticate with email/password; returns JWT access & refresh tokens |
| `POST` | `/api/auth/refresh` | Public | Rotate and refresh access token using a valid refresh token |
| `POST` | `/api/auth/logout` | Authenticated | Revoke user session and invalidate token |
| `GET` | `/api/auth/profile` | Authenticated | Retrieve current authenticated user profile |
| `PUT` | `/api/auth/profile` | Authenticated | Update user profile details (e.g. display name) |
| `POST` | `/api/auth/password` | Authenticated | Change account password |

#### Request Example: Login
```json
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}
```

#### Response Example:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "d748f2a9-7c39-44fb-93b5-442b0051e9b0",
    "user": {
      "id": "e6a2bdf9-03a1-4320-b49d-3f04494d4d62",
      "email": "user@example.com",
      "name": "Alex Mercer",
      "role": "USER"
    }
  },
  "message": "Login successful"
}
```

---

### 2. Document Management

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/documents/upload` | Authenticated | Upload document (`file`: multipart/form-data; PDF, DOCX, TXT) |
| `GET` | `/api/documents` | Authenticated | List paginated user documents (`page`, `limit`, `sortBy`, `sortOrder`) |
| `GET` | `/api/documents/{id}` | Authenticated | Retrieve metadata for a single document |
| `DELETE` | `/api/documents/{id}` | Authenticated | Soft-delete document and remove vectors from Qdrant |
| `POST` | `/api/documents/{id}/reindex` | Authenticated | Trigger re-extraction, re-chunking, and re-embedding |

#### Response Example: Upload Document
```json
{
  "success": true,
  "data": {
    "id": "18f2d5e7-380d-44aa-8fa2-6804bb07c73b",
    "name": "annual_financial_report.pdf",
    "type": "PDF",
    "sizeBytes": 1048576,
    "status": "PROCESSING",
    "totalChunks": 0,
    "createdAt": "2026-10-25T16:00:00"
  },
  "message": "Document uploaded successfully. Processing has started."
}
```

---

### 3. Conversations & Chat (RAG Search)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/conversations` | Authenticated | Create a new chat session (`{"title": "..."}`) |
| `GET` | `/api/conversations` | Authenticated | Get paginated list of conversations (`page`, `size`) |
| `GET` | `/api/conversations/{id}` | Authenticated | Get conversation metadata |
| `PATCH` | `/api/conversations/{id}` | Authenticated | Rename conversation title |
| `DELETE` | `/api/conversations/{id}` | Authenticated | Delete a conversation and all its messages |
| `GET` | `/api/conversations/{id}/messages` | Authenticated | Retrieve paginated message history with citation sources |
| `POST` | `/api/conversations/{id}/messages` | Authenticated | Send user query; executes RAG retrieval + LLM answer synthesis |

#### Request Example: Send Message (Triggers RAG)
```http
POST /api/conversations/3fa85f64-5717-4562-b3fc-2c963f66afa6/messages
Authorization: Bearer <accessToken>
X-Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d
Content-Type: application/json

{
  "content": "What were the primary revenue drivers mentioned in Q3?"
}
```

#### Response Example (Assistant Answer + Full Evidence Citations):
```json
{
  "success": true,
  "data": {
    "id": "7ca64732-c2cb-4a6c-9a4f-5ce7f10b7849",
    "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "sender": "ASSISTANT",
    "content": "Based on the financial report, primary revenue drivers in Q3 were enterprise SaaS subscription expansions (up 34% YoY) and international cloud adoption in the APAC region.",
    "createdAt": "2026-10-25T16:05:12",
    "sources": [
      {
        "chunkId": "48b61e27-fa84-4860-9856-4d048d91c1ee",
        "documentId": "18f2d5e7-380d-44aa-8fa2-6804bb07c73b",
        "documentName": "annual_financial_report.pdf",
        "pageNumber": 14,
        "snippet": "In Q3, enterprise subscription expansion accounted for $4.2M in net new ARR, driven by 34% YoY growth in enterprise cloud modules across APAC.",
        "relevanceScore": 0.842
      }
    ]
  },
  "message": "Message processed successfully"
}
```

---

### 4. User Analytics

All analytics endpoints are scoped to the authenticated user:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/analytics/user/summary` | Aggregate counts: documents, chunks, conversations, storage used |
| `GET` | `/api/analytics/user/document-types` | Breakdown of documents by type (PDF, DOCX, TXT) |
| `GET` | `/api/analytics/user/upload-timeline` | 30-day document upload trajectory |
| `GET` | `/api/analytics/user/recent-activity` | Last 10 user document actions (upload, reindex, delete) |
| `GET` | `/api/analytics/user/top-documents` | Top user documents ranked by chunk volume |

---

### 5. Admin Platform Management

All admin endpoints require the user to have the `ADMIN` role:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/users` | Paginated listing of all platform users with document counts |
| `GET` | `/api/admin/users/{userId}` | Detailed user profile inspection |
| `PUT` | `/api/admin/users/{userId}` | Update user name, email, role, or status |
| `PATCH` | `/api/admin/users/{userId}/activate` | Activate a user account |
| `PATCH` | `/api/admin/users/{userId}/deactivate` | Deactivate a user account |
| `PATCH` | `/api/admin/users/{userId}/status` | Update account status (`{"active": true/false}`) |
| `DELETE` | `/api/admin/users/{userId}` | Delete a user account (cannot delete admin users) |
| `GET` | `/api/admin/documents` | Cross-tenant document listing across all users |
| `GET` | `/api/admin/analytics/platform` | Platform-wide KPIs: active users, total storage, total chunks |
| `GET` | `/api/admin/analytics/usage-timeline` | Platform-wide 30-day registration and upload timelines |
| `GET` | `/api/admin/system/settings` | Get mutable platform configuration settings |
| `PUT` | `/api/admin/system/settings` | Update mutable platform configuration settings |

---

### 6. System Health

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Java backend health status, timestamp, and version |
| `GET` | `http://localhost:8000/health` | Python AI service status and version |

---

## ⚙️ Configuration Reference

### Java Backend (`application.properties`)

```properties
# Server Port
server.port=8082

# Database Connection
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/cognify}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:123456789}

# Storage Configuration
cognify.storage.base-path=${COGNIFY_STORAGE_BASE_PATH:./uploads}
cognify.storage.allowed-types=application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain
cognify.storage.max-file-size=52428800  # 50 MB

# JWT Configuration
cognify.jwt.secret=${COGNIFY_JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
cognify.jwt.access-expiration=900000      # 15 minutes
cognify.jwt.refresh-expiration=604800000  # 7 days

# Python AI Service Integration
cognify.ai-service.url=${COGNIFY_AI_SERVICE_URL:http://localhost:8000}
cognify.embedding.dimensions=768

# CORS Allowed Origins
cognify.cors.allowed-origins=${COGNIFY_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
```

### Python AI Service (`app/config.py` / `.env`)

```ini
SERVICE_PORT=8000
CORS_ALLOWED_ORIGINS=http://localhost:8082,http://localhost:3000,http://localhost:5173

# Qdrant Vector DB
QDRANT_HOST=localhost
QDRANT_PORT=6333
QDRANT_COLLECTION=cognify_vectors

# Local LLM (Ollama)
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:latest
LLM_TIMEOUT_FIRST_REQUEST=300   # 5 minutes for first cold-load into GPU/RAM
LLM_TIMEOUT_SUBSEQUENT=180      # 3 minutes for normal generation requests
LLM_REQUEST_RETRIES=2           # Exponential backoff retry count

# Embeddings
EMBEDDING_MODEL=sentence-transformers/all-mpnet-base-v2
VECTOR_DIMENSION=768
EMBEDDING_LOCAL_FILES_ONLY=false

# Chunking & Retrieval Parameters
CHUNK_SIZE=750
CHUNK_OVERLAP=120
MIN_CHUNK_CHAR_LENGTH=50
MIN_RETRIEVAL_SCORE=0.60
MIN_RETRIEVAL_SCORE_FALLBACK=0.45
CHUNK_QUALITY_THRESHOLD=0.35
LLM_MAX_CONTEXT_CHARS=10000
LLM_RESPONSE_FORMAT=answer_only
```

---

## ❓ Troubleshooting & FAQs

### 1. Ollama first request takes 30–60 seconds
- **Cause**: Ollama loads model weights from disk into system RAM/VRAM on the first query.
- **Solution**: The backend is configured with `LLM_TIMEOUT_FIRST_REQUEST=300` and `RestTemplateConfig` with extended timeouts to accommodate this cold load. Once warm, subsequent requests complete rapidly.

### 2. "Collection dimension mismatch" warning on startup
- **Cause**: If an existing Qdrant collection was created with a different dimension (e.g., 384 or 1536) and the embedding model is set to 768 (`all-mpnet-base-v2`).
- **Solution**: `VectorStore` automatically detects dimension mismatches during the startup health check and recreates the collection with the correct 768-dimension schema.

### 3. Graceful degradation: "❌ The local AI service is unavailable"
- **Behavior**: If Ollama or the Python service is not running when a user sends a chat message, the Spring Boot backend **does not crash**. Instead, it retrieves relevant document chunks from PostgreSQL, saves the citation cards, and gracefully informs the user that the AI generation service is temporarily offline while still displaying the relevant document evidence.

### 4. Running fully offline (Air-gapped mode)
- Pre-download the HuggingFace embedding model cache once:
  ```bash
  python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('sentence-transformers/all-mpnet-base-v2')"
  ```
- Set `EMBEDDING_LOCAL_FILES_ONLY=true` in `.env`.

---

## 📄 License & Maintainers

Built as a privacy-first, secure RAG solution for enterprise knowledge retrieval.  
Developed by the **Cognify Engineering Team**.
