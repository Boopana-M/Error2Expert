# 🧠 CodeMemory

> **An AI-powered personal coding memory system that learns from your mistakes, identifies recurring weaknesses, and gives targeted practice recommendations.**

CodeMemory is not another coding platform that simply tracks how many problems you solved.

It remembers **why you failed**.

Over time, it builds a personalized profile of your programming mistakes and identifies patterns such as:

- Binary-search boundary mistakes
- Graph visited-state mistakes
- DP base-case mistakes
- Off-by-one errors
- Duplicate handling mistakes
- Time-complexity misjudgments
- Recurring edge-case failures

Instead of saying:

> "You solved 100 problems."

CodeMemory aims to say:

> "You have made 5 boundary-related mistakes across 4 problems. Your weakness is boundary management, not binary search itself."

---

## 🎯 Problem

Current coding platforms mainly track:

- Problems solved
- Accepted submissions
- Difficulty
- Topics
- Submission history

But they do not build a meaningful **long-term memory of a student's mistakes**.

A student may make the same mistake across multiple problems without realizing it.

For example:

```text
Problem 1 → Binary Search → Boundary Error
Problem 2 → Two Pointers → Boundary Error
Problem 3 → Sliding Window → Boundary Error
Problem 4 → Binary Search → Boundary Error
```

The real weakness may not be "Binary Search".

It may be:

> **Boundary Management**

CodeMemory attempts to discover this hidden pattern.

---

## 💡 Solution

CodeMemory collects coding attempts from LeetCode through a Chrome extension.

The data is sent to a Spring Boot backend, where it is stored and analyzed by a Python ML service.

The AI system:

1. Analyzes coding attempts
2. Classifies mistakes
3. Generates semantic embeddings
4. Compares new mistakes with previous mistakes
5. Detects recurring mistake patterns
6. Builds a personalized weakness profile
7. Recommends targeted practice
8. Measures whether the student improves

### Core Loop

```text
Collect
   ↓
Analyze
   ↓
Remember
   ↓
Detect Patterns
   ↓
Recommend
   ↓
Practice
   ↓
Measure Improvement
   ↓
Learn Again
```

---

# 🏗️ System Architecture

```text
                         ┌─────────────────────┐
                         │      LeetCode       │
                         │ Problem + Submission│
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │  Chrome Extension   │
                         │     JavaScript      │
                         │                     │
                         │ • Problem metadata  │
                         │ • Submitted code    │
                         │ • Result            │
                         │ • User explanation  │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    Spring Boot      │
                         │       Backend       │
                         │                     │
                         │ • REST APIs         │
                         │ • Authentication    │
                         │ • Business Logic    │
                         └───────┬───────┬─────┘
                                 │       │
                    ┌────────────┘       └──────────────┐
                    ▼                                   ▼
          ┌─────────────────┐                 ┌──────────────────┐
          │   PostgreSQL    │                 │  Python ML API   │
          │                 │                 │     FastAPI      │
          │ • Users         │                 │                  │
          │ • Submissions   │                 │ • Classification │
          │ • Mistakes      │                 │ • Embeddings     │
          │ • Weaknesses    │                 │ • Similarity     │
          └─────────────────┘                 │ • Pattern detect │
                                              │ • Recommendation │
                                              └────────┬─────────┘
                                                       │
                                                       ▼
                                              ┌──────────────────┐
                                              │  FAISS / Chroma  │
                                              │  Mistake Memory  │
                                              └────────┬─────────┘
                                                       │
                                                       ▼
                         ┌─────────────────────────────────────┐
                         │           React Dashboard           │
                         │                                     │
                         │ • Mistake Profile                   │
                         │ • Weaknesses                         │
                         │ • Trends                             │
                         │ • Mastery                            │
                         │ • Recommendations                    │
                         └─────────────────────────────────────┘
```

---

# 🛠️ Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| 🧠 ML/AI | **Python** | Mistake classification, embeddings, pattern detection |
| 🔌 ML API | **FastAPI** | Expose ML functionality |
| ⚙️ Backend | **Spring Boot + Java** | REST APIs, authentication, business logic |
| 🎨 Frontend | **React** | Dashboard and analytics |
| 🧩 Extension | **JavaScript + Chrome Extension APIs** | Collect LeetCode data |
| 🗄️ Database | **PostgreSQL** | Users, submissions, mistakes, weaknesses |
| 🔎 Vector DB | **FAISS / ChromaDB** | Store and compare mistake embeddings |
| 🔧 Build | **Maven + npm** | Project builds |
| 📦 Version Control | **Git + GitHub** | Source control |

---

# 🔍 How It Works

## 1. Code on LeetCode

The student solves a problem normally.

```text
Problem:
Search in Rotated Sorted Array

Submission:
Wrong Answer
```

---

## 2. Chrome Extension Collects Data

The extension captures relevant submission information.

Example:

```json
{
  "problemTitle": "Search in Rotated Sorted Array",
  "difficulty": "Medium",
  "result": "WRONG_ANSWER",
  "language": "Java",
  "code": "...",
  "timestamp": "..."
}
```

After a failed submission, the extension can optionally ask:

```text
Why do you think this failed?

○ Didn't understand the concept
○ Wrong logic
○ Edge case
○ Boundary / index issue
○ Syntax / API mistake
○ Complexity / TLE
○ Didn't know how to approach
○ Other
```

---

## 3. Spring Boot Receives the Data

The backend stores the submission and sends the relevant information to the ML service.

```text
Chrome Extension
       ↓
Spring Boot
       ↓
PostgreSQL
       ↓
Python ML Service
```

---

## 4. AI Analyzes the Mistake

The ML service combines signals such as:

- Problem topic
- Difficulty
- Submission result
- User explanation
- Code characteristics
- Previous mistakes

Example:

```text
Result:
WRONG_ANSWER

Explanation:
"I wasn't sure whether high should be mid or mid - 1."

        ↓

AI

        ↓

Mistake:
BOUNDARY_ERROR
```

---

## 5. Mistake Memory

The mistake is converted into an embedding.

```text
"I updated high incorrectly."
        ↓
Sentence Transformer
        ↓
Vector
        ↓
FAISS / ChromaDB
```

The vector database searches for similar previous mistakes.

---

## 6. Recurring Weakness Detection

Suppose the student has:

```text
Binary Search       → Boundary Error
Two Pointers        → Boundary Error
Sliding Window      → Boundary Error
Binary Search       → Boundary Error
```

CodeMemory identifies:

```text
Recurring Weakness:
Boundary Management
```

---

## 7. Personalized Recommendation

The system recommends problems based on the detected weakness.

```text
Weakness:
Boundary Management

        ↓

Targeted Practice

        ↓

Easy → Easy → Medium → Medium
```

---

## 8. Improvement Tracking

The system observes future attempts.

```text
Before:
Boundary Errors = 8

After targeted practice:
Boundary Errors = 3
```

The dashboard can show:

```text
Boundary Management
        ↓
Improving
```

---

# 🧠 AI/ML Pipeline

The ML system will be developed incrementally.

### Phase 1 — Rule-Based Baseline

Start with simple interpretable rules.

```text
TLE + nested loops
        → COMPLEXITY_ERROR

Binary Search + boundary explanation
        → BOUNDARY_ERROR

DFS/BFS + visited explanation
        → GRAPH_VISITED_ERROR
```

### Phase 2 — Machine Learning Classification

Use:

```text
TF-IDF
+
Logistic Regression
```

to classify mistake descriptions.

### Phase 3 — Semantic Embeddings

Use:

```text
Sentence Transformers
```

for semantic similarity.

Initial model:

```text
all-MiniLM-L6-v2
```

### Phase 4 — Recurring Pattern Detection

Compare new mistakes with historical mistakes using:

```text
Embeddings
+
Vector Similarity
+
Topic Information
+
Mistake Frequency
```

### Phase 5 — Recommendation Engine

Generate recommendations based on:

```text
Weak concept
+
Mistake category
+
Difficulty
+
Previous performance
+
Historical mistakes
```

---

# 📊 Dashboard

The React dashboard will contain:

## Overview

```text
Problems Attempted
Problems Accepted
Total Mistakes
Recurring Mistakes
Strongest Concept
Weakest Concept
Current Mastery
```

## Mistake Profile

Example:

```text
Boundary Errors       32%
Complexity Errors     21%
Edge Cases            18%
Wrong Logic           14%
DP Base Cases         10%
Other                  5%
```

## Weaknesses

```text
1. Boundary Management     🔴 High
2. Time Complexity         🟠 Medium
3. Graph Visited State     🟠 Medium
4. DP Base Cases           🟢 Improving
```

## Recommendations

```text
🎯 Today's Focus

Boundary Management

You made 4 boundary-related mistakes
in your last 10 attempts.

Recommended:
→ 2 Easy problems
→ 2 Medium problems
```

## Progress

Track mistakes over time:

```text
Week 1 → 7 mistakes
Week 2 → 5 mistakes
Week 3 → 3 mistakes
Week 4 → 1 mistake
```

---

# 🗃️ Database

## Users

```text
id
name
email
password_hash
created_at
```

## Problems

```text
id
leetcode_id
title
difficulty
url
topics
```

## Submissions

```text
id
user_id
problem_id
code
language
result
attempt_number
submitted_at
```

## Mistakes

```text
id
submission_id
category
user_explanation
ai_explanation
confidence
created_at
```

## Weaknesses

```text
id
user_id
concept
mistake_category
frequency
confidence
mastery_score
first_observed
last_observed
```

## Recommendations

```text
id
user_id
weakness_id
problem_id
reason
status
created_at
```

---

# 📁 Project Structure

```text
CodeMemory/
│
├── backend/
│   ├── src/
│   └── pom.xml
│
├── ml-service/
│   ├── app/
│   │   ├── main.py
│   │   ├── models/
│   │   ├── classifiers/
│   │   ├── embeddings/
│   │   ├── similarity/
│   │   ├── pattern_detection/
│   │   └── recommendation/
│   └── requirements.txt
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── utils/
│   └── package.json
│
├── extension/
│   ├── manifest.json
│   ├── background.js
│   ├── content.js
│   ├── popup/
│   ├── services/
│   └── utils/
│
├── database/
│   ├── schema.sql
│   └── seed.sql
│
├── docs/
│   ├── architecture.md
│   ├── api.md
│   └── ml-pipeline.md
│
├── README.md
└── .gitignore
```

---

# 🚀 Development Roadmap

The project will be built in the following order.

### Phase 1 — Backend

- [ ] Create Spring Boot project
- [ ] Connect PostgreSQL
- [ ] Create entities
- [ ] Create repositories
- [ ] Create services
- [ ] Create REST APIs
- [ ] Test with Postman

### Phase 2 — ML Service

- [ ] Create FastAPI service
- [ ] Implement rule-based classifier
- [ ] Connect Spring Boot → Python
- [ ] Add TF-IDF classifier
- [ ] Add Sentence Transformers
- [ ] Add FAISS/ChromaDB
- [ ] Implement recurring mistake detection

### Phase 3 — Frontend

- [ ] Create React application
- [ ] Create dashboard layout
- [ ] Connect backend APIs
- [ ] Build mistake profile
- [ ] Build weakness cards
- [ ] Build progress charts
- [ ] Build recommendations

### Phase 4 — Chrome Extension

- [ ] Create Manifest V3 extension
- [ ] Detect relevant LeetCode page data
- [ ] Capture submission information where reliably available
- [ ] Add feedback popup
- [ ] Connect extension to Spring Boot
- [ ] Test end-to-end

### Phase 5 — Intelligence

- [ ] Improve mistake classification
- [ ] Improve similarity detection
- [ ] Add weakness scoring
- [ ] Add mastery tracking
- [ ] Add recommendation feedback loop
- [ ] Evaluate improvement

---

# 🎯 MVP

The first version should **not** contain every feature.

The MVP is:

```text
LeetCode
   ↓
Chrome Extension
   ↓
Spring Boot
   ↓
PostgreSQL
   ↓
Python ML
   ↓
Mistake Classification
   ↓
Recurring Mistake Detection
   ↓
React Dashboard
```

The MVP should successfully answer:

> **"What mistake does this student keep making?"**

Only after this works should advanced features be added.

---

# 📈 Evaluation

## ML Evaluation

Measure:

- Accuracy
- Precision
- Recall
- F1-score
- Confusion matrix

## Recommendation Evaluation

Measure:

- Recommendation relevance
- Acceptance of recommendations
- Repeated mistake rate
- Improvement after targeted practice

## Key Outcome

The most important experiment is:

```text
Mistake frequency before targeted practice
                    vs
Mistake frequency after targeted practice
```

Example:

```text
Boundary errors before  = 8
Boundary errors after   = 3

Improvement = 62.5%
```

---

# 🌟 What Makes CodeMemory Different?

### 1. It remembers mistakes

Not just solved problems.

### 2. It learns longitudinally

The system improves its understanding as more coding history is collected.

### 3. It detects hidden weaknesses

A weakness can appear across multiple topics.

```text
Binary Search
Two Pointers
Sliding Window
        ↓
Boundary Management
```

### 4. It personalizes recommendations

Different students receive different recommendations.

### 5. It measures improvement

It does not stop after making a recommendation.

It checks whether the mistake actually decreases.

---

# 🔐 Security & Privacy

CodeMemory should follow these principles:

- Never store unnecessary credentials.
- Hash user passwords.
- Keep API secrets in environment variables.
- Use HTTPS in production.
- Collect only the coding data required for the project.
- Clearly inform users what the extension collects.
- Do not hardcode secrets in the repository.

The extension should also avoid depending on undocumented/private LeetCode APIs unless their use is verified and appropriate.

---

# 🧪 Testing Strategy

### Backend

```text
JUnit
Mockito
Postman
```

### ML

Test:

```text
Known mistake → expected category
Similar mistakes → similar embeddings
Repeated mistakes → recurring weakness
```

### Frontend

Test:

```text
Dashboard loading
API integration
Charts
Empty states
Error states
```

### Extension

Test:

```text
Page detection
Submission detection
Result detection
Data transmission
User feedback
```

---

# 🔮 Future Enhancements

After the core system is stable:

- Code difference analysis between wrong and accepted submissions
- AST-based code analysis
- Cross-topic weakness detection
- Advanced mastery modeling
- Personalized learning paths
- More sophisticated recommendation algorithms
- Temporal learning
- Explainable AI insights
- Optional support for additional coding platforms

---

# 📌 Project Definition

### Name

**CodeMemory**

### One-Line Description

> An AI-powered personal coding memory system that learns from a student's mistakes, identifies recurring weaknesses, and provides targeted practice recommendations.

### Core Novelty

> **Longitudinal modeling of recurring programming mistakes across coding problems.**

### Core Pipeline

```text
Collect → Analyze → Remember → Detect → Recommend → Measure
```

---

# 👩‍💻 Development Principle

Build the project **incrementally**.

Do not start by building everything at once.

The first target is:

```text
Spring Boot
      ↓
PostgreSQL
      ↓
Python ML
      ↓
Mistake Detection
```

Then add:

```text
React
```

Then:

```text
Chrome Extension
```

Then:

```text
Recommendation + Mastery
```

This keeps the project manageable while preserving the full vision.

---

## 📄 Documentation

- [`Project Plan`](CodeMemory_Project_Plan.md) — Detailed architecture, implementation plan, database design, ML pipeline, APIs, testing, and roadmap.

---

## ⭐ CodeMemory

**Your coding history tells you what you solved.  
CodeMemory tells you why you struggled.**
