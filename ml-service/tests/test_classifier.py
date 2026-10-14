import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.schemas import MistakeCategory

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "HEALTHY"

def test_analyze_binary_search_boundary_mistake():
    payload = {
        "problemTitle": "Binary Search",
        "topic": ["Binary Search", "Array"],
        "difficulty": "EASY",
        "result": "WRONG_ANSWER",
        "code": "int l=0, r=n; while(l<r) { int m=(l+r)/2; if(a[m]==t) return m; l=m; }",
        "userExplanation": "I updated high = mid instead of high = mid - 1 causing boundary failure"
    }
    response = client.post("/analyze-attempt", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["mistakeCategory"] == MistakeCategory.BOUNDARY_ERROR.value
    assert data["confidence"] >= 0.90
    assert "boundary" in data["rootCause"].lower()

def test_analyze_graph_visited_mistake():
    payload = {
        "problemTitle": "Number of Islands",
        "topic": ["Graph", "Depth-First Search"],
        "difficulty": "MEDIUM",
        "result": "RUNTIME_ERROR",
        "userExplanation": "Forgot to mark cell visited in DFS traversal leading to infinite loop"
    }
    response = client.post("/analyze-attempt", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["mistakeCategory"] == MistakeCategory.GRAPH_VISITED_ERROR.value
    assert data["confidence"] >= 0.90

def test_analyze_complexity_tle_mistake():
    payload = {
        "problemTitle": "3Sum",
        "topic": ["Array"],
        "result": "TIME_LIMIT_EXCEEDED",
        "userExplanation": "Used nested loops with cubic O(N^3) complexity"
    }
    response = client.post("/analyze-attempt", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["mistakeCategory"] == MistakeCategory.COMPLEXITY_ERROR.value
    assert data["confidence"] >= 0.90

def test_classify_off_by_one_mistake():
    payload = {
        "result": "WRONG_ANSWER",
        "userExplanation": "Made an off by one mistake using <= instead of < on array length",
        "topics": ["Array"]
    }
    response = client.post("/classify-mistake", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["mistakeCategory"] == MistakeCategory.OFF_BY_ONE_ERROR.value

def test_analyze_accepted_submission():
    payload = {
        "problemTitle": "Binary Search",
        "result": "ACCEPTED"
    }
    response = client.post("/analyze-attempt", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["mistakeCategory"] == MistakeCategory.UNKNOWN.value
    assert "accepted" in data["rootCause"].lower()
