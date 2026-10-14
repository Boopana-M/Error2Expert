# 📡 CodeMemory API Reference Documentation

This document describes all REST API endpoints for the **Spring Boot Backend** (`http://localhost:8080`) and the **Python ML Microservice** (`http://localhost:8000`).

---

## 1. Spring Boot Backend APIs (`http://localhost:8080`)

### 1.1 Submissions
- `POST /api/submissions`
  - **Description**: Records a new coding attempt, triggers ML analysis, registers mistakes, updates longitudinal weakness models, and evaluates the recommendation feedback loop.
  - **Request Body**:
    ```json
    {
      "userId": 1,
      "problemTitle": "Search in Rotated Sorted Array",
      "difficulty": "MEDIUM",
      "topics": ["Binary Search", "Array"],
      "code": "int left = 0, right = nums.length - 1; ...",
      "language": "java",
      "result": "WRONG_ANSWER",
      "userExplanation": "I was confused whether high should be mid or mid - 1."
    }
    ```
  - **Response (200 OK)**:
    ```json
    {
      "id": 19,
      "problemTitle": "Search in Rotated Sorted Array",
      "result": "WRONG_ANSWER",
      "mistake": {
        "id": 13,
        "category": "BOUNDARY_ERROR",
        "userExplanation": "I was confused whether high should be mid or mid - 1.",
        "aiExplanation": "Incorrect binary-search boundary update (mid vs mid ± 1).",
        "confidence": 0.94
      }
    }
    ```
- `GET /api/submissions` &mdash; Returns list of all user submissions.
- `GET /api/submissions/{id}` &mdash; Returns submission details by ID.

### 1.2 Dashboard & Analytics
- `GET /api/dashboard/summary?userId=1`
  - **Description**: Returns longitudinal metrics for dashboard overview cards.
  - **Response**:
    ```json
    {
      "problemsAttempted": 19,
      "problemsAccepted": 12,
      "totalMistakes": 13,
      "recurringMistakes": 6,
      "strongestConcept": "Dynamic Programming",
      "weakestConcept": "Boundary Management",
      "currentMastery": 74
    }
    ```
- `GET /api/dashboard/trends?userId=1` &mdash; Returns month-over-month mistake trajectory.
- `GET /api/dashboard/profile?userId=1` &mdash; Returns semantic mistake category breakdown with percentage distributions.

### 1.3 Weaknesses
- `GET /api/weaknesses?userId=1`
  - **Description**: Returns ranked longitudinal weaknesses with severity tags (`HIGH`, `MEDIUM`, `IMPROVING`), confidence scores, and affected problem lists.
- `GET /api/weaknesses/{id}` &mdash; Returns weakness details by ID.

### 1.4 Recommendations
- `GET /api/recommendations?userId=1` &mdash; Returns all pending and completed targeted practice problems.
- `GET /api/recommendations/today?userId=1` &mdash; Returns "Today's Focus" curriculum for the top recurring weakness.
- `PUT /api/recommendations/{id}/status?status=SOLVED` &mdash; Updates recommendation progress.

### 1.5 Mistakes
- `GET /api/mistakes?userId=1` &mdash; Returns history of all recorded mistakes.
- `GET /api/mistakes/categories` &mdash; Lists all 17 supported domain mistake categories.

---

## 2. Python ML Microservice APIs (`http://localhost:8000`)

### 2.1 Attempt Analysis
- `POST /analyze-attempt`
  - Evaluates submission code and explanation using rule-based heuristics and vector matching.
  - **Response**:
    ```json
    {
      "mistakeCategory": "BOUNDARY_ERROR",
      "confidence": 0.94,
      "rootCause": "Incorrect binary-search boundary update (mid vs mid ± 1).",
      "similarMistakes": 4,
      "recurring": true
    }
    ```

### 2.2 Vector Memory & Pattern Detection
- `POST /generate-embedding`
  - Encodes explanation text into a dense 384-dimensional unit vector using `all-MiniLM-L6-v2`.
- `POST /find-similar-mistakes`
  - Queries FAISS vector memory for top-K nearest historical mistakes.
- `POST /detect-pattern`
  - Compares the active mistake embedding against past mistakes to flag recurring cross-problem blind spots.
