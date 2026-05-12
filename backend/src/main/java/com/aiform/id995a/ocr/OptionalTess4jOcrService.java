package com.aiform.id995a.ocr;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OptionalTess4jOcrService {

  private final boolean enabled;
  private final String language;
  private final String dataPath;

  public OptionalTess4jOcrService(
      @Value("${ocr.tesseract.enabled:false}") boolean enabled,
      @Value("${ocr.tesseract.language:eng+chi_tra}") String language,
      @Value("${ocr.tesseract.data-path:}") String dataPath
  ) {
    this.enabled = enabled;
    this.language = language;
    this.dataPath = dataPath;
  }

  public OcrResult recognize(List<BufferedImage> pages) {
    List<String> messages = new ArrayList<>();
    if (!enabled) {
      messages.add("Tess4J/Tesseract OCR 未启用；当前使用 PDF 文本层与模板图像差异识别。可在 application.yml 开启 ocr.tesseract.enabled。");
      return new OcrResult("", false, messages);
    }

    try {
      ITesseract tesseract = new Tesseract();
      tesseract.setLanguage(language);
      if (dataPath != null && !dataPath.isBlank()) {
        tesseract.setDatapath(dataPath);
      }

      StringBuilder text = new StringBuilder();
      for (int index = 0; index < pages.size(); index += 1) {
        text.append(tesseract.doOCR(pages.get(index))).append('\n');
      }
      messages.add("Tess4J/Tesseract OCR 已执行，语言：" + language + "，共识别 " + pages.size() + " 页");
      return new OcrResult(text.toString(), true, messages);
    } catch (Throwable throwable) {
      messages.add("Tess4J/Tesseract OCR 调用失败：" + throwable.getMessage());
      messages.add("已回退到 PDF 文本层与模板图像差异识别。");
      return new OcrResult("", true, messages);
    }
  }

  public boolean enabled() {
    return enabled;
  }

  public record OcrResult(String text, boolean attempted, List<String> messages) {}
}
