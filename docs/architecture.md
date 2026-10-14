# 🏗️ CodeMemory Architecture & System Design

**CodeMemory** is an AI-powered personal coding memory system that learns from a student's mistakes, identifies recurring weaknesses across disparate LeetCode attempts, and provides adaptive practice recommendations.

---

## 1. High-Level System Architecture

```text
                                 ┌─────────────────────────┐
                                 │   LeetCode Platform     │
                                 │  Problem + Submissions  │
                                 └────────────┬────────────┘
                                              │
                                              ▼
                                 ┌─────────────────────────┐
                                 │ Chrome Extension (MV3)  │
                                 │                         │
                                 │ • DOM Metadata Parser   │
                                 │ • Monaco Code Extractor │
                                 │ • Verdict Observer      │
                                 │ • User Feedback Popup   │
                                 └────────────┬────────────┘
                                              │ HTTP JSON
                                              ▼
                                 ┌─────────────────────────┐
                                 │   Spring Boot Backend   │
                                 │       (Java 21)         │
                                 │                         │
                                 │ • REST APIs             │
                                 │ • Weakness Modeling     │
                                 │ • Adaptive Feedback     │
                                 │ • Longitudinal Mastery  │
                                 └────────────┬────────────┘
                                              │
                   ┌──────────────────────────┴──────────────────────────┐
                   ▼                                                     ▼
        ┌─────────────────────┐                               ┌──────────────────────┐
        │  PostgreSQL 15+     │                               │   Python ML Service  │
        │                     │                               │       (FastAPI)      │
        │ • Users             │                               │                      │
        │ • Problems          │                               │ • Mistake Classifier │
        │ • Submissions       │                               │ • Sentence Xformers  │
        │ • Mistakes          │                               │ • FAISS Vector Store │
        │ • Weaknesses        │                               │ • Pattern Detector   │
        │ • Recommendations   │                               └──────────┬───────────┘
        └─────────────────────┘                                          │
                                                                         ▼
                                                              ┌──────────────────────┐
                                                              │  FAISS Vector Index  │
                                                              │  Dense 384-d Memory  │
                                                              └──────────────────────┘
                   ▲
                   │ REST Analytics
        ┌──────────┴──────────┐
        │   React Dashboard   │
        │    (Vite + Dark)    │
        │                     │
        │ • Mastery Gauge     │
        │ • Mistake Profile   │
        │ • Weaknesses Stack  │
        │ • Evolution Trend   │
        │ • Today's Focus     │
        └─────────────────────┘
```

---

## 2. Core Operational Workflow

1. **Capture**: When a student submits a problem on LeetCode, the Chrome extension captures:
   - Problem metadata (title, difficulty, topics, URL).
   - Source code from the editor DOM.
   - Result verdict (`WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `ACCEPTED`).
   - Human explanation from the dark-mode popup: *"Why do you think this failed?"*.
2. **Analysis**: Spring Boot dispatches the attempt to the Python ML microservice:
   - Evaluates rule heuristics across 17 fine-grained mistake categories.
   - Converts the student's explanation into a 384-dimensional dense semantic vector via `all-MiniLM-L6-v2`.
   - Normalizes vector to unit L2 length and searches the FAISS `IndexFlatIP` index for cosine similarity >= 0.70.
   - Identifies if this mistake matches past failures across distinct problem titles.
3. **Longitudinal Weakness Modeling**:
   - Updates the user's `Weakness` record in PostgreSQL.
   - If a failure repeats (`frequency >= 2`), decays concept mastery score and triggers adaptive problem recommendations.
4. **Adaptive Recommendation & Mastery Recovery**:
   - The recommendation engine selects targeted problems reinforcing boundary invariants or state tracking.
   - When the student subsequently solves the recommended problem with `ACCEPTED`, the feedback loop marks the recommendation as `SOLVED` and awards a **+8% mastery boost**.
5. **Insights on React Dashboard**:
   - Real-time dark-mode visual analytics showing category distributions, -71% error reduction trends, and "Today's Focus" curriculum.
