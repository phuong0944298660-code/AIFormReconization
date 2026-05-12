package com.aiform.id995a.ocr;

import com.aiform.id995a.review.EngineStatus;
import com.aiform.id995a.review.ExtractedField;
import com.aiform.id995a.review.FieldEvidence;
import com.aiform.id995a.review.PageSnapshot;
import com.aiform.id995a.template.Id995aTemplateRegistry;
import com.aiform.id995a.template.TemplateField;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PdfSignalExtractor {

  private final Id995aTemplateRegistry templateRegistry;
  private final OptionalTess4jOcrService ocrService;
  private final Path templatePath;

  public PdfSignalExtractor(
      Id995aTemplateRegistry templateRegistry,
      OptionalTess4jOcrService ocrService,
      @Value("${id995a.template-path:../official_templates/students/ID995A_来港就读申请表_申请人.pdf}") String templatePath
  ) {
    this.templateRegistry = templateRegistry;
    this.ocrService = ocrService;
    this.templatePath = Path.of(templatePath).toAbsolutePath().normalize();
  }

  public PdfAnalysis analyze(byte[] pdfBytes) throws IOException {
    List<String> messages = new ArrayList<>();
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      List<BufferedImage> renderedPages = renderPages(document, 80);
      List<BufferedImage> templatePages = loadTemplatePages(document.getNumberOfPages(), messages);
      String pdfText = extractText(document, document.getNumberOfPages());
      String acroText = extractAcroText(document);
      OptionalTess4jOcrService.OcrResult ocrResult = ocrService.recognize(renderedPages);
      messages.addAll(ocrResult.messages());

      Map<String, Boolean> present = new HashMap<>();
      Map<String, Double> confidence = new HashMap<>();
      List<ExtractedField> extractedFields = new ArrayList<>();
      String searchableText = (pdfText + "\n" + acroText + "\n" + ocrResult.text()).toLowerCase();

      for (TemplateField field : templateRegistry.fields()) {
        Signal signal = detectField(field, renderedPages, templatePages, searchableText);
        present.put(field.key(), signal.present());
        confidence.put(field.key(), signal.confidence());
        extractedFields.add(new ExtractedField(field.key(), field.label(), field.page(), signal.present(), signal.confidence()));
      }

      List<PageSnapshot> pageSnapshots = new ArrayList<>();
      for (int index = 0; index < renderedPages.size(); index += 1) {
        pageSnapshots.add(new PageSnapshot(index + 1, toDataUrl(renderedPages.get(index))));
      }
      String snapshot = "";
      EngineStatus status = new EngineStatus(
          ocrResult.attempted() ? "PDFBox + Tess4J + template-diff" : "PDFBox + template-diff",
          ocrService.enabled(),
          messages
      );

      String combinedText = pdfText + "\n" + acroText + "\n" + ocrResult.text();
      String extractedAge = extractApplicantAge(combinedText);
      String extractedSponsorType = extractSponsorType(combinedText);

      return new PdfAnalysis(
          new FieldEvidence(Map.copyOf(present), Map.copyOf(confidence), trimPreview(pdfText + "\n" + ocrResult.text(), 5000)),
          List.copyOf(extractedFields),
          List.copyOf(pageSnapshots),
          snapshot,
          status,
          extractedAge,
          extractedSponsorType
      );
    }
  }

  private List<BufferedImage> renderPages(PDDocument document, int dpi) throws IOException {
    PDFRenderer renderer = new PDFRenderer(document);
    int pageCount = document.getNumberOfPages();
    List<BufferedImage> pages = new ArrayList<>();
    for (int index = 0; index < pageCount; index += 1) {
      pages.add(renderer.renderImageWithDPI(index, dpi, ImageType.RGB));
    }
    return pages;
  }

  private List<BufferedImage> loadTemplatePages(int uploadedPageCount, List<String> messages) {
    if (!Files.exists(templatePath)) {
      messages.add("未找到本地 ID995A 空白模板，模板差异识别不可用：" + templatePath);
      return List.of();
    }

    try (PDDocument template = Loader.loadPDF(templatePath.toFile())) {
      return renderPages(template, 80).subList(0, Math.min(uploadedPageCount, template.getNumberOfPages()));
    } catch (IOException exception) {
      messages.add("读取本地 ID995A 空白模板失败：" + exception.getMessage());
      return List.of();
    }
  }

  private String extractText(PDDocument document, int maxPages) {
    try {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      stripper.setStartPage(1);
      stripper.setEndPage(Math.max(1, maxPages));
      return stripper.getText(document);
    } catch (IOException exception) {
      return "";
    }
  }

  private String extractAcroText(PDDocument document) {
    PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (PDField field : acroForm.getFieldTree()) {
      String value = field.getValueAsString();
      if (value != null && !value.isBlank()) {
        builder.append(field.getFullyQualifiedName()).append(':').append(value).append('\n');
      }
    }
    return builder.toString();
  }

  private Signal detectField(
      TemplateField field,
      List<BufferedImage> renderedPages,
      List<BufferedImage> templatePages,
      String searchableText
  ) {
    if (keywordPresent(field.key(), searchableText)) {
      return new Signal(true, 0.85);
    }

    int pageIndex = field.page() - 1;
    if (pageIndex < 0 || pageIndex >= renderedPages.size() || pageIndex >= templatePages.size()) {
      return new Signal(false, 0.0);
    }

    double ratio = changedPixelRatio(renderedPages.get(pageIndex), templatePages.get(pageIndex), field);
    boolean present = ratio >= field.diffThreshold();
    return new Signal(present, Math.min(0.98, ratio / Math.max(field.diffThreshold(), 0.001)));
  }

  private boolean keywordPresent(String key, String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    return switch (key) {
      case "previousShortTermStudyYes" -> text.contains("yes") && text.contains("short-term");
      case "previousShortTermStudyDetails" -> text.contains("short-term") && (text.contains("period") || text.contains("course"));
      default -> false;
    };
  }

  private double changedPixelRatio(BufferedImage target, BufferedImage baseline, TemplateField field) {
    int width = Math.min(target.getWidth(), baseline.getWidth());
    int height = Math.min(target.getHeight(), baseline.getHeight());
    int x0 = clamp((int) Math.round(field.x() * width), 0, width - 1);
    int y0 = clamp((int) Math.round(field.y() * height), 0, height - 1);
    int x1 = clamp((int) Math.round((field.x() + field.width()) * width), x0 + 1, width);
    int y1 = clamp((int) Math.round((field.y() + field.height()) * height), y0 + 1, height);

    int changed = 0;
    int sampled = 0;
    for (int y = y0; y < y1; y += 2) {
      for (int x = x0; x < x1; x += 2) {
        int delta = colorDelta(target.getRGB(x, y), baseline.getRGB(x, y));
        if (delta > 42) {
          changed += 1;
        }
        sampled += 1;
      }
    }

    if (sampled == 0) {
      return 0;
    }
    return (double) changed / sampled;
  }

  private int colorDelta(int left, int right) {
    int lr = (left >> 16) & 0xff;
    int lg = (left >> 8) & 0xff;
    int lb = left & 0xff;
    int rr = (right >> 16) & 0xff;
    int rg = (right >> 8) & 0xff;
    int rb = right & 0xff;
    return Math.abs(lr - rr) + Math.abs(lg - rg) + Math.abs(lb - rb);
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private String toDataUrl(BufferedImage image) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
  }

  private String trimPreview(String text, int maxLength) {
    if (text == null) {
      return "";
    }
    String normalized = text.replaceAll("[\\t ]+", " ").trim();
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, maxLength) + "...";
  }

  private static final Pattern DATE_PATTERN = Pattern.compile(
      "\\b(\\d{1,2})[\\s/.\\-年](\\d{1,2})[\\s/.\\-月](\\d{4})\\b"
      + "|\\b(\\d{4})[\\s/.\\-年](\\d{1,2})[\\s/.\\-月](\\d{1,2})\\b"
  );

  private String extractApplicantAge(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    String[] keywords = {
        "Date of Birth", "Date of birth", "DATE OF BIRTH",
        "出生日期", "DOB", "Birth Date"
    };
    for (String keyword : keywords) {
      int idx = text.indexOf(keyword);
      if (idx < 0) {
        continue;
      }
      String window = text.substring(idx, Math.min(text.length(), idx + 300));
      OptionalInt age = tryParseAge(window);
      if (age.isPresent()) {
        return String.valueOf(age.getAsInt());
      }
    }
    OptionalInt age = tryParseAge(text);
    return age.isPresent() ? String.valueOf(age.getAsInt()) : "";
  }

  private OptionalInt tryParseAge(String text) {
    Matcher matcher = DATE_PATTERN.matcher(text);
    LocalDate today = LocalDate.now();
    while (matcher.find()) {
      try {
        int year;
        int month;
        int day;
        if (matcher.group(1) != null) {
          day = Integer.parseInt(matcher.group(1));
          month = Integer.parseInt(matcher.group(2));
          year = Integer.parseInt(matcher.group(3));
        } else {
          year = Integer.parseInt(matcher.group(4));
          month = Integer.parseInt(matcher.group(5));
          day = Integer.parseInt(matcher.group(6));
        }
        if (year < 1900 || year > today.getYear()) {
          continue;
        }
        if (month < 1 || month > 12) {
          continue;
        }
        if (day < 1 || day > 31) {
          continue;
        }
        LocalDate birth = LocalDate.of(year, month, day);
        if (birth.isAfter(today)) {
          continue;
        }
        int age = Period.between(birth, today).getYears();
        if (age >= 0 && age <= 120) {
          return OptionalInt.of(age);
        }
      } catch (Exception ignored) {
        // skip and try next match
      }
    }
    return OptionalInt.empty();
  }

  private String extractSponsorType(String text) {
    if (text == null) {
      return "";
    }
    String lower = text.toLowerCase(Locale.ROOT);
    if (lower.contains("educational institution")
        || lower.contains("school as sponsor")
        || lower.contains("取录院校")
        || lower.contains("院校保证")
        || lower.contains("学校保证")) {
      return "institution";
    }
    if (lower.contains("individual sponsor")
        || lower.contains("personal sponsor")
        || lower.contains("个人保证")
        || lower.contains("亲友保证")) {
      return "individual";
    }
    return "";
  }

  private record Signal(boolean present, double confidence) {}
}
