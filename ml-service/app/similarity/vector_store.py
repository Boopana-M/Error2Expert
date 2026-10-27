import numpy as np
from typing import List, Dict, Any, Optional
import logging

try:
    import faiss
    HAS_FAISS = True
except Exception as e:
    HAS_FAISS = False
    logging.getLogger("codememory.vector_store").warning(
        f"FAISS unavailable ({e}). Activating high-performance NumPy dot-product fallback."
    )

from app.embeddings.embedder import EmbeddingService
from app.models.schemas import SimilarMistakeItem, MistakeCategory

logger = logging.getLogger("codememory.vector_store")

class FaissVectorMemory:
    """
    Longitudinal Vector Memory for Coding Mistakes using FAISS (or NumPy dot-product fallback).
    Stores and compares mistake semantic embeddings to identify recurring patterns.
    """

    def __init__(self, embedding_service: Optional[EmbeddingService] = None):
        self.embedder = embedding_service or EmbeddingService()
        self.dimension = self.embedder.dimension
        self.has_faiss = HAS_FAISS
        if self.has_faiss:
            try:
                self.index = faiss.IndexFlatIP(self.dimension)
            except Exception as e:
                logger.warning(f"Could not create faiss.IndexFlatIP ({e}), falling back to NumPy matrix.")
                self.has_faiss = False
                self.vectors = np.empty((0, self.dimension), dtype=np.float32)
        else:
            self.vectors = np.empty((0, self.dimension), dtype=np.float32)

        self.metadata: List[Dict[str, Any]] = []

    def count(self) -> int:
        if self.has_faiss:
            return self.index.ntotal
        return len(self.vectors)

    def add_mistake(
        self,
        mistake_id: str,
        text: str,
        category: MistakeCategory,
        problem_title: str,
        topics: Optional[List[str]] = None
    ) -> int:
        """
        Embeds mistake explanation and stores vector in FAISS/NumPy along with metadata.
        """
        vector = self.embedder.generate_embedding(text)
        vector_2d = np.expand_dims(vector, axis=0).astype(np.float32)

        if self.has_faiss:
            self.index.add(vector_2d)
        else:
            if len(self.vectors) == 0:
                self.vectors = vector_2d
            else:
                self.vectors = np.vstack([self.vectors, vector_2d])

        idx = len(self.metadata)
        meta = {
            "index": idx,
            "mistake_id": mistake_id,
            "text": text,
            "category": category,
            "problem_title": problem_title,
            "topics": topics or []
        }
        self.metadata.append(meta)
        return idx

    def search_similar(
        self,
        query_text: str,
        top_k: int = 5,
        threshold: float = 0.65
    ) -> List[SimilarMistakeItem]:
        """
        Searches vector store for semantic matches above the similarity threshold.
        Inner product on unit L2-normalized vectors is mathematically identical to cosine similarity.
        """
        total = self.count()
        if total == 0 or not query_text or not query_text.strip():
            return []

        query_vec = self.embedder.generate_embedding(query_text)
        k = min(top_k, total)

        if self.has_faiss:
            query_2d = np.expand_dims(query_vec, axis=0).astype(np.float32)
            distances, indices = self.index.search(query_2d, k)
            sim_list = distances[0]
            idx_list = indices[0]
        else:
            # Exact inner product (cosine similarity for unit-normalized vectors)
            sims = self.vectors @ query_vec
            sorted_indices = np.argsort(sims)[::-1][:k]
            sim_list = [sims[i] for i in sorted_indices]
            idx_list = sorted_indices

        matches: List[SimilarMistakeItem] = []
        for sim, idx in zip(sim_list, idx_list):
            if idx != -1 and sim >= threshold and idx < len(self.metadata):
                m = self.metadata[idx]
                matches.append(SimilarMistakeItem(
                    mistakeId=str(m["mistake_id"]),
                    text=m["text"],
                    category=m["category"],
                    similarity=round(float(sim), 4),
                    problemTitle=m["problem_title"],
                    topics=m["topics"]
                ))

        return matches
