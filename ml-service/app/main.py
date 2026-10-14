from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import List
from app.models.schemas import (
    AttemptAnalysisRequest,
    AttemptAnalysisResponse,
    MistakeClassificationRequest,
    MistakeClassificationResponse,
    EmbeddingRequest,
    EmbeddingResponse,
    IndexMistakeRequest,
    FindSimilarMistakesRequest,
    FindSimilarMistakesResponse,
    PatternDetectionRequest,
    PatternDetectionResponse,
    MistakeCategory,
    SubmissionResult
)
from app.classifiers.rule_based import RuleBasedClassifier
from app.embeddings.embedder import EmbeddingService
from app.similarity.vector_store import FaissVectorMemory
from app.pattern_detection.pattern_detector import PatternDetector

app = FastAPI(
    title="CodeMemory ML Service",
    description="AI Second Brain for Coding Mistakes - Analysis, Vector Memory, and Weakness Inference Engine",
    version="1.1.0"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

classifier = RuleBasedClassifier()
embedder = EmbeddingService()
vector_memory = FaissVectorMemory(embedder)
pattern_detector = PatternDetector(vector_memory)

# Seed vector memory with initial realistic historical patterns from Section 9 & 10
def _seed_initial_memory():
    initial_mistakes = [
        ("seed-1", "I updated high = mid instead of high = mid - 1", MistakeCategory.BOUNDARY_ERROR, "Binary Search", ["Binary Search", "Array"]),
        ("seed-2", "Low pointer was updated to mid instead of mid + 1 resulting in infinite loop", MistakeCategory.BOUNDARY_ERROR, "Search in Rotated Sorted Array", ["Binary Search", "Array"]),
        ("seed-3", "Left and right boundaries were not adjusted with offset +/- 1", MistakeCategory.BOUNDARY_ERROR, "Find Minimum in Rotated Sorted Array", ["Binary Search", "Array"]),
        ("seed-4", "Forgot to mark cell visited in DFS causing infinite recursion stack overflow", MistakeCategory.GRAPH_VISITED_ERROR, "Number of Islands", ["Graph", "Depth-First Search"]),
        ("seed-5", "Did not maintain visited cycle state in directed graph traversal", MistakeCategory.GRAPH_VISITED_ERROR, "Course Schedule", ["Graph", "Topological Sort"]),
    ]
    for mid, text, cat, title, topics in initial_mistakes:
        vector_memory.add_mistake(mid, text, cat, title, topics)

_seed_initial_memory()

@app.get("/")
def root():
    return {
        "service": "CodeMemory ML Service",
        "status": "online",
        "version": "1.1.0",
        "pipeline": "FastAPI + Sentence-Transformers + FAISS Vector Memory",
        "indexedMistakes": vector_memory.count()
    }

@app.get("/health")
def health():
    return {
        "status": "HEALTHY",
        "indexedMistakes": vector_memory.count(),
        "vectorDimension": embedder.dimension
    }

@app.post("/analyze-attempt", response_model=AttemptAnalysisResponse)
def analyze_attempt(request: AttemptAnalysisRequest):
    """
    Main attempt analysis pipeline endpoint.
    Processes a submission outcome and user explanation to identify
    root-cause mistakes and recurring weaknesses using FAISS memory.
    """
    if request.result == SubmissionResult.ACCEPTED and not request.userExplanation:
        return AttemptAnalysisResponse(
            mistakeCategory=MistakeCategory.UNKNOWN,
            confidence=1.0,
            rootCause="Accepted submission; no error detected.",
            similarMistakes=0,
            recurring=False,
            evidence=["Submission was marked ACCEPTED"]
        )

    category, confidence, root_cause, evidence = classifier.classify(
        result=request.result,
        user_explanation=request.userExplanation,
        code=request.code,
        topics=request.topic,
        problem_title=request.problemTitle
    )

    similar_count = 0
    is_recurring = False

    # Check FAISS vector memory if explanation is present
    if request.userExplanation and request.userExplanation.strip():
        pattern_res = pattern_detector.detect_pattern(
            new_mistake_text=request.userExplanation,
            category=category,
            problem_title=request.problemTitle,
            topics=request.topic
        )
        similar_count = len(pattern_res.matches)
        is_recurring = pattern_res.recurring
        evidence.append(pattern_res.patternSummary)

        # Index into longitudinal memory
        vector_memory.add_mistake(
            mistake_id=f"attempt-{vector_memory.count() + 1}",
            text=request.userExplanation,
            category=category,
            problem_title=request.problemTitle,
            topics=request.topic
        )

    return AttemptAnalysisResponse(
        mistakeCategory=category,
        confidence=confidence,
        rootCause=root_cause,
        similarMistakes=similar_count,
        recurring=is_recurring,
        evidence=evidence
    )

@app.post("/classify-mistake", response_model=MistakeClassificationResponse)
def classify_mistake(request: MistakeClassificationRequest):
    category, confidence, explanation, evidence = classifier.classify(
        result=request.result,
        user_explanation=request.userExplanation,
        code=request.code,
        topics=request.topics
    )

    return MistakeClassificationResponse(
        mistakeCategory=category,
        confidence=confidence,
        explanation=explanation,
        evidence=evidence
    )

@app.post("/generate-embedding", response_model=EmbeddingResponse)
def generate_embedding(request: EmbeddingRequest):
    vector = embedder.generate_embedding(request.text)
    return EmbeddingResponse(
        embedding=vector.tolist(),
        dimension=len(vector)
    )

@app.post("/index-mistake")
def index_mistake(request: IndexMistakeRequest):
    idx = vector_memory.add_mistake(
        mistake_id=request.mistakeId,
        text=request.text,
        category=request.category,
        problem_title=request.problemTitle,
        topics=request.topics
    )
    return {
        "success": True,
        "indexedAt": idx,
        "totalMistakes": vector_memory.count()
    }

@app.post("/find-similar-mistakes", response_model=FindSimilarMistakesResponse)
def find_similar_mistakes(request: FindSimilarMistakesRequest):
    matches = vector_memory.search_similar(
        query_text=request.text,
        top_k=request.topK or 5,
        threshold=request.threshold or 0.70
    )
    return FindSimilarMistakesResponse(
        matches=matches,
        totalMatches=len(matches)
    )

@app.post("/detect-pattern", response_model=PatternDetectionResponse)
def detect_pattern(request: PatternDetectionRequest):
    return pattern_detector.detect_pattern(
        new_mistake_text=request.newMistake,
        category=request.category,
        problem_title=request.problemTitle,
        topics=request.topics
    )
