from pydantic import BaseModel, Field


class RetrieveRequest(BaseModel):
    query: str = ""
    top_k: int = Field(default=8, ge=1, le=20)
    filters: dict[str, str] = Field(default_factory=dict)


class RuleEvidence(BaseModel):
    chunkId: str
    title: str
    content: str
    source: str
    ruleIds: list[str]
    keywordScore: float
    vectorScore: float
    hybridScore: float
    matchMode: str


class RetrievalStatus(BaseModel):
    mode: str
    vectorEnabled: bool
    vectorAvailable: bool
    embeddingModel: str
    candidateCount: int
    messages: list[str]


class RetrieveResponse(BaseModel):
    evidence: list[RuleEvidence]
    status: RetrievalStatus
