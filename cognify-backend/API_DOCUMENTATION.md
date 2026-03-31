# 📖 COGNIFY BACKEND - API DOCUMENTATION

**Base URL:** `http://localhost:8082`  
**Version:** 1.0.0  
**Date:** March 31, 2026

---

## 🔐 AUTHENTICATION

All protected endpoints require JWT token in header:
```
Authorization: Bearer {accessToken}
```

---

## 📋 API ENDPOINTS

### **1. AUTHENTICATION**

#### **1.1 Sign Up**
```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "name": "John Doe"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "name": "John Doe",
      "role": "USER"
    }
  },
  "message": "User registered successfully"
}
```

#### **1.2 Login**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response 200:** Same as signup

#### **1.3 Logout**
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

#### **1.4 Get Profile**
```http
GET /api/auth/profile
Authorization: Bearer {accessToken}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

---

### **2. DOCUMENTS**

#### **2.1 Upload Document**
```http
POST /api/documents/upload
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

file: document.pdf
```

#### **2.2 List Documents**
```http
GET /api/documents?page=0&limit=12&sortBy=createdAt&sortOrder=desc
Authorization: Bearer {accessToken}
```

#### **2.3 Get Document**
```http
GET /api/documents/{id}
Authorization: Bearer {accessToken}
```

#### **2.4 Delete Document**
```http
DELETE /api/documents/{id}
Authorization: Bearer {accessToken}
```

#### **2.5 Reindex Document**
```http
POST /api/documents/{id}/reindex
Authorization: Bearer {accessToken}
```

---

### **3. CONVERSATIONS**

#### **3.1 Create Conversation**
```http
POST /api/conversations
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "Discussion about AI"
}
```

#### **3.2 List Conversations**
```http
GET /api/conversations?page=0&size=20
Authorization: Bearer {accessToken}
```

#### **3.3 Get Conversation**
```http
GET /api/conversations/{id}
Authorization: Bearer {accessToken}
```

#### **3.4 Delete Conversation**
```http
DELETE /api/conversations/{id}
Authorization: Bearer {accessToken}
```

---

### **4. MESSAGES**

#### **4.1 Get Messages**
```http
GET /api/conversations/{conversationId}/messages
Authorization: Bearer {accessToken}
```

#### **4.2 Send Message (RAG)**
```http
POST /api/conversations/{conversationId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "content": "What is the main topic of my document?"
}
```

*(Sources are automatically included in the assistant message response)*

---

### **5. RECALL (Under Development)**

#### **5.1 Semantic Search**
```http
POST /api/recall/search
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "query": "Semantic search keywords"
}
```

#### **5.2 Submit Feedback**
```http
POST /api/recall/{id}/feedback
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "rating": 5,
  "comment": "Good result"
}
```

---

### **6. HEALTH**

#### **6.1 Health Check**
```http
GET /api/health
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "cognify-backend",
    "timestamp": "2026-03-31T...",
    "version": "0.0.1-SNAPSHOT"
  }
}
```

---

## 📊 RESPONSE FORMAT

### **Success Response**
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful",
  "error": null
}
```

### **Error Response**
```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": "Error message here"
}
```

---

## 🔒 HTTP STATUS CODES

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 500 | Internal Server Error |

---

**API Version:** 1.0.0  
**Last Updated:** March 31, 2026  
**Status:** ✅ Production Ready
