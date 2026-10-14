# 🧠 CodeMemory AI/ML Pipeline & Longitudinal Error Modeling

This document details the Machine Learning pipeline, vector embedding memory, and mathematical mastery formulas powering **CodeMemory**.

---

## 1. Multi-Stage Mistake Classification Pipeline

```text
                  Submission Code + User Explanation
                                  │
                                  ▼
                   ┌───────────────────────────────┐
                   │ Phase 1: Rule-Based Classifier│
                   │ (17 Fine-Grained Categories)  │
                   └──────────────┬────────────────┘
                                  │
                                  ▼
                   ┌───────────────────────────────┐
                   │ Phase 2: Embedding Generation │
                   │  (all-MiniLM-L6-v2, 384-d)    │
                   └──────────────┬────────────────┘
                                  │
                                  ▼
                   ┌───────────────────────────────┐
                   │ Phase 3: FAISS Vector Memory  │
                   │    (IndexFlatIP, Cosine Sim)  │
                   └──────────────┬────────────────┘
                                  │
                                  ▼
                   ┌───────────────────────────────┐
                   │ Phase 4: Pattern Detection    │
                   │ (Cross-Problem Recurrence)    │
                   └───────────────────────────────┘
```

---

## 2. Mistake Categories & Taxonomy

CodeMemory models 17 specific categories of programming misconceptions:

| Category | Primary Signals | Example Explanation |
|---|---|---|
| `BOUNDARY_ERROR` | `high = mid`, `<` vs `<=`, boundary, left pointer | "I set high = mid instead of high = mid - 1" |
| `GRAPH_VISITED_ERROR` | `visited`, graph, BFS, queue duplicate, cycle | "Forgot to mark node visited before pushing into BFS queue" |
| `COMPLEXITY_ERROR` | `TLE`, nested loops, O(N^2), quadratic, time limit | "Used brute-force double loop instead of frequency map" |
| `DP_BASE_CASE_ERROR` | DP, memo, base condition, length 0, initialization | "Missed base case when string length is 1 or empty" |
| `OFF_BY_ONE_ERROR` | `off by one`, `i < n - 1`, array index out of bounds | "Loop stopped at length - 2 missing last element" |
| `EDGE_CASE_ERROR` | Single element, empty array, negative numbers | "Failed on empty array input []" |
| `DATA_STRUCTURE_MISUSE` | Priority queue, heap, set vs list | "Used ArrayList for frequent contains() checks" |
| `POINTER_MOVEMENT_ERROR` | Two pointers, slow fast, head.next | "Forgot to advance slow pointer in cycle detection" |
| `DUPLICATE_HANDLING_ERROR` | Duplicate elements, sort skip, duplicate subsets | "Did not skip identical adjacent numbers" |

---

## 3. Dense Vector Embeddings & FAISS Memory

### 3.1 Sentence Transformer (`all-MiniLM-L6-v2`)
- Explanation text is mapped into dense 384-dimensional continuous semantic space.
- Paraphrased explanations cluster tightly together:
  - *"I forgot to update the left pointer."*
  - *"I didn't move the low pointer correctly."*
  - Semantic Cosine Similarity: **> 0.68** (substantially higher than unrelated topics < 0.20).

### 3.2 Index Structure (`IndexFlatIP`)
- Embeddings are unit L2-normalized:
  $$\hat{v} = \frac{v}{\|v\|_2}$$
- By using unit vectors, inner product computes exact Cosine Similarity:
  $$\cos(\theta) = \hat{u} \cdot \hat{v}$$
- Cosine similarity threshold:
  - $\text{similarity} \ge 0.70 \implies \text{Semantic Match}$
  - A new mistake is flagged as a **Recurring Pattern** if $\ge 2$ matches exist across different problem titles.

---

## 4. Longitudinal Mastery Scoring Formula

Mastery score ($M$) tracks true conceptual retention over time:

$$M_t = \text{clamp}\Big(M_{t-1} + \Delta, \; 10.0, \; 100.0\Big)$$

Where adjustments ($\Delta$) are dynamically applied:

| Event | Score Adjustment ($\Delta$) | Rationale |
|---|---|---|
| **Initial Observation** ($f = 1$) | $-5.0$ pts | First detected failure |
| **Recurring Mistake** ($f \ge 2$) | $-5.0$ pts | Repeated blind spot penalty |
| **Cross-Problem Recurrence** | $-8.0$ pts | Failure across distinct problem titles |
| **Successful Practice** | $+8.0$ pts | Accepted solution on recommended problem |
| **Consecutive Solves** | $+10.0$ pts | Sustained mastery without recurring errors |

### Severity Derivation:
- $M \ge 80.0 \implies \textbf{IMPROVING} \quad (\text{Mastery established})$
- $f \ge 3 \implies \textbf{HIGH} \quad (\text{Critical longitudinal blind spot})$
- Otherwise $\implies \textbf{MEDIUM}$
