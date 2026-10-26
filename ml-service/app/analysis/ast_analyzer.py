import ast
import re
from typing import Dict, Any, List

class AstAnalyzer:
    """
    Advanced Feature 2 (Section 31 of CodeMemory_Project_Plan.md):
    Performs Abstract Syntax Tree and structural analysis to detect architectural
    mistake patterns (nested loops, missing base cases, boundary update structures).
    """

    def analyze_code_structure(self, code: str, language: str = "java") -> Dict[str, Any]:
        if not code or not code.strip():
            return {
                "maxNestingDepth": 0,
                "complexityRisk": "LOW",
                "detectedPatterns": [],
                "structuralIssues": []
            }

        lang = language.lower()
        if "py" in lang:
            return self._analyze_python_ast(code)
        else:
            return self._analyze_c_family_structure(code)

    def _analyze_python_ast(self, code: str) -> Dict[str, Any]:
        try:
            tree = ast.parse(code)
        except SyntaxError as e:
            return {
                "maxNestingDepth": 1,
                "complexityRisk": "UNKNOWN",
                "detectedPatterns": ["SyntaxError during AST parsing"],
                "structuralIssues": [f"Parsing failed: {str(e)}"]
            }

        max_depth = 0
        patterns = []
        issues = []

        class LoopVisitor(ast.NodeVisitor):
            def __init__(self):
                self.current_depth = 0
                self.max_depth = 0

            def visit_For(self, node):
                self.current_depth += 1
                self.max_depth = max(self.max_depth, self.current_depth)
                self.generic_visit(node)
                self.current_depth -= 1

            def visit_While(self, node):
                self.current_depth += 1
                self.max_depth = max(self.max_depth, self.current_depth)
                self.generic_visit(node)
                self.current_depth -= 1

        visitor = LoopVisitor()
        visitor.visit(tree)
        max_depth = visitor.max_depth

        if max_depth >= 2:
            patterns.append(f"Nested loop structure detected (depth {max_depth})")
            issues.append(f"Risk of O(N^{max_depth}) polynomial runtime causing TLE")

        risk = "HIGH" if max_depth >= 2 else "LOW"

        return {
            "maxNestingDepth": max_depth,
            "complexityRisk": risk,
            "detectedPatterns": patterns,
            "structuralIssues": issues
        }

    def _analyze_c_family_structure(self, code: str) -> Dict[str, Any]:
        # Track block braces and loop keywords to calculate nesting depth
        lines = code.splitlines()
        max_depth = 0
        current_loop_depth = 0
        patterns = []
        issues = []

        # Find loop nestings
        for line in lines:
            trimmed = line.strip()
            if re.match(r'^(for|while)\s*\(', trimmed):
                current_loop_depth += 1
                max_depth = max(max_depth, current_loop_depth)
            if '}' in trimmed and current_loop_depth > 0:
                current_loop_depth = max(0, current_loop_depth - trimmed.count('}'))

        if max_depth >= 2:
            patterns.append(f"Nested loop structure detected (nesting depth {max_depth})")
            issues.append("Nested iteration indicates potential O(N^2) complexity vulnerability")

        # Binary search boundary pattern checks
        if re.search(r'while\s*\(\s*(low|left)\s*<\s*(high|right)\s*\)', code):
            patterns.append("Strict loop boundary: (left < right)")
            if re.search(r'(high|right)\s*=\s*mid\b(?!\s*-\s*1)', code):
                patterns.append("Search boundary assignment: high = mid without decrement")
                issues.append("Potential infinite loop if (left < right) and high = mid without offset")

        # Missing visited state in DFS/BFS
        if re.search(r'\b(dfs|bfs)\b', code, re.I) and not re.search(r'visited|seen', code, re.I):
            patterns.append("Graph traversal method detected without visited tracking variable")
            issues.append("Omission of visited state structure can cause recursion cycles and stack overflow")

        risk = "HIGH" if max_depth >= 2 or issues else "LOW"

        return {
            "maxNestingDepth": max_depth,
            "complexityRisk": risk,
            "detectedPatterns": patterns,
            "structuralIssues": issues
        }
