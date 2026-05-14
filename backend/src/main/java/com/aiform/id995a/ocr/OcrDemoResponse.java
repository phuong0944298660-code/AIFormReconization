package com.aiform.id995a.ocr;

import com.aiform.id995a.review.EngineStatus;
import java.util.List;

public record OcrDemoResponse(
    String filename,
    String model,
    int pageCount,
    List<OcrPage> pages,
    List<Id988aFieldExtraction> extractedFields,
    EngineStatus engineStatus
) {}
