import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.schemas import MistakeCategory

client = TestClient(app)

def test_tfidf_classifier():
    from app.classifiers.tfidf_classifier import TfidfMistakeClassifier
    clf = TfidfMistakeClassifier()
    cat, conf = clf.predict("I set high = mid instead of high = mid - 1 in binary search")
    assert cat == MistakeCategory.BOUNDARY_ERROR
    assert conf > 0.20

def test_code_diff_analyzer():
    wrong_code = "while (left < right) { int mid = (left+right)/2; high = mid; }"
    acc_code = "while (left <= right) { int mid = left + (right-left)/2; high = mid - 1; }"
    
    payload = {
        "wrongCode": wrong_code,
        "acceptedCode": acc_code,
        "problemTitle": "Binary Search"
    }
    response = client.post("/analyze-diff", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert len(data["detectedCorrectionPatterns"]) >= 1
    assert any("offset" in p.lower() or "boundary" in p.lower() for p in data["detectedCorrectionPatterns"])

def test_ast_analyzer_nested_loop_risk():
    code = """
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            if (nums[i] + nums[j] == target) return true;
        }
    }
    """
    payload = {
        "code": code,
        "language": "java",
        "problemTitle": "Two Sum"
    }
    response = client.post("/analyze-ast", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["maxNestingDepth"] >= 2
    assert data["complexityRisk"] == "HIGH"
    assert any("nested" in p.lower() for p in data["detectedPatterns"])

def test_recommendation_endpoint():
    payload = {
        "weaknessConcept": "Binary Search",
        "mistakeCategory": "BOUNDARY_ERROR",
        "currentMastery": 72.0,
        "frequency": 3
    }
    response = client.post("/generate-recommendation", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["weaknessConcept"] == "Binary Search"
    assert len(data["recommendedProblems"]) >= 3
    assert any(p["problemTitle"] == "Binary Search" for p in data["recommendedProblems"])
