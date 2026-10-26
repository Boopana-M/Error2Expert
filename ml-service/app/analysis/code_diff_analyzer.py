import difflib
import re
from typing import Dict, Any, List

class CodeDiffAnalyzer:
    """
    Advanced Feature 1 (Section 31 of CodeMemory_Project_Plan.md):
    Analyzes differences between wrong and accepted submissions to identify
    likely mistake correction patterns and pinpoint root causes.
    """

    def analyze_diff(self, wrong_code: str, accepted_code: str) -> Dict[str, Any]:
        if not wrong_code or not accepted_code:
            return {
                "diffSummary": "Insufficient code provided for difference analysis.",
                "detectedCorrectionPatterns": [],
                "diffLines": []
            }

        wrong_lines = wrong_code.splitlines(keepends=True)
        accepted_lines = accepted_code.splitlines(keepends=True)

        diff = list(difflib.unified_diff(
            wrong_lines, accepted_lines,
            fromfile='wrong_submission',
            tofile='accepted_solution',
            n=2
        ))

        patterns: List[str] = []
        removed_text = " ".join([l[1:] for l in diff if l.startswith('-') and not l.startswith('---')])
        added_text = " ".join([l[1:] for l in diff if l.startswith('+') and not l.startswith('+++')])

        # 1. Boundary / Pointer adjustment detection
        if re.search(r'mid\s*[-+]\s*1', added_text) and not re.search(r'mid\s*[-+]\s*1', removed_text):
            patterns.append("Added mid ± 1 offset to prevent binary search boundary lock")
        if re.search(r'<=\s*', added_text) and re.search(r'<\s*', removed_text):
            patterns.append("Adjusted search termination condition from strict inequality (<) to inclusive (<=)")

        # 2. Visited-state tracking detection
        if re.search(r'visited\[|\.add\(|seen\.', added_text) and not re.search(r'visited\[|\.add\(|seen\.', removed_text):
            patterns.append("Introduced visited tracking to eliminate cycle / infinite recursion in graph traversal")

        # 3. Guard conditions / Null checks
        if re.search(r'==\s*null|length\s*==\s*0|len\(|isEmpty', added_text):
            patterns.append("Added edge-case guard check for null/empty input")

        # 4. Loop nesting / complexity modification
        wrong_for_count = len(re.findall(r'\b(for|while)\b', wrong_code))
        acc_for_count = len(re.findall(r'\b(for|while)\b', accepted_code))
        if wrong_for_count > acc_for_count:
            patterns.append(f"Reduced loop nesting from {wrong_for_count} to {acc_for_count} loops (Time complexity optimization)")

        summary = f"Detected {len(patterns)} correction pattern(s) between failed and accepted attempts."
        if patterns:
            summary += f" Key change: {patterns[0]}."

        return {
            "diffSummary": summary,
            "detectedCorrectionPatterns": patterns,
            "diffLines": [l.rstrip('\r\n') for l in diff[:30]]  # Preview up to 30 diff lines
        }
