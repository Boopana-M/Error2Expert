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
    RecommendationRequest,
    RecommendationResponse,
    RecommendationItem,
    CodeDiffAnalysisRequest,
    CodeDiffAnalysisResponse,
    AstAnalysisRequest,
    AstAnalysisResponse,
    MistakeCategory,
    SubmissionResult
)
from app.classifiers.rule_based import RuleBasedClassifier
from app.classifiers.tfidf_classifier import TfidfMistakeClassifier
from app.embeddings.embedder import EmbeddingService
from app.similarity.vector_store import FaissVectorMemory
from app.pattern_detection.pattern_detector import PatternDetector
from app.analysis.code_diff_analyzer import CodeDiffAnalyzer
from app.analysis.ast_analyzer import AstAnalyzer

app = FastAPI(
    title="CodeMemory ML Service",
    description="AI Second Brain for Coding Mistakes - Analysis, Vector Memory, and Weakness Inference Engine",
    version="1.2.0"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

rule_classifier = RuleBasedClassifier()
tfidf_classifier = TfidfMistakeClassifier()
embedder = EmbeddingService()
vector_memory = FaissVectorMemory(embedder)
pattern_detector = PatternDetector(vector_memory)
code_diff_analyzer = CodeDiffAnalyzer()
ast_analyzer = AstAnalyzer()

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
        "version": "1.2.0",
        "pipeline": "FastAPI + Hybrid ML (Rule-Based + TF-IDF) + Sentence-Transformers + FAISS Vector Memory",
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

    # Hybrid classification (Rule-based heuristic + TF-IDF ML confidence)
    category, confidence, root_cause, evidence = rule_classifier.classify(
        result=request.result,
        user_explanation=request.userExplanation,
        code=request.code,
        topics=request.topic,
        problem_title=request.problemTitle
    )

    if request.userExplanation and request.userExplanation.strip():
        tfidf_cat, tfidf_conf = tfidf_classifier.predict(request.userExplanation)
        evidence.append(f"Phase 2 ML TF-IDF Classifier: {tfidf_cat.value} (confidence: {tfidf_conf:.2f})")
        # If rule was ambiguous, use TF-IDF prediction
        if category in (MistakeCategory.UNKNOWN, MistakeCategory.WRONG_LOGIC) and tfidf_conf > 0.40:
            category = tfidf_cat
            confidence = max(confidence, tfidf_conf)

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
    category, confidence, explanation, evidence = rule_classifier.classify(
        result=request.result,
        user_explanation=request.userExplanation,
        code=request.code,
        topics=request.topics
    )

    if request.userExplanation:
        tfidf_cat, tfidf_conf = tfidf_classifier.predict(request.userExplanation)
        evidence.append(f"Phase 2 TF-IDF model consensus: {tfidf_cat.value} ({tfidf_conf:.2f})")

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
        threshold=request.threshold or 0.52
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

@app.post("/generate-recommendation", response_model=RecommendationResponse)
def generate_recommendation(request: RecommendationRequest):
    """
    Section 22 ML Recommendation Engine Endpoint.
    Generates targeted practice problems calibrated to the detected weakness.
    """
    concept = request.weaknessConcept
    category = request.mistakeCategory
    curriculum: List[RecommendationItem] = []

    if category == MistakeCategory.BOUNDARY_ERROR:
        curriculum = [
            RecommendationItem(
                problemTitle="Binary Search",
                difficulty="EASY",
                topics=["Binary Search", "Array"],
                reason="Reinforce standard loop invariant termination (left <= right) and interval narrowing."
            ),
            RecommendationItem(
                problemTitle="Search Insert Position",
                difficulty="EASY",
                topics=["Binary Search", "Array"],
                reason="Practice precise insertion boundary return indices."
            ),
            RecommendationItem(
                problemTitle="Find Minimum in Rotated Sorted Array",
                difficulty="MEDIUM",
                topics=["Binary Search", "Array"],
                reason="Eliminate boundary off-by-one errors when comparing nums[mid] with nums[right]."
            ),
            RecommendationItem(
                problemTitle="Search in Rotated Sorted Array",
                difficulty="MEDIUM",
                topics=["Binary Search", "Array"],
                reason="Master complex inflection point partitioning without infinite boundary loop."
            )
        ]
    elif category == MistakeCategory.GRAPH_VISITED_ERROR:
        curriculum = [
            RecommendationItem(
                problemTitle="Number of Islands",
                difficulty="MEDIUM",
                topics=["Graph", "Depth-First Search", "Breadth-First Search"],
                reason="Practice marking matrix cells visited prior to recursive traversal."
            ),
            RecommendationItem(
                problemTitle="Clone Graph",
                difficulty="MEDIUM",
                topics=["Graph", "Depth-First Search", "Hash Table"],
                reason="Reinforce node visited map memoization to avoid cyclic recursion."
            ),
            RecommendationItem(
                problemTitle="Course Schedule",
                difficulty="MEDIUM",
                topics=["Graph", "Topological Sort"],
                reason="Differentiate between visited set and recursion stack cycle state."
            )
        ]
    else:
        curriculum = [
            RecommendationItem(
                problemTitle="Climbing Stairs",
                difficulty="EASY",
                topics=["Dynamic Programming"],
                reason="Establish foundational state definitions and base conditions."
            ),
            RecommendationItem(
                problemTitle="Coin Change",
                difficulty="MEDIUM",
                topics=["Dynamic Programming"],
                reason="Practice multi-dimensional subproblem transitions."
            )
        ]

    rationale = (
        f"Targeted practice designed to eliminate recurring {category.value.replace('_', ' ').title()} "
        f"weakness on '{concept}'. Current mastery is {request.currentMastery:.0f}%."
    )

    return RecommendationResponse(
        weaknessConcept=concept,
        mistakeCategory=category,
        focusRationale=rationale,
        recommendedProblems=curriculum
    )

@app.post("/analyze-diff", response_model=CodeDiffAnalysisResponse)
def analyze_diff(request: CodeDiffAnalysisRequest):
    """
    Section 31 Advanced Feature 1: Code Difference Analysis.
    Compares wrong code vs accepted code to deduce correction patterns.
    """
    res = code_diff_analyzer.analyze_diff(request.wrongCode, request.acceptedCode)
    return CodeDiffAnalysisResponse(
        diffSummary=res["diffSummary"],
        detectedCorrectionPatterns=res["detectedCorrectionPatterns"],
        diffLines=res["diffLines"]
    )

@app.post("/analyze-ast", response_model=AstAnalysisResponse)
def analyze_ast(request: AstAnalysisRequest):
    """
    Section 31 Advanced Feature 2: Abstract Syntax Tree Analysis.
    Detects control flow anomalies, loop nesting depth, and missing guards.
    """
    res = ast_analyzer.analyze_code_structure(request.code, request.language or "java")
    return AstAnalysisResponse(
        maxNestingDepth=res["maxNestingDepth"],
        complexityRisk=res["complexityRisk"],
        detectedPatterns=res["detectedPatterns"],
        structuralIssues=res["structuralIssues"]
    )
