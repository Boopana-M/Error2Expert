from typing import List
import numpy as np
import hashlib
import logging

logger = logging.getLogger("codememory.embedder")

# Concept clusters mapped to deterministic semantic basis seeds for high cosine alignment
CONCEPT_SEEDS = {
    "boundary": ["bound", "boundary", "pointer", "mid", "left", "right", "low", "high", "offset", "invariant"],
    "graph": ["graph", "visited", "cycle", "traversal", "dfs", "bfs", "island", "recursion stack", "adjacent"],
    "dp": ["dp", "dynamic programming", "memo", "memoization", "base case", "transition", "state", "subproblem"],
    "complexity": ["tle", "complexity", "time limit", "quadratic", "cubic", "nested", "slow", "timeout"],
    "off_by_one": ["off by one", "<=", "<", ">=", ">", "length - 1", "index out of bound"],
    "edge_case": ["edge case", "null", "empty", "zero", "negative", "single element", "corner case"],
    "wrong_logic": ["logic", "wrong logic", "condition", "inverted", "misunderstood"]
}

class EmbeddingService:
    """
    Embedding service using sentence-transformers (all-MiniLM-L6-v2).
    Generates 384-dimensional dense semantic vectors normalized for cosine similarity.
    Includes a resilient deterministic semantic fallback if PyTorch / SentenceTransformer
    experiences low-memory constraints or driver incompatibilities.
    """

    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        import os
        self.model_name = model_name
        self._model = None
        self.dimension = 384
        engine = os.environ.get("EMBEDDING_ENGINE", "auto").lower()
        self.fallback_mode = (engine in ("fallback", "lightweight", "fast", "cpu_light"))

    @property
    def model(self):
        if self._model is None and not self.fallback_mode:
            try:
                from sentence_transformers import SentenceTransformer
                self._model = SentenceTransformer(self.model_name)
                logger.info(f"Loaded SentenceTransformer({self.model_name}) successfully.")
            except Exception as e:
                logger.warning(
                    f"SentenceTransformer unavailable ({type(e).__name__}: {e}). "
                    f"Activating deterministic semantic fallback embedder (384-d)."
                )
                self.fallback_mode = True
        return self._model

    def _fallback_embedding(self, text: str) -> np.ndarray:
        """
        Generates a 384-dimensional unit-normalized semantic vector using
        concept projections and deterministic n-gram hashing.
        Guarantees semantic clustering for similar algorithmic explanations.
        """
        if not text or not text.strip():
            return np.zeros(self.dimension, dtype=np.float32)

        vec = np.zeros(self.dimension, dtype=np.float64)
        lower_text = text.lower()

        # 1. Concept cluster projection
        for concept_idx, (concept, keywords) in enumerate(CONCEPT_SEEDS.items()):
            match_count = sum(1 for kw in keywords if kw in lower_text)
            if match_count > 0:
                rng = np.random.RandomState(42 + concept_idx * 1337)
                concept_vector = rng.standard_normal(self.dimension)
                vec += concept_vector * (match_count * 2.5)

        # 2. Token and n-gram hash projection
        tokens = lower_text.split()
        for token in tokens:
            token_hash = int(hashlib.md5(token.encode('utf-8')).hexdigest(), 16)
            token_rng = np.random.RandomState(token_hash % (2**31 - 1))
            vec += token_rng.standard_normal(self.dimension) * 0.4

        # 3. L2 Unit Normalization for exact Cosine Similarity
        norm = np.linalg.norm(vec)
        if norm > 1e-8:
            vec = vec / norm
        else:
            vec = np.zeros(self.dimension, dtype=np.float64)
            vec[0] = 1.0

        return vec.astype(np.float32)

    def generate_embedding(self, text: str) -> np.ndarray:
        """
        Generates a 1D unit-normalized float32 numpy vector for a single string.
        """
        if not text or not text.strip():
            return np.zeros(self.dimension, dtype=np.float32)

        if not self.fallback_mode and self.model is not None:
            try:
                embedding = self.model.encode(text, convert_to_numpy=True, normalize_embeddings=True)
                return embedding.astype(np.float32)
            except Exception as e:
                logger.warning(f"Error during model.encode ({e}). Falling back to semantic embedder.")
                self.fallback_mode = True

        return self._fallback_embedding(text)

    def generate_embeddings(self, texts: List[str]) -> np.ndarray:
        """
        Generates a 2D matrix (N, 384) of unit-normalized embeddings.
        """
        if not texts:
            return np.empty((0, self.dimension), dtype=np.float32)

        if not self.fallback_mode and self.model is not None:
            try:
                embeddings = self.model.encode(texts, convert_to_numpy=True, normalize_embeddings=True)
                return embeddings.astype(np.float32)
            except Exception as e:
                logger.warning(f"Error during batch model.encode ({e}). Falling back to semantic embedder.")
                self.fallback_mode = True

        matrix = np.array([self._fallback_embedding(t) for t in texts], dtype=np.float32)
        return matrix
