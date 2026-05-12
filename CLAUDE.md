# AIFormReconization 项目协作说明

## 项目定位

本项目是一个面向香港入境事务处学生入境申请场景的审核 Demo，首个目标表格是 `ID995A` 来港就读申请表（申请人填写）。

用户在 Web 页面上传已填写的 PDF 后，系统通过 OCR（光学字符识别）和 PDF 文档解析提取表格内容，再结合官方规则知识库进行 Hybrid Retrieval（混合检索：向量检索 + 关键词检索），最终输出预审结论、失败原因和文件快照标注。

本系统只作为递交前预审辅助，不代表香港入境事务处的最终审批结果。

## 主要用户与使用场景

- 客户背景：香港入境事务处。
- 使用对象：内部审核人员、Demo 演示人员或业务验收人员。
- 核心目标：快速判断上传的 `ID995A` 是否具备递交完整性，并直观看到具体不通过位置。

## 用户偏好与硬性要求

- 前端代码放在 `frontend`。
- Java 后端接口服务放在 `backend`。
- RAG（Retrieval-Augmented Generation，检索增强生成）必须使用 Python 实现。
- Python RAG 优先使用 LangChain（LLM 应用开发框架）；如果后续流程复杂，再使用 LangGraph（有状态 Agent/工作流框架）。
- Java 只负责 API（应用程序接口）、文件上传、服务编排和结果聚合。
- 不要在 Java 中实现 RAG 核心逻辑。
- 前端框架使用 Vue。
- 前端图标使用 SVG（可缩放矢量图形）。
- OCR 使用开源 OCR 框架。
- 审核规则必须尽量来源于官方资料，并保留来源引用。
- 审核规则需要向量化存储。
- 文档上传后需要进行 Hybrid Retrieval（混合检索：向量 + 关键词匹配）。
- 向量模型使用 `bge-m3`。
- 判断结论可以使用指定 LLM（Large Language Model，大语言模型），但不能只依赖 LLM 做最终规则判断。
- API Key（接口密钥）不得写入代码、Markdown 文档、日志或前端产物。
- 所有密钥必须通过 Environment Variables（环境变量）注入。

## 外部模型配置

项目使用 OpenAI-compatible API（兼容 OpenAI 接口格式的模型服务），通过环境变量配置：

```env
LLM_BASE_URL=https://apie.zhisuaninfo.com/v1
LLM_MODEL=Qwen3.6-35B-A3B
LLM_API_KEY=...

EMBEDDING_BASE_URL=https://apie.zhisuaninfo.com/v1
EMBEDDING_MODEL=bge-m3
EMBEDDING_API_KEY=...
```

当前连通性记录：

- `bge-m3` Embedding（向量嵌入）接口已验证可用，返回 1024 维向量。
- `Qwen3.6-35B-A3B` Chat Completion（对话补全）接口已验证可用。
- `Qwen3.6-27b` 可作为备用模型名，但最近一次测试返回 upstream error（上游服务错误）。
- `Qwen3-Omni-30B-A3B-Instruct` 已验证可连通，短提示响应干净；但在真实审核长提示下容易输出 Markdown（标记语言）、emoji（表情符号）和建议性内容，因此后端必须保留 LLM 输出格式校验与确定性 fallback（回退）。
- 如果模型接口请求失败并表现为连接异常，需要提醒用户检查 VPN 是否已连接。

## 推荐目录结构

```text
AIFormReconization/
  frontend/                 Vue 前端：上传、审核结果、PDF 快照标注
  backend/                  Java Spring Boot 后端：API 和服务编排
  rag-service/              Python RAG 服务：规则向量化、混合检索
  official_templates/
    students/               官方 ID995A、ID995B、ID996 PDF 和规则文档
  docs/                     需求、架构、规则和实现说明
```

## 服务职责划分

### Frontend（前端）

前端负责用户可见的 Demo 流程：

- 上传已填写的 `ID995A` PDF。
- 收集单份 PDF 无法证明的补充材料信息，例如取录信、旅行证件副本、经济证明、监护同意书、住宿证明等。
- 展示整体通过/不通过结论。
- 分组展示不通过理由。
- 展示 PDF 页面快照。
- 使用 SVG Overlay（SVG 覆盖层）在 PDF 快照上绘制红圈、连线和标签。
- 展示 RAG 命中的官方规则证据。
- 展示 LLM 生成的中文审核结论。

### Backend（Java 后端）

Java 后端负责接口服务和编排：

- 提供上传和审核 API。
- 接收前端上传的 PDF 和 metadata（元数据）。
- 提取 PDF text layer（PDF 文本层）。
- 渲染 PDF page snapshot（页面快照）。
- 调用 OCR adapter（OCR 适配器），在配置开启时执行 OCR。
- 调用 Python RAG 服务获取相关官方规则证据。
- 执行 deterministic rule engine（确定性规则引擎）判断硬性阻断项。
- 可选调用 LLM 生成自然语言结论。
- 聚合 findings（审核发现）、annotations（页面标注）、retrieved rules（检索规则）、OCR 状态和官方来源后返回给前端。

Java 不负责以下内容：

- 规则向量化。
- 向量索引存储。
- Hybrid Retrieval（混合检索）核心逻辑。
- LangChain 或 LangGraph 工作流。

### RAG Service（Python 检索增强服务）

Python RAG 服务负责规则知识库和检索：

- 读取 `official_templates/students` 下的官方资料。
- 清洗和标准化规则文本。
- 将规则切分为 chunk（文本块）。
- 使用 `bge-m3` 生成 embedding（向量嵌入）。
- 将向量写入本地 vector store（向量库）。
- 执行 Hybrid Retrieval（混合检索：向量相似度 + 关键词/BM25）。
- 返回命中的规则证据、来源、规则 ID、匹配模式和分数。

推荐 Demo 技术栈：

- Python 3.11+
- FastAPI（Python Web API 框架）
- LangChain（RAG 组件和链式编排框架）
- FAISS 或 Chroma（本地向量库）
- BM25（关键词相关性排序算法）或自定义 keyword scoring（关键词评分）

只有在流程需要明确状态流转、重试、人机协同或多 Agent 编排时，才引入 LangGraph。

## 审核主流程

1. 用户在前端上传已填写 PDF，并勾选补充材料清单。
2. Java 后端解析上传内容：
   - PDF 文本层。
   - OCR 识别文本。
   - 字段填写信号。
   - 页面快照。
3. Java 后端构造 retrieval query（检索查询），内容包括：
   - 文档抽取文本。
   - 缺失字段信号。
   - 用户填写的 metadata。
   - 确定性规则初步 findings。
4. Java 后端调用 Python RAG 服务。
5. Python RAG 服务返回相关官方规则证据。
6. Java 后端运行确定性规则引擎，生成硬性阻断项。
7. Java 后端可选调用 LLM，基于规则发现和检索证据生成中文结论。
8. 前端展示：
   - 整体结论。
   - 不通过理由。
   - 命中的官方规则。
   - PDF 快照上的红圈、连线和标签标注。

## 官方规则来源范围

初始规则来源包括：

- `official_templates/students/ID995A_来港就读申请表_申请人.pdf`
- `official_templates/students/ID995B_来港就读保证人表格.pdf`
- `official_templates/students/ID-C-996_来港就读入境指南_中文.pdf`
- `official_templates/students/ID-E-996_Guidebook_for_Entry_for_Study_English.pdf`
- 香港入境事务处 `ID995A` 表格页面。
- 香港入境事务处来港就读签证/进入许可服务页面。

规则知识库必须保留 source attribution（来源归属），便于审核结果解释和后续验收。

## 初始规则分类

规则知识库应至少覆盖以下类别：

- 申请人个人资料完整性。
- 旅行证件资料和副本。
- 近照要求。
- 拟抵港日期和拟在港逗留时间。
- 在港就读学校名称、地址、年级和课程。
- 学历或专业资格资料。
- 预计生活开支和经济能力。
- 过去 12 个月短期课程资料。
- 声明签署和日期。
- 18 岁以下申请人的监护同意书和住宿证明。
- 不同保证人类型对应的材料。
- 内地中国居民递交路径。
- 随行受养人和乙部材料。
- 非中文或英文文件的译本要求。

## Deterministic Rules、RAG 与 LLM 的职责边界

Deterministic Rules（确定性规则）负责判断：

- 必填字段是否缺失。
- 必要随附材料是否缺失。
- 条件触发材料是否缺失。
- 是否存在硬性阻断项。
- 页面标注应该定位到哪个字段或区域。
- 整体预审结果是否为不通过。

RAG（检索增强生成）负责：

- 从官方规则知识库中找出与当前文档最相关的规则证据。
- 提供可引用的规则 chunk（文本块）。
- 给出检索分数、来源和规则 ID。

LLM（大语言模型）负责：

- 将确定性 findings 和 RAG 证据组织成自然、清晰的中文结论。
- 分点总结不通过理由。
- 提升解释可读性。

LLM 不得负责：

- 编造未提供的事实。
- 免除官方要求的材料。
- 覆盖确定性规则引擎给出的硬性阻断项。
- 单独决定最终通过/不通过。

## Java API 设计

### `POST /api/review`

请求：

- `file`：已填写 PDF。
- `metadata`：JSON 字符串，包含申请人年龄、保证人类型、补充材料勾选项等。

响应建议字段：

- `passed`：是否通过预审。
- `verdict`：整体结论。
- `overallReasons`：整体不通过理由。
- `findings`：规则引擎发现。
- `annotations`：页面标注数据。
- `extractedFields`：字段抽取状态。
- `pageSnapshots`：PDF 页面快照。
- `engineStatus`：OCR 和解析状态。
- `retrievedRules`：RAG 命中的规则证据。
- `retrievalStatus`：检索状态。
- `llmConclusion`：LLM 生成结论。
- `sources`：官方来源。

## Python RAG API 设计

### `POST /rag/retrieve`

请求示例：

```json
{
  "query": "extracted text and field signals",
  "top_k": 8,
  "filters": {
    "form": "ID995A",
    "scenario": "student-entry"
  }
}
```

响应示例：

```json
{
  "evidence": [
    {
      "chunk_id": "DOC-003",
      "title": "18 岁以下申请人的监护和住宿材料",
      "content": "申请人不足 18 岁时，父母须授权保证人或在港亲友作为监护人，并提交同意书和住宿安排证明。",
      "source": "ID(E)996",
      "rule_ids": ["DOC-003-GUARDIAN-CONSENT", "DOC-003-ACCOMMODATION"],
      "keyword_score": 0.8,
      "vector_score": 0.9,
      "hybrid_score": 0.86,
      "match_mode": "keyword+vector"
    }
  ],
  "status": {
    "mode": "hybrid",
    "embedding_model": "bge-m3",
    "vector_available": true
  }
}
```

## 安全与密钥处理

- 不得提交任何 API Key。
- 不得把密钥暴露给前端。
- Java 后端和 Python RAG 服务必须从环境变量读取密钥。
- 日志不得打印 Authorization header（授权请求头）。
- 日志不得打印完整上传 PDF 内容。
- 上传 PDF 应视为敏感个人资料。
- Demo 默认不持久化上传 PDF。
- 如后续需要持久化，必须补充 retention policy（保留策略）和 deletion policy（删除策略）。

## 开发命令

一键启动 Demo：

```bash
run-demo.cmd
```

服务地址：

- Frontend（前端）：`http://127.0.0.1:5173`
- Backend（后端）：`http://127.0.0.1:8080/api/health`
- RAG Service（检索服务）：`http://127.0.0.1:8090/health`

前端：

```bash
cd frontend
npm install
npm run dev
npm run build
```

Java 后端：

```bash
cd backend
mvn test
mvn spring-boot:run
```

Python RAG 服务目标命令：

```bash
cd rag-service
python -m venv .venv312
.venv312\Scripts\pip install -r requirements.txt
.venv312\Scripts\python -m uvicorn app.main:app --host 127.0.0.1 --port 8090
```

## 测试要求

- 对确定性规则使用 TDD（Test-Driven Development，测试驱动开发）。
- Unit Test（单元测试）覆盖规则引擎硬性阻断项。
- Unit Test 覆盖 RAG chunking（规则切块）和 hybrid ranking（混合排序）。
- Integration Test（集成测试）覆盖 Java 后端与 Python RAG 服务接口契约。
- Component Test（组件测试）覆盖前端结果渲染和标注展示。
- 至少保留一个通过路径样例和一个不通过路径样例。

## 当前实现状态说明

- 当前项目已有可运行的 Vue 前端、Java Spring Boot 后端和 Python FastAPI RAG 服务。
- 已有官方资料目录 `official_templates/students`。
- 已生成 `ID995A_审核规则.html`。
- RAG 核心已迁移到 `rag-service` Python 服务，Java 只通过 HTTP 调用检索服务。
- `rag-service/data/rule-vectors-bge-m3.json` 是本地向量缓存，包含官方规则 chunk 的 `bge-m3` embedding（向量嵌入）。
- 当前端到端上传样例 PDF 时，后端可返回整体结论、规则 findings、页面 annotations、5 页 PDF snapshot（快照）和 RAG 证据。
- Java 继续保留 API 编排、PDF 解析、规则结果聚合等职责。

## 非目标范围

Demo 阶段不做以下事项：

- 不做正式官方审批自动化。
- 不声称具备法律或审批确定性。
- 不默认建设完整用户权限体系。
- 不默认持久化上传的个人资料 PDF。
- 不在 `ID995A` 稳定前扩展到全部 APPLIES-2 表格。

## 协作原则

- 优先保持模块边界清晰。
- 优先使用官方资料作为规则来源。
- 规则判断必须可解释、可追溯。
- 关键判断应由确定性逻辑和官方规则证据支撑。
- LLM 只做表达增强，不做不可追溯的最终裁决。
- 对用户提供的密钥、PDF 和个人资料保持最小暴露。
