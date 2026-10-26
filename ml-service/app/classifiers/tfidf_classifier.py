from typing import Tuple, List, Optional
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
import numpy as np
from app.models.schemas import MistakeCategory

class TfidfMistakeClassifier:
    """
    Phase 2 ML Mistake Classifier using TF-IDF + Logistic Regression
    as specified in Section 13 of CodeMemory_Project_Plan.md.
    Provides interpretable probability distributions over mistake categories.
    """

    def __init__(self):
        self.pipeline = Pipeline([
            ('tfidf', TfidfVectorizer(ngram_range=(1, 2), sublinear_tf=True, min_df=1)),
            ('clf', LogisticRegression(max_iter=500, C=1.5, class_weight='balanced'))
        ])
        self.is_trained = False
        self._train_initial_model()

    def _train_initial_model(self):
        # Representative corpus of failure explanations from real LeetCode mistakes
        training_samples = [
            # BOUNDARY_ERROR
            ("high should be mid or mid minus one", MistakeCategory.BOUNDARY_ERROR),
            ("I set left = mid instead of mid + 1 causing boundary lock", MistakeCategory.BOUNDARY_ERROR),
            ("loop termination condition left <= right was wrong, infinite loop", MistakeCategory.BOUNDARY_ERROR),
            ("upper search bound was out of array length range", MistakeCategory.BOUNDARY_ERROR),
            ("binary search boundary management was incorrect", MistakeCategory.BOUNDARY_ERROR),
            ("high = mid - 1 vs high = mid confusion in binary search", MistakeCategory.BOUNDARY_ERROR),

            # GRAPH_VISITED_ERROR
            ("forgot to mark node as visited causing recursion stack overflow", MistakeCategory.GRAPH_VISITED_ERROR),
            ("visited state was not updated before pushing to queue in BFS", MistakeCategory.GRAPH_VISITED_ERROR),
            ("cycle in directed graph infinite loop because visited array was not reset", MistakeCategory.GRAPH_VISITED_ERROR),
            ("DFS infinite recursion missed visited check", MistakeCategory.GRAPH_VISITED_ERROR),

            # COMPLEXITY_ERROR
            ("used nested loops O(N^2) instead of hashmap O(N), got TLE", MistakeCategory.COMPLEXITY_ERROR),
            ("brute force exponential time complexity exceeded time limit", MistakeCategory.COMPLEXITY_ERROR),
            ("time limit exceeded due to nested for loops scanning array", MistakeCategory.COMPLEXITY_ERROR),
            ("recursion without memoization led to TLE timeout", MistakeCategory.COMPLEXITY_ERROR),

            # EDGE_CASE_ERROR
            ("failed on empty array input single element corner case", MistakeCategory.EDGE_CASE_ERROR),
            ("did not handle duplicate elements in input testcase", MistakeCategory.EDGE_CASE_ERROR),
            ("integer overflow with large numbers or negative values", MistakeCategory.EDGE_CASE_ERROR),

            # DP_STATE_ERROR
            ("DP memoization table state dimensions were wrong", MistakeCategory.DP_STATE_ERROR),
            ("overlapping subproblems transition was missing previous index", MistakeCategory.DP_STATE_ERROR),
            ("dynamic programming state definition was incorrect", MistakeCategory.DP_STATE_ERROR),

            # RECURSION_BASE_CASE_ERROR
            ("forgot recursive base case condition so function never returns", MistakeCategory.RECURSION_BASE_CASE_ERROR),
            ("base case check was placed after index access causing out of bounds", MistakeCategory.RECURSION_BASE_CASE_ERROR),

            # OFF_BY_ONE_ERROR
            ("array index out of bounds by one element length minus one", MistakeCategory.OFF_BY_ONE_ERROR),
            ("off by one error in loop condition < vs <=", MistakeCategory.OFF_BY_ONE_ERROR),

            # NULL_OR_EMPTY_INPUT_ERROR
            ("null pointer exception because root node was null", MistakeCategory.NULL_OR_EMPTY_INPUT_ERROR),
            ("forgot to check if head was null before reading next", MistakeCategory.NULL_OR_EMPTY_INPUT_ERROR),

            # WRONG_LOGIC
            ("wrong greedy choice instead of dynamic programming", MistakeCategory.WRONG_LOGIC),
            ("algorithm logic was incorrect for negative numbers", MistakeCategory.WRONG_LOGIC),
        ]

        texts = [s[0] for s in training_samples]
        labels = [s[1].value for s in training_samples]

        self.pipeline.fit(texts, labels)
        self.is_trained = True

    def predict(self, text: str) -> Tuple[MistakeCategory, float]:
        """
        Predicts mistake category and returns calibrated confidence score.
        """
        if not text or not text.strip() or not self.is_trained:
            return MistakeCategory.UNKNOWN, 0.50

        probs = self.pipeline.predict_proba([text])[0]
        max_idx = int(np.argmax(probs))
        predicted_class_name = self.pipeline.classes_[max_idx]
        confidence = float(probs[max_idx])

        try:
            category = MistakeCategory(predicted_class_name)
        except ValueError:
            category = MistakeCategory.UNKNOWN

        return category, round(confidence, 3)
