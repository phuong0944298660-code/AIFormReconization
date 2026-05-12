package com.aiform.id995a.ocr;

import com.aiform.id995a.review.EngineStatus;
import com.aiform.id995a.review.ExtractedField;
import com.aiform.id995a.review.FieldEvidence;
import com.aiform.id995a.review.PageSnapshot;
import java.util.List;

public record PdfAnalysis(
    FieldEvidence fieldEvidence,
    List<ExtractedField> extractedFields,
    List<PageSnapshot> pageSnapshots,
    String snapshotDataUrl,
    EngineStatus engineStatus,
    String extractedApplicantAge,
    String extractedSponsorType
) {}
