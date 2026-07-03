# 🌿 CanopyAI — AI-Powered Document Intelligence Platform

> Ask anything. Know everything. Chat with your documents using RAG-powered AI.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.3-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Gemini_2.5_Flash-API-4285F4?style=for-the-badge&logo=google&logoColor=white)
![Apache PDFBox](https://img.shields.io/badge/PDFBox-3.0.1-CC2929?style=for-the-badge&logo=apache&logoColor=white)
![Apache POI](https://img.shields.io/badge/Apache_POI-5.2.5-CC2929?style=for-the-badge&logo=apache&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-0.12.3-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Railway](https://img.shields.io/badge/Railway-Deploy-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)

---

## 📖 Overview

**CanopyAI** is a full-stack Retrieval-Augmented Generation (RAG) platform that lets users upload PDF, DOCX, or TXT documents and have intelligent, cited conversations with them. Built on Spring Boot 3.3 and powered by Google Gemini 2.5 Flash, it chunks documents into semantically meaningful pieces, generates vector embeddings via `gemini-embedding-001`, and performs cosine similarity search using a pure PostgreSQL LATERAL join to retrieve the most relevant context before answering. Every response is grounded in the actual document content with page-level source citations — no hallucinations, no guesswork.

---

## ✨ Features

- **Multi-format document upload** — PDF (Apache PDFBox 3.x), DOCX (Apache POI 5.x), and TXT files up to 20MB
- **Smart text chunking** — 500-token chunks with 50-token overlap for precise, context-aware retrieval
- **Vector embeddings** — `gemini-embedding-001` generates 768-dimension float vectors stored as native PostgreSQL float arrays
- **Cosine similarity search** — pure SQL LATERAL join computes similarity without needing PGVector extension
- **Grounded AI answers** — Gemini 2.5 Flash answers questions using only retrieved document chunks as context
- **Page-level source citations** — every answer shows which page and chunk the information came from
- **Auto document summary** — 5-point AI summary generated instantly on every upload, shown in the sidebar
- **Suggested questions** — 3 AI-generated smart questions per document to help users get started
- **Full chat history** — all Q&A pairs saved per document per user and restored on page reload
- **JWT authentication** — stateless HttpOnly cookie-based JWT with BCrypt password hashing at strength 12
- **User data isolation** — every document, chunk, and chat is scoped strictly to the authenticated user
- **Document management** — upload, view metadata, open chat, and delete documents from the dashboard
- **Usage stats dashboard** — total documents uploaded, total questions asked, and total AI answers per user
- **Drag and drop upload** — drag files directly onto the upload zone with live file preview
- **Forest Sage UI theme** — custom green color palette with Layered Tree SVG logo across all pages
- **Auto-dismiss alerts** — flash messages for success and error states auto-dismiss after 4 seconds

---

## 🏗️ Architecture

```
╔══════════════════════════════════════════════════════════════════╗
║                      UPLOAD PIPELINE                            ║
╚══════════════════════════════════════════════════════════════════╝

  ┌──────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────────────┐    ┌────────────┐
  │ PDF/DOCX │───►│ PDFBox/POI  │───►│ Text Chunker │───►│ GeminiService    │───►│ PostgreSQL │
  │   /TXT   │    │ Text Extract│    │ 500 tok/50   │    │ gemini-embedding │    │  float[]   │
  │  Upload  │    │             │    │ overlap       │    │ -001 (768-dim)   │    │  column    │
  └──────────┘    └─────────────┘    └──────────────┘    └──────────────────┘    └────────────┘
                                                                  │
                                                                  ▼
                                                    ┌──────────────────────┐
                                                    │  Gemini 2.5 Flash    │
                                                    │  generateSummary()   │
                                                    │  5-point AI summary  │
                                                    └──────────────────────┘

╔══════════════════════════════════════════════════════════════════╗
║                       QUERY PIPELINE                            ║
╚══════════════════════════════════════════════════════════════════╝

  ┌──────────┐    ┌──────────────────┐    ┌──────────────────────┐
  │   User   │───►│  GeminiService   │───►│    PostgreSQL        │
  │ Question │    │ generateEmbedding│    │  LATERAL cosine sim  │
  │          │    │ (768-dim vector) │    │  Top-4 chunks        │
  └──────────┘    └──────────────────┘    └──────────┬───────────┘
                                                     │
                                                     ▼
                                         ┌──────────────────────┐    ┌──────────┐
                                         │  Gemini 2.5 Flash    │───►│  Answer  │
                                         │  chat() with context │    │ +Sources │
                                         │  system prompt + RAG │    │ +Pages   │
                                         └──────────────────────┘    └──────────┘

╔══════════════════════════════════════════════════════════════════╗
║                      SECURITY LAYER                             ║
╚══════════════════════════════════════════════════════════════════╝

  HTTP Request
       │
       ▼
  JwtAuthFilter ──► Extract JWT from HttpOnly Cookie "canopy_jwt"
       │
       ▼
  JwtUtil.validateToken() ──► HMAC-SHA256 verify + expiry check
       │
       ▼
  CustomUserDetailsService ──► UserRepository ──► PostgreSQL users table
       │
       ▼
  SecurityContextHolder.setAuthentication()
       │
       ▼
  Controller method executes with authenticated user context
```

**Step-by-step request flow:**

1. User visits `http://localhost:8081` → redirected to `/auth/login` by the `authenticationEntryPoint`
2. After login, `AuthController` authenticates via `AuthenticationManager`, generates a JWT via `JwtUtil.generateToken()`, and sets it as an HttpOnly cookie named `canopy_jwt` with 1-hour expiry
3. Every subsequent request passes through `JwtAuthFilter` which extracts the cookie, validates the token, and sets the `SecurityContext`
4. User uploads a file to `POST /upload` → `DocumentController` calls `DocumentService.processDocument()`
5. `DocumentService` validates the file type (PDF/DOCX/TXT), extracts raw text using `Loader.loadPDF()` (PDFBox 3.x) or `XWPFDocument` (Apache POI), then splits into 500-token chunks with 50-token overlap
6. Each chunk is embedded by calling `GeminiService.generateEmbedding()` → `POST /v1beta/models/gemini-embedding-001:embedContent` → 768-dim float array saved to `document_chunks.embedding`
7. `GeminiService.generateSummary()` sends the first 8000 characters to Gemini 2.5 Flash → 5-point summary saved to `documents.summary`
8. User asks a question at `POST /api/ask` → `RagService.askQuestion()` embeds the query and runs a PostgreSQL LATERAL join to find top-4 similar chunks by cosine similarity
9. Retrieved chunks are assembled as context and sent to `GeminiService.chat()` with a system prompt instructing Gemini to answer only from context
10. Answer + source citations (page number + preview) are saved to `chat_messages` table and returned as JSON to the Thymeleaf/JS frontend

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.0 |
| Security | Spring Security 6.3 + JWT (jjwt 0.12.3) — HttpOnly cookie |
| AI Chat | Google Gemini 2.5 Flash (`gemini-2.5-flash`) via REST |
| AI Embeddings | Google Gemini Embedding (`gemini-embedding-001`) — 768-dim |
| HTTP Client | Spring WebFlux `WebClient` (non-blocking) |
| Database | PostgreSQL 15 |
| Vector Search | Native PostgreSQL `float[]` + LATERAL cosine similarity SQL |
| ORM | Spring Data JPA + Hibernate 6.5 |
| PDF Parsing | Apache PDFBox 3.0.1 (`Loader.loadPDF()`) |
| DOCX Parsing | Apache POI 5.2.5 (`XWPFDocument`) |
| Frontend | Thymeleaf 3.1 + Vanilla JS + Google Fonts (Inter) |
| Icons | Tabler Icons CDN |
| Build | Maven 3.9 |
| Dev Tools | Spring Boot DevTools + Lombok |
| Deployment | Railway |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Gemini API key — free at [aistudio.google.com](https://aistudio.google.com) (needs access to `gemini-2.5-flash` and `gemini-embedding-001`)

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/adityakamat2005/canopy.git
cd canopy
```

**2. Create the PostgreSQL database**
```sql
CREATE DATABASE canopy_db;
```
> No extensions needed — cosine similarity uses a pure SQL LATERAL join on native `float[]` columns. Hibernate `ddl-auto=update` creates all tables on first run.

**3. Set environment variables**

In IntelliJ IDEA: Run → Edit Configurations → Environment Variables → add:

| Variable | Value |
|---|---|
| `GEMINI_API_KEY` | Your Gemini API key from AI Studio |
| `DB_PASSWORD` | Your PostgreSQL password |
| `JWT_SECRET` | Any long random string (min 32 chars) |

Your `src/main/resources/application.properties` is already configured to read these:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/canopy_db
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

gemini.api.key=${GEMINI_API_KEY}
gemini.chat.url=https://generativelanguage.googleapis.com/v1beta
gemini.embedding.url=https://generativelanguage.googleapis.com/v1beta
gemini.model=gemini-2.5-flash
gemini.embedding.model=gemini-embedding-001

jwt.secret=${JWT_SECRET}
jwt.expiration=3600000

spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB
server.port=8081
```

**4. Run the application**
```bash
mvn spring-boot:run
```

**5. Open in browser**
```
http://localhost:8081
```

Register an account → upload a PDF → start chatting with your document.

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Redirect to `/dashboard` |
| `GET` | `/auth/login` | Render login page |
| `POST` | `/auth/login` | Authenticate user, issue JWT as HttpOnly cookie |
| `GET` | `/auth/register` | Render registration page |
| `POST` | `/auth/register` | Create new user, redirect to login |
| `GET` | `/auth/logout` | Clear `canopy_jwt` cookie, redirect to login |
| `GET` | `/dashboard` | User dashboard — documents list, stats (totalDocs, totalQuestions, totalDocQuestions) |
| `GET` | `/upload` | Render file upload page |
| `POST` | `/upload` | Upload file → extract text → chunk → embed → summarize → redirect to `/chat/{id}` |
| `GET` | `/chat/{docId}` | Chat interface with document sidebar (summary, suggestions, history) |
| `POST` | `/api/ask` | Submit question (params: `documentId`, `question`) → returns `{answer, sources}` JSON |
| `POST` | `/delete/{docId}` | Delete document + all chunks + all chat messages for authenticated user |

---

## 🗂️ Project Structure

```
CanopyAI/
├── src/
│   ├── main/
│   │   ├── java/com/canopy/
│   │   │   ├── CanopyApplication.java
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java          # JWT filter chain, BCrypt, AuthProvider
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java          # /auth/** routes
│   │   │   │   ├── DashboardController.java     # /dashboard
│   │   │   │   └── DocumentController.java      # /upload, /chat, /api/ask, /delete
│   │   │   ├── model/
│   │   │   │   ├── User.java                    # id, name, email, password, totalQuestions
│   │   │   │   ├── Document.java                # id, filename, fileType, fileSize, summary, chunkCount
│   │   │   │   ├── DocumentChunk.java           # id, content, chunkIndex, pageNumber, embedding float[]
│   │   │   │   └── ChatMessage.java             # id, question, answer, sources, createdAt
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java          # findByEmail, existsByEmail
│   │   │   │   ├── DocumentRepository.java      # findByUserOrderByUploadedAtDesc, sumQuestionCount
│   │   │   │   ├── DocumentChunkRepository.java # findTopKByCosineSimilarity (LATERAL join)
│   │   │   │   └── ChatMessageRepository.java   # findByDocumentAndUserOrderByCreatedAtAsc
│   │   │   ├── security/
│   │   │   │   ├── JwtUtil.java                 # generateToken, extractEmail, validateToken
│   │   │   │   └── JwtAuthFilter.java           # OncePerRequestFilter, cookie extraction
│   │   │   └── service/
│   │   │       ├── AuthService.java             # register, login, getUserByEmail
│   │   │       ├── CustomUserDetailsService.java # UserDetailsService impl (breaks circular dep)
│   │   │       ├── DocumentService.java         # processDocument, chunkText, extractFromPdf/Docx
│   │   │       ├── GeminiService.java           # generateEmbedding, chat, generateSummary, generateSuggestedQuestions
│   │   │       └── RagService.java              # askQuestion (embed → search → prompt → answer)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── templates/
│   │       │   ├── auth/
│   │       │   │   ├── login.html
│   │       │   │   └── register.html
│   │       │   ├── dashboard/
│   │       │   │   ├── index.html               # stats cards + document grid
│   │       │   │   └── upload.html              # drag-drop upload zone
│   │       │   └── chat/
│   │       │       └── index.html               # sidebar + chat interface
│   │       └── static/
│   │           ├── css/
│   │           │   └── style.css                # Forest Sage theme, full component library
│   │           └── js/
│   │               └── app.js                   # chat engine, typing indicator, upload preview
│   └── test/
│       └── java/com/canopy/
│           └── CanopyApplicationTests.java
├── pom.xml
└── README.md
```

---

## 📌 Roadmap

- **PGVector integration** — replace the LATERAL float[] cosine similarity with native PGVector `<=>` operator for better performance at scale
- **Multi-document chat** — allow users to ask questions across multiple documents simultaneously with merged context retrieval
- **Export chat as PDF** — download the full Q&A conversation with source citations as a formatted PDF report using Apache PDFBox
- **Document collections** — group related documents into named collections (e.g. "Project Reports", "Legal Docs") and search within a collection

---

## 👤 Author

**Aditya Kamat**
Information Science & Engineering, Canara Engineering College (VTU), Mangalore
Graduating 2027 — targeting Java Backend & Full Stack roles

- GitHub: [@adityakamat2005](https://github.com/adityakamat2005)
- Portfolio: [adityakamat2005.github.io](https://adityakamat2005.github.io)
- LinkedIn: [linkedin.com/in/adityakamat2005](https://linkedin.com/in/adityakamat2005)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">
  Built with 🌿 by Aditya Kamat
</p>
