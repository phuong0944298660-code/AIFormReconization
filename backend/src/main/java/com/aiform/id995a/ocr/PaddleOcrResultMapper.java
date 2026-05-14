package com.aiform.id995a.ocr;

import com.aiform.id995a.review.EngineStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaddleOcrResultMapper {

  private final ObjectMapper objectMapper;
  private final OcrCheckboxDetector checkboxDetector;
  private final Id988aTemplateExtractor id988aTemplateExtractor;

  public PaddleOcrResultMapper(
      ObjectMapper objectMapper,
      OcrCheckboxDetector checkboxDetector,
      Id988aTemplateExtractor id988aTemplateExtractor
  ) {
    this.objectMapper = objectMapper;
    this.checkboxDetector = checkboxDetector;
    this.id988aTemplateExtractor = id988aTemplateExtractor;
  }

  public OcrDemoResponse toResponse(String filename, String rawJson) throws IOException {
    JsonNode root = objectMapper.readTree(rawJson);
    JsonNode results = root.path("result").path("layoutParsingResults");
    if (!results.isArray()) {
      throw new IOException("PaddleOCR response missing result.layoutParsingResults");
    }

    List<OcrPage> pages = new ArrayList<>();
    for (int index = 0; index < results.size(); index += 1) {
      JsonNode pageNode = results.get(index);
      String markdown = pageNode.path("markdown").path("text").asText("");
      List<OcrTextBlock> blocks = extractBlocks(pageNode.path("prunedResult").path("parsing_res_list"));
      String imageDataUrl = toImageDataUrl(pageNode.path("inputImage").asText(""));
      OcrCheckboxDetector.DetectionResult checkboxResult = checkboxDetector.detectPage(imageDataUrl, blocks);
      List<OcrTextLine> lines = OcrCheckboxSemanticLines.merge(
          OcrTextHighlighter.toLines(markdown),
          checkboxResult.checkboxes(),
          blocks,
          checkboxResult.imageWidth(),
          checkboxResult.imageHeight()
      );
      pages.add(new OcrPage(
          index + 1,
          imageDataUrl,
          checkboxResult.imageWidth(),
          checkboxResult.imageHeight(),
          markdown,
          lines,
          blocks,
          checkboxResult.checkboxes()
      ));
    }
    List<OcrPage> immutablePages = List.copyOf(pages);
    List<Id988aFieldExtraction> extractedFields = id988aTemplateExtractor.extract(immutablePages);

    EngineStatus status = new EngineStatus(
        "PaddleOCR-VL layout-parsing",
        false,
        List.of("PaddleOCR-VL returned " + pages.size() + " page(s).")
    );
    return new OcrDemoResponse(filename, "PaddleOCR-VL", pages.size(), immutablePages, extractedFields, status);
  }

  private List<OcrTextBlock> extractBlocks(JsonNode blocksNode) {
    if (!blocksNode.isArray()) {
      return List.of();
    }
    List<OcrTextBlock> blocks = new ArrayList<>();
    for (JsonNode blockNode : blocksNode) {
      String label = blockNode.path("block_label").asText("text");
      String content = blockNode.path("block_content").asText("");
      blocks.add(new OcrTextBlock(
          label,
          content,
          extractBbox(blockNode.path("block_bbox")),
          OcrTextHighlighter.hasLikelyUserInput(content)
      ));
    }
    return List.copyOf(blocks);
  }

  private List<Integer> extractBbox(JsonNode bboxNode) {
    if (bboxNode.isArray()) {
      List<Integer> bbox = new ArrayList<>();
      for (JsonNode item : bboxNode) {
        bbox.add(item.asInt());
      }
      return List.copyOf(bbox);
    }
    if (bboxNode.isTextual()) {
      String[] parts = bboxNode.asText().trim().split("\\s+");
      List<Integer> bbox = new ArrayList<>();
      for (String part : parts) {
        try {
          bbox.add(Integer.parseInt(part));
        } catch (NumberFormatException ignored) {
          return List.of();
        }
      }
      return List.copyOf(bbox);
    }
    return List.of();
  }

  private String toImageDataUrl(String imageValue) {
    if (imageValue == null || imageValue.isBlank()) {
      return "";
    }
    if (imageValue.startsWith("data:") || imageValue.startsWith("http://") || imageValue.startsWith("https://")) {
      return imageValue;
    }
    return "data:" + inferImageMimeType(imageValue) + ";base64," + imageValue;
  }

  private String inferImageMimeType(String base64) {
    if (base64.startsWith("/9j/")) {
      return "image/jpeg";
    }
    if (base64.startsWith("iVBOR")) {
      return "image/png";
    }
    if (base64.startsWith("R0lG")) {
      return "image/gif";
    }
    return "image/png";
  }
}
