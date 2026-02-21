# 📖 COGNIFY BACKEND - API DOCUMENTATION

**Base URL:** `http://localhost:8082`  
**Version:** 1.0.0  
**Date:** February 7, 2026

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

**Response 200:**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

#### **1.4 Refresh Token**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
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

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "document.pdf",
    "type": "PDF",
    "size": 1024000,
    "status": "PROCESSING",
    "uploadStartedAt": "2026-02-07T...",
    "createdAt": "2026-02-07T..."
  },
  "message": "Document uploaded successfully. Processing has started."
}
```

#### **2.2 List Documents**
```http
GET /api/documents?page=0&size=10
Authorization: Bearer {accessToken}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "document.pdf",
        "type": "PDF",
        "size": 1024000,
        "status": "INDEXED",
        "chunksCount": 25,
        "createdAt": "2026-02-07T..."
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
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

---

### **3. CONVERSATIONS** ⭐ NEW

#### **3.1 Create Conversation**
```http
POST /api/conversations
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "Discussion about AI"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "title": "Discussion about AI",
    "messageCount": 0,
    "createdAt": "2026-02-07T...",
    "updatedAt": "2026-02-07T..."
  },
  "message": "Conversation created successfully"
}
```

#### **3.2 List Conversations**
```http
GET /api/conversations?page=0&size=20
Authorization: Bearer {accessToken}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "userId": "uuid",
        "title": "Discussion about AI",
        "messageCount": 5,
        "createdAt": "2026-02-07T...",
        "updatedAt": "2026-02-07T..."
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
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

**Response 200:**
```json
{
  "success": true,
  "message": "Conversation deleted successfully"
}
```

---

### **4. MESSAGES** ⭐ NEW

#### **4.1 Get Messages**
```http
GET /api/conversations/{conversationId}/messages
Authorization: Bearer {accessToken}
```

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "conversationId": "uuid",
      "sender": "USER",
      "content": "What is artificial intelligence?",
      "sources": [],
      "createdAt": "2026-02-07T..."
    },
    {
      "id": "uuid",
      "conversationId": "uuid",
      "sender": "ASSISTANT",
      "content": "Based on your documents, artificial intelligence is...",
      "sources": [
        {
          "chunkId": "uuid",
          "documentId": "uuid",
          "documentName": "ai-basics.pdf",
          "pageNumber": 5,
          "snippet": "Artificial intelligence (AI) is the simulation of human intelligence...",
          "relevanceScore": 0.92
        }
      ],
      "createdAt": "2026-02-07T..."
    }
  ]
}
```

#### **4.2 Send Message (RAG)** 🔥
```http
POST /api/conversations/{conversationId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "content": "What is the main topic of my document?"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "conversationId": "uuid",
    "sender": "ASSISTANT",
    "content": "Based on your documents, the main topic is...",
    "sources": [
      {
        "chunkId": "uuid",
        "documentId": "uuid",
        "documentName": "document.pdf",
        "pageNumber": 1,
        "snippet": "This document discusses the fundamental concepts of...",
        "relevanceScore": 0.95
      }
    ],
    "createdAt": "2026-02-07T..."
  },
  "message": "Message processed successfully"
}
```

**This endpoint:**
1. Saves your message
2. Retrieves relevant document chunks (RAG)
3. Generates AI response (with placeholder or real AI)
4. Saves assistant response with citations
5. Returns response immediately

#### **4.3 Get Message Sources**
```http
GET /api/conversations/messages/{messageId}/sources
Authorization: Bearer {accessToken}
```

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "chunkId": "uuid",
      "documentId": "uuid",
      "documentName": "document.pdf",
      "pageNumber": 5,
      "snippet": "...",
      "relevanceScore": 0.92
    }
  ]
}
```

---

### **5. HEALTH**

#### **5.1 Health Check**
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
    "timestamp": "2026-02-07T...",
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

## 🧪 POSTMAN COLLECTION

### **Example Flow**

```javascript
// 1. Sign Up
POST {{baseUrl}}/api/auth/signup
{
  "email": "test@test.com",
  "password": "Test123!",
  "name": "Test User"
}
// Save accessToken

// 2. Upload Document
POST {{baseUrl}}/api/documents/upload
Headers: Authorization: Bearer {{accessToken}}
Body: form-data, file: @document.txt

// 3. Create Conversation
POST {{baseUrl}}/api/conversations
Headers: Authorization: Bearer {{accessToken}}
{
  "title": "My Chat"
}
// Save conversation id

// 4. Send Message
POST {{baseUrl}}/api/conversations/{{conversationId}}/messages
Headers: Authorization: Bearer {{accessToken}}
{
  "content": "What is this document about?"
}

// 5. Get Messages
GET {{baseUrl}}/api/conversations/{{conversationId}}/messages
Headers: Authorization: Bearer {{accessToken}}
```

---

## 🎯 EXAMPLE USE CASES

### **Use Case 1: Document Q&A**
```
1. User uploads research paper (PDF)
2. System processes and chunks document
3. User creates conversation
4. User asks: "What are the main findings?"
5. System retrieves relevant chunks
6. System generates answer with citations
7. User sees response + source pages
```

### **Use Case 2: Multi-Document Chat**
```
1. User uploads multiple documents
2. User creates conversation
3. User asks: "Compare the methodologies"
4. System searches across all documents
5. System provides answer citing multiple sources
6. User asks follow-up questions
7. System maintains context from previous messages
```

### **Use Case 3: Conversation History**
```
1. User starts conversation yesterday
2. User returns today
3. User views conversation list
4. User opens previous conversation
5. User sees full message history
6. User continues conversation
7. System maintains context
```

---

## 🔧 TESTING TIPS

### **Using cURL**

```bash
# Set token variable
TOKEN="your_access_token_here"

# Test endpoint
curl -X GET http://localhost:8082/api/conversations \
  -H "Authorization: Bearer $TOKEN"
```

### **Using Postman**

1. Create environment with `baseUrl` = `http://localhost:8082`
2. Create variable `accessToken`
3. Set Authorization to Bearer Token: `{{accessToken}}`
4. Make requests

### **Using Browser DevTools**

```javascript
// In browser console
const token = "your_token";
fetch("http://localhost:8082/api/conversations", {
  headers: { "Authorization": `Bearer ${token}` }
})
.then(r => r.json())
.then(console.log);
```

---

## ✅ VALIDATION RULES

### **Email**
- Must be valid email format
- Must be unique

### **Password**
- Minimum 8 characters
- Must contain at least one uppercase letter
- Must contain at least one lowercase letter
- Must contain at least one number

### **Document Upload**
- Max file size: 50MB
- Allowed types: PDF, DOCX, TXT
- File must not be empty

### **Message Content**
- Cannot be blank
- No maximum length

---

**API Version:** 1.0.0  
**Last Updated:** February 7, 2026  
**Status:** ✅ Production Ready
