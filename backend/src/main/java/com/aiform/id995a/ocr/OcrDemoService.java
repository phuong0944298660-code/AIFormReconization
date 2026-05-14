package com.aiform.id995a.ocr;

import java.io.IOException;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class OcrDemoService {

  private final PaddleOcrGateway paddleOcrGateway;
  private final PaddleOcrResultMapper resultMapper;

  public OcrDemoService(PaddleOcrGateway paddleOcrGateway, PaddleOcrResultMapper resultMapper) {
    this.paddleOcrGateway = paddleOcrGateway;
    this.resultMapper = resultMapper;
  }

  public OcrDemoResponse recognize(String filename, String contentType, byte[] fileBytes) throws IOException {
    String rawJson = paddleOcrGateway.analyze(fileBytes, detectFileType(filename, contentType));
    return resultMapper.toResponse(filename == null || filename.isBlank() ? "uploaded-document" : filename, rawJson);
  }

  private int detectFileType(String filename, String contentType) {
    String lowerContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    String lowerFilename = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
    if (lowerContentType.startsWith("image/")
        || lowerFilename.endsWith(".png")
        || lowerFilename.endsWith(".jpg")
        || lowerFilename.endsWith(".jpeg")
        || lowerFilename.endsWith(".webp")
        || lowerFilename.endsWith(".bmp")) {
      return 1;
    }
    return 0;
  }
}
