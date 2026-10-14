from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field

class MistakeCategory(str, Enum):
    BOUNDARY_ERROR = "BOUNDARY_ERROR"
    EDGE_CASE_ERROR = "EDGE_CASE_ERROR"
    WRONG_LOGIC = "WRONG_LOGIC"
    COMPLEXITY_ERROR = "COMPLEXITY_ERROR"
    DATA_STRUCTURE_MISUSE = "DATA_STRUCTURE_MISUSE"
    RECURSION_BASE_CASE_ERROR = "RECURSION_BASE_CASE_ERROR"
    DP_STATE_ERROR = "DP_STATE_ERROR"
    DP_TRANSITION_ERROR = "DP_TRANSITION_ERROR"
    GRAPH_VISITED_ERROR = "GRAPH_VISITED_ERROR"
    DUPLICATE_HANDLING_ERROR = "DUPLICATE_HANDLING_ERROR"
    POINTER_MOVEMENT_ERROR = "POINTER_MOVEMENT_ERROR"
    OFF_BY_ONE_ERROR = "OFF_BY_ONE_ERROR"
    INITIALIZATION_ERROR = "INITIALIZATION_ERROR"
    NULL_OR_EMPTY_INPUT_ERROR = "NULL_OR_EMPTY_INPUT_ERROR"
    SYNTAX_OR_API_ERROR = "SYNTAX_OR_API_ERROR"
    APPROACH_MISUNDERSTANDING = "APPROACH_MISUNDERSTANDING"
    UNKNOWN = "UNKNOWN"

class SubmissionResult(str, Enum):
    ACCEPTED = "ACCEPTED"
    WRONG_ANSWER = "WRONG_ANSWER"
    TIME_LIMIT_EXCEEDED = "TIME_LIMIT_EXCEEDED"
    RUNTIME_ERROR = "RUNTIME_ERROR"
    COMPILE_ERROR = "COMPILE_ERROR"
    MEMORY_LIMIT_EXCEEDED = "MEMORY_LIMIT_EXCEEDED"
    UNKNOWN = "UNKNOWN"

class AttemptAnalysisRequest(BaseModel):
    problemTitle: str = Field(..., description="Name of the coding problem")
    topic: Optional[List[str]] = Field(default=[], description="List of problem topic tags")
    difficulty: Optional[str] = Field(default="MEDIUM", description="Problem difficulty")
    result: SubmissionResult = Field(..., description="Observed submission outcome")
    code: Optional[str] = Field(default=None, description="Submitted source code")
    userExplanation: Optional[str] = Field(default=None, description="Optional user explanation of the failure")

class AttemptAnalysisResponse(BaseModel):
    mistakeCategory: MistakeCategory
    confidence: float
    rootCause: str
    similarMistakes: int = 0
    recurring: bool = False
    evidence: List[str] = []

class MistakeClassificationRequest(BaseModel):
    result: SubmissionResult
    userExplanation: Optional[str] = None
    code: Optional[str] = None
    topics: Optional[List[str]] = []

class MistakeClassificationResponse(BaseModel):
    mistakeCategory: MistakeCategory
    confidence: float
    explanation: str
    evidence: List[str] = []

class EmbeddingRequest(BaseModel):
    text: str = Field(..., description="Text explanation to generate embedding for")

class EmbeddingResponse(BaseModel):
    embedding: List[float]
    dimension: int

class SimilarMistakeItem(BaseModel):
    mistakeId: str
    text: str
    category: MistakeCategory
    similarity: float
    problemTitle: str
    topics: List[str] = []

class IndexMistakeRequest(BaseModel):
    mistakeId: str
    text: str
    category: MistakeCategory
    problemTitle: str
    topics: List[str] = []

class FindSimilarMistakesRequest(BaseModel):
    text: str
    topK: Optional[int] = Field(default=5, description="Number of nearest neighbors to retrieve")
    threshold: Optional[float] = Field(default=0.70, description="Minimum cosine similarity threshold")

class FindSimilarMistakesResponse(BaseModel):
    matches: List[SimilarMistakeItem]
    totalMatches: int

class PatternDetectionRequest(BaseModel):
    newMistake: str
    category: MistakeCategory
    problemTitle: str
    topics: List[str] = []

class PatternDetectionResponse(BaseModel):
    recurring: bool
    recurrenceCount: int
    crossProblem: bool
    patternSummary: str
    matches: List[SimilarMistakeItem] = []
