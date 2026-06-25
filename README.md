# 🌿 Canopy — AI-Powered Document Intelligence

> **Ask anything. Know everything.**

Canopy is a full-stack RAG (Retrieval-Augmented Generation) platform built with Spring Boot and Google Gemini API. Upload any PDF, DOCX, or TXT file and have intelligent conversations with your documents — powered by vector embeddings and semantic search.

---

## ✨ Features

- **Multi-format upload** — PDF, DOCX, TXT support via Apache PDFBox & Apache POI
- **Smart chunking** — 500-token overlapping chunks for precise retrieval
- **Gemini embeddings** — text-embedding-004 model (768 dimensions)
- **PGVector similarity search** — cosine similarity retrieval in PostgreSQL
- **Source citations** — every answer shows exactly which page it came from
- **Auto document summary** — 5-point AI summary generated on upload
- **Suggested questions** — AI proposes 3 smart questions per document
- **Chat history** — full conversation saved per document per user
- **JWT authentication** — HttpOnly cookie-based, BCrypt password hashing
- **User data isolation** — each user sees only their own documents

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 21 |
| Security | Spring Security 6, JWT (jjwt 0.12) |
| AI | Google Gemini 1.5 Flash + text-embedding-004 |
| Database | PostgreSQL 15 + PGVector |
| Frontend | Thymeleaf 3, Vanilla JS |
| File parsing | Apache PDFBox 3, Apache POI 5 |
| HTTP client | Spring WebFlux (WebClient) |
| Deployment | Railway |

---

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 15+
- Gemini API key (free at [aistudio.google.com](https://aistudio.google.com))

### Setup

1. **Clone the repo**
```bash
git clone https://github.com/yourusername/canopy.git
cd canopy
```

2. **Create the database**
```sql
CREATE DATABASE canopy_db;
\c canopy_db
CREATE EXTENSION IF NOT EXISTS vector;
```

3. **Configure application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/canopy_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
```

4. **Run the application**
```bash
mvn spring-boot:run
```

5. **Open** [http://localhost:8080](http://localhost:8080)

---

## 🏗 Architecture

```
Upload Pipeline:
PDF/DOCX → Text Extraction → Chunking (500 tokens) → Gemini Embeddings → PGVector Store

Query Pipeline:
User Question → Embed Query → Cosine Similarity Search → Top-4 Chunks → Gemini LLM → Answer + Citations
```

---

## 📁 Project Structure

```
src/main/java/com/canopy/
├── config/          # Security configuration
├── controller/      # Auth, Dashboard, Document, Chat controllers
├── model/           # User, Document, DocumentChunk, ChatMessage entities
├── repository/      # JPA repositories with PGVector queries
├── security/        # JWT filter and utility
└── service/         # AuthService, DocumentService, GeminiService, RagService
```

---

## 👤 Author

**Aditya** — Information Science & Engineering, Canara Engineering College, VTU  
Built as a portfolio project showcasing RAG architecture, Spring Boot, and LLM integration.

---

## 📄 License

MIT License
