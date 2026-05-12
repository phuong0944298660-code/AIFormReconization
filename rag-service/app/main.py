from fastapi import FastAPI

from app.models import RetrieveRequest, RetrieveResponse
from app.retriever import HybridRuleRetriever
from app.settings import Settings

settings = Settings()
retriever = HybridRuleRetriever.from_settings(settings)

app = FastAPI(title="ID995A RAG Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "id995a-rag-service"}


@app.post("/rag/retrieve", response_model=RetrieveResponse)
def retrieve(request: RetrieveRequest) -> RetrieveResponse:
    return retriever.retrieve(request)
