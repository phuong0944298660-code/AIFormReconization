import hashlib
import json
from pathlib import Path
from typing import Protocol

import httpx

from app.settings import Settings


class EmbeddingClient(Protocol):
    model: str
    enabled: bool

    def embed(self, text: str) -> list[float] | None:
        ...


class OpenAICompatibleEmbeddingClient:
    def __init__(self, settings: Settings):
        self.model = settings.embedding_model
        self.enabled = settings.embedding_enabled and bool(settings.embedding_api_key)
        self._settings = settings

    def embed(self, text: str) -> list[float] | None:
        if not self.enabled or not text.strip():
            return None
        url = self._settings.embedding_base_url.rstrip("/") + "/embeddings"
        payload = {"model": self._settings.embedding_model, "input": [text]}
        headers = {"Authorization": f"Bearer {self._settings.embedding_api_key}"}
        try:
            with httpx.Client(timeout=self._settings.embedding_timeout_seconds) as client:
                response = client.post(url, json=payload, headers=headers)
                response.raise_for_status()
                data = response.json()
                embedding = data["data"][0]["embedding"]
                return [float(value) for value in embedding]
        except Exception:
            return None


class VectorCache:
    def __init__(self, path: Path):
        self._path = path
        self._vectors: dict[str, list[float]] = {}
        self._loaded = False

    def get_or_embed(self, key_text: str, model: str, embedder: EmbeddingClient) -> list[float] | None:
        if not embedder.enabled:
            return None
        self._load()
        key = self._key(model, key_text)
        if key in self._vectors:
            return self._vectors[key]
        vector = embedder.embed(key_text)
        if vector is None:
            return None
        self._vectors[key] = vector
        self._save()
        return vector

    def _load(self) -> None:
        if self._loaded:
            return
        self._loaded = True
        if not self._path.exists():
            return
        try:
            self._vectors = json.loads(self._path.read_text(encoding="utf-8"))
        except Exception:
            self._vectors = {}

    def _save(self) -> None:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        self._path.write_text(json.dumps(self._vectors), encoding="utf-8")

    def _key(self, model: str, text: str) -> str:
        digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        return f"{model}:{digest}"
