from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter


def base_rule_documents() -> list[Document]:
    docs = [
        rule_doc(
            "A1-001",
            "ID995A 个人资料必填",
            "申请人须在第 1 项填写英文姓、英文名、性别、出生日期、出生地点、国籍或原居地/定居地、联络电话和现时住址。缺少关键身份资料会导致表格不完整。",
            ["A1-001-SURNAME", "A1-001-GIVEN-NAMES", "A1-001-SEX", "A1-001-DOB", "A1-001-BIRTH-PLACE", "A1-001-NATIONALITY", "A1-001-CONTACT", "A1-001-PRESENT-ADDRESS"],
            ["个人资料", "英文姓", "英文名", "性别", "出生日期", "国籍", "电话", "住址", "identity", "personal"],
        ),
        rule_doc(
            "A1-002",
            "旅行证件资料和副本",
            "申请人须填写旅行证件类别、号码、签发日期和届满日期，并提交有效旅行证件副本。内地居民未获发旅行证件时，至少须提供内地身份证资料并补交身份证副本。",
            ["A1-002-TRAVEL-DOCUMENT", "DOC-001-TRAVEL-DOCUMENT-COPY"],
            ["旅行证件", "护照", "证件号码", "签发日期", "届满日期", "副本", "travel", "passport"],
        ),
        rule_doc(
            "A1-003",
            "近照要求",
            "ID995A 第 2 页须贴上申请人近照。照片大小应不大于 55×45 毫米且不小于 50×40 毫米。",
            ["A1-003-PHOTO"],
            ["照片", "近照", "photo", "photograph"],
        ),
        rule_doc(
            "A2-A4",
            "拟来港就读资料",
            "申请人须填写拟抵港日期、拟在港逗留时间、在港就读学校名称及地址、入读年级或修读课程，并应与取录信一致。",
            ["A2-001-ENTRY-DATE", "A2-001-DURATION", "A4-001-SCHOOL", "A4-001-COURSE"],
            ["抵港", "逗留", "学校", "课程", "年级", "取录信", "school", "course", "duration"],
        ),
        rule_doc(
            "A6-A7",
            "生活开支和经济能力",
            "申请人须填写预计在港生活开支及经济状况。学费、住宿、交通膳食、其他费用和总计应完整；经济证明应能覆盖在港学习和生活所需费用。",
            ["A6-001-LIVING-COST", "A7-001-FINANCIAL-SUPPORT", "DOC-004-APPLICANT-FINANCIAL-PROOF", "DOC-005-SPONSOR-FINANCIAL"],
            ["学费", "生活费", "住宿", "总计", "经济证明", "银行", "financial", "bank", "cost"],
        ),
        rule_doc(
            "A8-001",
            "过去 12 个月短期课程",
            "如申请人在过去 12 个月曾在香港修读指定短期课程，必须填写课程名称、学校及修读日期。只勾选曾修读但没有详情会导致资料不完整。",
            ["A8-001-SHORT-COURSE-DETAILS"],
            ["短期课程", "12个月", "课程名称", "修读日期", "short-term", "previous study"],
        ),
        rule_doc(
            "A9-001",
            "声明签署和日期",
            "申请人、父母或合法监护人声明必须签署并写明日期。16 岁以下申请人须由父母或合法监护人签署，OCR 只能确认签名存在，签署人身份需复核。",
            ["A9-001-SIGNATURE", "A9-001-DATE", "A9-001-UNDER-16-SIGNATORY"],
            ["声明", "签名", "签署日期", "监护人签署", "signature", "declaration"],
        ),
        rule_doc(
            "DOC-001",
            "基本随附材料",
            "所有申请通常须提交 ID995A、近照、有效旅行证件副本和拟就读院校发出的取录信。缺少取录信或旅行证件副本会影响递交完整性。",
            ["DOC-001-ACCEPTANCE-LETTER", "DOC-001-TRAVEL-DOCUMENT-COPY"],
            ["取录信", "旅行证件副本", "随附材料", "acceptance letter", "supporting document"],
        ),
        rule_doc(
            "DOC-003",
            "未成年人监护和住宿材料",
            "申请人不足 18 岁时，父母须授权保证人或在港亲友作为监护人，并提交双方签署的同意书；同时须提交在港住宿安排证明副本。",
            ["DOC-003-GUARDIAN-CONSENT", "DOC-003-ACCOMMODATION"],
            ["未成年人", "不足18岁", "监护", "同意书", "住宿证明", "guardian", "minor", "accommodation", "authorisation"],
        ),
        rule_doc(
            "DOC-005",
            "个人保证人材料",
            "个人保证人须提交 ID995B、身份证明、经济能力证明，以及愿意负担申请人生活费并提供住宿的承诺。",
            ["DOC-005-ID995B", "DOC-005-SPONSOR-ID", "DOC-005-SPONSOR-FINANCIAL"],
            ["个人保证人", "ID995B", "保证人身份证明", "保证人经济证明", "sponsor"],
        ),
        rule_doc(
            "DOC-006",
            "内地居民递交路径",
            "内地中国居民的来港就读申请须经由取录申请人的院校作为保证人向入境处递交，获批后还需办理往来港澳通行证及相关赴港签注。",
            ["DOC-006-MAINLAND-SUBMISSION"],
            ["内地居民", "中国居民", "取录院校递交", "往来港澳通行证", "mainland"],
        ),
        rule_doc(
            "DOC-008",
            "受养人材料",
            "如申请带同受养人，每名受养人须填妥 ID995A 乙部并提交照片、旅行证件副本和关系证明等材料。",
            ["DOC-008-DEPENDANTS"],
            ["受养人", "乙部", "关系证明", "dependant", "dependent"],
        ),
        rule_doc(
            "DOC-009",
            "非中英文文件译本",
            "如提交文件并非中文或英文，须附经认可人员核证的中文或英文译本。",
            ["DOC-009-TRANSLATION"],
            ["翻译", "译本", "非中文", "非英文", "translation"],
        ),
    ]
    splitter = RecursiveCharacterTextSplitter(chunk_size=700, chunk_overlap=80)
    return splitter.split_documents(docs)


def rule_doc(chunk_id: str, title: str, content: str, rule_ids: list[str], keywords: list[str]) -> Document:
    return Document(
        page_content=f"{title}\n{content}\n规则ID：{', '.join(rule_ids)}\n关键词：{', '.join(keywords)}",
        metadata={
            "chunk_id": chunk_id,
            "title": title,
            "content": content,
            "source": "香港入境事务处 ID995A / ID(E)996",
            "rule_ids": rule_ids,
            "keywords": keywords,
            "form": "ID995A",
            "scenario": "student-entry",
        },
    )
