package com.aiform.id995a.ocr;

import java.util.List;

public record OcrPage(
    int page,
    String sourceImageDataUrl,
    int imageWidth,
    int imageHeight,
    String markdown,
    List<OcrTextLine> lines,
    List<OcrTextBlock> blocks,
    List<OcrCheckbox> checkboxes
) {}
