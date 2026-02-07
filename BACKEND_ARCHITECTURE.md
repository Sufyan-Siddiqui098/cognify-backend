# Cognify Backend Architecture

<div align="center">

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue?style=flat)
![Pattern](https://img.shields.io/badge/Pattern-Event--Driven-green?style=flat)
![AI](https://img.shields.io/badge/AI-RAG%20System-purple?style=flat)
![Status](https://img.shields.io/badge/Status-Production%20Ready-brightgreen?style=flat)

**Production-Grade Architecture for the Digital Memory Reconstruction System**

*A comprehensive guide for building scalable, secure, and maintainable backend services*

</div>

---

## 📋 Table of Contents

- [Executive Summary](#executive-summary)
- [System Overview](#system-overview)
- [Architecture Diagram](#architecture-diagram)
- [Design Principles](#design-principles)
- [Service Specifications](#service-specifications)
- [Data Architecture](#data-architecture)
- [Document Lifecycle](#document-lifecycle)
- [RAG Pipeline Design](#rag-pipeline-design)
- [Technology Stack](#technology-stack)
- [Security Architecture](#security-architecture)
- [Deployment Strategy](#deployment-strategy)
- [Resilience & Fault Tolerance](#resilience--fault-tolerance)
- [Scaling Strategy](#scaling-strategy)
- [Performance Optimization](#performance-optimization)
- [Observability & Monitoring](#observability--monitoring)
- [Implementation Roadmap](#implementation-roadmap)
- [Appendix](#appendix)

---

## 📌 Executive Summary

### Project Vision

**Cognify** is a next-generation **Retrieval-Augmented Generation (RAG)** system designed to transform personal document collections into an intelligent, queryable knowledge base. The system enables users to:

- **Upload** documents (PDF, DOCX, TXT) for automatic processing
- **Index** content using AI-powered semantic chunking and embedding
- **Query** their knowledge base using natural language
- **Reconstruct** memories from vague hints through intelligent semantic search
- **Converse** with their documents through an AI-powered chat interface

### Business Value

| Capability | Benefit |
|------------|---------|
| Semantic Search | Find information by meaning, not just keywords |
| AI-Powered Recall | Reconstruct context from partial memories |
| Document Intelligence | Automatic summarization and topic extraction |
| Conversational Interface | Natural language interaction with documents |
| Privacy-First Design | User data isolation and security |

### Technical Highlights

- **Microservices Architecture**: Independent, scalable services
- **Event-Driven Processing**: Async document pipeline via message queues
- **Multi-Model AI**: Flexible LLM and embedding model support
- **Vector Search**: Sub-second semantic similarity matching
- **Enterprise Security**: JWT auth, RBAC, data isolation

---

## 🎯 System Overview

Cognify is a **Retrieval-Augmented Generation (RAG)** system that allows users to upload documents, have them indexed/chunked using AI, and then query their personal knowledge base using natural language. The system reconstructs memory from vague hints through intelligent semantic search.

### Core Workflows

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           COGNIFY CORE WORKFLOWS                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  📤 INGEST WORKFLOW           🔍 QUERY WORKFLOW          💬 CHAT WORKFLOW   │
│  ─────────────────           ──────────────────          ───────────────    │
│  User uploads doc     →      User asks question   →      User sends message│
│  File validation      →      Query embedding      →      Context retrieval │
│  Object storage       →      Vector similarity    →      RAG augmentation  │
│  Async processing     →      Result ranking       →      Response + sources│
│  Chunking + embed     →      Source attribution   →      History tracking  │
│  Vector storage       →      Response delivery    →      Learning feedback │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Design Principles

| Principle | Implementation | Benefit |
|-----------|----------------|---------|
| **Microservices** | Independent, deployable services with clear boundaries | Scalability, maintainability |
| **Event-Driven** | Async processing via message queues | Decoupling, resilience |
| **User Data Isolation** | Strict tenant separation at data layer | Security, privacy |
| **Fail-Fast with Graceful Degradation** | Circuit breakers and fallbacks | Reliability |
| **Observability-First** | Built-in metrics, logging, and tracing | Debuggability |
| **API-First Design** | OpenAPI specifications before implementation | Consistency |
| **Domain-Driven Design** | Bounded contexts per service | Clear ownership |

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  FRONTEND                                    │
│                          React + Vite + TailwindCSS                         │
│                                                                              │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│   │ Upload   │ │ Recall   │ │Documents │ │Analytics │ │  Chat Interface  │  │
│   └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬─────────┘  │
│        │            │            │            │                 │            │
└────────┼────────────┼────────────┼────────────┼─────────────────┼────────────┘
         │            │            │            │                 │
         ▼            ▼            ▼            ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY                                     │
│                         (Spring Cloud Gateway)                              │
│                                                                              │
│   • Rate Limiting        • JWT Validation        • Request Routing          │
│   • CORS Configuration   • Load Balancing        • Circuit Breaker          │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MICROSERVICES LAYER                               │
│                                                                              │
│  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────────┐ │
│  │   AUTH SERVICE     │  │  DOCUMENT SERVICE  │  │    RECALL SERVICE      │ │
│  │   (Spring Boot)    │  │   (Spring Boot)    │  │   (Python FastAPI)     │ │
│  │                    │  │                    │  │                        │ │
│  │ • User Registration│  │ • File Upload      │  │ • Semantic Search      │ │
│  │ • JWT Token Gen    │  │ • Document CRUD    │  │ • RAG Querying         │ │
│  │ • Password Reset   │  │ • Status Tracking  │  │ • Result Ranking       │ │
│  │ • Session Mgmt     │  │ • Tags Management  │  │ • Feedback Learning    │ │
│  │                    │  │ • Admin Operations │  │ • Study List Mgmt      │ │
│  └─────────┬──────────┘  └─────────┬──────────┘  └───────────┬────────────┘ │
│            │                       │                          │              │
│  ┌─────────┴───────────────────────┴──────────────────────────┴───────────┐ │
│  │                        MESSAGE BROKER (RabbitMQ / Kafka)               │ │
│  │                                                                         │ │
│  │   Queues:                                                               │ │
│  │   • document.uploaded     → Triggers chunking pipeline                  │ │
│  │   • document.chunk        → Triggers embedding generation               │ │
│  │   • feedback.received     → Triggers model fine-tuning                  │ │
│  │   • analytics.event       → Tracks user behavior                        │ │
│  └─────────┬───────────────────────────────────────────────────────────────┘ │
│            │                                                                 │
│  ┌─────────▼──────────────────────────────────────────────────────────────┐ │
│  │                    INDEXING SERVICE (Python)                            │ │
│  │                                                                         │ │
│  │   ┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │ │
│  │   │  PDF/DOCX/TXT   │    │   Chunking       │    │   Embedding      │  │ │
│  │   │   Parsing       │ -> │   Strategy       │ -> │   Generation     │  │ │
│  │   │  (PyPDF2,       │    │   (Semantic/     │    │   (OpenAI /      │  │ │
│  │   │   python-docx)  │    │    Fixed-size)   │    │    Sentence      │  │ │
│  │   └─────────────────┘    └──────────────────┘    │    Transformers) │  │ │
│  │                                                   └──────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌─────────────────────┐  ┌────────────────────┐                            │
│  │ ANALYTICS SERVICE  │  │   ADMIN SERVICE    │                            │
│  │   (Spring Boot)    │  │   (Spring Boot)    │                            │
│  │                    │  │                    │                            │
│  │ • Usage Tracking   │  │ • User Management  │                            │
│  │ • Query Analytics  │  │ • Job Monitoring   │                            │
│  │ • Storage Metrics  │  │ • System Stats     │                            │
│  │ • Topic Trends     │  │ • Platform Analytics│                           │
│  └────────────────────┘  └────────────────────┘                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                      │
│                                                                              │
│  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────────┐ │
│  │    PostgreSQL      │  │   Vector DB        │  │    Object Storage      │ │
│  │                    │  │   (Pinecone /      │  │   (MinIO / S3)         │ │
│  │ Tables:            │  │    Qdrant /        │  │                        │ │
│  │ • users            │  │    Weaviate)       │  │ • Original Files       │ │
│  │ • documents        │  │                    │  │ • Thumbnails           │ │
│  │ • chunks           │  │ Collections:       │  │ • Processed Outputs    │ │
│  │ • feedback         │  │ • document_chunks  │  │                        │ │
│  │ • study_list       │  │ • user_memories    │  │                        │ │
│  │ • analytics_events │  │                    │  │                        │ │
│  └────────────────────┘  └────────────────────┘  └────────────────────────┘ │
│                                                                              │
│  ┌────────────────────┐  ┌────────────────────┐                            │
│  │      Redis         │  │   Elasticsearch    │                            │
│  │                    │  │   (Optional)       │                            │
│  │ • Session Cache    │  │                    │                            │
│  │ • Rate Limiting    │  │ • Full-text Search │                            │
│  │ • Job Queues       │  │ • Faceted Search   │                            │
│  │ • Token Blacklist  │  │ • Analytics Index  │                            │
│  └────────────────────┘  └────────────────────┘                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Service Specifications

### Service Overview Matrix

| Service | Language | Framework | Port | Responsibilities |
|---------|----------|-----------|------|------------------|
| API Gateway | Java | Spring Cloud Gateway | 8080 | Routing, Auth, Rate Limiting |
| Auth Service | Java | Spring Boot 3.x | 8081 | Authentication, Authorization |
| Document Service | Java | Spring Boot 3.x | 8082 | Document CRUD, Storage |
| Indexing Service | Python | FastAPI | 8083 | Parsing, Chunking, Embedding |
| Recall Service | Python | FastAPI | 8084 | Semantic Search, RAG |
| Chat Service | Python | FastAPI | 8085 | Conversations, Context |
| Analytics Service | Java | Spring Boot 3.x | 8086 | Metrics, Insights |
| Admin Service | Java | Spring Boot 3.x | 8087 | Administration |

### 1. Auth Service (Spring Boot)

**Purpose**: Handle user authentication, authorization, and session management.

**Domain Responsibility**: User identity, credentials, tokens, sessions

**Endpoints**:

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/login` | Authenticate user, return JWT | ❌ |
| POST | `/api/auth/signup` | Register new user | ❌ |
| POST | `/api/auth/refresh` | Refresh access token | ❌ (refresh token) |
| POST | `/api/auth/logout` | Invalidate token | ✅ |
| GET | `/api/auth/profile` | Get current user profile | ✅ |
| PUT | `/api/auth/profile` | Update user profile | ✅ |
| POST | `/api/auth/password/reset` | Request password reset | ❌ |
| POST | `/api/auth/password/change` | Change password | ✅ |

**Request/Response Examples**:

```json
// POST /api/auth/login
// Request
{
  "email": "user@example.com",
  "password": "securePassword123"
}

// Response (200 OK)
{
  "success": true,
  "data": {
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "name": "John Doe",
      "role": "user",
      "avatar": "https://..."
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "expiresIn": 900
  }
}
```

**Technologies**:
- Spring Boot 3.x
- Spring Security with JWT (RS256 signing)
- BCrypt for password hashing (cost factor 12)
- Redis for token blacklisting and session management

**JWT Token Structure**:
```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-id",
    "email": "user@example.com",
    "role": "user",
    "iat": 1703145600,
    "exp": 1703146500,
    "jti": "unique-token-id"
  }
}
```

**Database Schema (PostgreSQL)**:
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  role VARCHAR(50) DEFAULT 'user', -- 'user' | 'admin'
  status VARCHAR(50) DEFAULT 'active', -- 'active' | 'inactive'
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_login_at TIMESTAMP
);

CREATE TABLE user_sessions (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  refresh_token TEXT,
  device_info JSONB, -- {browser, os, ip}
  expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_token ON user_sessions(refresh_token);
```

**Security Considerations**:
- Passwords hashed with BCrypt (cost factor 12)
- JWT access tokens expire in 15 minutes
- Refresh tokens expire in 7 days
- Token blacklisting on logout via Redis
- Rate limiting: 5 login attempts per minute

---

### 2. Document Service (Spring Boot)

**Purpose**: Handle document upload, storage, metadata management, and trigger indexing.

**Domain Responsibility**: File storage, document metadata, processing status

**Endpoints**:

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/documents` | List user's documents (paginated) | ✅ |
| GET | `/api/documents/:id` | Get document details | ✅ |
| POST | `/api/documents/upload` | Upload new document (multipart) | ✅ |
| PUT | `/api/documents/:id` | Update document metadata | ✅ |
| DELETE | `/api/documents/:id` | Delete document and chunks | ✅ |
| POST | `/api/documents/:id/reindex` | Trigger re-indexing | ✅ |
| PUT | `/api/documents/:id/replace` | Replace document file | ✅ |
| GET | `/api/documents/:id/chunks` | Get document chunks | ✅ |
| GET | `/api/documents/:id/download` | Download original file | ✅ |

**Query Parameters for List Endpoint**:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 1 | Page number |
| `limit` | int | 12 | Items per page (max 50) |
| `search` | string | - | Search in name/content |
| `status` | enum | - | Filter: processing, indexed, failed |
| `type` | enum | - | Filter: pdf, docx, txt |
| `sortBy` | string | createdAt | Sort field |
| `sortOrder` | enum | desc | asc or desc |

**Technologies**:
- Spring Boot 3.x
- Spring Data JPA
- MinIO/S3 SDK for object storage
- RabbitMQ/Kafka for async processing

**Database Schema**:
```sql
CREATE TABLE documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  name VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL, -- 'pdf', 'docx', 'txt'
  size BIGINT NOT NULL,
  status VARCHAR(50) DEFAULT 'processing', -- 'processing' | 'indexed' | 'failed'
  chunks_count INTEGER DEFAULT 0,
  storage_url TEXT,
  summary TEXT,
  tags TEXT[], -- PostgreSQL array
  upload_started_at TIMESTAMP DEFAULT NOW(),
  indexed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chunks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
  chunk_index INTEGER NOT NULL,
  content TEXT NOT NULL,
  page_number INTEGER,
  token_count INTEGER,
  vector_id VARCHAR(255), -- Reference to vector DB
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_documents_user ON documents(user_id);
CREATE INDEX idx_chunks_document ON chunks(document_id);
```

---

### 3. Indexing Service (Python FastAPI)

**Purpose**: Process uploaded documents, chunk them intelligently, and generate embeddings.

**Message Queue Consumers**:
- `document.uploaded` → Start processing pipeline
- `document.reindex` → Re-process existing document

**Processing Pipeline**:

```python
# Pseudo-code for indexing pipeline
async def process_document(document_id: str):
    # 1. Download file from object storage
    file = await storage.download(document_id)
    
    # 2. Parse document based on type
    if file.type == 'pdf':
        text, pages = pdf_parser.extract(file)
    elif file.type == 'docx':
        text, pages = docx_parser.extract(file)
    else:
        text = file.read()
    
    # 3. Chunk the text (semantic chunking)
    chunks = semantic_chunker.chunk(
        text,
        chunk_size=512,
        overlap=50,
        respect_sentences=True
    )
    
    # 4. Generate embeddings
    embeddings = embedding_model.encode(chunks)
    
    # 5. Store in vector database
    vector_db.upsert(
        collection='document_chunks',
        vectors=embeddings,
        metadata=[{
            'document_id': document_id,
            'chunk_index': i,
            'page': chunk.page,
            'content': chunk.text
        } for i, chunk in enumerate(chunks)]
    )
    
    # 6. Store chunks in PostgreSQL
    await db.bulk_insert_chunks(document_id, chunks)
    
    # 7. Generate summary
    summary = llm.summarize(text[:4000])
    
    # 8. Extract topics/tags
    tags = topic_extractor.extract(text)
    
    # 9. Update document status
    await db.update_document(document_id, {
        'status': 'indexed',
        'chunks_count': len(chunks),
        'summary': summary,
        'tags': tags
    })
```

**Technologies**:
- Python 3.11+
- FastAPI for health endpoints
- LangChain for document processing
- Sentence Transformers / OpenAI for embeddings
- PyPDF2, python-docx for parsing

---

### 4. Recall Service (Python FastAPI)

**Purpose**: Handle semantic search queries using RAG architecture.

**Endpoints**:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/recall/search` | Semantic search with RAG |
| POST | `/api/recall/:id/feedback` | Mark result useful/not useful |
| GET | `/api/recall/study-list` | Get user's study list |
| POST | `/api/recall/study-list/:id` | Add to study list |
| DELETE | `/api/recall/study-list/:id` | Remove from study list |

**Search Algorithm**:

```python
async def semantic_search(query: str, user_id: str, options: dict):
    # 1. Generate query embedding
    query_embedding = embedding_model.encode(query)
    
    # 2. Search vector database (filtered by user)
    vector_results = await vector_db.search(
        collection='document_chunks',
        query_vector=query_embedding,
        filter={'user_id': user_id},
        top_k=options.get('limit', 10)
    )
    
    # 3. Rerank results (optional - using cross-encoder)
    if options.get('rerank', True):
        reranked = cross_encoder.rerank(query, vector_results)
    else:
        reranked = vector_results
    
    # 4. Optional: Generate LLM-augmented response
    if options.get('augment', True):
        context = '\n'.join([r.content for r in reranked[:3]])
        augmented_response = llm.generate(
            prompt=f"Based on this context: {context}\n\n"
                   f"Answer this query naturally: {query}"
        )
    
    # 5. Track analytics
    await analytics.track_query(user_id, query, len(reranked))
    
    # 6. Return results with metadata
    return [{
        'id': r.id,
        'score': r.score,
        'snippet': r.content,
        'documentId': r.metadata['document_id'],
        'documentName': r.metadata['document_name'],
        'page': r.metadata['page'],
        'topic': topic_classifier.classify(r.content),
        'isUseful': None,
        'inStudyList': r.id in user_study_list
    } for r in reranked]
```

**Technologies**:
- Python 3.11+
- FastAPI
- Pinecone / Qdrant / Weaviate client
- OpenAI API / Local LLM (Ollama)
- Sentence Transformers

---

### 5. Chat Service (Python FastAPI)

**Purpose**: Manage conversational interactions, chat history, and document-to-chat context mapping.

> **Critical Requirement**: Chat history must be persisted and linked to document context.

**Endpoints**:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/chat/conversations` | List user's conversations |
| POST | `/api/chat/conversations` | Create new conversation |
| GET | `/api/chat/conversations/:id` | Get conversation with messages |
| DELETE | `/api/chat/conversations/:id` | Delete conversation |
| POST | `/api/chat/conversations/:id/messages` | Send message (triggers RAG) |
| GET | `/api/chat/conversations/:id/messages` | Get message history |

**Database Schema**:
```sql
CREATE TABLE conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  title VARCHAR(255), -- Auto-generated from first message
  document_ids UUID[], -- Documents included in context
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL, -- 'user' | 'assistant'
  content TEXT NOT NULL,
  sources JSONB, -- [{chunkId, documentId, snippet, score}]
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_conversations_user ON conversations(user_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id);
```

**Chat Flow with Document Context**:

```python
async def process_chat_message(
    conversation_id: str, 
    user_message: str, 
    user_id: str
):
    # 1. Get conversation and its document scope
    conversation = await db.get_conversation(conversation_id)
    document_ids = conversation.document_ids or []
    
    # 2. Get previous messages for context (last 10)
    history = await db.get_messages(conversation_id, limit=10)
    
    # 3. Build context from chat history
    chat_context = format_chat_history(history)
    
    # 4. Query vector DB with document filter
    query_embedding = embedding_model.encode(user_message)
    vector_results = await vector_db.search(
        collection='document_chunks',
        query_vector=query_embedding,
        filter={
            'user_id': user_id,
            **(document_ids and {'document_id': {'$in': document_ids}})
        },
        top_k=5
    )
    
    # 5. Build RAG prompt with sources
    rag_context = '\n\n'.join([
        f"[Source: {r.metadata['document_name']}, p.{r.metadata['page']}]\n{r.content}"
        for r in vector_results
    ])
    
    # 6. Generate LLM response
    response = await llm.generate(
        system_prompt=COGNIFY_SYSTEM_PROMPT,
        messages=[
            *chat_context,
            {'role': 'user', 'content': f"""Based on these sources:
{rag_context}

User question: {user_message}

Provide a helpful response citing sources when applicable."""}
        ]
    )
    
    # 7. Save messages to database
    await db.insert_message(conversation_id, 'user', user_message)
    await db.insert_message(
        conversation_id, 
        'assistant', 
        response.content,
        sources=[{
            'chunkId': r.id,
            'documentId': r.metadata['document_id'],
            'snippet': r.content[:200],
            'score': r.score
        } for r in vector_results]
    )
    
    # 8. Update conversation timestamp
    await db.update_conversation(conversation_id, {'updated_at': 'NOW()'})
    
    return {
        'message': response.content,
        'sources': vector_results
    }
```

**Technologies**:
- Python 3.11+
- FastAPI
- LangChain for conversation memory
- PostgreSQL for persistence

---

### 6. Analytics Service (Spring Boot)

**Purpose**: Track user activity and provide insights.

**Endpoints**:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analytics/summary` | User dashboard stats |
| GET | `/api/analytics/queries` | Query history with trends |
| GET | `/api/analytics/storage` | Storage usage breakdown |
| GET | `/api/analytics/topics` | Top searched topics |

**Database Schema**:
```sql
CREATE TABLE analytics_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  event_type VARCHAR(50) NOT NULL, -- 'query', 'upload', 'view', 'feedback'
  event_data JSONB,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE query_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  query TEXT NOT NULL,
  results_count INTEGER,
  top_result_score DECIMAL(5,4),
  execution_time_ms INTEGER,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_analytics_user_date ON analytics_events(user_id, created_at);
```

---

### 6. Admin Service (Spring Boot)

**Purpose**: Platform administration and monitoring.

**Domain Responsibility**: User management, system monitoring, platform analytics

**Authorization**: All endpoints require `role: admin`

**Endpoints**:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/users` | List all users (paginated) |
| GET | `/api/admin/users/:id` | Get user details with stats |
| PUT | `/api/admin/users/:id` | Update user |
| DELETE | `/api/admin/users/:id` | Delete user and all data |
| POST | `/api/admin/users/:id/activate` | Activate user |
| POST | `/api/admin/users/:id/deactivate` | Deactivate user |
| GET | `/api/admin/documents` | All documents across users |
| DELETE | `/api/admin/documents/:id` | Force delete any document |
| GET | `/api/admin/jobs` | Indexing job status |
| POST | `/api/admin/jobs/:id/reprocess` | Reprocess failed job |
| POST | `/api/admin/jobs/:id/cancel` | Cancel pending job |
| GET | `/api/admin/stats` | System statistics |
| GET | `/api/admin/analytics` | Platform-wide analytics |
| GET | `/api/admin/health` | Service health status |

---

## 💾 Data Architecture

### Database Strategy

Cognify employs a **polyglot persistence** strategy, using the right database for each use case:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATA ARCHITECTURE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         OPERATIONAL DATA                                │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │ │
│  │  │   PostgreSQL     │  │      Redis       │  │    MinIO/S3          │  │ │
│  │  │                  │  │                  │  │                      │  │ │
│  │  │ • User data      │  │ • Sessions       │  │ • Original files     │  │ │
│  │  │ • Documents      │  │ • Token cache    │  │ • Thumbnails         │  │ │
│  │  │ • Chunks meta    │  │ • Rate limits    │  │ • Exports            │  │ │
│  │  │ • Conversations  │  │ • Job queues     │  │                      │  │ │
│  │  │ • Analytics      │  │ • Pub/Sub        │  │                      │  │ │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                          AI/ML DATA                                     │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │              Vector Database (Pinecone / Qdrant / Weaviate)      │  │ │
│  │  │                                                                   │  │ │
│  │  │  • Document chunk embeddings (1536 dimensions)                   │  │ │
│  │  │  • User-scoped collections for data isolation                    │  │ │
│  │  │  • Metadata: document_id, user_id, page, chunk_index            │  │ │
│  │  │  • Approximate Nearest Neighbor (ANN) indexing                   │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                       OPTIONAL: ANALYTICS                               │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │                    Elasticsearch                                  │  │ │
│  │  │  • Full-text search indexing                                     │  │ │
│  │  │  • Analytics aggregations                                        │  │ │
│  │  │  • Log storage and analysis                                      │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   users     │       │    documents    │       │     chunks      │
├─────────────┤       ├─────────────────┤       ├─────────────────┤
│ id (PK)     │──┐    │ id (PK)         │──┐    │ id (PK)         │
│ email       │  │    │ user_id (FK)    │◀─┘    │ document_id (FK)│
│ password    │  │    │ name            │  │    │ chunk_index     │
│ name        │  │    │ type            │  ├───▶│ content         │
│ role        │  │    │ size            │  │    │ page_number     │
│ status      │  │    │ status          │       │ token_count     │
│ created_at  │  │    │ chunks_count    │       │ vector_id       │
└─────────────┘  │    │ storage_url     │       │ created_at      │
                 │    │ summary         │       └─────────────────┘
                 │    │ tags            │
                 │    │ created_at      │       ┌─────────────────┐
                 │    └─────────────────┘       │ conversations   │
                 │                              ├─────────────────┤
                 │    ┌─────────────────┐       │ id (PK)         │
                 │    │    messages     │       │ user_id (FK)    │◀─┐
                 │    ├─────────────────┤       │ title           │  │
                 │    │ id (PK)         │       │ document_ids[]  │  │
                 └───▶│ conversation_id │◀──────│ created_at      │  │
                      │ role            │       │ updated_at      │  │
                      │ content         │       └─────────────────┘  │
                      │ sources (JSON)  │                            │
                      │ created_at      │       ┌─────────────────┐  │
                      └─────────────────┘       │ study_list      │  │
                                                ├─────────────────┤  │
                                                │ id (PK)         │  │
                                                │ user_id (FK)    │◀─┘
                                                │ chunk_id (FK)   │
                                                │ notes           │
                                                │ created_at      │
                                                └─────────────────┘
```

### Vector Database Schema

**Collection**: `document_chunks`

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique vector ID |
| `values` | float[1536] | Embedding vector |
| `metadata.user_id` | string | Owner user ID (for filtering) |
| `metadata.document_id` | string | Source document ID |
| `metadata.document_name` | string | Document filename |
| `metadata.chunk_index` | int | Position in document |
| `metadata.page_number` | int | Source page (if applicable) |
| `metadata.content` | string | Original text content |
| `metadata.token_count` | int | Token count |

**Index Configuration**:
```json
{
  "name": "document_chunks",
  "dimension": 1536,
  "metric": "cosine",
  "pods": 1,
  "replicas": 1,
  "pod_type": "p1.x1"
}
```

---

## 📄 Document Lifecycle

> Critical path from upload to queryable knowledge

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DOCUMENT LIFECYCLE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. UPLOAD                      2. STORAGE                                  │
│  ┌─────────────────────┐        ┌─────────────────────┐                    │
│  │ User uploads file   │ ───→   │ File stored in     │                    │
│  │ via /documents      │        │ MinIO/S3 bucket    │                    │
│  │                     │        │                     │                    │
│  │ • Validate type     │        │ • Generate URL     │                    │
│  │ • Check size limit  │        │ • Store metadata   │                    │
│  │ • Scan for malware  │        │ • Status: 'pending'│                    │
│  └─────────────────────┘        └──────────┬──────────┘                    │
│                                             │                               │
│                                             ▼                               │
│  3. QUEUE                       4. PARSE                                    │
│  ┌─────────────────────┐        ┌─────────────────────┐                    │
│  │ Publish message to  │ ───→   │ Extract text from  │                    │
│  │ document.uploaded   │        │ PDF/DOCX/TXT       │                    │
│  │ queue               │        │                     │                    │
│  │                     │        │ • Page boundaries   │                    │
│  │ • document_id       │        │ • Preserve structure│                    │
│  │ • user_id           │        │ • Extract metadata  │                    │
│  └─────────────────────┘        └──────────┬──────────┘                    │
│                                             │                               │
│                                             ▼                               │
│  5. CHUNK                       6. EMBED                                    │
│  ┌─────────────────────┐        ┌─────────────────────┐                    │
│  │ Split text into     │ ───→   │ Generate vector    │                    │
│  │ semantic chunks     │        │ embeddings         │                    │
│  │                     │        │                     │                    │
│  │ • 512 tokens target │        │ • OpenAI or local  │                    │
│  │ • 50 token overlap  │        │ • Batch for perf   │                    │
│  │ • Sentence-aware    │        │ • Handle failures  │                    │
│  └─────────────────────┘        └──────────┬──────────┘                    │
│                                             │                               │
│                                             ▼                               │
│  7. STORE VECTORS               8. FINALIZE                                 │
│  ┌─────────────────────┐        ┌─────────────────────┐                    │
│  │ Upsert to vector DB │ ───→   │ Update document    │                    │
│  │                     │        │ status             │                    │
│  │ • User ID metadata  │        │                     │                    │
│  │ • Document context  │        │ • Status: 'indexed'│                    │
│  │ • Page numbers      │        │ • chunks_count     │                    │
│  └─────────────────────┘        │ • summary          │                    │
│                                  │ • auto-tags        │                    │
│                                  └─────────────────────┘                    │
│                                                                              │
│  9. QUERYABLE ✓                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Document chunks now searchable via /recall/search or /chat          │   │
│  │ • Semantic similarity matching                                       │   │
│  │ • RAG-augmented responses                                           │   │
│  │ • Source attribution in chat                                        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Failure States & Recovery

| Stage | Failure | HTTP Code | Recovery Action | User Notification |
|-------|---------|-----------|-----------------|-------------------|
| Upload | File too large | 413 | Reject immediately | Show size limit |
| Upload | Invalid type | 400 | Reject immediately | Show allowed types |
| Upload | Duplicate file | 409 | Reject or prompt replace | Ask to replace |
| Parse | Corrupted file | - | Mark 'failed' | Email notification |
| Chunk | Memory limit | - | Process in smaller batches | None (automatic) |
| Embed | OpenAI timeout | - | Retry 3x, fallback to local | None (automatic) |
| Embed | Rate limited | - | Exponential backoff, queue | Processing delayed |
| Vector DB | Write failed | - | Retry, dead-letter after 5 | Email if failed |
| Finalize | DB error | - | Transaction rollback, retry | Retry or notify |

---

## 🤖 RAG Pipeline Design

### Retrieval-Augmented Generation Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RAG PIPELINE ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  USER QUERY: "What are the key findings about climate change?"              │
│                                 │                                            │
│                                 ▼                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 1. QUERY PROCESSING                                                  │    │
│  │    • Query embedding generation (OpenAI text-embedding-3-small)      │    │
│  │    • Query expansion (optional): synonyms, related terms             │    │
│  │    • Intent classification: search vs. summarize vs. compare        │    │
│  └────────────────────────────────┬────────────────────────────────────┘    │
│                                   ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 2. RETRIEVAL                                                         │    │
│  │    • Vector similarity search (cosine, top-k=10)                     │    │
│  │    • Metadata filtering (user_id, document_ids if scoped)            │    │
│  │    • Hybrid search: vector + BM25 keyword (optional)                │    │
│  └────────────────────────────────┬────────────────────────────────────┘    │
│                                   ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 3. RERANKING                                                         │    │
│  │    • Cross-encoder reranking (ms-marco-MiniLM-L-12-v2)              │    │
│  │    • Diversity filtering (MMR - Maximal Marginal Relevance)         │    │
│  │    • Score threshold filtering (>0.7)                                │    │
│  └────────────────────────────────┬────────────────────────────────────┘    │
│                                   ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 4. CONTEXT CONSTRUCTION                                              │    │
│  │    • Select top 3-5 chunks within token budget                      │    │
│  │    • Include source attribution metadata                            │    │
│  │    • Order by relevance score                                       │    │
│  └────────────────────────────────┬────────────────────────────────────┘    │
│                                   ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 5. LLM GENERATION                                                    │    │
│  │    • System prompt with Cognify instructions                        │    │
│  │    • Context injection with source markers                          │    │
│  │    • User query + chat history (if conversation)                    │    │
│  │    • Streaming response (SSE)                                       │    │
│  └────────────────────────────────┬────────────────────────────────────┘    │
│                                   ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 6. RESPONSE DELIVERY                                                 │    │
│  │    • Answer with inline source citations [1], [2]                   │    │
│  │    • Source list with document names and page numbers               │    │
│  │    • Confidence indicators                                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Chunking Strategies

| Strategy | Use Case | Parameters |
|----------|----------|------------|
| **Fixed-Size** | Simple documents | 512 tokens, 50 overlap |
| **Semantic** | Complex documents | Sentence boundaries, topic detection |
| **Recursive** | Hierarchical content | Split by headers, then paragraphs |
| **Document-Aware** | Structured docs | Respect PDF pages, DOCX sections |

### Embedding Models Comparison

| Model | Dimensions | Speed | Quality | Cost |
|-------|------------|-------|---------|------|
| OpenAI text-embedding-3-small | 1536 | Fast | High | $0.02/1M tokens |
| OpenAI text-embedding-3-large | 3072 | Medium | Higher | $0.13/1M tokens |
| all-MiniLM-L6-v2 (local) | 384 | Very Fast | Good | Free |
| all-mpnet-base-v2 (local) | 768 | Fast | Better | Free |

### Prompt Engineering

**System Prompt for RAG**:
```
You are Cognify, an AI assistant that helps users recall information from their 
personal document collection. Your responses should:

1. Base answers ONLY on the provided context from the user's documents
2. Cite sources using [Source: Document Name, p.X] format
3. If information is not in the context, say "I couldn't find this in your documents"
4. Be concise but comprehensive
5. Highlight key facts and dates when relevant
6. Suggest related topics the user might want to explore

Never make up information. Never use external knowledge unless explicitly asked.
```

---

## 🛠️ Technology Stack Summary

### Backend Services

| Service | Language | Framework | Version | Purpose |
|---------|----------|-----------|---------|---------|
| API Gateway | Java | Spring Cloud Gateway | 2023.0.x | Routing, Auth, Rate Limiting |
| Auth Service | Java | Spring Boot | 3.2.x | Authentication & Authorization |
| Document Service | Java | Spring Boot | 3.2.x | Document Management |
| Indexing Service | Python | FastAPI | 0.109.x | Document Processing & Embedding |
| Recall Service | Python | FastAPI | 0.109.x | Semantic Search & RAG |
| Chat Service | Python | FastAPI | 0.109.x | Conversations & Context |
| Analytics Service | Java | Spring Boot | 3.2.x | Usage Tracking & Metrics |
| Admin Service | Java | Spring Boot | 3.2.x | Platform Administration |

### Data Stores

| Store | Version | Purpose | Scaling Strategy |
|-------|---------|---------|------------------|
| PostgreSQL | 15+ | Primary relational database | Read replicas |
| Pinecone/Qdrant | Latest | Vector embeddings | Sharding by user_id |
| MinIO/S3 | Latest | Object storage | Standard S3 scaling |
| Redis | 7+ | Cache, sessions, queues | Redis Cluster |
| Elasticsearch | 8.x | Full-text search (optional) | Multi-node cluster |

### Message Broker

| Technology | Version | Use Cases |
|------------|---------|-----------|
| RabbitMQ | 3.12+ | Document processing queue, dead-letter queues |
| Kafka (alternative) | 3.6+ | High-throughput event streaming |

### AI/ML Components

| Component | Options | Purpose | Fallback |
|-----------|---------|---------|----------|
| Embedding Model | OpenAI `text-embedding-3-small` | Vector generation | Sentence Transformers |
| LLM | GPT-4, GPT-4-turbo, Claude 3 | RAG augmentation | Ollama (local) |
| Reranker | Cross-Encoder models | Result quality | Score-based ranking |

### Development Dependencies

| Category | Tools |
|----------|-------|
| Build | Maven/Gradle (Java), Poetry (Python) |
| Testing | JUnit 5, pytest, Testcontainers |
| Code Quality | SonarQube, Black, Ruff |
| Documentation | OpenAPI/Swagger, Sphinx |

---

## 🚀 Deployment Strategy

### Docker Compose (Development)

```yaml
version: '3.8'
services:
  gateway:
    build: ./gateway
    ports:
      - "8080:8080"
    depends_on:
      - auth-service
      - document-service
      - recall-service
  
  auth-service:
    build: ./auth-service
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cognify
      - REDIS_HOST=redis
  
  document-service:
    build: ./document-service
    environment:
      - MINIO_ENDPOINT=http://minio:9000
      - RABBITMQ_HOST=rabbitmq
  
  indexing-service:
    build: ./indexing-service
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - PINECONE_API_KEY=${PINECONE_API_KEY}
  
  recall-service:
    build: ./recall-service
    environment:
      - PINECONE_API_KEY=${PINECONE_API_KEY}
  
  postgres:
    image: postgres:15
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7-alpine
  
  rabbitmq:
    image: rabbitmq:3-management
  
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
```

### Kubernetes (Production)

**Cluster Architecture**:
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        KUBERNETES CLUSTER                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         INGRESS LAYER                                   │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────────┐ │ │
│  │  │   NGINX     │  │   Cert      │  │        WAF (Optional)           │ │ │
│  │  │   Ingress   │  │   Manager   │  │   • Rate limiting               │ │ │
│  │  │   Controller│  │   (TLS)     │  │   • SQL injection protection    │ │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        APPLICATION LAYER                                │ │
│  │  ┌───────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    Deployments (with HPA)                          │ │ │
│  │  │  gateway(2-10) | auth(2-5) | documents(2-8) | recall(3-15)        │ │ │
│  │  │  indexing(2-20)| chat(2-10)| analytics(2-4) | admin(1-2)          │ │ │
│  │  └───────────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         DATA LAYER                                      │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │ │
│  │  │ PostgreSQL  │  │    Redis    │  │  RabbitMQ   │  │    MinIO    │   │ │
│  │  │ (StatefulSet│  │  (Cluster)  │  │  (Cluster)  │  │ (StatefulSet│   │ │
│  │  │  + PVC)     │  │             │  │             │  │  + PVC)     │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      OBSERVABILITY LAYER                                │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │ │
│  │  │ Prometheus  │  │   Grafana   │  │    Jaeger   │  │    Loki     │   │ │
│  │  │  (metrics)  │  │ (dashboards)│  │  (tracing)  │  │  (logging)  │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Helm Chart Structure**:
```
cognify-helm/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-staging.yaml
├── values-prod.yaml
├── templates/
│   ├── _helpers.tpl
│   ├── configmap.yaml
│   ├── secrets.yaml
│   ├── gateway/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── hpa.yaml
│   ├── auth-service/
│   ├── document-service/
│   └── ...
└── charts/
    ├── postgresql/
    ├── redis/
    └── rabbitmq/
```

**Key Kubernetes Resources**:

| Resource | Purpose | Configuration |
|----------|---------|---------------|
| Deployments | Stateless services | Rolling updates, resource limits |
| StatefulSets | PostgreSQL, MinIO | Persistent storage, ordered startup |
| HPA | Auto-scaling | CPU/memory/custom metrics |
| PVC | Persistent storage | Storage classes, retention |
| ConfigMaps | Configuration | Environment-specific settings |
| Secrets | Credentials | External secrets operator |
| NetworkPolicies | Security | Pod-to-pod communication rules |
| Ingress | External access | TLS, path-based routing |

**Resource Recommendations**:

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit |
|---------|-------------|-----------|----------------|--------------|
| Gateway | 200m | 500m | 256Mi | 512Mi |
| Auth | 200m | 500m | 256Mi | 512Mi |
| Documents | 300m | 1000m | 512Mi | 1Gi |
| Indexing | 500m | 2000m | 1Gi | 4Gi |
| Recall | 500m | 2000m | 1Gi | 2Gi |
| Chat | 300m | 1000m | 512Mi | 1Gi |

---

## 🔒 Security Architecture

### Security Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      PERIMETER SECURITY                                 │ │
│  │  • WAF (Web Application Firewall)                                      │ │
│  │  • DDoS Protection                                                     │ │
│  │  • TLS 1.3 Termination                                                 │ │
│  │  • IP Allowlisting (Admin endpoints)                                   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│                                    ▼                                         │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      APPLICATION SECURITY                               │ │
│  │  • JWT Authentication (RS256)      • Rate Limiting (sliding window)   │ │
│  │  • RBAC Authorization              • Input Validation (JSON Schema)   │ │
│  │  • CORS Policy                     • CSRF Protection                  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│                                    ▼                                         │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        DATA SECURITY                                    │ │
│  │  • User Data Isolation             • Encryption at Rest (AES-256)     │ │
│  │  • Document Access Control         • Encryption in Transit (TLS)      │ │
│  │  • Vector DB Filtering             • Secure File Storage              │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                         │
│                                    ▼                                         │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    INFRASTRUCTURE SECURITY                              │ │
│  │  • Network Policies                • Secrets Management (Vault)       │ │
│  │  • Service Mesh (mTLS)             • Audit Logging                    │ │
│  │  • Pod Security Standards          • Vulnerability Scanning           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Authentication & Authorization

| Layer | Implementation | Details |
|-------|----------------|---------|
| **JWT Tokens** | RS256 signing | Access: 15 min, Refresh: 7 days |
| **RBAC** | Role-based | `user`, `admin` roles |
| **Token Blacklisting** | Redis-backed | Immediate logout invalidation |
| **Password Policy** | BCrypt + rules | Min 8 chars, complexity requirements |
| **MFA** | TOTP (optional) | Google Authenticator compatible |

### Data Isolation

| Control | Implementation | Enforcement Point |
|---------|----------------|-------------------|
| **User Data Isolation** | `user_id` filtering | Database queries |
| **Document Access** | Ownership validation | Service middleware |
| **Vector DB Filtering** | Metadata filters | Vector search queries |
| **Object Storage** | Bucket per user | MinIO policies |

### Input & Upload Security

| Control | Implementation | Limits |
|---------|----------------|--------|
| **Input Validation** | JSON Schema + Zod | All endpoints |
| **File Type Validation** | Magic bytes + extension | PDF, DOCX, TXT |
| **File Size Limits** | Configurable | Default: 50MB |
| **Malware Scanning** | ClamAV integration | Production only |
| **Filename Sanitization** | Strip special chars | All uploads |

### Rate Limiting Configuration

| Endpoint Category | Limit | Window | Penalty |
|-------------------|-------|--------|---------|
| Authentication | 5 requests | 1 minute | 15 min lockout |
| File Upload | 10 requests | 1 minute | Throttle |
| Search/Recall | 60 requests | 1 minute | Queue delay |
| General API | 100 requests | 1 minute | 429 response |
| Admin API | 30 requests | 1 minute | Alert |

### Network Security

```yaml
# Kubernetes NetworkPolicy: Only allow gateway traffic
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-gateway-only
spec:
  podSelector:
    matchLabels:
      app: document-service
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
      ports:
        - protocol: TCP
          port: 8080
```

### Security Checklist

- [ ] TLS 1.3 for all external traffic
- [ ] JWT tokens with RS256 signing
- [ ] Password hashing with BCrypt (cost 12)
- [ ] Rate limiting on all endpoints
- [ ] Input validation on all inputs
- [ ] File type and size validation
- [ ] User data isolation at DB layer
- [ ] Secrets in Vault/K8s Secrets
- [ ] Network policies between pods
- [ ] Regular dependency updates
- [ ] Security scanning in CI/CD
- [ ] Audit logging enabled
- [ ] CORS properly configured
- [ ] CSRF protection enabled

---

## 🔄 Resilience & Fault Tolerance

### Document Processing Failures

```
Upload Failed?
    └── Retry up to 3 times with exponential backoff
    └── If still failing → Mark as 'failed', notify user

Chunking Failed?
    └── Log error with document context
    └── Store partial progress if possible
    └── Allow manual re-trigger via /reindex endpoint

Embedding Generation Failed (OpenAI timeout)?
    └── Fallback to local Sentence Transformers model
    └── If fallback fails → Queue for retry with backoff
    └── After 5 failures → Mark document as 'failed'
```

### Circuit Breaker Pattern

```java
// Spring Cloud Circuit Breaker example
@CircuitBreaker(name = "embeddingService", fallbackMethod = "localEmbeddingFallback")
@Retry(name = "embeddingService")
public EmbeddingResponse generateEmbedding(String text) {
    return openAiClient.embed(text);
}

public EmbeddingResponse localEmbeddingFallback(String text, Exception ex) {
    log.warn("OpenAI unavailable, using local model", ex);
    return sentenceTransformers.embed(text);
}
```

### Dead Letter Queues

```yaml
# RabbitMQ DLQ configuration
exchanges:
  - name: document.dlx
    type: direct

queues:
  - name: document.processing.dlq
    arguments:
      x-message-ttl: 86400000  # 24 hours

bindings:
  - exchange: document.dlx
    queue: document.processing.dlq
    routing-key: document.processing
```

### Graceful Degradation Strategies

| Component | Failure Mode | Degradation Strategy |
|-----------|--------------|---------------------|
| Vector DB (Pinecone) | Timeout | Return cached results, fall back to PostgreSQL full-text |
| LLM (OpenAI) | Rate limit | Queue request, show "processing" state |
| LLM (OpenAI) | Down | Return raw chunk matches without augmentation |
| Object Storage | Down | Show document metadata only, hide preview/download |
| Analytics Service | Down | Continue core operations, buffer events locally |

---

## Scaling Strategy

### Horizontal Scaling Triggers

| Service | CPU Threshold | Memory Threshold | Custom Metric |
|---------|---------------|------------------|---------------|
| API Gateway | 70% | 80% | Request queue depth > 100 |
| Document Service | 60% | 70% | Upload queue size > 50 |
| Indexing Service | 80% | 85% | Job queue depth > 20 |
| Recall Service | 65% | 75% | P95 latency > 500ms |
| Chat Service | 60% | 70% | Active connections > 1000 |

### Kubernetes HPA Configuration

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: recall-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: recall-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 65
    - type: Pods
      pods:
        metric:
          name: response_latency_p95
        target:
          type: AverageValue
          averageValue: 500m  # 500ms
```

### Database Scaling

| Database | Read Scaling | Write Scaling |
|----------|--------------|---------------|
| PostgreSQL | Read replicas (async) | Connection pooling (PgBouncer) |
| Vector DB (Qdrant) | Sharding by user_id | Cluster mode with replicas |
| Redis | Redis Cluster | Redis Cluster with writes to primary |

### Embedding Generation Scaling

```python
# Batch processing for efficiency
class EmbeddingBatcher:
    def __init__(self, batch_size=32, max_wait_ms=100):
        self.batch_size = batch_size
        self.max_wait_ms = max_wait_ms
        self.pending = []
    
    async def add(self, text: str) -> Awaitable[List[float]]:
        future = asyncio.Future()
        self.pending.append((text, future))
        
        if len(self.pending) >= self.batch_size:
            await self._flush()
        else:
            asyncio.create_task(self._flush_after_delay())
        
        return await future
    
    async def _flush(self):
        if not self.pending:
            return
        
        batch = self.pending[:self.batch_size]
        self.pending = self.pending[self.batch_size:]
        
        texts = [t for t, _ in batch]
        embeddings = await openai_client.embed_batch(texts)
        
        for (_, future), embedding in zip(batch, embeddings):
            future.set_result(embedding)
```

---

## ⚡ Performance Optimization

### Caching Strategy

| Cache Layer | Technology | TTL | Use Case |
|-------------|------------|-----|----------|
| API Response | Redis | 5 min | Document list, analytics |
| Session | Redis | 24 hr | User sessions |
| Vector Search | In-memory | 1 min | Repeated queries |
| LLM Response | Redis | 1 hr | Common questions |
| Static Assets | CDN | 1 year | Images, thumbnails |

### Optimization Techniques

| Area | Technique | Impact |
|------|-----------|--------|
| **Database** | Connection pooling (HikariCP, asyncpg) | Reduce connection overhead |
| **Database** | Read replicas | Scale read operations |
| **Embeddings** | Batch processing (32 items) | 10x throughput improvement |
| **Vector Search** | Approximate NN (HNSW) | Sub-100ms queries |
| **File Processing** | Async queue processing | Non-blocking uploads |
| **API** | Response compression (gzip) | 60-80% size reduction |
| **Frontend** | CDN for static assets | Global latency reduction |

### Performance Benchmarks (Target)

| Operation | Target Latency | Notes |
|-----------|----------------|-------|
| Authentication | < 100ms | Including JWT generation |
| Document Upload | < 500ms | Excluding processing |
| Vector Search | < 200ms | Top-10 results |
| RAG Response | < 2s | First token (streaming) |
| Page Load | < 1s | Dashboard with data |

---

## 📊 Observability & Monitoring

### Observability Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        OBSERVABILITY ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  METRICS                    LOGGING                    TRACING              │
│  ┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐  │
│  │   Prometheus    │        │      Loki       │        │     Jaeger      │  │
│  │                 │        │                 │        │                 │  │
│  │ • Service health│        │ • Structured    │        │ • Request trace │  │
│  │ • Request rates │        │   JSON logs     │        │ • Span duration │  │
│  │ • Error rates   │        │ • Error stacks  │        │ • Dependencies  │  │
│  │ • Latency P99   │        │ • Audit trail   │        │ • Bottlenecks   │  │
│  └────────┬────────┘        └────────┬────────┘        └────────┬────────┘  │
│           │                          │                          │            │
│           └──────────────────────────┼──────────────────────────┘            │
│                                      ▼                                       │
│                          ┌─────────────────────┐                            │
│                          │      Grafana        │                            │
│                          │   (Visualization)   │                            │
│                          │                     │                            │
│                          │ • Unified dashboards│                            │
│                          │ • Alert management  │                            │
│                          │ • SLO tracking      │                            │
│                          └─────────────────────┘                            │
│                                      │                                       │
│                                      ▼                                       │
│                          ┌─────────────────────┐                            │
│                          │   PagerDuty/Slack   │                            │
│                          │    (Alerting)       │                            │
│                          └─────────────────────┘                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Metrics

| Category | Metric | Alert Threshold |
|----------|--------|-----------------|
| **Availability** | Uptime % | < 99.9% |
| **Latency** | P95 response time | > 500ms |
| **Errors** | Error rate | > 1% |
| **Saturation** | CPU utilization | > 80% |
| **Saturation** | Memory utilization | > 85% |
| **Business** | Documents processed/hour | < 100 |
| **Business** | Search queries/min | Trending down |

### Logging Standards

```json
{
  "timestamp": "2024-12-21T10:30:00.000Z",
  "level": "INFO",
  "service": "document-service",
  "traceId": "abc123",
  "spanId": "def456",
  "userId": "user-789",
  "action": "document.upload",
  "documentId": "doc-101",
  "message": "Document uploaded successfully",
  "metadata": {
    "fileName": "research.pdf",
    "fileSize": 1048576,
    "processingTime": 250
  }
}
```

### Health Check Endpoints

| Service | Endpoint | Checks |
|---------|----------|--------|
| All | `/health/live` | Service running |
| All | `/health/ready` | Dependencies available |
| All | `/health/startup` | Initial setup complete |

---

## 🛠️ Development Workflow

### Development Lifecycle

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Feature │ →  │   Dev   │ →  │   PR    │ →  │ Staging │ →  │  Prod   │
│ Branch  │    │ Testing │    │ Review  │    │ Deploy  │    │ Deploy  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
     │              │              │              │              │
     ▼              ▼              ▼              ▼              ▼
  Create        Unit +         Code +        E2E Tests     Canary +
  feature      Integration    Security       Smoke         Gradual
  branch        tests         review         tests         rollout
```

### CI/CD Pipeline

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run tests
        run: |
          ./gradlew test (Java)
          pytest (Python)
      - name: Security scan
        uses: snyk/actions/node@master
      - name: SonarQube analysis
        run: ./gradlew sonarqube

  build:
    needs: test
    steps:
      - name: Build Docker images
        run: docker build -t cognify/$SERVICE .
      - name: Push to registry
        run: docker push cognify/$SERVICE

  deploy-staging:
    needs: build
    if: github.ref == 'refs/heads/develop'
    steps:
      - name: Deploy to staging
        run: helm upgrade --install cognify ./helm -f values-staging.yaml

  deploy-prod:
    needs: build
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Deploy to production
        run: helm upgrade --install cognify ./helm -f values-prod.yaml
```

### API-First Development

1. **Design**: Create OpenAPI specification
2. **Review**: API design review with stakeholders
3. **Mock**: Generate mock server for frontend development
4. **Implement**: Build service matching specification
5. **Test**: Contract testing against specification
6. **Document**: Auto-generate documentation

---

## 📋 Implementation Roadmap

### Phase 1: Foundation (Weeks 1-3)

| Task | Status | Owner | Priority |
|------|--------|-------|----------|
| ✅ Frontend complete (React + Vite) | Done | Frontend | - |
| ⬜ Set up Spring Boot project structure | Pending | Backend | P0 |
| ⬜ Set up FastAPI project structure | Pending | Backend | P0 |
| ⬜ Configure PostgreSQL schemas | Pending | Backend | P0 |
| ⬜ Set up Docker Compose for dev | Pending | DevOps | P0 |
| ⬜ Configure CI/CD pipeline | Pending | DevOps | P1 |

### Phase 2: Core Services (Weeks 4-7)

| Task | Status | Owner | Priority |
|------|--------|-------|----------|
| ⬜ Implement Auth Service | Pending | Backend | P0 |
| ⬜ Implement Document Service | Pending | Backend | P0 |
| ⬜ Set up MinIO/S3 storage | Pending | Backend | P0 |
| ⬜ Implement file upload flow | Pending | Backend | P0 |
| ⬜ Connect frontend to backend | Pending | Full Stack | P0 |

### Phase 3: AI/ML Pipeline (Weeks 8-11)

| Task | Status | Owner | Priority |
|------|--------|-------|----------|
| ⬜ Set up vector database (Qdrant) | Pending | Backend | P0 |
| ⬜ Implement Indexing Service | Pending | ML | P0 |
| ⬜ Implement Recall Service | Pending | ML | P0 |
| ⬜ Integrate OpenAI embeddings | Pending | ML | P0 |
| ⬜ Implement RAG pipeline | Pending | ML | P0 |
| ⬜ Build Chat Service | Pending | ML | P1 |

### Phase 4: Production Ready (Weeks 12-14)

| Task | Status | Owner | Priority |
|------|--------|-------|----------|
| ⬜ Implement Analytics Service | Pending | Backend | P1 |
| ⬜ Implement Admin Service | Pending | Backend | P1 |
| ⬜ Set up monitoring (Prometheus) | Pending | DevOps | P1 |
| ⬜ Security hardening | Pending | Security | P0 |
| ⬜ Load testing | Pending | QA | P1 |
| ⬜ Deploy to cloud | Pending | DevOps | P0 |

---

## 📚 Appendix

### A. Environment Variables Reference

**Auth Service**:
```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cognify
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=secret

# JWT
JWT_SECRET_KEY=your-256-bit-secret
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

**Indexing Service**:
```properties
# OpenAI
OPENAI_API_KEY=sk-...
OPENAI_EMBEDDING_MODEL=text-embedding-3-small

# Vector DB
QDRANT_HOST=localhost
QDRANT_PORT=6333
QDRANT_COLLECTION=document_chunks

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
```

### B. API Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `AUTH_001` | 401 | Invalid credentials |
| `AUTH_002` | 401 | Token expired |
| `AUTH_003` | 403 | Insufficient permissions |
| `DOC_001` | 400 | Invalid file type |
| `DOC_002` | 413 | File too large |
| `DOC_003` | 404 | Document not found |
| `DOC_004` | 409 | Duplicate document |
| `SEARCH_001` | 400 | Empty query |
| `SEARCH_002` | 503 | Vector DB unavailable |
| `RATE_001` | 429 | Rate limit exceeded |
| `SYS_001` | 500 | Internal server error |

### C. Database Migration Naming

```
V{version}__{description}.sql

Examples:
V001__create_users_table.sql
V002__create_documents_table.sql
V003__add_user_status_column.sql
V004__create_conversations_table.sql
```

### D. Glossary

| Term | Definition |
|------|------------|
| **RAG** | Retrieval-Augmented Generation - combining retrieval with LLM generation |
| **Embedding** | Vector representation of text for semantic similarity |
| **Chunking** | Splitting documents into smaller searchable pieces |
| **Vector DB** | Database optimized for similarity search on embeddings |
| **LLM** | Large Language Model (GPT-4, Claude, etc.) |
| **Cross-Encoder** | Model that jointly encodes query and document for reranking |
| **HNSW** | Hierarchical Navigable Small World - ANN index algorithm |
| **mTLS** | Mutual TLS - two-way certificate authentication |

### E. References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [LangChain RAG Guide](https://python.langchain.com/docs/use_cases/question_answering/)
- [Pinecone Vector Database](https://docs.pinecone.io/)
- [Qdrant Vector Database](https://qdrant.tech/documentation/)
- [OpenAI Embeddings Guide](https://platform.openai.com/docs/guides/embeddings)

---

<div align="center">

**Cognify Backend Architecture v2.0**

*Last Updated: December 2024*

*Document Authors: Development Team*

</div>
