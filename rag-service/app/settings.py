from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    embedding_enabled: bool = Field(default=False, alias="EMBEDDING_ENABLED")
    embedding_base_url: str = Field(default="https://apie.zhisuaninfo.com/v1", alias="EMBEDDING_BASE_URL")
    embedding_api_key: str = Field(default="", alias="EMBEDDING_API_KEY")
    embedding_model: str = Field(default="bge-m3", alias="EMBEDDING_MODEL")
    embedding_timeout_seconds: int = Field(default=30, alias="EMBEDDING_TIMEOUT_SECONDS")
    vector_cache_path: Path = Field(default=Path("data/rule-vectors-bge-m3.json"), alias="VECTOR_CACHE_PATH")


@lru_cache
def get_settings() -> Settings:
    return Settings()
