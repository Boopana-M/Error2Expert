from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.models.schemas import (
    AttemptAnalysisRequest,
    AttemptAnalysisResponse,
    MistakeClassificationRequest,
    MistakeClassificationResponse,
    MistakeCategory,
    SubmissionResult
)
from app.classifiers.rule_based import RuleBasedClassifier

app = FastAPI(
    title="CodeMemory ML Service",
    description="AI Second Brain for Coding Mistakes - Analysis and Weakness Inference Engine",
    version="1.0.0"
)

# CORS Middleware to support Spring Boot & Dev requests
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

classifier = RuleBasedClassifier()

@app.get("/")
def root():
    return {
        "service": "CodeMemory ML Service",
        "status": "online",
        "version": "1.0.0",
        "pipeline": "FastAPI + Rule-Based Mistake Classifier"
    }

@app.get("/health")
def health():
    return {"status": "HEALTHY"}

@app.post("/analyze-attempt", response_model=AttemptAnalysisResponse)
def analyze_attempt(request: AttemptAnalysisRequest):
    """
    Main attempt analysis pipeline endpoint.
    Processes a submission outcome and user explanation to identify
    root-cause mistakes and recurring weaknesses.
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

    return AttemptAnalysisResponse(
        mistakeCategory=category,
        confidence=confidence,
        rootCause=root_cause,
        similarMistakes=0,
        recurring=False,
        evidence=evidence
    )

@app.post("/classify-mistake", response_model=MistakeClassificationResponse)
def classify_mistake(request: MistakeClassificationRequest):
    """
    Direct mistake classification endpoint based on outcome, code, and user feedback.
    """
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
