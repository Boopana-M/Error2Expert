from typing import List, Optional
from app.similarity.vector_store import FaissVectorMemory
from app.models.schemas import MistakeCategory, SimilarMistakeItem, PatternDetectionResponse

class PatternDetector:
    """
    Longitudinal pattern detection engine.
    Analyzes new mistakes against vector memory to identify recurring weaknesses across problems.
    """

    def __init__(self, vector_memory: FaissVectorMemory):
        self.memory = vector_memory

    def detect_pattern(
        self,
        new_mistake_text: str,
        category: MistakeCategory,
        problem_title: str,
        topics: Optional[List[str]] = None,
        user_id: Optional[str] = None
    ) -> PatternDetectionResponse:
        # Calibrated threshold (0.52) to capture nuanced explanations and paraphrasing
        matches = self.memory.search_similar(
            query_text=new_mistake_text,
            top_k=8,
            threshold=0.52,
            user_id=user_id
        )

        # Relevant matches: same mistake category with meaningful semantic alignment or high similarity
        relevant_matches = [
            m for m in matches
            if (m.category == category and m.similarity >= 0.52) or m.similarity >= 0.72
        ]
        recurrence_count = len(relevant_matches) + 1  # Includes current mistake

        # Check cross-problem recurrence
        problem_set = {m.problemTitle for m in relevant_matches}
        problem_set.add(problem_title)
        is_cross_problem = len(problem_set) >= 2
        is_recurring = recurrence_count >= 2

        category_name = category.value.replace('_', ' ').title()

        if is_recurring and is_cross_problem:
            summary = (
                f"Strong recurring {category_name} weakness detected. "
                f"Observed {recurrence_count} times across {len(problem_set)} different problems: "
                f"{', '.join(sorted(problem_set))}."
            )
        elif is_recurring:
            summary = (
                f"Emerging {category_name} weakness pattern. "
                f"Repeated {recurrence_count} times on '{problem_title}'."
            )
        else:
            summary = f"First observed occurrence of {category_name} on '{problem_title}'."

        return PatternDetectionResponse(
            recurring=is_recurring,
            recurrenceCount=recurrence_count,
            crossProblem=is_cross_problem,
            patternSummary=summary,
            matches=relevant_matches
        )
