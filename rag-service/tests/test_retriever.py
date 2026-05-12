from app.embeddings import VectorCache
from app.models import RetrieveRequest
from app.retriever import HybridRuleRetriever
from app.rules import base_rule_documents


class NoopEmbedder:
    model = "none"
    enabled = False

    def embed(self, text: str):
        return None


class FakeEmbedder:
    model = "fake-bge"
    enabled = True

    def embed(self, text: str):
        normalized = text.lower()
        if "guardian" in normalized or "minor" in normalized or "未成年" in normalized or "监护" in normalized:
            return [1.0, 0.0]
        if "travel" in normalized or "旅行" in normalized:
            return [0.0, 1.0]
        return [0.2, 0.2]


def test_retrieves_guardian_consent_rule_by_keyword(tmp_path):
    retriever = HybridRuleRetriever(
        base_rule_documents(),
        NoopEmbedder(),
        VectorCache(tmp_path / "vectors.json"),
    )

    result = retriever.retrieve(
        RetrieveRequest(query="申请人 17 岁 未成年人 缺少 监护 同意书 和 住宿 证明", top_k=3)
    )

    assert result.evidence
    assert result.evidence[0].chunkId == "DOC-003"
    assert result.evidence[0].matchMode == "keyword"
    assert result.status.mode == "keyword-only"


def test_combines_vector_and_keyword_scores(tmp_path):
    retriever = HybridRuleRetriever(
        base_rule_documents(),
        FakeEmbedder(),
        VectorCache(tmp_path / "vectors.json"),
    )

    result = retriever.retrieve(
        RetrieveRequest(query="minor applicant needs authorisation and guardian arrangement", top_k=3)
    )

    assert result.status.vectorAvailable is True
    assert result.evidence[0].chunkId == "DOC-003"
    assert result.evidence[0].vectorScore > 0.8
    assert "vector" in result.evidence[0].matchMode
