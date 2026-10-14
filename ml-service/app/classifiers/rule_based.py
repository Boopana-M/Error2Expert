import re
from typing import List, Optional, Tuple
from app.models.schemas import MistakeCategory, SubmissionResult

class RuleBasedClassifier:
    """
    Phase 1 Rule-Based Mistake Classifier
    Infers root cause programming weaknesses from:
    1. Observable Result (Level 1)
    2. User-Reported Explanation (Level 2)
    3. Topic Tags & Constraints
    4. Code Pattern Matching
    """

    def classify(
        self,
        result: SubmissionResult,
        user_explanation: Optional[str] = None,
        code: Optional[str] = None,
        topics: Optional[List[str]] = None,
        problem_title: Optional[str] = None
    ) -> Tuple[MistakeCategory, float, str, List[str]]:
        """
        Returns: (category, confidence, root_cause_explanation, evidence_list)
        """
        explanation = (user_explanation or "").lower()
        topic_list = [t.lower() for t in (topics or [])]
        code_str = code or ""
        evidence: List[str] = []

        # 1. Compile Error -> Syntax / API Error
        if result == SubmissionResult.COMPILE_ERROR or any(k in explanation for k in ["syntax", "type error", "compil", "semicolon", "cannot find symbol"]):
            evidence.append(f"Submission status: {result}")
            if explanation:
                evidence.append(f"User noted syntax/type difficulty: '{user_explanation}'")
            return (
                MistakeCategory.SYNTAX_OR_API_ERROR,
                0.95,
                "Language syntax or API usage error prevented execution.",
                evidence
            )

        # 2. Time Limit Exceeded / Complexity Misjudgment
        if result == SubmissionResult.TIME_LIMIT_EXCEEDED or any(k in explanation for k in ["tle", "time limit", "nested loop", "o(n^2)", "o(n2)", "quadratic", "exponential", "too slow"]):
            evidence.append(f"Submission status: {result}")
            if any(k in explanation for k in ["nested", "quadratic", "o(n^2)", "inner loop"]):
                evidence.append("User confirmed sub-optimal nested iteration or high complexity")
            has_nested_loops = bool(re.search(r"for\b.*\{?[^}]*for\b", code_str, re.DOTALL))
            if has_nested_loops:
                evidence.append("Detected nested loop patterns in submitted code")
            return (
                MistakeCategory.COMPLEXITY_ERROR,
                0.93 if explanation else 0.88,
                "Algorithmic complexity misconception exceeding problem time constraints.",
                evidence
            )

        # 3. Off-by-one Error
        if any(k in explanation for k in ["off by one", "off-by-one", "<= instead of <", "< instead of <=", "length - 1", "size - 1", "index 0", "last element missed"]):
            evidence.append(f"Direct explanation indicator: '{user_explanation}'")
            return (
                MistakeCategory.OFF_BY_ONE_ERROR,
                0.94,
                "Off-by-one indexing offset in boundary or loop termination condition.",
                evidence
            )

        # 4. Graph / Tree Visited-State Mismanagement
        is_graph_topic = any("graph" in t or "depth-first search" in t or "breadth-first search" in t for t in topic_list)
        graph_keywords = ["visited", "visit", "cycle", "recursion stack", "seen", "infinite recursion", "unvisited"]
        if any(k in explanation for k in graph_keywords) or (is_graph_topic and ("infinite loop" in explanation or "overflow" in explanation or result == SubmissionResult.RUNTIME_ERROR)):
            evidence.append(f"Graph traversal signals identified: '{user_explanation}'")
            if is_graph_topic:
                evidence.append("Contextual topic: Graph traversal / DFS / BFS")
            return (
                MistakeCategory.GRAPH_VISITED_ERROR,
                0.93,
                "Omitted or improperly maintained visited-state transition causing cyclic infinite traversal.",
                evidence
            )

        # 5. Binary Search / Pointer Boundary Error
        boundary_keywords = ["boundary", "bound", "high = mid", "low = mid", "high=mid", "low=mid", "left = mid", "right = mid", "pointer update", "mid + 1", "mid - 1", "low pointer", "high pointer", "left pointer", "right pointer"]
        is_search_topic = any("binary search" in t or "two pointer" in t or "sliding window" in t for t in topic_list)
        if any(k in explanation for k in boundary_keywords) or (is_search_topic and any(k in explanation for k in ["high", "low", "mid", "left", "right", "interval"])):
            evidence.append(f"Boundary keyword match in explanation: '{user_explanation}'")
            if is_search_topic:
                evidence.append("Contextual topic: Binary Search / Pointer problem")
            return (
                MistakeCategory.BOUNDARY_ERROR,
                0.92,
                f"Incorrect boundary update or loop termination in pointer logic on {problem_title or 'problem'}.",
                evidence
            )

        # 6. Recursion Base-Case Failure
        if any(k in explanation for k in ["base case", "base-case", "stack overflow", "maximum recursion depth", "termination condition"]):
            evidence.append(f"Recursion base-case keywords found in explanation: '{user_explanation}'")
            return (
                MistakeCategory.RECURSION_BASE_CASE_ERROR,
                0.90,
                "Missing or prematurely triggered base case in recursive decomposition.",
                evidence
            )

        # 7. Dynamic Programming State / Transition Error
        is_dp_topic = any("dynamic programming" in t or "memoization" in t for t in topic_list)
        if any(k in explanation for k in ["dp", "subproblem", "transition", "state", "memo", "overlapping", "memoization"]) or (is_dp_topic and "wrong logic" in explanation):
            evidence.append(f"Dynamic programming state cues in explanation: '{user_explanation}'")
            return (
                MistakeCategory.DP_STATE_ERROR,
                0.89,
                "Misformulated state representation or incorrect transition equation.",
                evidence
            )

        # 8. Null, Empty, or Edge-Case Handling Error
        if any(k in explanation for k in ["null", "empty", "zero", "none", "single element", "corner case", "edge case", "empty array", "out of bounds", "indexoutofbound"]):
            evidence.append(f"Edge case / null handling cues: '{user_explanation}'")
            category = MistakeCategory.NULL_OR_EMPTY_INPUT_ERROR if any(k in explanation for k in ["null", "empty", "none"]) else MistakeCategory.EDGE_CASE_ERROR
            return (
                category,
                0.88,
                "Failure to guard against edge-case input values (empty collections, boundary limits).",
                evidence
            )

        # 9. Duplicate Handling Error
        if any(k in explanation for k in ["duplicate", "distinct", "unique", "counted twice", "set"]):
            evidence.append(f"Duplicate value management cues: '{user_explanation}'")
            return (
                MistakeCategory.DUPLICATE_HANDLING_ERROR,
                0.87,
                "Unhandled duplicate entries resulting in inaccurate frequency or double counting.",
                evidence
            )

        # 10. Default Fallback
        evidence.append(f"General logic flaw; result={result}")
        if user_explanation:
            evidence.append(f"Explanation: '{user_explanation}'")
        return (
            MistakeCategory.WRONG_LOGIC,
            0.75,
            f"Algorithmic decision logic did not satisfy expected assertions on {problem_title or 'problem'}.",
            evidence
        )
