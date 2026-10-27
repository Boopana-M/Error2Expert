# 🧠 Error2Expert

> **An AI-powered personal coding memory system that learns from your mistakes, identifies recurring weaknesses across disparate problems, and delivers targeted practice curricula.**

[![CI Pipeline](https://img.shields.io/badge/CI-Passing-10b981?style=for-the-badge&logo=githubactions&logoColor=white)](#-automated-ci--testing)
[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)](#-tech-stack)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring_Boot-3.4.3-6db33f?style=for-the-badge&logo=springboot&logoColor=white)](#-tech-stack)
[![Python 3.10](https://img.shields.io/badge/Python-3.10-3776ab?style=for-the-badge&logo=python&logoColor=white)](#-tech-stack)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110-009688?style=for-the-badge&logo=fastapi&logoColor=white)](#-tech-stack)
[![React 19](https://img.shields.io/badge/React-19.2-61dafb?style=for-the-badge&logo=react&logoColor=black)](#-tech-stack)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql&logoColor=white)](#-tech-stack)
[![FAISS](https://img.shields.io/badge/Vector_Memory-FAISS-purple?style=for-the-badge)](#-tech-stack)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ed?style=for-the-badge&logo=docker&logoColor=white)](#-production-deployment)
[![Theme: Dark Only](https://img.shields.io/badge/UI-Dark_Mode_Only-0f172a?style=for-the-badge)](#-frontend-analytics-dashboard)

---

## 🎯 The Core Philosophy: "Remember Why You Failed"

Current coding platforms track:
- Problems solved count
- Accepted submissions
- Topic tags and difficulty levels

**None of them build a long-term memory of why you failed.**

A developer often makes the exact same algorithmic mistake across completely different problem titles without realizing it:

```text
Problem 1: Search in Rotated Sorted Array  ──►  Boundary lock (high = mid instead of high = mid - 1)
Problem 2: Find Minimum in Rotated Array   ──►  Boundary convergence error
Problem 3: Search Insert Position          ──►  Boundary index return overshoot
Problem 4: Binary Search                   ──►  Loop invariant termination (<= vs <)
```

Traditional platforms say: *"You attempted 4 Binary Search problems."*  
**Error2Expert says:**
> *"You made 4 boundary-related mistakes across 4 different problems. Your actual weakness is boundary update logic (mid ± 1), not binary search itself."*

---

## 🏗️ Production Architecture & Data Flow

```mermaid
graph TD
    subgraph Client ["Client Layer"]
        LC[("LeetCode.com")] -->|DOM Observer / Monaco Extractor| EXT["Chrome Extension (MV3)<br/>Dark Mode Popup + Settings"]
        DASH["React 19 Analytics Dashboard<br/>Vite + Dark Mode + Mastery Gauge"]
    end

    subgraph Backend ["Backend Tier (Port 8080)"]
        EXT -->|POST /api/submissions| SB["Spring Boot 3.4 REST API<br/>• JWT Security & CORS<br/>• Auto DataInitializer<br/>• Adaptive Mastery Engine"]
        DASH <-->|REST Analytics APIs| SB
    end

    subgraph ML ["AI / ML Tier (Port 8000)"]
        SB <-->|HTTP JSON /analyze-attempt| FAST["Python FastAPI Service<br/>• 17-Class Rule Heuristics<br/>• TF-IDF Mistake Classifier<br/>• Resilient Embedder Fallback"]
        FAST <-->|Unit L2 Normalized 384-d Cosine Search| FAISS[("FAISS Vector Memory<br/>IndexFlatIP Dense Store")]
    end

    subgraph Storage ["Persistent Database Tier"]
        SB <-->|HikariCP / JPA Hibernate| PG[("PostgreSQL 16 Relational DB<br/>• Users & Problems<br/>• Submissions & Mistakes<br/>• Weaknesses & Recommendations")]
    end

    classDef primary fill:#1e1b4b,stroke:#6366f1,stroke-width:2px,color:#fff;
    classDef secondary fill:#0f172a,stroke:#06b6d4,stroke-width:1.5px,color:#fff;
    classDef accent fill:#14532d,stroke:#22c55e,stroke-width:1.5px,color:#fff;
    class EXT,DASH secondary;
    class SB primary;
    class FAST,FAISS accent;
    class PG primary;
```

### Complete End-to-End Operational Lifecycle

1. **Capture**: The student attempts a LeetCode problem. Upon verdict (`WRONG_ANSWER`, `TLE`, etc.), the Chrome content script captures DOM metadata, problem title, tags, and source code.
2. **Diagnosis**: The user clicks the extension icon, selects a failure category (e.g. *Boundary / index issue*), and enters a brief note (*"Used high = mid causing infinite loop"*).
3. **Dispatch**: The extension sends the submission payload to the Spring Boot backend (`POST /api/submissions`).
4. **AI Second Brain Inference**: Spring Boot calls the Python ML microservice:
   - Rule heuristics evaluate 17 mistake categories.
   - User explanation is vectorized into a **384-dimensional dense semantic vector** via `all-MiniLM-L6-v2`.
   - FAISS `IndexFlatIP` performs cosine search (threshold $\ge 0.70$) against historical mistake memories.
   - Detects if the failure matches previous errors across disparate problem titles.
5. **Longitudinal Weakness Modeling**: Spring Boot records the mistake in PostgreSQL, increments the weakness frequency, and lowers the concept mastery score.
6. **Adaptive Curricula**: The recommendation engine generates targeted practice problems calibrated to eliminate the detected weakness.
7. **Longitudinal Mastery Recovery**: When the student later solves the recommended problem (`ACCEPTED`), the system marks the recommendation as `SOLVED` and awards an **+8% mastery recovery boost**.
8. **Real-Time Analytics**: The React Dashboard updates with real-time mastery gauges, mistake profiles, longitudinal trends, and "Today's Focus".

---

## 🛠️ Tech Stack & Microservice Roles

| Microservice | Technology | Port | Key Responsibilities |
|---|---|---|---|
| **Frontend** | React 19, Vite, Lucide Icons, Vanilla CSS | `3000` (Docker) / `5173` (Dev) | Dark-mode visual analytics, mastery gauge, mistake timeline, attempt simulator |
| **Backend** | Spring Boot 3.4, Java 21, Spring Data JPA, Security | `8080` | REST API gateway, JWT auth, universal database config, adaptive recommendation loop |
| **ML Service** | Python 3.10, FastAPI, SentenceTransformers, FAISS | `8000` | Semantic vector embeddings (384-d), cosine similarity search, AST analysis, code diff |
| **Database** | PostgreSQL 16 (or H2 in-memory dev mode) | `5432` | Relational storage for users, problems, submissions, mistakes, weaknesses |
| **Extension** | JavaScript (ES6+), Chrome Extensions API (MV3) | Browser | LeetCode DOM parser, verdict observer, floating alerts, configurable settings |

---

## 🚀 Production Deployment Options

### Option 1: 1-Click Render Cloud Deployment (`render.yaml`)

This repository includes a production **Render Blueprint** (`render.yaml`) that deploys the entire 4-tier stack in one click:
- PostgreSQL database (`error2expert-db`)
- Python ML microservice (`error2expert-ml-service`)
- Spring Boot backend (`error2expert-backend`)
- React static dashboard (`error2expert-frontend`)

1. Fork or push this repository to your GitHub account.
2. Log into [Render.com](https://render.com) and click **New +** ➔ **Blueprint**.
3. Select your repository. Render automatically reads `render.yaml` and provisions all 4 services with connected environment variables!

---

### Option 2: Docker Compose Production Stack

Run the entire containerized architecture locally or on any cloud VPS (Ubuntu, Debian, AWS EC2, DigitalOcean):

```bash
# 1. Clone repository
git clone https://github.com/Boopana-M/Error2Expert.git
cd Error2Expert

# 2. Copy production environment variables
cp .env.example .env

# 3. Build and launch all 4 microservices
docker compose up --build -d
```

Verify service health:
```bash
docker compose ps
```

| Service | Local URL | Healthcheck |
|---|---|---|
| **Frontend Dashboard** | `http://localhost:3000` | Nginx HTTP 200 |
| **Spring Boot Backend** | `http://localhost:8080/api` | `http://localhost:8080/actuator/health` |
| **Python ML Service** | `http://localhost:8000` | `http://localhost:8000/health` |
| **PostgreSQL Database** | `localhost:5432` | `pg_isready -U postgres -d codememory` |

---

### Option 3: Managed Cloud Database (Neon / Supabase) + Vercel Frontend

Error2Expert includes a **Universal Database Connector** (`DatabaseConfig.java`) that automatically converts cloud `DATABASE_URL` strings (`postgres://...` or `postgresql://...` with SSL) into JDBC format:

1. **Database**: Create a free PostgreSQL instance on [Neon.tech](https://neon.tech) or [Supabase.com](https://supabase.com).
2. **Backend**: Deploy the backend container to Render, Railway, or Fly.io and set:
   ```env
   DATABASE_URL=postgresql://user:password@ep-host.neon.tech/codememory?sslmode=require
   ML_SERVICE_URL=https://your-ml-service.onrender.com
   ```
   *Spring Boot will automatically bootstrap the problem catalog, demo user, and seed mistake patterns on first launch!*
3. **Frontend**: Deploy `frontend/` to [Vercel](https://vercel.com) using the included `frontend/vercel.json`:
   ```env
   VITE_API_URL=https://your-backend.onrender.com/api
   ```

---

### Option 4: Local Development Quickstart (Without Docker)

You can run each service independently for fast local iteration:

#### Step 1: Start Python ML Microservice
```bash
cd ml-service
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

#### Step 2: Start Spring Boot Backend
```bash
cd backend
# Uses PostgreSQL or falls back to in-memory H2 if Postgres is not running:
./mvnw spring-boot:run
```

#### Step 3: Start React Dashboard
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## 🧩 Chrome Extension Installation & Setup

The **Error2Expert Chrome Extension** (Manifest V3) allows you to capture LeetCode submissions directly from the browser.

### 1. Load Unpacked Extension into Chrome
1. Open Google Chrome and navigate to `chrome://extensions/`.
2. Enable **Developer mode** (toggle in the top-right corner).
3. Click **Load unpacked** in the top-left corner.
4. Select the `d:\Error2Expert\extension` (or `/path/to/Error2Expert/extension`) folder.
5. The extension icon 🧠 **Error2Expert** will appear in your Chrome toolbar. Pin it for easy access!

### 2. Configure Backend & Dashboard URLs
1. Click the **Error2Expert** extension icon.
2. Click the **⚙️ (Settings gear)** in the popup header.
3. Set your target URLs:
   - **Backend URL**: `http://localhost:8080/api` (or your deployed cloud backend URL: `https://your-backend.onrender.com/api`)
   - **Dashboard URL**: `http://localhost:5173` (or your deployed dashboard URL: `https://your-frontend.vercel.app`)
4. Click **Save & Test Connection**. The indicator will immediately turn green (**API Connected ✓**).

### 3. Test on LeetCode
1. Open any LeetCode problem, e.g.:
   `https://leetcode.com/problems/search-in-rotated-sorted-array/`
2. Submit code. The extension content script detects the verdict in real time and displays a subtle dark-mode notification banner.
3. Open the popup: problem title, difficulty, and verdict are pre-populated!
4. Type your brief reflection (*"I wasn't sure whether high should be mid or mid - 1"*).
5. Click **⚡ Analyze & Remember Mistake**.
6. The AI Second Brain will diagnose the root cause, identify the recurring pattern, update your mastery score, and link you directly to the full analytics dashboard!

---

## 🧪 Automated CI & Testing

Error2Expert includes automated end-to-end tests across all microservices:

### 1. Full E2E Pipeline Walkthrough
```bash
python scripts/verify_e2e_pipeline.py
```
*Validates the full cycle: submission ➔ rule classification ➔ 384-d vector embedding ➔ FAISS similarity retrieval ➔ cross-problem weakness recurrence ➔ adaptive recommendation ➔ +8% mastery recovery.*

### 2. Python ML Test Suite
```bash
cd ml-service
python -m pytest tests/
```
*15 unit & integration tests covering heuristic classifiers, TF-IDF model consensus, FAISS vector indexing, AST complexity risk, and code diff analysis.*

### 3. Spring Boot Backend Compilation
```bash
cd backend
./mvnw clean test-compile
```
*Validates 64 source classes, security configs, entity relationships, and REST controllers.*

### 4. React Frontend Production Build
```bash
cd frontend
npm run build
```
*Compiles Vite production assets into `dist/` in under 3 seconds.*

---

## 📊 REST API Reference

### Backend Endpoints (`http://localhost:8080/api`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/submissions` | Record LeetCode attempt, trigger ML diagnosis, update weaknesses |
| `GET` | `/submissions` | Retrieve all historical submissions |
| `GET` | `/dashboard/summary` | Longitudinal metrics (attempted, accepted, mastery %, weakest concept) |
| `GET` | `/dashboard/profile` | Categorical mistake breakdown with percentage distribution |
| `GET` | `/dashboard/trends` | 3-month mistake evolution and reduction trajectory |
| `GET` | `/weaknesses` | Prioritized list of recurring weaknesses across problems |
| `GET` | `/recommendations/today` | Adaptive practice curriculum targeting top recurring blind spot |
| `POST` | `/recommendations/{id}/solve` | Mark practice problem solved and award +8% mastery boost |
| `POST` | `/auth/login` | Authenticate user and receive JWT Bearer token |
| `POST` | `/auth/register` | Register new user account |

### ML Microservice Endpoints (`http://localhost:8000`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Service health, total indexed mistakes, vector dimension (384) |
| `POST` | `/analyze-attempt` | End-to-end attempt analysis (classification + embedding + FAISS search) |
| `POST` | `/classify-mistake` | Hybrid heuristic + TF-IDF root cause categorization |
| `POST` | `/generate-embedding` | 384-dimensional unit-normalized dense semantic vector |
| `POST` | `/find-similar-mistakes` | FAISS cosine similarity search against mistake memory |
| `POST` | `/detect-pattern` | Cross-problem recurrence and blind spot detection |
| `POST` | `/generate-recommendation` | Calibrated practice problem recommendation |
| `POST` | `/analyze-diff` | Wrong vs accepted code diff pattern analysis |
| `POST` | `/analyze-ast` | AST complexity, loop nesting depth, and guard condition check |

---

## 🔒 Security & Production Hardening

- **Stateless JWT Authentication**: Passwords hashed with BCrypt (10 rounds); HMAC-SHA256 tokens with configurable expiration.
- **Strict Dark-Mode Design**: Fully tailored HSL color tokens (`#080c14`, `#0f1523`, `#121a2a`), glassmorphic panels, and zero light-mode flashes.
- **CORS Management**: Configured for local extensions (`chrome-extension://`), frontend domains, and wildcard pattern validation.
- **Fail-Safe Fallbacks**: If the ML service is under high load or network partition, Spring Boot automatically degrades to internal heuristic classification so attempts are never lost.
- **Zero-Crash Embeddings**: The Python ML service includes a deterministic semantic projection fallback if PyTorch encounters low-RAM container limits.

---

## 📄 License & Author

Developed by **Boopana M**.  
Built for software engineers, competitive programmers, and technical interview candidates who want to systematically eliminate their blind spots.
