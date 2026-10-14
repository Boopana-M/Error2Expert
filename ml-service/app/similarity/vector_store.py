import faiss
import numpy as np
from typing import List, Dict, Any, Optional
from app.embeddings.embedder import EmbeddingService
from app.models.schemas import SimilarMistakeItem, MistakeCategory

class FaissVectorMemory:
    """
    Longitudinal Vector Memory for Coding Mistakes using FAISS.
    Stores and compares mistake semantic embeddings to identify recurring patterns.
    """

    def __init__(self, embedding_service: Optional[EmbeddingService] = None):
        self.embedder = embedding_service or EmbeddingService()
        self.dimension = self.embedder.dimension
        # IndexFlatIP calculates inner product, which equals cosine similarity for normalized vectors
        self.index = faiss.IndexFlatIP(self.dimension)
        self.metadata: List[Dict[str, Any]] = []

    def count(self) -> int:
        return self.index.ntotal

    def add_mistake(
        self,
        mistake_id: str,
        text: str,
        category: MistakeCategory,
        problem_title: str,
        topics: Optional[List[str]] = None
    ) -> int:
        """
        Embeds mistake explanation and stores vector in FAISS along with metadata.
        """
        vector = self.embedder.generate_embedding(text)
        vector_2d = np.expand_dims(vector, axis=0).astype(np.float32)

        self.index.add(vector_2d)
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
        threshold: float = 0.70
    ) -> List[SimilarMistakeItem]:
        """
        Searches FAISS vector store for semantic matches above the similarity threshold.
        """
        if self.index.ntotal == 0 or not query_text or not query_text.strip():
            return []

        query_vec = self.embedder.generate_embedding(query_text)
        query_2d = np.expand_dims(query_vec, axis=0).astype(np.float32)

        k = min(top_k, self.index.ntotal)
        distances, indices = self.index.search(query_2d, k)

        matches: List[SimilarMistakeItem] = []
        for sim, idx in zip(distances[0], indices[0]):
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
