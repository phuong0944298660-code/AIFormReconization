import math
import re
import unicodedata

from langchain_core.documents import Document

from app.embeddings import EmbeddingClient, OpenAICompatibleEmbeddingClient, VectorCache
from app.models import RetrievalStatus, RetrieveRequest, RetrieveResponse, RuleEvidence
from app.rules import base_rule_documents
from app.settings import Settings


class HybridRuleRetriever:
    def __init__(
        self,
        documents: list[Document],
        embedder: EmbeddingClient,
        vector_cache: VectorCache,
    ):
        self._documents = documents
        self._embedder = embedder
        self._vector_cache = vector_cache

    @classmethod
    def from_settings(cls, settings: Settings) -> "HybridRuleRetriever":
        return cls(
            documents=base_rule_documents(),
            embedder=OpenAICompatibleEmbeddingClient(settings),
            vector_cache=VectorCache(settings.vector_cache_path),
        )

    def retrieve(self, request: RetrieveRequest) -> RetrieveResponse:
        query = request.query or ""
        query_vector = self._embedder.embed(query)
        scored = [self._score(document, query, query_vector) for document in self._documents]
        evidence = [
            item
            for item in sorted(scored, key=lambda item: item.hybridScore, reverse=True)
            if item.hybridScore > 0
        ][: request.top_k]

        messages: list[str]
        if not self._embedder.enabled:
            messages = ["Embedding 未启用，当前只使用关键词检索。"]
            mode = "keyword-only"
        elif query_vector is None:
            messages = ["查询向量生成失败，已回退关键词检索。"]
            mode = "keyword-only"
        else:
            messages = [f"已使用 {self._embedder.model} 执行向量检索，并与关键词评分合并。"]
            mode = "hybrid-vector-keyword"

        return RetrieveResponse(
            evidence=evidence,
            status=RetrievalStatus(
                mode=mode,
                vectorEnabled=self._embedder.enabled,
                vectorAvailable=query_vector is not None,
                embeddingModel=self._embedder.model,
                candidateCount=len(self._documents),
                messages=messages,
            ),
        )

    def _score(self, document: Document, query: str, query_vector: list[float] | None) -> RuleEvidence:
        keyword_score = keyword_score_for(document, query)
        vector_score = 0.0
        if query_vector is not None:
            document_vector = self._vector_cache.get_or_embed(document.page_content, self._embedder.model, self._embedder)
            if document_vector is not None:
                vector_score = max(0.0, cosine_similarity(query_vector, document_vector))
        hybrid_score = (0.55 * keyword_score + 0.45 * vector_score) if query_vector is not None else keyword_score

        if keyword_score > 0 and vector_score > 0:
            match_mode = "keyword+vector"
        elif vector_score > 0:
            match_mode = "vector"
        else:
            match_mode = "keyword"

        metadata = document.metadata
        return RuleEvidence(
            chunkId=str(metadata["chunk_id"]),
            title=str(metadata["title"]),
            content=str(metadata["content"]),
            source=str(metadata["source"]),
            ruleIds=list(metadata["rule_ids"]),
            keywordScore=round(keyword_score, 4),
            vectorScore=round(vector_score, 4),
            hybridScore=round(hybrid_score, 4),
            matchMode=match_mode,
        )


def keyword_score_for(document: Document, query: str) -> float:
    normalized_query = normalize(query)
    if not normalized_query:
        return 0.0
    keywords = [normalize(str(value)) for value in document.metadata.get("keywords", [])]
    content = normalize(document.page_content)
    hits: set[str] = set()
    for keyword in keywords:
        if keyword and keyword in normalized_query:
            hits.add(keyword)
    for token in re.split(r"[^\w\u4e00-\u9fff]+", normalized_query):
        if len(token) >= 3 and token in content:
            hits.add(token)
    if not hits:
        return 0.0
    return min(1.0, len(hits) / max(4.0, len(keywords) * 0.45))


def normalize(value: str) -> str:
    return unicodedata.normalize("NFKC", value or "").lower().strip()


def cosine_similarity(left: list[float], right: list[float]) -> float:
    length = min(len(left), len(right))
    if length == 0:
        return 0.0
    dot = sum(left[index] * right[index] for index in range(length))
    left_norm = math.sqrt(sum(left[index] * left[index] for index in range(length)))
    right_norm = math.sqrt(sum(right[index] * right[index] for index in range(length)))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return dot / (left_norm * right_norm)
