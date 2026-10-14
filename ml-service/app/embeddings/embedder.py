from typing import List
import numpy as np

class EmbeddingService:
    """
    Embedding service using sentence-transformers (all-MiniLM-L6-v2).
    Generates 384-dimensional dense semantic vectors normalized for cosine similarity.
    """

    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        self.model_name = model_name
        self._model = None
        self.dimension = 384

    @property
    def model(self):
        if self._model is None:
            from sentence_transformers import SentenceTransformer
            self._model = SentenceTransformer(self.model_name)
        return self._model

    def generate_embedding(self, text: str) -> np.ndarray:
        """
        Generates a 1D unit-normalized float32 numpy vector for a single string.
        """
        if not text or not text.strip():
            # Return zero vector if text is empty
            return np.zeros(self.dimension, dtype=np.float32)

        embedding = self.model.encode(text, convert_to_numpy=True, normalize_embeddings=True)
        return embedding.astype(np.float32)

    def generate_embeddings(self, texts: List[str]) -> np.ndarray:
        """
        Generates a 2D matrix (N, 384) of unit-normalized embeddings.
        """
        if not texts:
            return np.empty((0, self.dimension), dtype=np.float32)

        embeddings = self.model.encode(texts, convert_to_numpy=True, normalize_embeddings=True)
        return embeddings.astype(np.float32)
