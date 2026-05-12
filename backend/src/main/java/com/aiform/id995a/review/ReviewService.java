package com.aiform.id995a.review;

import com.aiform.id995a.ocr.PdfAnalysis;
import com.aiform.id995a.llm.LlmConclusion;
import com.aiform.id995a.llm.LlmConclusionService;
import com.aiform.id995a.ocr.PdfSignalExtractor;
import com.aiform.id995a.rag.RagServiceClient;
import com.aiform.id995a.rag.RetrievalResult;
import com.aiform.id995a.rules.Id995aRuleEngine;
import com.aiform.id995a.rules.RuleFinding;
import com.aiform.id995a.rules.RuleReview;
import com.aiform.id995a.template.Id995aTemplateRegistry;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

  private final PdfSignalExtractor pdfSignalExtractor;
  private final Id995aRuleEngine ruleEngine;
  private final Id995aTemplateRegistry templateRegistry;
  private final LlmConclusionService llmConclusionService;
  private final RagServiceClient ragServiceClient;

  public ReviewService(
      PdfSignalExtractor pdfSignalExtractor,
      Id995aRuleEngine ruleEngine,
      Id995aTemplateRegistry templateRegistry,
      LlmConclusionService llmConclusionService,
      RagServiceClient ragServiceClient
  ) {
    this.pdfSignalExtractor = pdfSignalExtractor;
    this.ruleEngine = ruleEngine;
    this.templateRegistry = templateRegistry;
    this.llmConclusionService = llmConclusionService;
    this.ragServiceClient = ragServiceClient;
  }

  public ReviewResponse review(String filename, byte[] pdfBytes, Map<String, String> metadata) throws IOException {
    PdfAnalysis analysis = pdfSignalExtractor.analyze(pdfBytes);
    Map<String, String> enrichedMetadata = new HashMap<>(metadata);
    if (analysis.extractedApplicantAge() != null && !analysis.extractedApplicantAge().isBlank()) {
      enrichedMetadata.put("applicantAge", analysis.extractedApplicantAge());
    }
    if (analysis.extractedSponsorType() != null && !analysis.extractedSponsorType().isBlank()) {
      enrichedMetadata.put("sponsorType", analysis.extractedSponsorType());
    }
    FormReviewInput input = new FormReviewInput(
        enrichedMetadata,
        analysis.fieldEvidence(),
        AttachmentEvidence.fromMetadata(enrichedMetadata)
    );
    RuleReview review = ruleEngine.review(input);
    RetrievalResult retrievalResult = ragServiceClient.retrieve(buildRetrievalQuery(analysis, input, review));
    LlmConclusion llmConclusion = llmConclusionService.generate(review, filename, retrievalResult.evidence());
    List<DocumentAnnotation> annotations = review.findings().stream()
        .map(this::toAnnotation)
        .flatMap(java.util.Optional::stream)
        .toList();

    return new ReviewResponse(
        review.passed(),
        review.verdict(),
        review.blockingReasons(),
        review.findings(),
        annotations,
        analysis.extractedFields(),
        analysis.pageSnapshots(),
        analysis.snapshotDataUrl(),
        analysis.engineStatus(),
        llmConclusion,
        retrievalResult.evidence(),
        retrievalResult.status(),
        sources(),
        analysis.fieldEvidence().extractedText()
    );
  }

  private String buildRetrievalQuery(PdfAnalysis analysis, FormReviewInput input, RuleReview review) {
    StringBuilder query = new StringBuilder();
    query.append(analysis.fieldEvidence().extractedText()).append('\n');
    input.metadata().forEach((key, value) -> query.append(key).append('=').append(value).append('\n'));
    review.findings().forEach(finding -> query.append(finding.ruleId())
        .append(' ')
        .append(finding.title())
        .append(' ')
        .append(finding.message())
        .append('\n'));
    return query.toString();
  }

  private java.util.Optional<DocumentAnnotation> toAnnotation(RuleFinding finding) {
    return templateRegistry.find(finding.fieldKey()).map(field -> new DocumentAnnotation(
        finding.ruleId(),
        field.page(),
        field.x(),
        field.y(),
        field.width(),
        field.height(),
        field.label(),
        finding.message(),
        finding.severity()
    ));
  }

  private List<SourceReference> sources() {
    return List.of(
        new SourceReference("ID995A 表格页", "https://www.immd.gov.hk/hks/forms/forms/id995a.html"),
        new SourceReference("学生签证服务页", "https://www.immd.gov.hk/hks/services/visas/study.html"),
        new SourceReference("ID995A PDF", "https://www.immd.gov.hk/pdforms/ID995A.pdf"),
        new SourceReference("ID(E)996 指南 PDF", "https://www.immd.gov.hk/pdforms/ID%28E%29996.pdf")
    );
  }
}
