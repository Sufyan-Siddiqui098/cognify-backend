# 📚 Cognify Documentation - Privacy-First Local AI

> **Start Here to Build Your Local AI RAG System**

---

## 🎯 Quick Navigation

**New to the project?** Read in this order:

1. **This File** (5 min) - Overview
2. **PROJECT_STATUS.md** (10 min) - Architecture & what's needed
3. **AI_SERVICE_IMPLEMENTATION_GUIDE.md** (Main guide) - Complete implementation

---

## 🔒 Core Architecture: Privacy-First

**Your Requirement:** "User privacy is our critical concern"

**Our Solution:** 100% Local AI Processing

```
┌─────────────────────────────────────────────────┐
│  Frontend (React) → Spring Boot Backend         │
│            ↓                                     │
│  Python AI Service (ALL LOCAL):                 │
│    • sentence-transformers (768-dim)            │
│    • Qdrant (self-hosted vector DB)             │
│    • LLaMA/Mistral (local LLM via Ollama)       │
│    • No external API calls!                     │
└─────────────────────────────────────────────────┘
```

**Result:** User documents never leave your server.

---

## 📖 Documentation Files

| File | Purpose | Read Time |
|------|---------|-----------|
| **PROJECT_STATUS.md** | What's built vs what's needed | 10 min |
| **AI_SERVICE_IMPLEMENTATION_GUIDE.md** | Complete code + setup | 2-3 hours |
| **README_AI_SERVICE.md** | Quick start guide | 15 min |

---

## 🚀 30-Minute Quick Start

### Step 1: Setup Local AI Stack (20 min)

```powershell
# 1. Install Qdrant (Vector Database)
docker pull qdrant/qdrant
docker run -d -p 6333:6333 -p 6334:6334 --name qdrant qdrant/qdrant

# 2. Install Ollama (Local LLM)
# Download from: https://ollama.com/download
# After install:
ollama pull llama3.2

# 3. Setup Python Environment
mkdir cognify-ai-service
cd cognify-ai-service
python -m venv venv
.\venv\Scripts\Activate.ps1

# 4. Install Dependencies
pip install fastapi uvicorn sentence-transformers qdrant-client PyPDF2 python-docx requests pydantic pydantic-settings python-dotenv
```

### Step 2: Create Project Structure (5 min)

```powershell
# Create directories
New-Item -ItemType Directory -Path "app", "app\services", "app\api", "logs"
New-Item -ItemType File -Path "app\__init__.py", "app\services\__init__.py", "app\api\__init__.py"

# Create .env file
@"
SERVICE_PORT=8000
QDRANT_HOST=localhost
QDRANT_PORT=6333
OLLAMA_URL=http://localhost:11434
EMBEDDING_MODEL=all-MiniLM-L6-v2
VECTOR_DIMENSION=768
CHUNK_SIZE=512
CHUNK_OVERLAP=50
STORAGE_BASE_PATH=C:/cognify-uploads
"@ | Out-File -FilePath .env -Encoding utf8
```

### Step 3: Copy Code (5 min)

Open `AI_SERVICE_IMPLEMENTATION_GUIDE.md` and copy code sections for:
- `app/main.py`
- `app/config.py`
- `app/models.py`
- `app/services/*` (all service files)
- `app/api/routes.py`

---

## 🔧 Technology Stack

### Backend (✅ Complete)
- Spring Boot 3.4.2 + Java 17
- PostgreSQL 17
- Redis (optional, for sessions)

### AI Service (📋 To Build)
- **Python 3.11** + FastAPI
- **sentence-transformers** - Local embeddings (768-dim)
  - Model: `all-MiniLM-L6-v2` (22MB)
  - Speed: ~500 sentences/sec on CPU
- **Qdrant** - Self-hosted vector database
  - Runs in Docker
  - Cosine similarity search
- **Ollama** - Local LLM runtime
  - LLaMA 3.2 3B (~2GB) or Mistral 7B (~4GB)
  - No API keys needed!

---

## 💡 Why Local AI?

### Privacy Benefits
✅ User documents never sent to external services  
✅ No API keys needed (no cost, no limits)  
✅ Complete data sovereignty  
✅ Compliant with data privacy regulations  

### Challenges
⚠️ Requires more RAM (8-16GB)  
⚠️ LLM responses slower than GPT-4 (but acceptable)  
⚠️ Lower quality than GPT-4 (but good enough for prototype)  

### Best Practices
- Use GPU if available (speeds up embeddings)
- Choose LLaMA 3.2 for speed or Mistral for quality
- Keep documents under 50 pages for prototype
- Designed for 1-5 concurrent users (prototype scale)

---

## 📊 System Architecture

```
User uploads PDF
    ↓
Spring Boot Backend (Port 8082)
    ├─ Saves file locally
    ├─ Creates document record (PostgreSQL)
    └─ Calls Python AI Service
        ↓
Python AI Service (Port 8000) - ALL LOCAL
    ├─ Parse PDF (PyPDF2)
    ├─ Chunk text (512 tokens)
    ├─ Generate embeddings (sentence-transformers, 768-dim)
    ├─ Store in Qdrant (with user_id, document_id)
    └─ Return chunks to backend
        ↓
Spring Boot saves chunks to PostgreSQL
    ↓
Document status: INDEXED ✅

User asks question
    ↓
Spring Boot receives question
    ↓
Python AI Service - ALL LOCAL
    ├─ Generate query embedding (sentence-transformers)
    ├─ Search Qdrant (top 5, filter by user_id)
    ├─ Build context from chunks
    ├─ Call Ollama (LLaMA/Mistral) with context
    └─ Return answer with sources
        ↓
Spring Boot saves message + sources
    ↓
Frontend displays answer + citations
```

**Privacy:** 🔒 Everything happens on your server!

---

## 🎯 Implementation Checklist

### Setup (30 min)
- [ ] Install Docker
- [ ] Run Qdrant container
- [ ] Install Ollama
- [ ] Pull LLaMA 3.2 or Mistral
- [ ] Setup Python environment
- [ ] Install dependencies

### Code (2-3 hours)
- [ ] Create project structure
- [ ] Copy main.py, config.py, models.py
- [ ] Copy all services (document, chunking, embedding, vector, llm, rag)
- [ ] Copy API routes
- [ ] Configure .env

### Test (30 min)
- [ ] Start Qdrant: `docker start qdrant`
- [ ] Start Ollama: `ollama serve`
- [ ] Start AI service: `uvicorn app.main:app --reload`
- [ ] Start backend: `./mvnw spring-boot:run`
- [ ] Upload PDF and ask questions

### Verify (10 min)
- [ ] Document processes in < 2 minutes
- [ ] Embeddings generated (768-dim)
- [ ] Vectors in Qdrant (check http://localhost:6334)
- [ ] Chat returns relevant answers
- [ ] Sources displayed correctly

---

## 🔍 Key Concepts

### Embeddings (768 dimensions)
```python
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('all-MiniLM-L6-v2')
text = "climate change impacts"
embedding = model.encode(text)  
# Returns: array of 768 numbers
# Similar texts have similar embeddings
```

### Qdrant (Vector Database)
```python
from qdrant_client import QdrantClient

client = QdrantClient(host="localhost", port=6333)

# Store vector
client.upsert(
    collection_name="documents",
    points=[{
        "id": "chunk_1",
        "vector": embedding,
        "payload": {"user_id": "uuid", "content": "text..."}
    }]
)

# Search (with user isolation)
results = client.search(
    collection_name="documents",
    query_vector=query_embedding,
    query_filter={"must": [{"key": "user_id", "match": {"value": user_id}}]},
    limit=5
)
```

### Ollama (Local LLM)
```python
import requests

response = requests.post(
    'http://localhost:11434/api/generate',
    json={
        'model': 'llama3.2',
        'prompt': f"Context: {context}\n\nQuestion: {question}",
        'stream': False
    }
)
answer = response.json()['response']
```

---

## 📝 Model Comparison

| Model | Size | RAM | Speed | Quality | Best For |
|-------|------|-----|-------|---------|----------|
| **all-MiniLM-L6-v2** | 22MB | 500MB | Fast | Good | Embeddings ✅ |
| **LLaMA 3.2 3B** | 2GB | 4GB | Fast | Good | Prototype ✅ |
| **Mistral 7B** | 4GB | 8GB | Medium | Better | Better quality |
| **GPT-4** | Cloud | N/A | Fast | Best | ❌ Not private |

**Recommendation:** 
- Embeddings: `all-MiniLM-L6-v2` (only option for 768-dim)
- LLM: Start with LLaMA 3.2, switch to Mistral if quality insufficient

---

## 🐛 Common Issues

### Issue 1: "Qdrant connection refused"
```powershell
# Check if Qdrant is running
docker ps | findstr qdrant

# Start if not running
docker start qdrant

# Or run new container
docker run -d -p 6333:6333 -p 6334:6334 --name qdrant qdrant/qdrant
```

### Issue 2: "Ollama not responding"
```powershell
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama service
ollama serve

# In another terminal, verify model is pulled
ollama list
# If not, pull it:
ollama pull llama3.2
```

### Issue 3: "sentence-transformers model not found"
```python
# Model downloads automatically on first use
from sentence_transformers import SentenceTransformer
model = SentenceTransformer('all-MiniLM-L6-v2')
# Wait for download (~22MB)
```

### Issue 4: "Out of memory"
- Close other applications
- Use LLaMA 3.2 instead of Mistral (smaller)
- Reduce batch size in embedding generation
- Consider adding RAM or using GPU

---

## 📈 Performance Expectations

**For Prototype:**
- Document Processing: 1-2 minutes (10-page PDF)
- Embedding Generation: 5-10 seconds per document
- Vector Search: <1 second
- LLM Response: 5-15 seconds (LLaMA 3.2)

**Concurrent Users:** Designed for 1-5 users (prototype scale)

**Model Response Quality:**
- LLaMA 3.2: ~70-80% of GPT-4 quality (good for prototype)
- Mistral 7B: ~80-85% of GPT-4 quality (better, slower)

---

## ✅ Success Criteria

Your prototype works correctly when:

- [ ] Documents upload successfully
- [ ] Processing completes in < 2 minutes
- [ ] Qdrant contains vectors (check dashboard: http://localhost:6334)
- [ ] Search returns relevant chunks
- [ ] Answers reference uploaded documents (not general knowledge)
- [ ] Sources displayed with page numbers
- [ ] Multiple users isolated (can't see each other's docs)
- [ ] No external API calls (verify with network monitor)

---

## 🎓 Resources

### Documentation
- **Main Guide:** `AI_SERVICE_IMPLEMENTATION_GUIDE.md` (Complete implementation)
- **Status:** `PROJECT_STATUS.md` (Architecture overview)
- **This File:** Quick reference

### External Tools
- [Qdrant Docs](https://qdrant.tech/documentation/)
- [Ollama Docs](https://ollama.com/docs)
- [sentence-transformers](https://www.sbert.net/)
- [FastAPI Tutorial](https://fastapi.tiangolo.com/tutorial/)

### Tutorials
- "RAG with Local LLMs" - YouTube
- "sentence-transformers Tutorial" - Hugging Face
- "Qdrant Quickstart" - Official docs

---

## 🎯 Next Steps

1. **Read:** PROJECT_STATUS.md (understand architecture)
2. **Setup:** Install Qdrant, Ollama, Python deps
3. **Code:** Follow AI_SERVICE_IMPLEMENTATION_GUIDE.md
4. **Test:** Upload PDF, ask questions
5. **Verify:** Check privacy (no external API calls)

---

**You're building a privacy-first RAG system with 100% local AI processing. No external APIs, complete data sovereignty!** 🔒🚀

_Last updated: February 21, 2026_
