import pytest
from fastapi.testclient import TestClient
import numpy as np
from app.main import app
from app.embeddings.embedder import EmbeddingService
from app.similarity.vector_store import FaissVectorMemory
from app.models.schemas import MistakeCategory

client = TestClient(app)

def test_embedding_service_dimension():
    embedder = EmbeddingService()
    vec = embedder.generate_embedding("Binary search pointer update issue")
    assert isinstance(vec, np.ndarray)
    assert vec.shape == (384,)
    # Vector should be L2 unit normalized
    norm = np.linalg.norm(vec)
    assert pytest.approx(norm, rel=1e-3) == 1.0

def test_semantic_similarity_between_paraphrased_mistakes():
    embedder = EmbeddingService()
    v1 = embedder.generate_embedding("I forgot to update the left pointer.")
    v2 = embedder.generate_embedding("I didn't move the low pointer correctly.")
    v3 = embedder.generate_embedding("The database connection timed out.")

    sim_related = float(np.dot(v1, v2))
    sim_unrelated = float(np.dot(v1, v3))

    assert sim_related > 0.60, f"Expected high semantic similarity, got {sim_related}"
    assert sim_related > sim_unrelated, "Related mistakes should have higher similarity than unrelated topics"

def test_endpoint_generate_embedding():
    response = client.post("/generate-embedding", json={"text": "high should have been mid - 1"})
    assert response.status_code == 200
    data = response.json()
    assert data["dimension"] == 384
    assert len(data["embedding"]) == 384

def test_endpoint_find_similar_mistakes():
    query = "I set high = mid instead of mid - 1"
    response = client.post("/find-similar-mistakes", json={"text": query, "topK": 3, "threshold": 0.65})
    assert response.status_code == 200
    data = response.json()
    assert data["totalMatches"] >= 1
    assert any(m["category"] == MistakeCategory.BOUNDARY_ERROR.value for m in data["matches"])

def test_endpoint_detect_recurring_cross_problem_pattern():
    payload = {
        "newMistake": "Low boundary was set to mid without adding 1",
        "category": MistakeCategory.BOUNDARY_ERROR.value,
        "problemTitle": "Search Insert Position",
        "topics": ["Binary Search", "Array"]
    }
    response = client.post("/detect-pattern", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["recurring"] is True
    assert data["recurrenceCount"] >= 2
    assert data["crossProblem"] is True
    assert "recurring" in data["patternSummary"].lower()
