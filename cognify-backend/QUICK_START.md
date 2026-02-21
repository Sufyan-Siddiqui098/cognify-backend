# 🚀 QUICK START GUIDE

**Your Cognify RAG System is READY!**

---

## ⚡ START IN 3 STEPS

### **Step 1: Restart Application** (2 minutes)

```powershell
cd "C:\Users\Sufyan\OneDrive\Documents\University Material\8th Semester (Fall)\FYP\cognify-backend\cognify-backend"
.\mvnw.cmd spring-boot:run
```

**Wait for:**
```
Started CognifyBackendApplication in X seconds
Tomcat started on port 8082
```

---

### **Step 2: Test API** (5 minutes)

#### A. Sign Up
```bash
curl -X POST http://localhost:8082/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!","name":"Test User"}'
```

**Copy the `accessToken` from response!**

#### B. Upload Document
```bash
curl -X POST http://localhost:8082/api/documents/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@C:\path\to\document.txt"
```

#### C. Create Chat
```bash
curl -X POST http://localhost:8082/api/conversations \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"My First Chat"}'
```

**Copy the `id` from response!**

#### D. Send Message (RAG!)
```bash
curl -X POST http://localhost:8082/api/conversations/YOUR_CONVERSATION_ID/messages \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"What is this document about?"}'
```

**You'll get:**
- AI response (placeholder mode)
- Source citations from your document
- Chunk IDs and page numbers

---

### **Step 3: Explore** (10 minutes)

#### List Your Conversations
```bash
curl http://localhost:8082/api/conversations \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### Get Message History
```bash
curl http://localhost:8082/api/conversations/YOUR_CONVERSATION_ID/messages \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### List Your Documents
```bash
curl http://localhost:8082/api/documents \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## ✅ YOU'RE DONE!

**What Works Now:**
- ✅ Full authentication
- ✅ Document upload
- ✅ Chat conversations
- ✅ Message history
- ✅ RAG with placeholder AI
- ✅ Source citations

**To Add Real AI:**
1. See `IMPLEMENTATION_COMPLETE.md`
2. Add OpenAI API key
3. Uncomment TODO sections
4. Enjoy real AI responses!

---

## 📚 Documentation

- **Full Guide:** `IMPLEMENTATION_COMPLETE.md`
- **API Docs:** `API_DOCUMENTATION.md`
- **Complete Summary:** `PROJECT_COMPLETE_SUMMARY.md`

---

**Status:** ✅ READY TO USE  
**Time to Start:** 2 minutes  
**Test Time:** 15 minutes  

🎉 **Enjoy your RAG system!**
